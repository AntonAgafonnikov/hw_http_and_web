package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static final int PORT = 9999;
    private final static int NUMBER_THREADS = 64;
    static final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png",
            "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html",
            "/events.html", "/events.js");
    private final static ExecutorService threadPool = Executors.newFixedThreadPool(NUMBER_THREADS);
    private ConcurrentMap<ConcurrentMap<String, String>, Handler> handlers = new ConcurrentHashMap<>();

    public void startServer() throws IOException, InterruptedException {
        try (final var serverSocket = new ServerSocket(PORT)) {
            //TODO
            System.out.println("SIZE = " + handlers.size());
            for (ConcurrentMap.Entry<ConcurrentMap<String, String>, Handler> entryMapHandler : handlers.entrySet()) {
                ConcurrentMap<String, String> nestedMap = entryMapHandler.getKey();
                for (ConcurrentMap.Entry<String, String> entry : nestedMap.entrySet()) {
                    if ("GET".equals(entry.getKey())) {
                        if ("/messages".equals(entry.getValue())) {
                            System.out.println("ОНИ ТАМ ЕСТЬ!");
                        } else {
                            System.out.println("ИХ ТАМ НЕТ!");
                        }
                    } else {
                        System.out.println("ТУТ БЛЯТЬ ДАЖ ГЕТА НЕТУ");
                    }
                }
            }
            //TODO
            while (true) {
                try (var waitingConnection = serverSocket.accept()) {
                    threadPool.execute(connectionProcessing(serverSocket));
                    Thread.sleep(30);
                } catch (IOException e) {
                    threadPool.shutdown();
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    public Runnable connectionProcessing(ServerSocket serverSocket) throws IOException {
        return () -> {
            try (var socket = serverSocket.accept()) {
                var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                var out = new BufferedOutputStream(socket.getOutputStream());

                final var requestLine = in.readLine();
                final var parts = requestLine.split(" ");

                if (parts.length != 3) {
                    in.close();
                    out.close();
                    return;
                }

                Request request = new Request(parts[0], parts[1], parts[2]); //TODO
                for (ConcurrentMap.Entry<ConcurrentMap<String, String>, Handler> entryMapHandler : handlers.entrySet()) {
                    ConcurrentMap<String, String> nestedMap = entryMapHandler.getKey();
                    for (ConcurrentMap.Entry<String, String> entry : nestedMap.entrySet()) {
                        if (request.method.equals(entry.getKey())) {
                            if (request.headers.equals(entry.getValue())) {
                                entryMapHandler.getValue().handle(request, out);
                            }
                        }
                    }
                }

                final var path = parts[1];
                if (!validPaths.contains(path)) {
                    out.write((
                            "HTTP/1.1 404 Not Found\r\n" +
                                    "Content-Length: 0\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.flush();
                    in.close();
                    out.close();
                    return;
                }

                final var filePath = Path.of(".", "public", path);
                final var mimeType = Files.probeContentType(filePath);

                // special case for classic
                if (path.equals("/classic.html")) {
                    final var template = Files.readString(filePath);
                    final var content = template.replace(
                            "{time}",
                            LocalDateTime.now().toString()
                    ).getBytes();
                    out.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + mimeType + "\r\n" +
                                    "Content-Length: " + content.length + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.write(content);
                    out.flush();
                    in.close();
                    out.close();
                    return;
                }

                final var length = Files.size(filePath);
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                Files.copy(filePath, out);
                out.flush();
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }

    public void addHandler(String method, String path, Handler handler) {
        ConcurrentMap<String, String> methodAndPathMap = new ConcurrentHashMap<>();
        methodAndPathMap.put(method, path);

        for (ConcurrentMap.Entry<ConcurrentMap<String, String>, Handler> entryMapHandler : handlers.entrySet()) {
            ConcurrentMap<String, String> nestedMap = entryMapHandler.getKey();
            for (ConcurrentMap.Entry<String, String> entryStringString : nestedMap.entrySet()) {
                if (!method.equals(entryStringString.getKey())) {
                    if (!path.equals(entryStringString.getValue())) {
                        handlers.put(methodAndPathMap, handler);
                    }
                }
            }
        }
    }
}
