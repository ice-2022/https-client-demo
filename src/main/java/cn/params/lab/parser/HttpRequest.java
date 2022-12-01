package cn.params.lab.parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private String protocol;
    private String host;
    private int port;
    private String path;
    private final Map<String, String> headers = new HashMap<>();

    public String string() {
        StringBuilder headerContent = new StringBuilder();
        headers.forEach((name, value) -> {
            headerContent.append(name).append(": ").append(value).append("\r\n");
        });
        return "GET " + path + " HTTP/1.1\r\n" + headerContent + "\r\n";
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
