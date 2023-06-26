package ru.netology;


import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

public class Request {
    private String method;
    private String path;
    private String version;
    private List<String> headers;
    private String body;
    private List<NameValuePair> queryParams;

    public Request(BufferedInputStream in) {
        createRequest(in);
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getVersion() {
        return version;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public List<NameValuePair> getQueryParams() {
        return queryParams;
    }

    public List<NameValuePair> getQueryParam(String name) {
        return queryParams
                .stream()
                .filter(o -> Objects.equals(o.getName(), name))
                .collect(Collectors.toList());
    }

    private void createRequest(BufferedInputStream in) {
        // лимит на request line + заголовки
        final var limit = 4096;

        in.mark(limit);
        final var buffer = new byte[limit];
        final int read;
        try {
            read = in.read(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // ищем request line
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);

        // читаем request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        this.method = requestLine[0];
        final var pathAndQuery = requestLine[1];
        this.path = pathAndQuery;
        if (pathAndQuery.contains("?")) {
            this.path = pathAndQuery.split("\\?")[0];
        }
        // Используем библиотеку Apache для парсинга
        try {
            this.queryParams = new URIBuilder(pathAndQuery).getQueryParams();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        this.version = requestLine[2];

        // ищем заголовки
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);

        // отматываем на начало буфера
        try {
            in.reset();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // пропускаем requestLine
        try {
            in.skip(headersStart);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final byte[] headersBytes;
        try {
            headersBytes = in.readNBytes(headersEnd - headersStart);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.headers = Arrays.asList(new String(headersBytes).split("\r\n"));

        // для GET тела нет
        String body = null;
        if (!method.equals("GET")) {
            try {
                in.skip(headersDelimiter.length);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // вычитываем Content-Length, чтобы прочитать body
            final var contentLength = extractHeader(headers, "Content-Length");
            if (contentLength.isPresent()) {
                final var length = Integer.parseInt(contentLength.get());
                final byte[] bodyBytes;
                try {
                    bodyBytes = in.readNBytes(length);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                body = new String(bodyBytes);
            }
        }
        this.body = body;
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}