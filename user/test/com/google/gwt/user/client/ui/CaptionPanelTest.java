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

import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests {@link CaptionPanel}.
 */
public class CaptionPanelTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testHasWidgets() {
    // With no caption.
    HasWidgetsTester.testAll(new CaptionPanel());

    // With a text caption.
    HasWidgetsTester.testAll(new CaptionPanel("some text"));

    // With a complex HTML caption.
    HasWidgetsTester.testAll(new CaptionPanel("<legend>not the <i>actual</i> legend<legend>", true));
  }

  public void testCaptionAcceptsEmptyStringAndRemovesLegendElement() {
    // Ctor/HTML.
    {
      CaptionPanel panel = new CaptionPanel("");
      assertEquals("", panel.getCaptionHTML());
      assertNull(panel.getElement().getFirstChild());
    }

    // Setter/HTML.
    {
      CaptionPanel panel = new CaptionPanel("not null");
      assertEquals("not null", panel.getCaptionHTML());

      panel.setCaptionHTML("");
      assertEquals("", panel.getCaptionHTML());
      assertNull(panel.getElement().getFirstChild());
    }

    // Setter/Text.
    {
      CaptionPanel panel = new CaptionPanel("not null");
      assertEquals("not null", panel.getCaptionHTML());

      panel.setCaptionText("");
      assertEquals("", panel.getCaptionHTML());
      assertNull(panel.getElement().getFirstChild());
    }
  }

  /**
   * When the caption is null, it needs to be actually removed from the DOM (to
   * compensate for browser bugs). This formulation requires no widget to have
   * been set first.
   */
  public void testCaptionAssertsAgainstNull() {
    // Ctor.
    {
      try {
        new CaptionPanel(null);
        fail("Should've asserted!");
      } catch (AssertionError e) {
        // good to make it here
      }
    }

    // Setter/HTML.
    {
      try {
        CaptionPanel panel = new CaptionPanel("stuff");
        panel.setCaptionHTML(null);
        fail("Should've asserted!");
      } catch (AssertionError e) {
        // good to make it here
      }
    }

    // Setter/Text.
    {
      try {
        CaptionPanel panel = new CaptionPanel("stuff");
        panel.setCaptionText(null);
        fail("Should've asserted!");
      } catch (AssertionError e) {
        // good to make it here
      }
    }
  }

  public void testCtorAsHtmlFlag() {
    String s = "this is <b>not</b> null";

    // Ctor/Text.
    {
      CaptionPanel panel = new CaptionPanel(s, false);
      assertEquals(s, panel.getCaptionText());
      assertFalse(s.equals(panel.getCaptionHTML()));
    }

    // Ctor/HTML.
    {
      CaptionPanel panel = new CaptionPanel(s, true);
      assertEquals("this is not null", panel.getCaptionText());
      assertEquals(s, panel.getCaptionHTML());
    }
  }

  public void testDefaultCaptionIsEmptyString() {
    CaptionPanel panel = new CaptionPanel();
    assertEquals("", panel.getCaptionText());
    assertEquals("", panel.getCaptionHTML());
    // Wigets may be supported in the future.
    // assertNull(panel.getCaptionWidget());
    assertNull(panel.getElement().getFirstChild());
  }

  public void testGetSetHTMLCaption() {
    CaptionPanel panel = new CaptionPanel();
    panel.setCaptionHTML("<b>bold</b>");
    assertEquals("<b>bold</b>", panel.getCaptionHTML());
    assertEquals("bold", panel.getCaptionText());
  }

  public void testGetSetTextCaption() {
    String s = "this is <b>not</b> null";
    CaptionPanel panel = new CaptionPanel();
    panel.setCaptionText(s);
    assertEquals(s, panel.getCaptionText());
    assertFalse(s.equals(panel.getCaptionHTML()));
  }

  public void testOneArgCtorIsTextCaption() {
    String s = "this is <b>not</b> null";
    CaptionPanel panel = new CaptionPanel(s);
    assertEquals(s, panel.getCaptionText());
    assertFalse(s.equals(panel.getCaptionHTML()));
  }

  public void testGetSetContentWidget() {
    {
      // No Widget set in the default ctor.
      CaptionPanel panel = new CaptionPanel("no widget");
      assertNull(panel.getContentWidget());
    }

    {
      // Set widget and remove() it.
      CaptionPanel panel = new CaptionPanel("no widget yet");
      assertNull(panel.getContentWidget());

      HTML widget = new HTML("widget");
      panel.setContentWidget(widget);
      assertSame(widget, panel.getContentWidget());

      panel.remove(widget);
      assertNull(panel.getContentWidget());
    }

    {
      // Set widget and clear() it.
      CaptionPanel panel = new CaptionPanel("no widget yet");
      assertNull(panel.getContentWidget());

      HTML widget = new HTML("widget");
      panel.setContentWidget(widget);
      assertSame(widget, panel.getContentWidget());

      panel.clear();
      assertNull(panel.getContentWidget());
    }

    {
      // Set widget and replace it.
      CaptionPanel panel = new CaptionPanel("no widget yet");
      assertNull(panel.getContentWidget());

      HTML widget1 = new HTML("widget1");
      panel.setContentWidget(widget1);
      assertSame(widget1, panel.getContentWidget());

      HTML widget2 = new HTML("widget2");
      panel.setContentWidget(widget2);
      assertSame(widget2, panel.getContentWidget());
    }
  }

  public void testGetSetCaptionAmidstContentWidget() {
    CaptionPanel panel = new CaptionPanel("caption");
    HTML widget = new HTML("widget");
    panel.setContentWidget(widget);
    assertEquals("caption", panel.getCaptionText());
    assertSame(widget, panel.getContentWidget());

    {
      // Set the caption to an empty string and verify that the legend element is removed
      panel.setCaptionText("");
      assertEquals("", panel.getCaptionText());
      assertSame(widget, panel.getContentWidget());
      Element panelFirstChild = panel.getElement().getFirstChildElement();
      // The legend element ought to be removed from the DOM at this point.
      assertFalse("legend".equalsIgnoreCase(panelFirstChild.getTagName()));
      // (Perhaps redundantly) check that the one child is the content widget.
      assertSame(panelFirstChild, widget.getElement());
      assertNull(panelFirstChild.getNextSibling());
    }

    {
      // Set the caption to a non-empty string and verify that the legend element is readded to the
      // 0th index
      panel.setCaptionText("new caption");
      assertEquals("new caption", panel.getCaptionText());
      assertSame(widget, panel.getContentWidget());
      Element panelFirstChild = panel.getElement().getFirstChildElement();
      // The legend element ought to be the 0th element in the DOM at this point.
      assertTrue("legend".equalsIgnoreCase(panelFirstChild.getTagName()));
      // Check that the second child is the content widget.
      assertSame(panelFirstChild.getNextSibling(), widget.getElement());
    }

    {
      // Set the caption to a non-empty string and verify that the legend element remains at the 0th
      // index
      panel.setCaptionText("newer caption");
      assertEquals("newer caption", panel.getCaptionText());
      assertSame(widget, panel.getContentWidget());
      Element panelFirstChild = panel.getElement().getFirstChildElement();
      // The legend element ought to be the 0th element in the DOM at this point.
      assertTrue("legend".equalsIgnoreCase(panelFirstChild.getTagName()));
      // Check that the second child is the content widget.
      assertSame(panelFirstChild.getNextSibling(), widget.getElement());
    }

    {
      // Remove the widget and verify the caption remains at the 0th index
      panel.remove(widget);
      assertEquals("newer caption", panel.getCaptionText());
      assertNull(panel.getContentWidget());
      Element panelFirstChild = panel.getElement().getFirstChildElement();
      // The legend element ought to be the 0th element in the DOM at this point.
      assertTrue("legend".equalsIgnoreCase(panelFirstChild.getTagName()));
      // (Perhaps redundantly) check that the one child is the legend element.
      assertNull(panelFirstChild.getNextSibling());
    }
  }

}
