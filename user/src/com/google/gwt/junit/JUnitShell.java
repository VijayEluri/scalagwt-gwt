/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.junit;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.GWTShell;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.Properties;
import com.google.gwt.dev.cfg.Property;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.shell.CheckForUpdates;
import com.google.gwt.dev.util.arg.ArgHandlerLogLevel;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.junit.client.TimeoutException;
import com.google.gwt.junit.client.impl.GWTRunner;
import com.google.gwt.junit.client.impl.JUnitResult;
import com.google.gwt.junit.client.impl.JUnitHost.TestInfo;
import com.google.gwt.util.tools.ArgHandler;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerString;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import junit.framework.TestResult;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is responsible for hosting JUnit test case execution. There are
 * three main pieces to the JUnit system.
 * 
 * <ul>
 * <li>Test environment</li>
 * <li>Client classes</li>
 * <li>Server classes</li>
 * </ul>
 * 
 * <p>
 * The test environment consists of this class and the non-translatable version
 * of {@link com.google.gwt.junit.client.GWTTestCase}. These two classes
 * integrate directly into the real JUnit test process.
 * </p>
 * 
 * <p>
 * The client classes consist of the translatable version of
 * {@link com.google.gwt.junit.client.GWTTestCase}, translatable JUnit classes,
 * and the user's own {@link com.google.gwt.junit.client.GWTTestCase}-derived
 * class. The client communicates to the server via RPC.
 * </p>
 * 
 * <p>
 * The server consists of {@link com.google.gwt.junit.server.JUnitHostImpl}, an
 * RPC servlet which communicates back to the test environment through a
 * {@link JUnitMessageQueue}, thus closing the loop.
 * </p>
 */
@SuppressWarnings("deprecation")
public class JUnitShell extends GWTShell {

  /**
   * A strategy for running the test.
   */
  public interface Strategy {
    String getModuleInherit();

    String getSyntheticModuleExtension();

    void processResult(TestCase testCase, JUnitResult result);
  }

  class ArgProcessor extends GWTShell.ArgProcessor {

    public ArgProcessor() {
      super(options, true, true);

      // Override port to set auto by default.
      registerHandler(new ArgHandlerPort(options) {
        @Override
        public String[] getDefaultArgs() {
          return new String[] {"-port", "auto"};
        }
      });

      // Override log level to set WARN by default..
      registerHandler(new ArgHandlerLogLevel(options) {
        @Override
        protected Type getDefaultLogLevel() {
          return TreeLogger.WARN;
        }
      });

      registerHandler(new ArgHandlerFlag() {
        @Override
        public String getPurpose() {
          return "Causes your test to run in -noserver hosted mode (defaults to hosted mode)";
        }

        @Override
        public String getTag() {
          return "-noserver";
        }

        @Override
        public boolean setFlag() {
          runStyle = new RunStyleNoServerHosted(JUnitShell.this);
          numClients = 1;
          return true;
        }
      });

      registerHandler(new ArgHandlerFlag() {
        @Override
        public String getPurpose() {
          return "Causes your test to run in web (compiled) mode (defaults to hosted mode)";
        }

        @Override
        public String getTag() {
          return "-web";
        }

        @Override
        public boolean setFlag() {
          runStyle = new RunStyleLocalWeb(JUnitShell.this);
          numClients = 1;
          return true;
        }
      });

      registerHandler(new ArgHandlerString() {
        @Override
        public String getPurpose() {
          return "Runs web mode via RMI to a set of BrowserManagerServers; "
              + "e.g. rmi://localhost/ie6,rmi://localhost/firefox";
        }

        @Override
        public String getTag() {
          return "-remoteweb";
        }

        @Override
        public String[] getTagArgs() {
          return new String[] {"rmiUrl"};
        }

        @Override
        public boolean isUndocumented() {
          return true;
        }

        @Override
        public boolean setString(String str) {
          String[] urls = str.split(",");
          runStyle = RunStyleRemoteWeb.create(JUnitShell.this, urls);
          numClients = urls.length;
          return runStyle != null;
        }
      });

      registerHandler(new ArgHandlerString() {
        @Override
        public String getPurpose() {
          return "Runs web mode via HTTP to a set of Selenium servers; "
              + "e.g. localhost:4444/*firefox,remotehost:4444/*iexplore";
        }

        @Override
        public String getTag() {
          return "-selenium";
        }

        @Override
        public String[] getTagArgs() {
          return new String[] {"seleniumHost"};
        }

        @Override
        public boolean setString(String str) {
          String[] targets = str.split(",");
          numClients = targets.length;
          runStyle = RunStyleSelenium.create(JUnitShell.this, targets);
          return runStyle != null;
        }
      });

      registerHandler(new ArgHandlerString() {
        @Override
        public String getPurpose() {
          return "Runs web mode via HTMLUnit given a list of browsers; "
              + "e.g. " + RunStyleHtmlUnit.getBrowserList();
        }

        @Override
        public String getTag() {
          return "-htmlunit";
        }

        @Override
        public String[] getTagArgs() {
          return new String[] {"browserNames"};
        }

        @Override
        public boolean setString(String str) {
          String[] targets = str.split(",");
          try {
            runStyle = new RunStyleHtmlUnit(JUnitShell.this, targets);
            numClients = ((RunStyleHtmlUnit) runStyle).numBrowsers();
            return true;
          } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            return false;
          }
        }
      });

