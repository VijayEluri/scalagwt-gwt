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
package com.google.gwt.jsonp.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * A JSONP request that is waiting for a response. The request can be cancelled.
 * 
 * @param <T> the type of the response object.
 */
public class JsonpRequest<T> {

  /**
   * Each request will be assigned a new id.
   */
  private static int callbackCounter = 0;

  private static native Node getDocumentElement() /*-{
    return $doc.documentElement;
  }-*/;
  
  private static String nextCallbackId() {
    return "__jsonp" + (callbackCounter++) + "__";
  }
  
  private final String callbackId;

  private final int timeout;
  
  private final AsyncCallback<T> callback;
  
  /**
   * Whether the result is expected to be an integer or not
   */
  @SuppressWarnings("unused") // used by JSNI
  private final boolean expectInteger;

  private final String callbackParam;

  private final String failureCallbackParam;

  /**
   * Timer which keeps track of timeouts.
   */
  private Timer timer;

  /**
   * Create a new JSONP request.
   * 
   * @param callback The callback instance to notify when the response comes
   *          back
   * @param timeout Time in ms after which a {@link TimeoutException} will be
   *          thrown
   * @param expectInteger Should be true if T is {@link Integer}, false
   *          otherwise
   * @param callbackParam Name of the url param of the callback function name
   * @param failureCallbackParam Name of the url param containing the the
   *          failure callback function name, or null for no failure callback
   */
  JsonpRequest(AsyncCallback<T> callback, int timeout, boolean expectInteger,
      String callbackParam, String failureCallbackParam) {
    callbackId = nextCallbackId();
    this.callback = callback;
    this.timeout = timeout;
    this.expectInteger = expectInteger;
    this.callbackParam = callbackParam;
    this.failureCallbackParam = failureCallbackParam;
  }

  /**
   * Cancels a pending request.
   */
  public void cancel() {
    timer.cancel();
    unload();
  }

  public AsyncCallback<T> getCallback() {
    return callback;
  }

  public int getTimeout() {
    return timeout;
  }

  /**
   * Sends a request using the JSONP mechanism.
   * 
   * @param baseUri To be sent to the server.
   */
  void send(final String baseUri) {
    registerCallbacks();
    StringBuffer uri = new StringBuffer(baseUri);
    uri.append(baseUri.contains("?") ? "&" : "?");
    uri.append(callbackParam).append("=").append(callbackId).append(
        ".onSuccess");
    if (failureCallbackParam != null) {
      uri.append("&");
      uri.append(failureCallbackParam).append("=").append(callbackId).append(
          ".onFailure");
    }
    ScriptElement script = Document.get().createScriptElement();
    script.setType("text/javascript");
    script.setId(callbackId);
    script.setSrc(uri.toString());
    getDocumentElement().getFirstChild().appendChild(script);
    timer = new Timer() {
      @Override
      public void run() {
        onFailure(new TimeoutException("Timeout while calling " + baseUri));
      }
    };
    timer.schedule(timeout);
  }

  @SuppressWarnings("unused") // used by JSNI
  private void onFailure(String message) {
    onFailure(new Exception(message));
  }

  private void onFailure(Throwable ex) {
    timer.cancel();
    try {
      if (callback != null) {
        callback.onFailure(ex);
      }
    } finally {
      unload();
    }
  }

  @SuppressWarnings("unused") // used by JSNI
  private void onSuccess(T data) {
    timer.cancel();
    try {
      if (callback != null) {
        callback.onSuccess(data);
      }
    } finally {
      unload();
    }
  }

  /**
   * Registers the callback methods that will be called when the JSONP response
   * comes back. 2 callbacks are created, one to return the value, and one to
   * notify a failure.
   */
  private native void registerCallbacks() /*-{
    var self = this;
    var callback = new Object();
    $wnd[this.@com.google.gwt.jsonp.client.JsonpRequest::callbackId] = callback;
    callback.onSuccess = function(data) {
      // Box primitive types
      if (typeof data == 'boolean') {
        data = @java.lang.Boolean::new(Z)(data);
      } else if (typeof data == 'number') {
        if (self.@com.google.gwt.jsonp.client.JsonpRequest::expectInteger) {
          data = @java.lang.Integer::new(I)(data);
        } else {
          data = @java.lang.Double::new(D)(data);
        }
      }
      self.@com.google.gwt.jsonp.client.JsonpRequest::onSuccess(Ljava/lang/Object;)(data);
    };
    if (this.@com.google.gwt.jsonp.client.JsonpRequest::failureCallbackParam) {
      callback.onFailure = function(message) {
        self.@com.google.gwt.jsonp.client.JsonpRequest::onFailure(Ljava/lang/String;)(message);
      };
    }
  }-*/;

  /**
   * Cleans everything once the response has been received: deletes the script
   * tag, unregisters the callback.
   */
  private void unload() {
    /*
     * Some browsers (IE7) require the script tag to be deleted outside the
     * scope of the script itself. Therefore, we need to defer the delete
     * statement after the callback execution.
     */
    DeferredCommand.addCommand(new Command() {
      public void execute() {
        unregisterCallbacks();
        Node script = Document.get().getElementById(callbackId);
        getDocumentElement().getFirstChild().removeChild(script);
      }
    });
  }

  private native void unregisterCallbacks() /*-{
    delete $wnd[this.@com.google.gwt.jsonp.client.JsonpRequest::callbackId];
  }-*/;
}
