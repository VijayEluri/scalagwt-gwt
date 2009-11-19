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
package com.google.gwt.dev.javac;

/**
 * Test access to longs from JSNI.
 */
public class JsniCheckerTest extends CheckerTestCase {

  /**
   * JSNI references to anonymous inner classes is deprecated.
   */
  public void testAnoymousJsniRef() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static void main() {\n");
    code.append("    new Object() {\n");
    code.append("      int foo = 3;\n");
    code.append("    };\n");
    code.append("  }\n");
    code.append("  native void jsniMeth(Object o) /*-{\n");
    code.append("    o.@Buggy$1::foo;\n");
    code.append("  }-*/;\n");
    code.append("}\n");

    shouldGenerateError(code, 8, "Referencing class \'Buggy$1: "
        + "JSNI references to anonymous classes are illegal");
  }

  /**
   * JSNI references to anonymous inner classes is deprecated.
   */
  public void testAnoymousJsniRefNested() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static void main() {\n");
    code.append("    new Object() {\n");
    code.append("      class A {\n");
    code.append("        int foo = 3;\n");
    code.append("      };\n");
    code.append("    };\n");
    code.append("  }\n");
    code.append("  native void jsniMeth(Object o) /*-{\n");
    code.append("    o.@Buggy$1.A::foo;\n");
    code.append("  }-*/;\n");
    code.append("}\n");

    shouldGenerateError(code, 10, "Referencing class \'Buggy$1.A: "
        + "JSNI references to anonymous classes are illegal");
  }

