package ru.netology;

public class Request {
    public String method;
    public String headers;
    public String body;

    public Request(String method, String headers) {
        this.method = method;
        this.headers = headers;
    }

    public Request(String method, String headers, String body) {
        this(method, headers);
        this.body = body;
    }
}
