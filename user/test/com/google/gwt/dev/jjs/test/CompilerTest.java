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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.junit.client.GWTTestCase;

import junit.framework.Assert;

/**
 * Miscellaneous tests of the Java to JavaScript compiler.
 */
@SuppressWarnings("unused")
public class CompilerTest extends GWTTestCase {

  private abstract static class Apple implements Fruit {
  }

  private static interface Fruit {
  }

  private static class Fuji extends Apple {
  }

  private static class Granny extends Apple {
  }

  private static class NonSideEffectCauser {
    public static final String NOT_A_COMPILE_TIME_CONSTANT = null;
  }

  private static class SideEffectCauser {
    private static Object instance = new Object();

    static {
      CompilerTest.sideEffectChecker++;
    }

    public static Object causeClinitSideEffect() {
      return instance;
    }
  }

  private static class SideEffectCauser2 {
    static {
      CompilerTest.sideEffectChecker++;
    }

    public static Object causeClinitSideEffect() {
      return null;
    }
  }

  private static class SideEffectCauser3 {
    static {
      CompilerTest.sideEffectChecker++;
    }

    public static void causeClinitSideEffect() {
    }
  }

  private static class SideEffectCauser4 {
    public static String causeClinitSideEffectOnRead = "foo";

    static {
      CompilerTest.sideEffectChecker++;
    }
  }

  private static class SideEffectCauser5 {
    public static String causeClinitSideEffectOnRead = "bar";

    static {
      CompilerTest.sideEffectChecker++;
    }
  }

  private static class SideEffectCauser6 extends SideEffectCauser6Super {
    public static String causeClinitSideEffectOnRead = "bar";
  }

  private static class SideEffectCauser6Super {
    static {
      CompilerTest.sideEffectChecker++;
    }
  }

  /**
   * Ensures that a superclass's clinit is run before supercall arguments are
   * evaluated.
   */
  private static class SideEffectCauser7 extends SideEffectCauser7Super {
    public SideEffectCauser7() {
      super(SideEffectCauser7Super.SHOULD_BE_TRUE);
    }
  }

  private static class SideEffectCauser7Super {
    public static boolean SHOULD_BE_TRUE = false;

    static {
      SHOULD_BE_TRUE = true;
    }

    public SideEffectCauser7Super(boolean should_be_true) {
      if (should_be_true) {
        CompilerTest.sideEffectChecker++;
      }
    }
  }

  private static final class UninstantiableType {
    public Object field;

    private UninstantiableType() {
    }

    public Object returnNull() {
      return null;
    }
  }

  private static volatile boolean FALSE = false;

  private static int sideEffectChecker;

  private static volatile boolean TRUE = true;

  private static native void accessUninstantiableField(UninstantiableType u) /*-{
    u.@com.google.gwt.dev.jjs.test.CompilerTest$UninstantiableType::field.toString();
  }-*/;

  private static native void accessUninstantiableMethod(UninstantiableType u) /*-{
    u.@com.google.gwt.dev.jjs.test.CompilerTest$UninstantiableType::returnNull()();
  }-*/;

  private static String barShouldInline() {
    return "bar";
  }

  private static void foo(String string) {
  }

  private static void foo(Throwable throwable) {
  }

  private static native String jsniReadSideEffectCauser5() /*-{
    return @com.google.gwt.dev.jjs.test.CompilerTest$SideEffectCauser5::causeClinitSideEffectOnRead;
  }-*/;

  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testArrayStore() {
    Object[][] oaa;
    oaa = new Object[4][4];
    oaa[0][0] = "foo";
    assertEquals(oaa[0][0], "foo");

    oaa = new Object[4][];
    oaa[0] = new Object[4];
    oaa[0][0] = "bar";
    assertEquals(oaa[0][0], "bar");

    Apple[] apple = TRUE ? new Granny[3] : new Apple[3];
    Apple g = TRUE ? (Apple) new Granny() : (Apple) new Fuji();
    Apple a = apple[0] = g;
    assertEquals(g, a);

    byte[] bytes = new byte[10];
    bytes[0] = (byte) '1';
    assertEquals(49, bytes[0]);
  }

  public void testCastOptimizer() {
    Granny g = new Granny();
    Apple a = g;
    Fruit f = g;
    a = (Apple) f;
    g = (Granny) a;
    g = (Granny) f;
  }

