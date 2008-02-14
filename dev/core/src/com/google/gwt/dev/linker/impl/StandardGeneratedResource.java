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
package com.google.gwt.dev.linker.impl;

import com.google.gwt.dev.linker.GeneratedResource;

import java.net.URL;

/**
 * The standard implementation of {@link GeneratedResource}.
 */
public class StandardGeneratedResource extends StandardModuleResource implements
    GeneratedResource {

  public StandardGeneratedResource(String partialPath, URL url) {
    super(partialPath, url);
  }

  public String getPartialPath() {
    return getId();
  }
}
