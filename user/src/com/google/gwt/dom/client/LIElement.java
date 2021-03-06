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
package com.google.gwt.dom.client;

/**
 * List item.
 * 
 * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/lists.html#edef-LI">W3C HTML Specification</a>
 */
@TagName(LIElement.TAG)
public class LIElement extends Element {

  static final String TAG = "li";

  /**
   * Assert that the given {@link Element} is compatible with this class and
   * automatically typecast it.
   */
  public static LIElement as(Element elem) {
    assert elem.getTagName().equalsIgnoreCase(TAG);
    return (LIElement) elem;
  }

  protected LIElement() {
  }
}
