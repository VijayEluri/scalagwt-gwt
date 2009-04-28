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
package com.google.gwt.user.linker.rpc;

import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.EmittedArtifact;

/**
 * This artifact provides information about which proxy classes resulted in
 * which rpc proxy files.
 */
public class RpcPolicyFileArtifact extends Artifact<RpcPolicyFileArtifact> {

  private final String proxyClass;
  private final EmittedArtifact artifact;

  public RpcPolicyFileArtifact(String proxyClass, EmittedArtifact artifact) {
    super(RpcPolicyManifestLinker.class);
    this.proxyClass = proxyClass;
    this.artifact = artifact;
  }

  public EmittedArtifact getEmittedArtifact() {
    return artifact;
  }

  public String getProxyClass() {
    return proxyClass;
  }

  @Override
  public final int hashCode() {
    return getProxyClass().hashCode();
  }

  @Override
  protected final int compareToComparableArtifact(RpcPolicyFileArtifact o) {
    return getProxyClass().compareTo(o.getProxyClass());
  }

  @Override
  protected final Class<RpcPolicyFileArtifact> getComparableArtifactType() {
    return RpcPolicyFileArtifact.class;
  }
}
