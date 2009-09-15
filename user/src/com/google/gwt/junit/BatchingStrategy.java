/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.junit.client.impl.JUnitHost.TestInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * An interface that specifies how tests should be batched. A single batch
 * should never include tests from more than one module, or the browser will
 * load the new module and lose results from existing tests.
 */
public abstract class BatchingStrategy {

  /**
   * Returns an ordered list of all tests blocks that should be executed for the
   * specified module. Each test block is an array of {@link TestInfo}.
   * 
   * @param syntheticModuleName the name of the synthetic module
   * @return an ordered list of test blocks to run
   */
  public abstract List<TestInfo[]> getTestBlocks(String syntheticModuleName);
}

/**
 * 
 * Strategy that does not batch tests.
 */
class NoBatchingStrategy extends BatchingStrategy {
  @Override
  public List<TestInfo[]> getTestBlocks(String syntheticModuleName) {
    Set<TestInfo> allTestsInModule = GWTTestCase.getTestsForModule(
        syntheticModuleName).getTests();
    List<TestInfo[]> testBlocks = new ArrayList<TestInfo[]>();
    for (TestInfo testInfo : allTestsInModule) {
      testBlocks.add(new TestInfo[] {testInfo});
    }
    return testBlocks;
  }
}

/**
 * Strategy that batches all tests belonging to one class.
 */
class ClassBatchingStrategy extends BatchingStrategy {
  @Override
  public List<TestInfo[]> getTestBlocks(String syntheticModuleName) {
    Set<TestInfo> allTestsInModule = GWTTestCase.getTestsForModule(
        syntheticModuleName).getTests();
    List<TestInfo[]> testBlocks = new ArrayList<TestInfo[]>();
    String lastTestClass = null;
    List<TestInfo> lastTestBlock = null;
    for (TestInfo testInfo : allTestsInModule) {
      String testClass = testInfo.getTestClass();
      if (!testClass.equals(lastTestClass)) {
        // Add the last test block to the collection.
        if (lastTestBlock != null) {
          testBlocks.add(lastTestBlock.toArray(new TestInfo[lastTestBlock.size()]));
        }

        // Start a new test block.
        lastTestClass = testClass;
        lastTestBlock = new ArrayList<TestInfo>();
      }
      lastTestBlock.add(testInfo);
    }

    // Add the last test block.
    if (lastTestBlock != null) {
      testBlocks.add(lastTestBlock.toArray(new TestInfo[lastTestBlock.size()]));
    }
    return testBlocks;
  }
}

/**
 * Strategy that batches all tests belonging to one module.
 */
class ModuleBatchingStrategy extends BatchingStrategy {
  @Override
  public List<TestInfo[]> getTestBlocks(String syntheticModuleName) {
    Set<TestInfo> allTestsInModule = GWTTestCase.getTestsForModule(
        syntheticModuleName).getTests();
    TestInfo[] testBlock = allTestsInModule.toArray(new TestInfo[allTestsInModule.size()]);
    List<TestInfo[]> testBlocks = new ArrayList<TestInfo[]>();
    testBlocks.add(testBlock);
    return testBlocks;
  }
}