  public void testClassLiterals() {
    assertEquals("void", void.class.toString());
    assertEquals("int", int.class.toString());
    assertEquals("class java.lang.String", String.class.toString());
    assertEquals("class com.google.gwt.dev.jjs.test.CompilerTest",
        CompilerTest.class.toString());
    assertEquals(
        "class com.google.gwt.dev.jjs.test.CompilerTest$UninstantiableType",
        UninstantiableType.class.toString());
    assertEquals("interface com.google.gwt.dev.jjs.test.CompilerTest$Fruit",
        Fruit.class.toString());
    assertEquals("class [I", int[].class.toString());
    assertEquals("class [Ljava.lang.String;", String[].class.toString());
    assertEquals("class [Lcom.google.gwt.dev.jjs.test.CompilerTest;",
        CompilerTest[].class.toString());
    assertEquals(
        "class [Lcom.google.gwt.dev.jjs.test.CompilerTest$UninstantiableType;",
        UninstantiableType[].class.toString());
    assertEquals("class [Lcom.google.gwt.dev.jjs.test.CompilerTest$Fruit;",
        Fruit[].class.toString());
  }

  public void testClinitSideEffectInlining() {
    sideEffectChecker = 0;
    SideEffectCauser.causeClinitSideEffect();
    assertEquals(1, sideEffectChecker);
    SideEffectCauser2.causeClinitSideEffect();
    assertEquals(2, sideEffectChecker);
    SideEffectCauser3.causeClinitSideEffect();
    assertEquals(3, sideEffectChecker);
    String foo = SideEffectCauser4.causeClinitSideEffectOnRead;
    assertEquals(4, sideEffectChecker);
    jsniReadSideEffectCauser5();
    assertEquals(5, sideEffectChecker);
    foo = SideEffectCauser6.causeClinitSideEffectOnRead;
    assertEquals(6, sideEffectChecker);
    new SideEffectCauser7();
    assertEquals(7, sideEffectChecker);
    String checkRescued = NonSideEffectCauser.NOT_A_COMPILE_TIME_CONSTANT;
    assertEquals(null, checkRescued);
  }

  public void testConditionals() {
    assertTrue(TRUE ? TRUE : FALSE);
    assertFalse(FALSE ? TRUE : FALSE);
    assertFalse(TRUE ? FALSE : TRUE);
    assertTrue(FALSE ? FALSE : TRUE);

    assertTrue(true ? TRUE : FALSE);
    assertFalse(false ? TRUE : FALSE);
    assertFalse(true ? FALSE : TRUE);
    assertTrue(false ? FALSE : TRUE);

    assertTrue(TRUE ? true : FALSE);
    assertFalse(FALSE ? true : FALSE);
    assertFalse(TRUE ? false : TRUE);
    assertTrue(FALSE ? false : TRUE);

    assertTrue(TRUE ? TRUE : false);
    assertFalse(FALSE ? TRUE : false);
    assertFalse(TRUE ? FALSE : true);
    assertTrue(FALSE ? FALSE : true);
  }

  public void testDeadCode() {
    while (returnFalse()) {
      break;
    }

    do {
      break;
    } while (false);

    do {
      break;
    } while (returnFalse());

    for (; returnFalse();) {
    }

    boolean check = false;
    for (check = true; returnFalse(); fail()) {
      fail();
    }
    assertTrue(check);

    if (returnFalse()) {
      fail();
    } else {
    }

    if (!returnFalse()) {
    } else {
      fail();
    }

    // For these following tests, make sure that side effects in conditions
    // get propagated, even if they cause introduction of dead code.
    // 
    boolean b = false;
    if ((b = true) ? true : true) {
    }
    assertTrue(b);

    boolean c = true;
    int val = 0;
    for (val = 1; c = false; ++val) {
    }
    assertFalse(c);

    boolean d = true;
    while (d = false) {
    }
    assertFalse(d);

    boolean e = true;
    if (true | (e = false)) {
    }
    assertFalse(e);
  }

  public void testDeadTypes() {
    if (false) {
      new Object() {
      }.toString();

      class Foo {
        void a() {
        }
      }
      new Foo().a();
    }
  }

  public void testEmptyBlockStatements() {
    boolean b = false;
    while (b) {
    }

    do {
    } while (b);

    for (; b;) {
    }

    for (;;) {
      break;
    }

    if (b) {
    }

    if (b) {
    } else {
      b = false;
    }

    if (b) {
    } else {
    }
  }

  public native void testEmptyBlockStatementsNative() /*-{
    var b = false;
    while (b) {
    }

    do {
    } while (b);

    for (; b; ) {
    }

    for (;;) {
      break;
    }

    if (b) {
    }

    if (b) {
    } else {
      b = false;
    }

    if (b) {
    } else {
    }
  }-*/;

  // CHECKSTYLE_OFF

  public void testEmptyStatements() {
    boolean b = false;

    while (b);

    do; while (b);

    for (; b;);

    for (;;)
      break;

    if (b)
      ;

    if (b)
      ;
    else
      b = false;

    if (b)
      ;
    else
      ;
  }

  // CHECKSTYLE_ON