      registerHandler(new ArgHandlerString() {
        @Override
        public String getPurpose() {
          return "Run external browsers in web mode (pass a comma separated list of executables.)";
        }

        @Override
        public String getTag() {
          return "-externalbrowser";
        }

        @Override
        public String[] getTagArgs() {
          return new String[] {"browserPaths"};
        }

        @Override
        public boolean isUndocumented() {
          return true;
        }

        @Override
        public boolean setString(String str) {
          String[] paths = str.split(",");
          runStyle = new RunStyleExternalBrowser(JUnitShell.this, paths);
          numClients = paths.length;
          return runStyle != null;
        }
      });

      // TODO: currently, only two values but soon may have multiple values.
      registerHandler(new ArgHandlerString() {
        @Override
        public String getPurpose() {
          return "Configure batch execution of tests";
        }

        @Override
        public String getTag() {
          return "-batch";
        }

        @Override
        public String[] getTagArgs() {
          return new String[] {"module"};
        }

        @Override
        public boolean isUndocumented() {
          return true;
        }

        @Override
        public boolean setString(String str) {
          if (str.equals("module")) {
            batchingStrategy = new ModuleBatchingStrategy();
          }
          return true;
        }
      });

      registerHandler(new ArgHandler() {
        @Override
        public String[] getDefaultArgs() {
          return null;
        }

        @Override
        public String getPurpose() {
          return "Causes the system to wait for a remote browser to connect";
        }

        @Override
        public String getTag() {
          return "-manual";
        }

        @Override
        public String[] getTagArgs() {
          return new String[] {"[numClients]"};
        }

        @Override
        public int handle(String[] args, int tagIndex) {
          int value = 1;
          if (tagIndex + 1 < args.length) {
            try {
              // See if the next item is an integer.
              value = Integer.parseInt(args[tagIndex + 1]);
              if (value >= 1) {
                setInt(value);
                return 1;
              }
            } catch (NumberFormatException e) {
              // fall-through
            }
          }
          setInt(1);
          return 0;
        }

        public void setInt(int value) {
          runStyle = new RunStyleManual(JUnitShell.this, value);
          numClients = value;
        }
      });

