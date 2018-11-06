/*
 *
 * Copyright (c) 2013 - 2018 Lijun Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xipki.ca.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.xipki.security.SecurityFactory;
import org.xipki.util.IoUtil;
import org.xipki.util.ParamUtil;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

class HttpClientCmpAgent extends ClientCmpAgent {

  private static final String CMP_REQUEST_MIMETYPE = "application/pkixcmp";

  private static final String CMP_RESPONSE_MIMETYPE = "application/pkixcmp";

  private final URL serverUrl;

  private final SSLSocketFactory sslSocketFactory;

  private final HostnameVerifier hostnameVerifier;

  HttpClientCmpAgent(ClientCmpRequestor requestor, ClientCmpResponder responder,
      String serverUrl, SecurityFactory securityFactory,
      SSLSocketFactory sslSocketFactory, HostnameVerifier hostnameVerifier) {
    super(requestor, responder, securityFactory);
    ParamUtil.requireNonBlank("serverUrl", serverUrl);

    this.sslSocketFactory = sslSocketFactory;
    this.hostnameVerifier = hostnameVerifier;
    try {
      this.serverUrl = new URL(serverUrl);
    } catch (MalformedURLException ex) {
      throw new IllegalArgumentException("invalid URL: " + serverUrl);
    }
  }

  @Override
  public byte[] send(byte[] request) throws IOException {
    ParamUtil.requireNonNull("request", request);
    HttpURLConnection httpUrlConnection = IoUtil.openHttpConn(serverUrl);
    if (httpUrlConnection instanceof HttpsURLConnection) {
      if (sslSocketFactory != null) {
        ((HttpsURLConnection) httpUrlConnection).setSSLSocketFactory(sslSocketFactory);
      }

      if (hostnameVerifier != null) {
        ((HttpsURLConnection) httpUrlConnection).setHostnameVerifier(hostnameVerifier);
      }
    }

    httpUrlConnection.setDoOutput(true);
    httpUrlConnection.setUseCaches(false);

    int size = request.length;

    httpUrlConnection.setRequestMethod("POST");
    httpUrlConnection.setRequestProperty("Content-Type", CMP_REQUEST_MIMETYPE);
    httpUrlConnection.setRequestProperty("Content-Length", java.lang.Integer.toString(size));
    OutputStream outputstream = httpUrlConnection.getOutputStream();
    outputstream.write(request);
    outputstream.flush();

    InputStream inputStream = httpUrlConnection.getInputStream();
    if (httpUrlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
      inputStream.close();
      throw new IOException("bad response: " + httpUrlConnection.getResponseCode() + "    "
              + httpUrlConnection.getResponseMessage());
    }

    String responseContentType = httpUrlConnection.getContentType();
    boolean isValidContentType = false;
    if (responseContentType != null) {
      if (responseContentType.equalsIgnoreCase(CMP_RESPONSE_MIMETYPE)) {
        isValidContentType = true;
      }
    }

    if (!isValidContentType) {
      inputStream.close();
      throw new IOException("bad response: mime type " + responseContentType + " not supported!");
    }

    return IoUtil.read(inputStream);
  } // method send

}
