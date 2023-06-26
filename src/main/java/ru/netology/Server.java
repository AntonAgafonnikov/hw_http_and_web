package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static final int PORT = 9999;
    private static final int NUMBER_THREADS = 64;
    private final static ExecutorService threadPool = Executors.newFixedThreadPool(NUMBER_THREADS);
    private final ConcurrentMap<ConcurrentMap<String, String>, Handler> handlers = new ConcurrentHashMap<>();

    public void startServer() throws IOException, InterruptedException {
        try (final var serverSocket = new ServerSocket(PORT)) {
            while (true) {
                try (var ignored = serverSocket.accept()) {
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
            try (
                    final var socket = serverSocket.accept();
                    final var in = new BufferedInputStream(socket.getInputStream());
                    final var out = new BufferedOutputStream(socket.getOutputStream())
            ) {
                Request request = new Request(in);
                // Поиск и вызов нужного хэндлера
                for (ConcurrentMap.Entry<ConcurrentMap<String, String>, Handler> entryMapHandler : handlers.entrySet()) {
                    ConcurrentMap<String, String> nestedMap = entryMapHandler.getKey();
                    for (ConcurrentMap.Entry<String, String> entry : nestedMap.entrySet()) {
                        if (request.getMethod().equals(entry.getKey())) {
                            if (request.getPath().equals(entry.getValue())) {
                                entryMapHandler.getValue().handle(request, out);
                            }
                        }
                    }
                }
                //Вывод запроса в консоль
                request.getQueryParams().forEach(System.out::println);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }

    public void addHandler(String method, String path, Handler handler) {
        ConcurrentMap<String, String> methodAndPathMap = new ConcurrentHashMap<>();
        methodAndPathMap.put(method, path);

        if (handlers.size() == 0) {
            handlers.put(methodAndPathMap, handler);
            return;
        }

        // Пробегаемся по внутренней мапе.
        // Если хэндлера для такого набора "метод-путь" нет, то добавляем его
        for (ConcurrentMap.Entry<ConcurrentMap<String, String>, Handler> entryMapHandler : handlers.entrySet()) {
            ConcurrentMap<String, String> nestedMap = entryMapHandler.getKey();

            for (ConcurrentMap.Entry<String, String> entryStringString : nestedMap.entrySet()) {
                if (!method.equals(entryStringString.getKey())) {
                    handlers.put(methodAndPathMap, handler);
                    return;
                } else {
                    if (!path.equals(entryStringString.getValue())) {
                        handlers.put(methodAndPathMap, handler);
                    }
                }
            }
        }
    }
}