      registerHandler(new ArgHandlerFlag() {
        @Override
        public String getPurpose() {
          return "Causes the log window and browser windows to be displayed; useful for debugging";
        }

        @Override
        public String getTag() {
          return "-notHeadless";
        }

        @Override
        public boolean setFlag() {
          setHeadless(false);
          return true;
        }
      });
    }
  }

  private static class JUnitStrategy implements Strategy {
    public String getModuleInherit() {
      return "com.google.gwt.junit.JUnit";
    }

    public String getSyntheticModuleExtension() {
      return "JUnit";
    }

    public void processResult(TestCase testCase, JUnitResult result) {
    }
  }

  /**
   * This is a system property that, when set, emulates command line arguments.
   */
  private static final String PROP_GWT_ARGS = "gwt.args";

  /**
   * This legacy system property, when set, causes us to run in web mode.
   * (Superceded by passing "-web" into gwt.args).
   */
  private static final String PROP_JUNIT_HYBRID_MODE = "gwt.hybrid";

  /**
   * The amount of time to wait for all clients to have contacted the server and
   * begin running the test. "Contacted" does not necessarily mean "the test has
   * begun," e.g. for linker errors stopping the test initialization.
   */
  private static final int TEST_BEGIN_TIMEOUT_MILLIS = 60000;

  /**
   * The amount of time to wait for all clients to complete a single test
   * method, in milliseconds, measured from when the <i>last</i> client connects
   * (and thus starts the test). 20 minutes.
   */
  private static final long TEST_METHOD_TIMEOUT_MILLIS = 4 * 300000;

  /**
   * Singleton object for hosting unit tests. All test case instances executed
   * by the TestRunner will use the single unitTestShell.
   */
  private static JUnitShell unitTestShell;

  /**
   * Called by {@link com.google.gwt.junit.server.JUnitHostImpl} to get an
   * interface into the test process.
   * 
   * @return The {@link JUnitMessageQueue} interface that belongs to the
   *         singleton {@link JUnitShell}, or <code>null</code> if no such
   *         singleton exists.
   */
  public static JUnitMessageQueue getMessageQueue() {
    if (unitTestShell == null) {
      return null;
    }
    return unitTestShell.messageQueue;
  }

  /**
   * Entry point for {@link com.google.gwt.junit.client.GWTTestCase}. Gets or
   * creates the singleton {@link JUnitShell} and invokes its
   * {@link #runTestImpl(String, TestCase, TestResult, Strategy)}.
   */
  public static void runTest(String moduleName, TestCase testCase,
      TestResult testResult) throws UnableToCompleteException {
    getUnitTestShell().runTestImpl(moduleName, testCase, testResult,
        new JUnitStrategy());
  }

  public static void runTest(String moduleName, TestCase testCase,
      TestResult testResult, Strategy strategy)
      throws UnableToCompleteException {
    getUnitTestShell().runTestImpl(moduleName, testCase, testResult, strategy);
  }

  /**
   * Sanity check; if the type we're trying to run did not actually wind up in
   * the type oracle, there's no way this test can possibly run. Bail early
   * instead of failing on the client.
   */
  private static JUnitFatalLaunchException checkTestClassInCurrentModule(
      TreeLogger logger, ModuleDef currentModule, String moduleName,
      TestCase testCase) throws UnableToCompleteException {
    TypeOracle typeOracle = currentModule.getTypeOracle(logger);
    String typeName = testCase.getClass().getName();
    typeName = typeName.replace('$', '.');
    JClassType foundType = typeOracle.findType(typeName);
    if (foundType != null) {
      return null;
    }
    Map<String, CompilationUnit> unitMap = currentModule.getCompilationState(
        logger).getCompilationUnitMap();
    CompilationUnit unit = unitMap.get(typeName);
    String errMsg;
    if (unit == null) {
      errMsg = "The test class '" + typeName + "' was not found in module '"
          + moduleName + "'; no compilation unit for that type was seen";
    } else if (unit.isError()) {
      errMsg = "The test class '" + typeName
          + "' had compile errors; check log for details";
    } else if (!unit.isCompiled()) {
      errMsg = "The test class '"
          + typeName
          + "' depends on a unit that had compile errors; check log for details";
    } else {
      errMsg = "Unexpected error: the test class '"
          + typeName
          + "' appears to be valid, but no corresponding type was found in TypeOracle; please contact GWT support";
    }
    return new JUnitFatalLaunchException(errMsg);
  }

  /**
   * Retrieves the JUnitShell. This should only be invoked during TestRunner
   * execution of JUnit tests.
   */
  private static JUnitShell getUnitTestShell() {
    if (unitTestShell == null) {
      unitTestShell = new JUnitShell();
      unitTestShell.lastLaunchFailed = true;
      String[] args = unitTestShell.synthesizeArgs();
      ArgProcessor argProcessor = unitTestShell.new ArgProcessor();
      if (!argProcessor.processArgs(args)) {
        throw new JUnitFatalLaunchException("Error processing shell arguments");
      }

      unitTestShell.messageQueue = new JUnitMessageQueue(
          unitTestShell.numClients);

      if (!unitTestShell.startUp()) {
        throw new JUnitFatalLaunchException("Shell failed to start");
      }
      // TODO: install a shutdown hook? Not necessary with GWTShell.
      unitTestShell.lastLaunchFailed = false;
    }

    return unitTestShell;
  }

  /**
   * Determines how to batch up tests for execution.
   */
  private BatchingStrategy batchingStrategy = new NoBatchingStrategy();

  /**
   * When headless, all logging goes to the console.
   */
  private PrintWriterTreeLogger consoleLogger;

  /**
   * Name of the module containing the current/last module to run.
   */
  private ModuleDef currentModule;

  /**
   * If true, no launches have yet been successful.
   */
  private boolean firstLaunch = true;

  /**
   * If true, the last attempt to launch failed.
   */
  private boolean lastLaunchFailed;

  /**
   * We need to keep a hard reference to the last module that was launched until
   * all client browsers have successfully transitioned to the current module.
   * Failure to do so allows the last module to be GC'd, which transitively
   * kills the {@link com.google.gwt.junit.server.JUnitHostImpl} servlet. If the
   * servlet dies, the client browsers will be unable to transition.
   */
  @SuppressWarnings("unused")
  private ModuleDef lastModule;

  /**
   * Portal to interact with the servlet.
   */
  private JUnitMessageQueue messageQueue;

  /**
   * The number of test clients executing in parallel. With -remoteweb, users
   * can specify a number of parallel test clients, but by default we only have
   * 1.
   */
  private int numClients = 1;

  /**
   * What type of test we're running; Local hosted, local web, or remote web.
   */
  private RunStyle runStyle = new RunStyleLocalHosted(this);

  /**
   * The time the test actually began.
   */
  private long testBeginTime;

  /**
   * The time at which the current test will fail if the client has not yet
   * started the test.
   */
  private long testBeginTimeout;

  /**
   * Timeout for individual test method. If System.currentTimeMillis() is later
   * than this timestamp, then we need to pack up and go home. Zero for "not yet
   * set" (at the start of a test). This interval begins when the
   * testBeginTimeout interval is done.
   */
  private long testMethodTimeout;

  private Map<TestInfo, Map<String, JUnitResult>> cachedResults = new HashMap<TestInfo, Map<String, JUnitResult>>();

  /**
   * Enforce the singleton pattern. The call to {@link GWTShell}'s ctor forces
   * server mode and disables processing extra arguments as URLs to be shown.
   */
  private JUnitShell() {
    setRunTomcat(true);
    setHeadless(true);

    // Legacy: -Dgwt.hybrid runs web mode
    if (System.getProperty(PROP_JUNIT_HYBRID_MODE) != null) {
      runStyle = new RunStyleLocalWeb(this);
    }
  }

  @Override
  public TreeLogger getTopLogger() {
    if (consoleLogger != null) {
      return consoleLogger;
    } else {
      return super.getTopLogger();
    }
  }

  /**
   * Check for updates once a minute.
   */
  @Override
  protected long checkForUpdatesInterval() {
    return CheckForUpdates.ONE_MINUTE;
  }

  @Override
  protected void initializeLogger() {
    if (isHeadless()) {
      consoleLogger = new PrintWriterTreeLogger();
      consoleLogger.setMaxDetail(getCompilerOptions().getLogLevel());
    } else {
      super.initializeLogger();
    }
  }

  /**
   * Overrides {@link GWTShell#notDone()} to wait for the currently-running test
   * to complete.
   */
  @Override
  protected boolean notDone() {
    int activeClients = messageQueue.getNumClientsRetrievedCurrentTest();
    if (firstLaunch && runStyle instanceof RunStyleManual) {
      String[] newClients = messageQueue.getNewClients();
      int printIndex = activeClients - newClients.length + 1;
      for (String newClient : newClients) {
        System.out.println(printIndex + " - " + newClient);
        ++printIndex;
      }
      if (activeClients == this.numClients) {
        System.out.println("Starting tests");
      } else {
        // Wait forever for first contact; user-driven.
        return true;
      }
    }

    long currentTimeMillis = System.currentTimeMillis();
    if (activeClients == numClients) {
      firstLaunch = false;
      /*
       * It's now safe to release any reference to the last module since all
       * clients have transitioned to the current module.
       */
      lastModule = currentModule;
      if (testMethodTimeout == 0) {
        testMethodTimeout = currentTimeMillis + TEST_METHOD_TIMEOUT_MILLIS;
      } else if (testMethodTimeout < currentTimeMillis) {
        double elapsed = (currentTimeMillis - testBeginTime) / 1000.0;
        throw new TimeoutException(
            "The browser did not complete the test method "
                + messageQueue.getCurrentTestName() + " in "
                + TEST_METHOD_TIMEOUT_MILLIS
                + "ms.\n  We have no results from: "
                + messageQueue.getWorkingClients() + "\n Actual time elapsed: "
                + elapsed + " seconds.\n");
      }
    } else if (testBeginTimeout < currentTimeMillis) {
      double elapsed = (currentTimeMillis - testBeginTime) / 1000.0;
      throw new TimeoutException(
          "The browser did not contact the server within "
              + TEST_BEGIN_TIMEOUT_MILLIS + "ms.\n"
              + messageQueue.getUnretrievedClients()
              + "\n Actual time elapsed: " + elapsed + " seconds.\n");
    }

    if (runStyle.wasInterrupted()) {
      throw new TimeoutException("A remote browser died a mysterious death.");
    }

    return !messageQueue.hasResult();
  }

  @Override
  protected boolean shouldAutoGenerateResources() {
    return runStyle.shouldAutoGenerateResources();
  }

  @Override
  protected void sleep() {
    if (runStyle.isLocal()) {
      super.sleep();
    } else {
      messageQueue.waitForResults(1000);
    }
  }

  void compileForWebMode(String moduleName, String... userAgents)
      throws UnableToCompleteException {
    // Never fresh during JUnit.
    ModuleDef module = ModuleDefLoader.loadFromClassPath(getTopLogger(),
        moduleName, false);
    if (userAgents != null && userAgents.length > 0) {
      Properties props = module.getProperties();
      Property userAgent = props.find("user.agent");
      if (userAgent instanceof BindingProperty) {
        BindingProperty bindingProperty = (BindingProperty) userAgent;
        bindingProperty.setAllowedValues(bindingProperty.getRootCondition(),
            userAgents);
      }
    }
    super.compile(getTopLogger(), module);
  }

  /**
   * returns the set of banned {@code Platform} for a test method.
   */
  private Set<Platform> getBannedPlatforms(TestCase testCase) {
    Class<?> testClass = testCase.getClass();
    Set<Platform> bannedSet = EnumSet.noneOf(Platform.class);
    if (testClass.isAnnotationPresent(DoNotRunWith.class)) {
      bannedSet.addAll(Arrays.asList(testClass.getAnnotation(DoNotRunWith.class).value()));
    }
    try {
      Method testMethod = testClass.getMethod(testCase.getName());
      if (testMethod.isAnnotationPresent(DoNotRunWith.class)) {
        bannedSet.addAll(Arrays.asList(testMethod.getAnnotation(
            DoNotRunWith.class).value()));
      }
    } catch (SecurityException e) {
      // should not happen
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      // should not happen
      e.printStackTrace();
    }
    return bannedSet;
  }

  private boolean mostNotExecuteTest(Set<Platform> bannedPlatforms) {
    // TODO (amitmanjhi): Remove this hard-coding. A RunStyle somehow needs to
    // specify how it interacts with the platforms.
    return runStyle instanceof RunStyleHtmlUnit
        && bannedPlatforms.contains(Platform.Htmlunit);
  }

  /**
   * Checks if a testCase should not be executed. Currently, a test is either
   * executed on all clients (mentioned in this test) or on no clients.
   * 
   * @param testCase current testCase.
   * @return true iff the test should not be executed on any of the specified
   *         clients.
   */
  private boolean mustNotExecuteTest(TestCase testCase) {
    // TODO: collect stats on tests that were not run
    return mostNotExecuteTest(getBannedPlatforms(testCase));
  }

  private void processTestResult(TestInfo testInfo, TestCase testCase,
      TestResult testResult, Strategy strategy) {

    Map<String, JUnitResult> results = cachedResults.get(testInfo);
    assert results != null;

    boolean parallelTesting = numClients > 1;

    for (Entry<String, JUnitResult> entry : results.entrySet()) {
      String clientId = entry.getKey();
      JUnitResult result = entry.getValue();
      assert (result != null);
      Throwable exception = result.getException();
      // In the case that we're running multiple clients at once, we need to
      // let the user know the browser in which the failure happened
      if (parallelTesting && exception != null) {
        String msg = "Remote test failed at " + clientId;
        if (exception instanceof AssertionFailedError) {
          AssertionFailedError newException = new AssertionFailedError(msg
              + "\n" + exception.getMessage());
          newException.setStackTrace(exception.getStackTrace());
          newException.initCause(exception.getCause());
          exception = newException;
        } else {
          exception = new RuntimeException(msg, exception);
        }
      }

      // A "successful" failure.
      if (exception instanceof AssertionFailedError) {
        testResult.addFailure(testCase, (AssertionFailedError) exception);
      } else if (exception != null) {
        // A real failure
        if (exception instanceof JUnitFatalLaunchException) {
          lastLaunchFailed = true;
        }
        testResult.addError(testCase, exception);
      }

      strategy.processResult(testCase, result);
    }
  }

  /**
   * Runs a particular test case.
   */
  private void runTestImpl(String moduleName, TestCase testCase,
      TestResult testResult, Strategy strategy)
      throws UnableToCompleteException {

    if (mustNotExecuteTest(testCase)) {
      return;
    }

    if (lastLaunchFailed) {
      throw new UnableToCompleteException();
    }

    String syntheticModuleName = moduleName + "."
        + strategy.getSyntheticModuleExtension();
    boolean sameTest = (currentModule != null)
        && syntheticModuleName.equals(currentModule.getName());
    if (sameTest && lastLaunchFailed) {
      throw new UnableToCompleteException();
    }

    if (!sameTest) {
      /*
       * Synthesize a synthetic module that derives from the user-specified
       * module but also includes JUnit support.
       */
      currentModule = ModuleDefLoader.createSyntheticModule(getTopLogger(),
          syntheticModuleName, new String[] {
              moduleName, strategy.getModuleInherit()}, true);
      // Replace any user entry points with our test runner.
      currentModule.clearEntryPoints();
      currentModule.addEntryPointTypeName(GWTRunner.class.getName());
      // Squirrel away the name of the active module for GWTRunnerGenerator
      ConfigurationProperty moduleNameProp = currentModule.getProperties().createConfiguration(
          "junit.moduleName", false);
      moduleNameProp.setValue(moduleName);
      runStyle.maybeCompileModule(syntheticModuleName);
    }

    JUnitFatalLaunchException launchException = checkTestClassInCurrentModule(
        getTopLogger(), currentModule, moduleName, testCase);
    if (launchException != null) {
      testResult.addError(testCase, launchException);
      return;
    }

    TestInfo testInfo = new TestInfo(currentModule.getName(),
        testCase.getClass().getName(), testCase.getName());
    if (cachedResults.containsKey(testInfo)) {
      // Already have a result.
      processTestResult(testInfo, testCase, testResult, strategy);
      return;
    }

    /*
     * Need to process test. Set up synchronization.
     */
    TestInfo[] testBlock = batchingStrategy.getTestBlock(testInfo);
    messageQueue.setNextTestBlock(testBlock);

    try {
      if (firstLaunch) {
        runStyle.launchModule(currentModule.getName());
      }
    } catch (UnableToCompleteException e) {
      lastLaunchFailed = true;
      testResult.addError(testCase, new JUnitFatalLaunchException(e));
      return;
    }

    // Wait for test to complete
    try {
      // Set a timeout period to automatically fail if the servlet hasn't been
      // contacted; something probably went wrong (the module failed to load?)
      testBeginTime = System.currentTimeMillis();
      testBeginTimeout = testBeginTime + TEST_BEGIN_TIMEOUT_MILLIS;
      testMethodTimeout = 0; // wait until test execution begins
      pumpEventLoop();
    } catch (TimeoutException e) {
      lastLaunchFailed = true;
      testResult.addError(testCase, e);
      return;
    }

    assert (messageQueue.hasResult());
    cachedResults = messageQueue.getResults();
    assert cachedResults.containsKey(testInfo);
    processTestResult(testInfo, testCase, testResult, strategy);
  }

  /**
   * Synthesize command line arguments from a system property.
   */
  private String[] synthesizeArgs() {
    ArrayList<String> argList = new ArrayList<String>();

    String args = System.getProperty(PROP_GWT_ARGS);
    if (args != null) {
      // Match either a non-whitespace, non start of quoted string, or a
      // quoted string that can have embedded, escaped quoting characters
      //
      Pattern pattern = Pattern.compile("[^\\s\"]+|\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\"");
      Matcher matcher = pattern.matcher(args);
      Pattern quotedArgsPattern = Pattern.compile("^([\"'])(.*)([\"'])$");

      while (matcher.find()) {
        // Strip leading and trailing quotes from the arg
        String arg = matcher.group();
        Matcher qmatcher = quotedArgsPattern.matcher(arg);
        if (qmatcher.matches()) {
          argList.add(qmatcher.group(2));
        } else {
          argList.add(arg);
        }
      }
    }

    return argList.toArray(new String[argList.size()]);
  }
}