  public void testArrayBadMember() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy[][]::blah;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(
        code,
        3,
        "Referencing member 'Buggy[][].blah': 'class' is the only legal reference for array types");
  }

  public void testArrayClass() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy[][]::class;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);
  }

  public void testCyclicReferences() {
    {
      StringBuffer buggy = new StringBuffer();
      buggy.append("class Buggy {\n");
      buggy.append("  static int anint = 3;\n");
      buggy.append("  native void jsniMeth() /*-{\n");
      buggy.append("    $wnd.alert(@Extra::along);\n");
      buggy.append("  }-*/;\n");
      buggy.append("}\n");

      StringBuffer extra = new StringBuffer();
      extra.append("class Extra {\n");
      extra.append("  static long along = 3;\n");
      extra.append("  native void jsniMeth() /*-{\n");
      extra.append("    $wnd.alert(@Buggy::anint);\n");
      extra.append("  }-*/;\n");
      extra.append("}\n");

      shouldGenerateError(buggy, extra, 4, "Referencing field 'Extra.along': "
          + "type 'long' is not safe to access in JSNI code");
    }

    {
      StringBuffer buggy = new StringBuffer();
      buggy.append("class Buggy {\n");
      buggy.append("  Extra anExtra = new Extra();\n");
      buggy.append("  static int anint = 3;\n");
      buggy.append("  native void jsniMeth() /*-{\n");
      buggy.append("    $wnd.alert(@Extra::along);\n");
      buggy.append("  }-*/;\n");
      buggy.append("}\n");

      StringBuffer extra = new StringBuffer();
      extra.append("class Extra {\n");
      extra.append("  Buggy mattress = new Buggy();\n");
      extra.append("  static long along = 3;\n");
      extra.append("  native void jsniMeth() /*-{\n");
      extra.append("    $wnd.alert(@Buggy::anint);\n");
      extra.append("  }-*/;\n");
      extra.append("}\n");

      shouldGenerateError(buggy, extra, 5, "Referencing field 'Extra.along': "
          + "type 'long' is not safe to access in JSNI code");
    }
  }

  public void testDeprecationField() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  @Deprecated int bar;\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy::bar;\n");
    code.append("  }-*/;\n");
    code.append("}\n");

    shouldGenerateWarning(code, 4, "Referencing deprecated field 'Buggy.bar'");
  }

  public void testDeprecationMethod() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  @Deprecated void foo(){}\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy::foo();\n");
    code.append("  }-*/;\n");
    code.append("}\n");

    shouldGenerateWarning(code, 4, "Referencing deprecated method 'Buggy.foo'");
  }

  public void testDeprecationSuppression() {
    StringBuffer code = new StringBuffer();
    code.append("@Deprecated class D {\n");
    code.append("  int bar;\n");
    code.append("}\n");
    code.append("class Buggy {\n");
    code.append("  @Deprecated void foo(){}\n");
    code.append("  @Deprecated int bar;\n");
    code.append("  @SuppressWarnings(\"deprecation\")\n");
    code.append("  native void jsniMethod1() /*-{\n");
    code.append("    @Buggy::foo();\n");
    code.append("    @Buggy::bar;\n");
    code.append("    @D::bar;\n");
    code.append("  }-*/;\n");
    code.append("  @SuppressWarnings({\"deprecation\", \"other\"})\n");
    code.append("  native void jsniMethod2() /*-{\n");
    code.append("    @Buggy::foo();\n");
    code.append("    @Buggy::bar;\n");
    code.append("    @D::bar;\n");
    code.append("  }-*/;\n");
    code.append("}\n");

    shouldGenerateNoWarning(code);
  }

  public void testDeprecationType() {
    StringBuffer code = new StringBuffer();
    code.append("@Deprecated class D {\n");
    code.append("  int bar;\n");
    code.append("}\n");
    code.append("class Buggy {\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @D::bar;\n");
    code.append("  }-*/;\n");
    code.append("}\n");

    shouldGenerateWarning(code, 6, "Referencing deprecated class 'D'");
  }

  public void testFieldAccess() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("volatile long x = -1;\n");
    code.append("native void jsniMeth() /*-{\n");
    code.append("  $wnd.alert(\"x is: \"+this.@Buggy::x); }-*/;\n");
    code.append("}\n");

    shouldGenerateError(code, 4,
        "Referencing field 'Buggy.x': type 'long' is not safe to access in JSNI code");
  }

  public void testInnerClass() {
    StringBuffer code = new StringBuffer();
    code.append("public class Buggy {\n");
    code.append("  static class Inner {\n");
    code.append("    long x = 3;\n");
    code.append("  }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(@Buggy.Inner::x);\n");
    code.append("  }-*/;\n");
    code.append("}\n");

    shouldGenerateError(code, 6, "Referencing field 'Buggy.Inner.x': "
        + "type 'long' is not safe to access in JSNI code");
  }

  public void testInnerClassDollar() {
    StringBuffer code = new StringBuffer();
    code.append("public class Buggy {\n");
    code.append("  static class Inner {\n");
    code.append("    long x = 3;\n");
    code.append("  }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(@Buggy$Inner::x);\n");
    code.append("  }-*/;\n");
    code.append("}\n");

    shouldGenerateError(code, 6, "Referencing field 'Buggy$Inner.x': "
        + "type 'long' is not safe to access in JSNI code");
  }

  public void testInnerNew() {
    StringBuffer code = new StringBuffer();
    code.append("public class Buggy {\n");
    code.append("  class Inner {\n");
    code.append("    long x = 3;\n");
    code.append("    Inner(boolean b) { };\n");
    code.append("  }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(@Buggy.Inner::new(Z)(true).toString());\n");
    code.append("  }-*/;\n");
    code.append("}\n");

    // Cannot resolve, missing synthetic enclosing instance.
    shouldGenerateWarning(code, 7, "Referencing method 'Buggy.Inner.new(Z)': "
        + "unable to resolve method, expect subsequent failures");

    code = new StringBuffer();
    code.append("public class Buggy {\n");
    code.append("  static class Inner {\n");
    code.append("    long x = 3;\n");
    code.append("    Inner(boolean b) { };\n");
    code.append("  }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(@Buggy.Inner::new(Z)(this, true).toString());\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);

    code = new StringBuffer();
    code.append("public class Buggy {\n");
    code.append("  class Inner {\n");
    code.append("    long x = 3;\n");
    code.append("    Inner(boolean b) { };\n");
    code.append("  }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(@Buggy.Inner::new(LBuggy;Z)(this, true).toString());\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);
  }

  /**
   * The proper behavior here is a close call. In hosted mode, Java arrays are
   * completely unusable in JavaScript, so the current reasoning is to allow
   * them.
   */
  public void testLongArray() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  long[] m() { return new long[] { -1 }; }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(this.@Buggy::m()()); }-*/;\n");
    code.append("}\n");

    shouldGenerateNoError(code);
  }

  public void testLongParameter() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMeth(long x) /*-{ return; }-*/;\n");
    code.append("}\n");

    shouldGenerateError(code, 2,
        "Parameter 'x': type 'long' is not safe to access in JSNI code");
  }

  public void testLongReturn() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native long jsniMeth() /*-{ return 0; }-*/;\n");
    code.append("}\n");

    shouldGenerateError(code, 2,
        "Type 'long' may not be returned from a JSNI method");
  }

  public void testMalformedJsniRef() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(code, 3, "Expected \":\" in JSNI reference");
  }

  public void testMethodArgument() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  void print(long x) { }\n");
    code.append("  native void jsniMeth() /*-{ this.@Buggy::print(J)(0); }-*/;\n");
    code.append("}\n");

    shouldGenerateError(
        code,
        3,
        "Parameter 1 of method \'Buggy.print\': type 'long' may not be passed out of JSNI code");
  }

  public void testMethodReturn() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  long m() { return -1; }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(this.@Buggy::m()()); }-*/;\n");
    code.append("}\n");

    shouldGenerateError(
        code,
        4,
        "Referencing method 'Buggy.m': return type 'long' is not safe to access in JSNI code");
  }

  public void testNew() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static native Object main() /*-{\n");
    code.append("    return @Buggy::new()();\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);

    code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  Buggy(boolean b) { }\n");
    code.append("  static native Object main() /*-{\n");
    code.append("    return @Buggy::new(Z)(true);\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);
  }

  public void testNullField() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static native Object main() /*-{\n");
    code.append("    return @null::nullField;\n");
    code.append("  }-*/;\n");
    code.append("}\n");

    shouldGenerateNoWarning(code);
  }

  public void testNullMethod() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  static native Object main() /*-{\n");
    code.append("    return @null::nullMethod()();\n");
    code.append("  }-*/;\n");
    code.append("}\n");

    shouldGenerateNoWarning(code);
  }

  public void testOverloadedMethodWithNoWarning() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  long m(int x) { return -1; }\n");
    code.append("  int m(String x) { return -1; }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(this.@Buggy::m(Ljava/lang/String;)(\"hello\")); }-*/;\n");
    code.append("}\n");

    shouldGenerateNoError(code);
  }

  public void testOverloadedMethodWithWarning() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  long m(int x) { return -1; }\n");
    code.append("  int m(String x) { return -1; }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(this.@Buggy::m(I)(10)); }-*/;\n");
    code.append("}\n");

    shouldGenerateError(
        code,
        5,
        "Referencing method 'Buggy.m': return type 'long' is not safe to access in JSNI code");
  }

  public void testPrimitiveBadMember() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Z::blah;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateError(
        code,
        3,
        "Referencing member 'Z.blah': 'class' is the only legal reference for primitive types");
  }

  public void testPrimitiveClass() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Z::class;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateNoWarning(code);
  }

  public void testRefInString() {
    {
      StringBuffer code = new StringBuffer();
      code.append("import com.google.gwt.core.client.UnsafeNativeLong;");
      code.append("class Buggy {\n");
      code.append("  void print(long x) { }\n");
      code.append("  native void jsniMeth() /*-{ 'this.@Buggy::print(J)(0)'; }-*/;\n");
      code.append("}\n");

      shouldGenerateNoError(code);
    }
  }

  public void testUnresolvedClass() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Foo::x;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateWarning(code, 3,
        "Referencing class 'Foo': unable to resolve class, expect subsequent failures");
  }

  public void testUnresolvedField() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy::x;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateWarning(
        code,
        3,
        "Referencing field 'Buggy.x': unable to resolve field, expect subsequent failures");
  }

  public void testUnresolvedMethod() {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMethod() /*-{\n");
    code.append("    @Buggy::x(Ljava/lang/String);\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    shouldGenerateWarning(
        code,
        3,
        "Referencing method 'Buggy.x(Ljava/lang/String)': unable to resolve method, expect subsequent failures");
  }

  public void testUnsafeAnnotation() {
    {
      StringBuffer code = new StringBuffer();
      code.append("import com.google.gwt.core.client.UnsafeNativeLong;");
      code.append("class Buggy {\n");
      code.append("  void print(long x) { }\n");
      code.append("  @UnsafeNativeLong\n");
      code.append("  native void jsniMeth() /*-{ this.@Buggy::print(J)(0); }-*/;\n");
      code.append("}\n");

      shouldGenerateNoError(code);
    }
  }

  public void testViolator() {
    {
      StringBuffer okay = new StringBuffer();
      okay.append("class Buggy {\n");
      okay.append("  native void jsniMeth() /*-{\n");
      okay.append("    $wnd.alert(@Extra.Inner::x);\n");
      okay.append("  }-*/;\n");
      okay.append("}\n");

      StringBuffer extra = new StringBuffer();
      extra.append("class Extra {\n");
      extra.append("  private static class Inner { \n");
      extra.append("    private static int x = 3;\n");
      extra.append("  }\n");
      extra.append("}\n");

      shouldGenerateNoError(okay, extra);
    }

    {
      StringBuffer buggy = new StringBuffer();
      buggy.append("class Buggy {\n");
      buggy.append("  native void jsniMeth() /*-{\n");
      buggy.append("    $wnd.alert(@Extra.Inner::x);\n");
      buggy.append("  }-*/;\n");
      buggy.append("}\n");

      StringBuffer extra = new StringBuffer();
      extra.append("class Extra {\n");
      extra.append("  private static class Inner { \n");
      extra.append("    private static long x = 3;\n");
      extra.append("  }\n");
      extra.append("}\n");

      shouldGenerateError(
          buggy,
          extra,
          3,
          "Referencing field 'Extra.Inner.x': type 'long' is not safe to access in JSNI code");
    }
  }
}
