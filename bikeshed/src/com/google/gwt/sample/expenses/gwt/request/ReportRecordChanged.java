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
package com.google.gwt.sample.expenses.gwt.request;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.valuestore.shared.RecordChangedEvent;
import com.google.gwt.requestfactory.shared.RequestFactory.WriteOperation;

/**
 * "API Generated" event posted when the values of a {@link EmployeeRecord}
 * change.
 * <p>
 * IRL this class will be generated by a JPA-savvy tool run before compilation.
 */
public class ReportRecordChanged extends
    RecordChangedEvent<ReportRecord, ReportRecordChanged.Handler> {

  /**
   * Implemented by handlers of this type of event.
   */
  public interface Handler extends EventHandler {
    void onReportChanged(ReportRecordChanged event);
  }

  public static final Type<Handler> TYPE = new Type<Handler>();

  public ReportRecordChanged(ReportRecord record, WriteOperation writeOperation) {
    super(record, writeOperation);
  }

  @Override
  public GwtEvent.Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onReportChanged(this);
  }
}
