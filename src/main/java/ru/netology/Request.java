package ru.netology;

public class Request {
    private final String method;
    private final String headers;
    private final String version;

    public Request(String method, String headers, String version) {
        this.method = method;
        this.headers = headers;
        this.version = version;
    }

    public String getMethod() {
        return method;
    }

    public String getHeaders() {
        return headers;
    }

    public String getVersion() {
        return version;
    }
}
