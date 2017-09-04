/*
* Copyright (c) 2012-2017 "JUSPAY Technologies"
* JUSPAY Technologies Pvt. Ltd. [https://www.juspay.in]
*
* This file is part of JUSPAY Platform.
*
* JUSPAY Platform is free software: you can redistribute it and/or modify
* it for only educational purposes under the terms of the GNU Affero General
* Public License (GNU AGPL) as published by the Free Software Foundation,
* either version 3 of the License, or (at your option) any later version.
* For Enterprise/Commerical licenses, contact <info@juspay.in>.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  The end user will
* be liable for all damages without limitation, which is caused by the
* ABUSE of the LICENSED SOFTWARE and shall INDEMNIFY JUSPAY for such
* damages, claims, cost, including reasonable attorney fee claimed on Juspay.
* The end user has NO right to claim any indemnification based on its use
* of Licensed Software. See the GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/agpl.html>.
*/

package in.juspay.mystique;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HostnameVerifier;


public class RestClient {

    private static final String LOG_TAG = RestClient.class.getName();
    private static DefaultHttpClient httpClient;
    private static DefaultHttpClient redirectHttpClient;

    static {
        init();
    }

    private static DefaultHttpClient getHttpClient(HttpParams params) {
        SchemeRegistry registry = new SchemeRegistry();

        SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
        HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
        socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);

        registry.register(new Scheme("https", socketFactory, 443));
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        ThreadSafeClientConnManager clientConnManager = new ThreadSafeClientConnManager(
                params, registry);

        return new DefaultHttpClient(clientConnManager, params);
    }

    public static void init() {
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
        HttpConnectionParams.setConnectionTimeout(params, 10 * 1000);
        HttpConnectionParams.setSoTimeout(params, 30 * 1000);
        httpClient = getHttpClient(params);

        HttpParams redirectParams = new BasicHttpParams();
        HttpProtocolParams.setContentCharset(redirectParams, HTTP.UTF_8);
        HttpConnectionParams.setConnectionTimeout(redirectParams, 5 * 1000);
        HttpConnectionParams.setSoTimeout(redirectParams, 10 * 1000);
        redirectHttpClient = getHttpClient(redirectParams);
    }

    public static void changeRedirectHttpClientParams(HttpParams params) {
        if (redirectHttpClient != null) {
            redirectHttpClient.setParams(params);
        }
    }

    public static byte[] post(String url, String body) {

        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(asStringEntity(body));
        return execute(httpPost);
    }

    public static byte[] postData(String url, Map parameters) {
        HttpPost httpPost = new HttpPost(url);
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(mapToNameValuePairs(parameters)));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage());
        }
        return execute(httpPost);
    }

    public static byte[] get(String url) {
        return execute(new HttpGet(url));
    }

    public static byte[] get(String url, Map<String, String> queryParameters) {
        try {
            return get(url + mapToQueryString(queryParameters));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static byte[] post(String url, String body, HashMap<String, String> headers) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        if (headers != null) {
            Iterator it = headers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                httpPost.setHeader((String) pair.getKey(), (String) pair.getValue());
            }
        }
        byte[] data = body.getBytes("UTF-8");
        httpPost.setEntity(new ByteArrayEntity(data));
        return execute(httpPost);
    }

    public static String mapToQueryString(Map<String, String> queryParameters) throws UnsupportedEncodingException {
        StringBuilder string = new StringBuilder();

        if (queryParameters.size() > 0) {
            string.append("?");
        }

        for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
            string.append(entry.getKey());
            string.append("=");
            if (entry.getValue() == null) {
                string.append("");
            } else {
                string.append(URLEncoder.encode(entry.getValue(), "utf-8"));
            }
            string.append("&");
        }

        return string.toString();
    }

    private static byte[] responseHandler(HttpRequestBase request, HttpResponse response) throws IOException {
        try {
            if (isSuccessful(response)) {
                Header hce = response.getEntity().getContentEncoding();
                if (hce != null && hce.getValue().equals("gzip")) {
                    GZIPInputStream zis = new GZIPInputStream(response.getEntity().getContent());
                    byte[] bytes = new byte[1024];
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    int numRead = 0;
                    try {
                        while ((numRead = zis.read(bytes)) >= 0) {
                            out.write(bytes, 0, numRead);
                        }
                        return out.toByteArray();
                    } finally {
                        if (zis != null) {
                            zis.close();
                        }
                    }
                } else {
                    return EntityUtils.toByteArray(response.getEntity());
                }
            } else {
                throw new UnsuccessfulRestCall(request, response);
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage());
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private static byte[] execute(HttpRequestBase request) {
        try {
            request.setHeader("Accept-Encoding", "gzip");
            HttpResponse response = httpClient.execute(request);
            return responseHandler(request, response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean isSuccessful(HttpResponse response) {
        int code = response.getStatusLine().getStatusCode();
        return code >= 200 && code < 300;
    }

    private static HttpEntity asStringEntity(String body) {
        try {
            return new StringEntity(body);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private static List<NameValuePair> mapToNameValuePairs(Map parameters) {
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        for (Object key : parameters.keySet()) {
            if (parameters.get(key) != null) {
                nameValuePairs.add(new BasicNameValuePair(key.toString(), parameters.get(key).toString()));
            }
        }
        return nameValuePairs;
    }

    public static void postZip(String url, String body) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Content-Encoding", "gzip");
        byte[] data = gzipContent(body.getBytes("UTF-8"));
        httpPost.setEntity(new ByteArrayEntity(data));
        execute(httpPost);
    }

    public static byte[] gzipContent(byte[] uncompressed) {
        ByteArrayOutputStream os;
        try {
            os = new ByteArrayOutputStream(uncompressed.length);
            GZIPOutputStream gzos = new GZIPOutputStream(os);
            gzos.write(uncompressed);
            os.close();
            gzos.close();
            return os.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static byte[] fetchIfModified(String url, Map<String, String> connectionHeaders) throws IOException {

        HttpGet httpGet = new HttpGet();
        for (Map.Entry<String, String> entry : connectionHeaders.entrySet()) {
            httpGet.setHeader(entry.getKey(), entry.getValue());
        }
        httpGet.setURI(URI.create(url));
        HttpResponse response = httpClient.execute(httpGet);
        if (response.getStatusLine().getStatusCode() == 200) {
            return responseHandler(httpGet, response);
        } else if (response.getStatusLine().getStatusCode() == 304) {
            return null;
        } else {
            return null;
        }
    }

    public static class UnsuccessfulRestCall extends RuntimeException {

        private final HttpResponse response;
        private final HttpRequestBase request;

        public UnsuccessfulRestCall(HttpRequestBase request, HttpResponse response) {
            super(request.getURI() + " returned " + response.getStatusLine().getStatusCode());
            this.response = response;
            this.request = request;
        }

        public HttpResponse getResponse() {
            return response;
        }

        public HttpRequestBase getRequest() {
            return request;
        }
    }

}

