import java.io.*;
import java.net.*;
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
                System.out.println("[" + new java.text.SimpleDateFormat("HH:mm:ss").format(new Date()) +
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

class ClientHandler extends Thread {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private String clientName;

    public ClientHandler(Socket socket, String name) {
        this.clientSocket = socket;
        this.clientName = name;

        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            dataIn = new DataInputStream(clientSocket.getInputStream());
            dataOut = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            System.err.println("Error setting up streams: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        String message;

        try {
            sendMessage("SERVER: Welcome " + clientName + "! You are now connected.");
            ChatServer.broadcast("SERVER: " + clientName + " has joined the chat!", this);

            while ((message = in.readLine()) != null) {
                if (message.startsWith("FILE:")) {
                    handleFileTransfer(message.substring(5));
                } else {
                    System.out.println("[" + clientName + "] " + message);
                    ChatServer.broadcast(clientName + ": " + message, this);
                }
            }

        } catch (IOException e) {
            System.out.println(clientName + " disconnected");
        } finally {
            cleanup();
        }
    }

    private void handleFileTransfer(String metadata) {
        try {
            String[] parts = metadata.split("\\|");
            String fileName = parts[0];
            long fileSize = Long.parseLong(parts[1]);
            String sender = clientName;

            System.out.println("[FILE] " + sender + " is sending: " + fileName + " (" + (fileSize/1024) + " KB)");

            File downloadDir = new File(ChatServer.UPLOAD_DIR);
            downloadDir.mkdir();

            String filePath = ChatServer.UPLOAD_DIR + File.separator + System.currentTimeMillis() + "_" + fileName;
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                byte[] buffer = new byte[4096];
                long totalRead = 0;
                int bytesRead;

                while (totalRead < fileSize && (bytesRead = dataIn.read(buffer, 0, (int) Math.min(4096, fileSize - totalRead))) > 0) {
                    fos.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }
            }

            System.out.println("[FILE] Saved: " + filePath);
            ChatServer.broadcast("SERVER: " + sender + " shared file: " + fileName, this);

        } catch (Exception e) {
            System.err.println("File transfer error: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    private void cleanup() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (dataIn != null) dataIn.close();
            if (dataOut != null) dataOut.close();
            if (clientSocket != null) clientSocket.close();

            ChatServer.removeClient(this);
            ChatServer.broadcast("SERVER: " + clientName + " left the chat.", this);

        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }

    public String getClientName() {
        return clientName;
    }
}