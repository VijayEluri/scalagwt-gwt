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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

/**
 * No-op MortalLogger for unit testing, which holds on to the last
 * "die" message it receives.
 */
public final class MockMortalLogger extends MortalLogger {
  public String died;
  
  public MockMortalLogger() {
    super(TreeLogger.NULL);
  }

  @Override
  public void die(String message, Object... params)
      throws UnableToCompleteException {
    died = String.format(message, params);
    super.die(message, params);
  }
}