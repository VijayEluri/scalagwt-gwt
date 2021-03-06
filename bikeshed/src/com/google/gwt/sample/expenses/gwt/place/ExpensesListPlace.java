/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.sample.expenses.gwt.place;

import com.google.gwt.valuestore.shared.Record;

/**
 * Place in an app that lists
 * {@link com.google.gwt.valuestore.shared.ValueStore ValueStore} records of a
 * particular type.
 */
public class ExpensesListPlace extends ExpensesPlace {
  private final Class<? extends Record> key;

  /**
   * @param key the schema of the entities at this place
   */
  public ExpensesListPlace(Class<? extends Record> key) {
    this.key = key;
  }

  public Class<? extends Record> getType() {
    return key;
  }
}
