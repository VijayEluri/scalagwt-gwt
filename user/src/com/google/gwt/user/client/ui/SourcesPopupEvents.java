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
package com.google.gwt.user.client.ui;

/**
 * A widget that implements this interface sources the events defined by the
 * {@link com.google.gwt.user.client.ui.PopupListener} interface.
 * 
 * @deprecated use {@link com.google.gwt.event.logical.shared.HasCloseHandlers}
 *             instead
 */
@Deprecated
public interface SourcesPopupEvents {

  /**
   * Adds a listener interface to receive popup events.
   * 
   * @param listener the listener interface to add.
   * @deprecated use {@link com.google.gwt.event.logical.shared.HasCloseHandlers#addCloseHandler}
   *             instead
   */
  @Deprecated
  void addPopupListener(PopupListener listener);

  /**
   * Removes a previously added popup listener.
   * 
   * @param listener the listener interface to remove.
   * @deprecated Use the {@link HandlerRegistration#removeHandler}
   * method on the object returned by {@link
   * com.google.gwt.event.logical.shared.HasCloseHandlers#addCloseHandler}
   * instead
   */
  @Deprecated
  void removePopupListener(PopupListener listener);
}