  public native void testEmptyStatementsNative() /*-{
    var b = false;

    while (b);

    do; while (b);

    for (; b;);

    for (;;)
      break;

    if (b)
      ;

    if (b)
      ;
    else
      b = false;

    if (b)
      ;
    else
      ;
  }-*/;

  public void testEmptyTryBlock() {
    int x = 0;
    try {
    } finally {
      x = 1;
    }
    assertEquals(1, x);
  }

  public void testForStatement() {
    {
      int i;
      for (i = 0; i < 10; ++i) {
      }
      assertEquals(i, 10);
    }
    {
      int i, c;
      for (i = 0, c = 10; i < c; ++i) {
      }
      assertEquals(i, 10);
      assertEquals(c, 10);
    }
    {
      int j = 0;
      for (int i = 0; i < 10; ++i) {
        ++j;
      }
      assertEquals(j, 10);
    }
    {
      int j = 0;
      for (int i = 0, c = 10; i < c; ++i) {
        ++j;
      }
      assertEquals(j, 10);
    }
  }

  /**
   * Issue #615: Internal Compiler Error.
   */
  public void testImplicitNull() {
    boolean b;
    String test = ((((b = true) ? null : null) + " ") + b);
    assertTrue(b);
    assertEquals("null true", test);
  }

  public void testJavaScriptReservedWords() {
    boolean delete = TRUE;
    for (int in = 0; in < 10; ++in) {
      assertTrue(in < 10);
      assertTrue(delete);
    }
  }

  public void testLabels() {
    int i = 0, j = 0;
    outer : for (i = 0; i < 1; ++i) {
      inner : for (j = 0; j < 1; ++j) {
        break outer;
      }
      fail();
    }
    assertEquals(0, i);
    assertEquals(0, j);

    outer : for (i = 0; i < 1; ++i) {
      inner : for (j = 0; j < 1; ++j) {
        continue outer;
      }
      fail();
    }
    assertEquals(1, i);
    assertEquals(0, j);

    outer : for (i = 0; i < 1; ++i) {
      inner : for (j = 0; j < 1; ++j) {
        break inner;
      }
    }
    assertEquals(1, i);
    assertEquals(0, j);

    outer : for (i = 0; i < 1; ++i) {
      inner : for (j = 0; j < 1; ++j) {
        continue inner;
      }
    }
    assertEquals(1, i);
    assertEquals(1, j);
  }

  public void testLocalClasses() {
    class Foo {
      public Foo(int j) {
        assertEquals(1, j);
      };
    }
    final int i;
    new Foo(i = 1) {
      {
        assertEquals(1, i);
      }
    };
    assertEquals(1, i);
  }

  public void testLocalRefs() {
    final String foo = TRUE ? "foo" : "bar";
    final String bar = TRUE ? "bar" : "foo";
    String result = new Object() {

      private String a = foo;

      {
        a = foo;
      }

      public String toString() {
        return new Object() {

          private static final String constantString = "wallawalla";

          private String ai = foo;

          {
            ai = foo;
          }

          public String toString() {
            // this line used to cause ICE due to no synthetic path to bar
            bar.valueOf(false);

            assertEquals("wallawalla", constantString);
            return foo + a + ai;
          }

        }.toString() + a;
      }

    }.toString();
    assertEquals(result, "foofoofoofoo");
  }

  public void testNotOptimizations() {
    assertFalse(!true);
    assertTrue(!false);

    assertTrue(!(TRUE == FALSE));
    assertFalse(!(TRUE != FALSE));

    assertFalse(!(3 < 4));
    assertFalse(!(3 <= 4));
    assertTrue(!(3 > 4));
    assertTrue(!(3 >= 4));

    assertTrue(!(4 < 3));
    assertTrue(!(4 <= 3));
    assertFalse(!(4 > 3));
    assertFalse(!(4 >= 3));

    assertTrue(!!TRUE);
    assertFalse(!!FALSE);
  }

  public void testNullFlow() {
    UninstantiableType f = null;

    try {
      f.returnNull().toString();
      fail();
    } catch (NullPointerException e) {
      // hosted mode
    } catch (JavaScriptException e) {
      // web mode
    }

    try {
      UninstantiableType[] fa = null;
      fa[4] = null;
      fail();
    } catch (NullPointerException e) {
      // hosted mode
    } catch (JavaScriptException e) {
      // web mode
    }
  }

  public void testNullFlowArray() {
    UninstantiableType[] uta = new UninstantiableType[10];
    assertEquals(uta.length, 10);
    assertEquals(uta[0], null);
    uta[1] = null;
    assertEquals(uta[1], null);
  }

  public void testNullFlowOverloads() {
    foo((Throwable) null);
    foo((String) null);
  }

  public void testNullFlowVsClassCastPrecedence() {
    try {
      ((UninstantiableType) new Object()).returnNull();
      fail();
    } catch (ClassCastException e) {
      // success
    }
  }

