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
package com.google.gwt.soyc;

import com.google.gwt.dev.util.collect.HashSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A size breakdown of one code collection.
 */
public class SizeBreakdown {
  private static void initializeLiteralsCollection(
      Map<String, LiteralsCollection> nameToLitColl) {
    nameToLitColl.put("long", new LiteralsCollection("long"));
    nameToLitColl.put("null", new LiteralsCollection("null"));
    nameToLitColl.put("class", new LiteralsCollection("class"));
    nameToLitColl.put("int", new LiteralsCollection("int"));
    nameToLitColl.put("string", new LiteralsCollection("string"));
    nameToLitColl.put("number", new LiteralsCollection("number"));
    nameToLitColl.put("boolean", new LiteralsCollection("boolean"));
    nameToLitColl.put("double", new LiteralsCollection("double"));
    nameToLitColl.put("char", new LiteralsCollection("char"));
    nameToLitColl.put("undefined", new LiteralsCollection("undefined"));
    nameToLitColl.put("float", new LiteralsCollection("float"));
  }

  private static void initializeNameToCodeCollection(
      HashMap<String, CodeCollection> nameToCodeColl) {
    nameToCodeColl.put("allOther", new CodeCollection("allOther"));
    nameToCodeColl.put("widget", new CodeCollection("widget"));
    nameToCodeColl.put("rpcUser", new CodeCollection("rpcUser"));
    nameToCodeColl.put("rpcGen", new CodeCollection("rpcGen"));
    nameToCodeColl.put("rpcGwt", new CodeCollection("rpcGwt"));
    nameToCodeColl.put("gwtLang", new CodeCollection("long"));
    nameToCodeColl.put("jre", new CodeCollection("jre"));
  }

  public Map<String, Float> classToPartialSize = new HashMap<String, Float>();
  public Map<String, Integer> classToSize = new HashMap<String, Integer>();
  public Map<String, LiteralsCollection> nameToLitColl = new TreeMap<String, LiteralsCollection>();
  public int nonAttributedBytes = 0;
  public Set<String> nonAttributedStories = new HashSet<String>();;
  public Map<String, Float> packageToPartialSize = new HashMap<String, Float>();
  public Map<String, Integer> packageToSize = new HashMap<String, Integer>();
  public HashMap<String, CodeCollection> nameToCodeColl = new HashMap<String, CodeCollection>();

  public int sizeAllCode;

  private final String description;
  private final String id;

  public SizeBreakdown(String description, String id) {
    this.description = description;
    this.id = id;

    initializeLiteralsCollection(nameToLitColl);
    initializeNameToCodeCollection(nameToCodeColl);
  }

  /**
   * A short but human-readable description of this code collection.
   */
  public String getDescription() {
    return description;
  }

  /**
   * An identifier for this code collection suitable for use within file names.
   */
  public String getId() {
    return id;
  }
  
  @Override
  public String toString() {
    return getId();
  }
}
