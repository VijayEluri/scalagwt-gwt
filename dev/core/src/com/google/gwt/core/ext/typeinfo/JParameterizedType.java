/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.core.ext.typeinfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a parameterized type in a declaration.
 */
public class JParameterizedType extends JDelegatingClassType {

  private final List<JClassType> typeArgs = new ArrayList<JClassType>();

  JParameterizedType(JGenericType baseType) {
    super.setBaseType(baseType);
  }

  @Override
  public JField findField(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public JMethod findMethod(String name, JType[] paramTypes) {
    // TODO Auto-generated method stub
    return null;
  }

  public JGenericType getBaseType() {
    return (JGenericType) baseType;
  }

  @Override
  public JField getField(String name) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public JField[] getFields() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public JMethod getMethod(String name, JType[] paramTypes)
      throws NotFoundException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public JMethod[] getMethods() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * @deprecated see {@link #getQualifiedSourceName()}
   */
  @Deprecated
  public String getNonParameterizedQualifiedSourceName() {
    return getQualifiedSourceName();
  }

  @Override
  public String getParameterizedQualifiedSourceName() {
    StringBuffer sb = new StringBuffer();
    sb.append(getQualifiedSourceName());

    sb.append('<');
    boolean needComma = false;
    for (JType typeArg : typeArgs) {
      if (needComma) {
        sb.append(", ");
      } else {
        needComma = true;
      }
      sb.append(typeArg.getParameterizedQualifiedSourceName());
    }
    sb.append('>');
    return sb.toString();
  }

  /**
   * Everything is fully qualified and includes the &lt; and &gt; in the
   * signature.
   */
  public String getQualifiedSourceName() {
    return getBaseType().getQualifiedSourceName();
  }

  public JClassType getRawType() {
    return getBaseType().getRawType();
  }

  /**
   * In this case, the raw type name.
   */
  public String getSimpleSourceName() {
    return getRawType().getSimpleSourceName();
  }

  public JClassType[] getTypeArgs() {
    return typeArgs.toArray(TypeOracle.NO_JCLASSES);
  }

  @Override
  public JGenericType isGenericType() {
    return null;
  }

  @Override
  public JParameterizedType isParameterized() {
    return this;
  }

  @Override
  public JRawType isRawType() {
    return null;
  }

  void addTypeArg(JClassType type) {
    assert (type.isPrimitive() == null);
    typeArgs.add(type);
  }

}
