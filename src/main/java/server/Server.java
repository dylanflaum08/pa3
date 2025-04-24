package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Server {
    public static final String BASE_DIR = "files" + System.getProperty("file.separator");
    public static final List<Long> rtts = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: java server.Server <port_number>");
            return;
        }
        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);
            while (true) {
                Socket sock = serverSocket.accept();
                System.out.println("Client connected: " + sock.getInetAddress());
                new ClientHandler(sock, rtts).start(); 
            }
        }
    }
}
