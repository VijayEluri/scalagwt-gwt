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
package com.google.gwt.module.rebind;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.Util;
import com.google.gwt.module.client.NoDeployTest;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Creates two files in the generated output directory.
 */
public class NoDeployGenerator extends Generator {

  @Override
  public String generate(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {

    try {
      createFile(logger, context, "publicFile.txt", false);
      createFile(logger, context, "privateFile.txt", true);
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to create test file", e);
      throw new UnableToCompleteException();
    }

    return typeName;
  }

  private void createFile(TreeLogger logger, GeneratorContext context,
      String path, boolean isPrivate) throws UnableToCompleteException,
      IOException {

    OutputStream out = context.tryCreateResource(logger, path);
    if (out == null) {
      return;
    }

    out.write(Util.getBytes(NoDeployTest.TEST_TEXT));
    context.commitResource(logger, out).setPrivate(isPrivate);
  }
}
