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
package com.google.gwt.dev.resource.impl;

import com.google.gwt.dev.util.StringInterner;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

/**
 * Represents a resource contained in a jar or zip file.
 */
public class ZipFileResource extends AbstractResource {

  private final ZipFileClassPathEntry classPathEntry;
  private final String path;
  private long lastModified;

  public ZipFileResource(ZipFileClassPathEntry classPathEntry, String path,
      long lastModified) {
    this.classPathEntry = classPathEntry;
    this.path = StringInterner.get().intern(path);
    this.lastModified = lastModified;
  }

  @Override
  public ZipFileClassPathEntry getClassPathEntry() {
    return classPathEntry;
  }

  @Override
  public long getLastModified() {
    return lastModified;
  }

  @Override
  public String getLocation() {
    return "jar:" + classPathEntry.getLocation() + "!/" + path;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public InputStream openContents() {
    try {
      return classPathEntry.getZipFile().getInputStream(new ZipEntry(path));
    } catch (IOException e) {
      // The spec for this method says it can return null.
      return null;
    }
  }

  @Override
  public boolean wasRerooted() {
    return false;
  }
}
