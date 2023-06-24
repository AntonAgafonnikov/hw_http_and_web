package ru.netology;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        final var server = new Server();

        // добавление хендлеров (обработчиков)
        server.addHandler("GET", "/messages", (request, responseStream) -> {
            String text = "Hello from GET!";
            responseStream.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + "text/plain" + "\r\n" +
                            "Content-Length: " + text.length() + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            responseStream.write(text.getBytes());
        });

        server.addHandler("POST", "/messages", (request, responseStream) -> {
            String text = "Hello from POST!";
            responseStream.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + "text/plain" + "\r\n" +
                            "Content-Length: " + text.length() + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            responseStream.write(text.getBytes());
        });

        server.startServer();
    }
}
