package com.multichat.server;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatServer {
    private static final int PORT = 1234;
    private static List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    public static final String UPLOAD_DIR = "downloads";

    public static void main(String[] args) {
        new File(UPLOAD_DIR).mkdir();
        System.out.println("=== Chat Server Starting ===");

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server listening on port " + PORT);
            System.out.println("Downloads folder: " + new File(UPLOAD_DIR).getAbsolutePath());

            int clientCount = 0;
            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientCount++;
                String clientName = "User" + clientCount;
                System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) +
                                 "] New client: " + clientName + " from " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler handler = new ClientHandler(clientSocket, clientName);
                clients.add(handler);
                handler.start();
                System.out.println("Total clients: " + clients.size());
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public static void broadcast(String message, ClientHandler sender) {
        synchronized(clients) {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }

    public static List<ClientHandler> getClients() {
        return new ArrayList<>(clients);
    }

    public static void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Client disconnected. Total: " + clients.size());
    }
}
