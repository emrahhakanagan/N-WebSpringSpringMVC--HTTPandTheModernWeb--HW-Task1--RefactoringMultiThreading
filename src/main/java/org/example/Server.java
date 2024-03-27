package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final ExecutorService threadPool = Executors.newFixedThreadPool(64);
    private final Map<String, Map<String, Handler>> handlers = new HashMap<>();
    private ServerPropertiesLoading serverPropertiesLoading;
    private int port;

    public Server() {
        serverPropertiesLoading = new ServerPropertiesLoading();
        this.port = serverPropertiesLoading.getPort();
    }

    public void addHandler(String method, String path, Handler handler) {
        handlers.computeIfAbsent(method, k -> new HashMap<>()).put(path, handler);
    }

    public void listen() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                threadPool.submit(() -> handleConnection(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void handleConnection(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())) {

            Request request = parseRequest(in);
            if (request == null) {
                System.out.println("in is closed");
                in.close();
            }

            Handler handler = findHandler(request);
            if (handler != null) {
                handler.handle(request, out);
            } else {
                sendNotFound(out);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private Request parseRequest(BufferedReader reader) throws IOException {
        String requestLine = reader.readLine();

        if (requestLine == null) {
            throw new IOException("Client closed the connection.");
        }
//        System.out.println("REQUEST LINE --->>> " + requestLine);

        if (requestLine.isEmpty()) {
            throw new IOException("Received an empty request line");
        }

        String[] parts = requestLine.split(" ");
//        System.out.println("parts 0 -> " + parts[0] + " / " + " parts 1 -> " + parts[1] + " parts 2 -> " + parts[2] );

        if (parts.length != 3) {
            throw new IOException("Invalid request line: " + requestLine);
        }

        String method = parts[0];
        String path = parts[1];
        Map<String, String> headers = new HashMap<>();

        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            String[] headerParts = line.split(": ");
            if (headerParts.length != 2) {
                continue; // Пропустить некорректный заголовок
            }
            headers.put(headerParts[0].trim(), headerParts[1].trim());
        }

        // Примечание: теперь мы передаем BufferedReader в Request
        return new Request(method, path, headers, reader);
    }


    private Handler findHandler(Request request) {
        Map<String, Handler> methodHandlers = handlers.get(request.getMethod());

        // Ищем путь, не полный, а только сам путь без QS
        return methodHandlers.getOrDefault(request.getPathWithoutQS(), null);
    }

    private void sendNotFound(BufferedOutputStream out) throws IOException {
        String response = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n";
        out.write(response.getBytes());
        out.flush();
    }

}
