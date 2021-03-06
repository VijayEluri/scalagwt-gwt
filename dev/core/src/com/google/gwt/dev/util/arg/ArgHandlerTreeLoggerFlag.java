/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.util.arg;

import com.google.gwt.util.tools.ArgHandlerFlag;

/**
 * Argument handler for processing the GUI tree logger boolean flag.
 */
public final class ArgHandlerTreeLoggerFlag extends ArgHandlerFlag {

  private final OptionGuiLogger option;

  public ArgHandlerTreeLoggerFlag(OptionGuiLogger option) {
    this.option = option;
  }

  public String getPurpose() {
    return "Logs output in a graphical tree view";
  }

  public String getTag() {
    return "-treeLogger";
  }

  @Override
  public boolean isUndocumented() {
    return true;
  }

  public boolean setFlag() {
    option.setUseGuiLogger(true);
    return true;
  }
}