  public void testOuterSuperThisRefs() {
    new B();
  }

  public void testReturnStatementInCtor() {
    class Foo {
      int i;

      Foo(int i) {
        this.i = i;
        if (i == 0) {
          return;
        } else if (i == 1) {
          return;
        }
        return;
      }
    }
    assertEquals(new Foo(0).i, 0);
    assertEquals(new Foo(1).i, 1);
    assertEquals(new Foo(2).i, 2);
  }

  public void testStringOptimizations() {
    assertEquals("Herro, AJAX", "Hello, AJAX".replace('l', 'r'));
    assertEquals('J', "Hello, AJAX".charAt(8));
    assertEquals(11, "Hello, AJAX".length());
    assertFalse("Hello, AJAX".equals("me"));
    assertTrue("Hello, AJAX".equals("Hello, AJAX"));
    assertTrue("Hello, AJAX".equalsIgnoreCase("HELLO, ajax"));
    assertEquals("hello, ajax", "Hello, AJAX".toLowerCase());

    assertEquals("foobar", "foo" + barShouldInline());
    assertEquals("1bar", 1 + barShouldInline());
    assertEquals("fbar", 'f' + barShouldInline());
    assertEquals("truebar", true + barShouldInline());
    assertEquals("3.3bar", 3.3 + barShouldInline());
    assertEquals("3.3bar", 3.3f + barShouldInline());
    assertEquals("27bar", 27L + barShouldInline());
    assertEquals("nullbar", null + barShouldInline());
  }

  public void testSubclassStaticInnerAndClinitOrdering() {
    new CheckSubclassStaticInnerAndClinitOrdering();
  }

  public void testSwitchStatement() {
    switch (0) {
      case 0:
        // Once caused an ICE.
        int test;
        break;
    }
  }

  /**
   * Tests cases where the compiler will convert to a simple if or block.
   */
  public void testSwitchStatementConversions() {
    int i = 1;

    switch (i) {
      case 1:
        i = 2;
    }
    assertEquals(2, i);

    switch (i) {
      case 1:
        i = 3;
    }
    assertEquals(2, i);

    switch (i) {
      default:
        i = 3;
    }
    assertEquals(3, i);
  }

  public void testSwitchStatementEmpty() {
    switch (0) {
    }
  }

  public void testSwitchStatementFallthroughs() {
    int i = 1;
    switch (i) {
      case 1:
        i = 2;
      case 2:
        break;
      case 3:
        fail("Shouldn't get here");
    }
    assertEquals(2, i);

    switch (i) {
      case 1:
        break;
      case 2:
        i = 3;
        break;
      case 3:
        break;
      case 4:
        fail("Shouldn't get here");
        break;
    }
    assertEquals(3, i);
  }

  public void testSwitchStatementWithUsefulDefault() {
    switch (1) {
      case 1:
      case 2:
      case 3: {
        break;
      }
      case 4:
      case 5:
      case 6:
      default:
        fail("Shouldn't get here");
        break;
    }
  }

  public void testSwitchStatementWithUselessCases() {
    switch (1) {
      case 1:
      case 2:
      case 3: {
        break;
      }
      case 4:
      case 5:
      case 6:
      default:
        break;
    }
  }

  public void testSwitchStatementWontRemoveExpression() {
    class Foo {
      boolean setFlag;

      int setFlag() {
        setFlag = true;
        return 3;
      }
    }

    Foo foo = new Foo();
    switch (foo.setFlag()) {
      case 3:
        break;
    }

    // Make sure that compiler didn't optimize away the switch statement's
    // expression
    assertTrue(foo.setFlag);
  }

  public void testUnaryPlus() {
    int x, y = -7;
    x = +y;
    assertEquals(-7, x);
  }

  public void testUninstantiableNativeAccess() {
    UninstantiableType u = null;

    try {
      accessUninstantiableField(u);
      fail("Expected JavaScriptException");
    } catch (JavaScriptException expected) {
    }

    try {
      accessUninstantiableMethod(u);
      fail("Expected JavaScriptException");
    } catch (JavaScriptException expected) {
    }
  }

  private boolean returnFalse() {
    return false;
  }

}

class A {
  public abstract class AA {
  }
}

class B extends A {
  {
    new AA() {
    };
  }
}

// This construct used to cause an ICE
class CheckSubclassStaticInnerAndClinitOrdering extends Outer.StaticInner {
  private static class Foo {
  }

  private static final Foo FOO = new Foo();

  public CheckSubclassStaticInnerAndClinitOrdering() {
    this(FOO);
  }

  public CheckSubclassStaticInnerAndClinitOrdering(Foo foo) {
    // This used to be null due to clinit ordering issues
    Assert.assertNotNull(foo);
  }
}

class Outer {
  public static class StaticInner {
  }
}
