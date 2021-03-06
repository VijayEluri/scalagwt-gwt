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
package com.google.gwt.app.place;

import com.google.gwt.user.client.ui.Renderer;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Presenter that goes to {@link Place}s the user picks.
 *
 * @param <P> the type of places listed
 */
public class PlacePicker<P extends Place> implements
    PlacePickerView.Listener<P> {
  private final PlacePickerView<P> view;
  private final PlaceController<? super P> placeController;
  private final Renderer<P> renderer;

  public PlacePicker(PlacePickerView<P> view,
      PlaceController<? super P> placeController, Renderer<P> renderer) {
    // ? super P to allow us to pick subtypes of the PlaceController's type
    this.view = view;
    this.placeController = placeController;
    this.view.setListener(this);
    this.renderer = renderer;
  }

  public void placePicked(P place) {
    placeController.goTo(place);
  }

  public void setPlaces(List<? extends P> places) {
    LinkedHashMap<P, String> map = new LinkedHashMap<P, String>();
    for (P place : places) {
      map.put(place, renderer.render(place));
    }
    view.setValues(map);
  }
}
