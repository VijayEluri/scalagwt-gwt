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
package com.google.gwt.core.client.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

/**
 * Encapsulates logic to create a stack trace. This class should only be used in
 * web mode.
 */
public class StackTraceCreator {
  /**
   * This class acts as a deferred-binding hook point to allow more optimal
   * versions to be substituted. This base version simply crawls
   * <code>arguments.callee.caller</code>.
   */
  static class Collector {
    public native JsArrayString collect() /*-{
      var seen = {};
      var toReturn = [];

      // Ignore the collect() and fillInStackTrace call
      var callee = arguments.callee.caller.caller;
      while (callee) {
        var name = this.@com.google.gwt.core.client.impl.StackTraceCreator.Collector::extractName(Ljava/lang/String;)(callee.toString());
        toReturn.push(name);

        // Avoid infinite loop by associating names to function objects.  We
        // record each caller in the withThisName variable to handle functions
        // with identical names but separate identity (such as 'anonymous')
        var keyName = ':' + name;
        var withThisName = seen[keyName];
        if (withThisName) {
          var i, j;
          for (i = 0, j = withThisName.length; i < j; i++) {
            if (withThisName[i] === callee) {
              return toReturn;
            }
          }
        }

        (withThisName || (seen[keyName] = [])).push(callee);
        callee = callee.caller;
      }
      return toReturn;
    }-*/;

    /**
     * Attempt to infer the stack from an unknown JavaScriptObject that had been
     * thrown. The default implementation just returns an empty array.
     */
    public JsArrayString inferFrom(JavaScriptObject e) {
      return JavaScriptObject.createArray().cast();
    }

    /**
     * Extract the name of a function from it's toString() representation.
     * Package-access for testing.
     */
    protected String extractName(String fnToString) {
      return extractNameFromToString(fnToString);
    }

    /**
     * Raise an exception and return it.
     */
    protected native JavaScriptObject makeException() /*-{
      try {
        null.a();
      } catch (e) {
        return e;
      }
    }-*/;
  }

  /**
   * Mozilla provides a <code>stack</code> property in thrown objects.
   */
  static class CollectorMoz extends Collector {
    /**
     * This implementation doesn't suffer from the limitations of crawling
     * <code>caller</code> since Mozilla provides proper activation records.
     */
    @Override
    public JsArrayString collect() {
      return splice(inferFrom(makeException()), toSplice());
    }

    @Override
    public JsArrayString inferFrom(JavaScriptObject e) {
      JsArrayString stack = getStack(e);
      for (int i = 0, j = stack.length(); i < j; i++) {
        stack.set(i, extractName(stack.get(i)));
      }
      return stack;
    }

    protected native JsArrayString getStack(JavaScriptObject e) /*-{
      return (e && e.stack) ? e.stack.split('\n') : [];
    }-*/;

    protected int toSplice() {
      return 2;
    }
  }

  /**
   * Opera encodes stack trace information in the error's message.
   */
  static class CollectorOpera extends CollectorMoz {
    /**
     * We have much a much simpler format to work with.
     */
    @Override
    protected String extractName(String fnToString) {
      return fnToString.length() == 0 ? "anonymous" : fnToString;
    }

    /**
     * Opera has the function name on every-other line.
     */
    @Override
    protected JsArrayString getStack(JavaScriptObject e) {
      JsArrayString toReturn = getMessage(e);
      assert toReturn.length() % 2 == 0 : "Expecting an even number of lines";

      int i, i2, j;
      for (i = 0, i2 = 0, j = toReturn.length(); i2 < j; i++, i2 += 2) {
        int idx = toReturn.get(i2).lastIndexOf("function ");
        if (idx == -1) {
          toReturn.set(i, "");
        } else {
          toReturn.set(i, toReturn.get(i2).substring(idx + 9).trim());
        }
      }
      setLength(toReturn, i);

      return toReturn;
    }

    @Override
    protected int toSplice() {
      return 3;
    }

    private native JsArrayString getMessage(JavaScriptObject e) /*-{
      return (e && e.message) ? e.message.split('\n') : [];
    }-*/;

    private native void setLength(JsArrayString obj, int length) /*-{
      obj.length = length;
    }-*/;
  }

  /**
   * Create a stack trace based on the current execution stack. This method
   * should only be called in web mode.
   */
  public static JsArrayString createStackTrace() {
    if (!GWT.isScript()) {
      throw new RuntimeException(
          "StackTraceCreator should only be called in web mode");
    }

    return GWT.<Collector> create(Collector.class).collect();
  }

  /**
   * Create a stack trace based on a JavaScriptException. This method should
   * only be called in web mode.
   */
  public static void createStackTrace(JavaScriptException e) {
    if (!GWT.isScript()) {
      throw new RuntimeException(
          "StackTraceCreator should only be called in web mode");
    }

    JsArrayString stack = GWT.<Collector> create(Collector.class).inferFrom(
        e.getException());

    StackTraceElement[] stackTrace = new StackTraceElement[stack.length()];
    for (int i = 0, j = stackTrace.length; i < j; i++) {
      stackTrace[i] = new StackTraceElement("Unknown", stack.get(i),
          "Unknown source", 0);
    }
    e.setStackTrace(stackTrace);
  }

  static String extractNameFromToString(String fnToString) {
    String toReturn = "";
    fnToString = fnToString.trim();
    int index = fnToString.indexOf("(");
    if (index != -1) {
      int start = fnToString.startsWith("function") ? 8 : 0;
      toReturn = fnToString.substring(start, index).trim();
    }

    return toReturn.length() > 0 ? toReturn : "anonymous";
  }

  private static native JsArrayString splice(JsArrayString arr, int length) /*-{
    (arr.length >= length) && arr.splice(0, length);
    return arr;
  }-*/;
}
