package ru.netology;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        final var server = new Server();

        // добавление хендлеров (обработчиков)
        server.addHandler("GET", "/messages", (request, responseStream) ->
                System.out.println("GET request done!"));
        server.addHandler("POST", "/messages", (request, responseStream) ->
                System.out.println("POST request done!"));

        server.startServer();


    }
}
