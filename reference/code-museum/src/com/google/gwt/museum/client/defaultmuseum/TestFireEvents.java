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
package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;

import java.util.HashMap;
import java.util.Map;

/**
 * Verify that events fire in all browsers.
 */
public class TestFireEvents extends AbstractIssue {
  /**
   * The main grid used for layout.
   */
  private FlexTable layout = null;

  private Map<Integer, Integer> eventMap = new HashMap<Integer, Integer>();

  @Override
  public Widget createIssue() {
    // Create a grid to hold all of the tests
    eventMap.clear();
    layout = new FlexTable();
    layout.setCellPadding(3);
    layout.setBorderWidth(2);
    layout.setHTML(0, 0, "<b>Action to Perform</b>");
    layout.setHTML(0, 1, "<b>Event</b>");
    layout.setHTML(0, 2, "<b>Status</b>");

    // Mouse and click events
    Button button = new Button("Double-click me") {
      @Override
      public void onBrowserEvent(Event event) {
        passTest(event);
      }
    };
    button.sinkEvents(Event.ONCLICK | Event.ONDBLCLICK | Event.MOUSEEVENTS);
    addTest(Event.ONCLICK, "click", button);
    addDependentTest(Event.ONDBLCLICK, "dblclick");
    addDependentTest(Event.ONMOUSEDOWN, "mousedown");
    addDependentTest(Event.ONMOUSEUP, "mouseup");
    addDependentTest(Event.ONMOUSEOVER, "mouseover");
    addDependentTest(Event.ONMOUSEOUT, "mouseout");
    addDependentTest(Event.ONMOUSEMOVE, "mousemove");

    // Keyboard events
    TextBox textBox = new TextBox() {
      @Override
      public void onBrowserEvent(Event event) {
        passTest(event);
      }
    };
    textBox.sinkEvents(Event.KEYEVENTS | Event.ONFOCUS | Event.ONBLUR
        | Event.ONCHANGE);
    textBox.setText("Type a letter");
    addTest(Event.ONKEYDOWN, "keydown", textBox);
    addDependentTest(Event.ONKEYPRESS, "keypress");
    addDependentTest(Event.ONKEYUP, "keyup");
    addDependentTest(Event.ONFOCUS, "focus");
    addDependentTest(Event.ONBLUR, "blur");
    addDependentTest(Event.ONCHANGE, "change");

    // onscroll and onmousewheel
    String scrollableMessage = "Scroll to the bottom<br>(using mouse wheel<br>"
        + "if supported)";
    HTML scrollableContents = new HTML(scrollableMessage);
    scrollableContents.setPixelSize(400, 400);
    scrollableContents.getElement().getStyle().setProperty("textAlign", "left");
    ScrollPanel scrollable = new ScrollPanel(scrollableContents) {
      @Override
      public void onBrowserEvent(Event event) {
        passTest(event);
      }
    };
    scrollable.sinkEvents(Event.ONSCROLL | Event.ONMOUSEWHEEL);
    scrollable.setAlwaysShowScrollBars(true);
    scrollable.setPixelSize(200, 100);
    addTest(Event.ONSCROLL, "scroll", scrollable);
    addDependentTest(Event.ONMOUSEWHEEL, "mousewheel");

    // onload
    Image loadable = new Image() {
      @Override
      public void onBrowserEvent(Event event) {
        passTest(event);
        if (DOM.eventGetType(event) == Event.ONERROR) {
          setUrl("issues/images/gwtLogo.png");
        }
      }
    };
    addTest(Event.ONERROR, "error", loadable);
    addDependentTest(Event.ONLOAD, "load");
    loadable.setUrl("imageDoesNotExist.abc");

    // The following are not testable or not supported in all browsers
    // onlosecapture

    return layout;
  }

  @Override
  public String getInstructions() {
    return "Use the Widgets below to verify that all events fire in all "
        + "browsers.  To test an event, perform the action require to trigger "
        + "the event using the provided widget.";
  }

  @Override
  public String getSummary() {
    return "Events fire in current browsers";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

  /**
   * Add a test that is dependent on a previous test.
   * 
   * @param eventType the type of event defined in {@link Event}
   * @param eventName the name of the event
   */
  private void addDependentTest(int eventType, String eventName) {
    // Find the last test
    int numRows = layout.getRowCount();
    eventMap.put(new Integer(eventType), new Integer(numRows));
    for (int i = numRows - 1; i > 0; i--) {
      if (layout.getCellCount(i) == 3) {
        FlexCellFormatter formatter = layout.getFlexCellFormatter();
        int rowSpan = formatter.getRowSpan(i, 0);
        formatter.setRowSpan(i, 0, rowSpan + 1);
        break;
      }
    }
    layout.setText(numRows, 0, eventName);
    layout.setText(numRows, 1, "?");
  }

  /**
   * Add a new test that tests one or more events.
   * 
   * @param eventType the type of event defined in {@link Event}
   * @param eventName the name of the event
   * @param trigger the widget that triggers the events
   */
  private void addTest(int eventType, String eventName, Widget trigger) {
    int numRows = layout.getRowCount();
    eventMap.put(new Integer(eventType), new Integer(numRows));
    layout.setWidget(numRows, 0, trigger);
    layout.setText(numRows, 1, eventName);
    layout.setText(numRows, 2, "?");

    FlexCellFormatter formatter = layout.getFlexCellFormatter();
    formatter.setVerticalAlignment(numRows, 0,
        HasVerticalAlignment.ALIGN_MIDDLE);
    formatter.setHorizontalAlignment(numRows, 0,
        HasHorizontalAlignment.ALIGN_CENTER);
  }

  /**
   * Mark the event as passed.
   * 
   * @param event the event that was triggered
   */
  private void passTest(Event event) {
    int eventType = DOM.eventGetType(event);
    int rowIndex = eventMap.get(new Integer(eventType));
    if (layout.getCellCount(rowIndex) == 3) {
      layout.setHTML(rowIndex, 2, "pass");
    } else {
      layout.setHTML(rowIndex, 1, "pass");
    }
  }
}
