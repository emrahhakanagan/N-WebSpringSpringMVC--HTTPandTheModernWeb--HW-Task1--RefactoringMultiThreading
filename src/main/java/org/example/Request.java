package org.example;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;

import java.io.BufferedReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class Request {
    private final String method;
    private final String path;
    private final String pathWithoutQS;
    private final Map<String, String> headers;
    private final BufferedReader body;
    private final List<NameValuePair> params;

    public Request(String method, String path, Map<String, String> headers, BufferedReader body) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;
        this.pathWithoutQS = getOnlyPathWithoutQS(path);
        this.params = parsQueryString(path);
    }

    private List<NameValuePair> parsQueryString(String path) {
        try {
            return URLEncodedUtils.parse(new URI(path), Charset.forName("UTF-8"));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public List<NameValuePair> getQueryParams() {
        return params;
    }

    private String getOnlyPathWithoutQS(String path) {
        String[] parts = path.split("\\?", 2);
        return parts[0];
    }

    public String getQueryParam(String name) {
        return params.stream()
                .filter(param -> name.equals(param.getName()))
                .findFirst()
                .map(NameValuePair::getValue)
                .orElse(null);
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public BufferedReader getBody() {
        return body;
    }

    public String getPathWithoutQS() {
        return pathWithoutQS;
    }
}

