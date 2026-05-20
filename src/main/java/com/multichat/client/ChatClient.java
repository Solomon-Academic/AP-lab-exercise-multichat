package com.multichat.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatClient extends JFrame {
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton fileButton;
    private JLabel statusLabel;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1234;
    private static final int MAX_FILE_SIZE = 100 * 1024 * 1024;

    public ChatClient() {
        initializeGUI();
        connectToServer();
    }

    private void initializeGUI() {
        setTitle("Chat Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        chatArea.setBackground(new Color(245, 245, 245));

        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        messageField = new JTextField();
        messageField.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        sendButton = new JButton("Send");
        fileButton = new JButton("Send File");
        buttonPanel.add(sendButton);
        buttonPanel.add(fileButton);

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        statusLabel = new JLabel("Connecting...");
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 10));
        statusLabel.setForeground(Color.GRAY);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(new EmptyBorder(0, 5, 5, 5));
        bottomPanel.add(inputPanel, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        fileButton.addActionListener(e -> sendFile());
        messageField.addActionListener(e -> sendMessage());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnectFromServer();
            }
        });

        setVisible(true);
        messageField.requestFocusInWindow();
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            dataIn = new DataInputStream(socket.getInputStream());
            dataOut = new DataOutputStream(socket.getOutputStream());

            appendToChat("Connected to server at " + SERVER_ADDRESS + ":" + SERVER_PORT);
            updateStatus("Connected", Color.GREEN);

            new Thread(new MessageReceiver()).start();

        } catch (UnknownHostException e) {
            appendToChat("Error: Cannot find server at " + SERVER_ADDRESS);
            updateStatus("Connection failed", Color.RED);
            disableInput();
        } catch (IOException e) {
            appendToChat("Error: Cannot connect to server - " + e.getMessage());
            updateStatus("Connection failed", Color.RED);
            disableInput();
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();

        if (message.isEmpty()) {
            return;
        }

        if (out != null) {
            out.println(message);

            if (out.checkError()) {
                appendToChat("Error: Message could not be sent.");
                return;
            }

            messageField.setText("");
            messageField.requestFocusInWindow();
        } else {
            appendToChat("Error: Not connected to server.");
        }
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = fileChooser.getSelectedFile();
        long fileSize = selectedFile.length();

        if (fileSize > MAX_FILE_SIZE) {
            JOptionPane.showMessageDialog(this, "File too large! Max: 100MB", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new Thread(() -> transferFile(selectedFile, fileSize)).start();
    }

    private void transferFile(File file, long fileSize) {
        try {
            fileButton.setEnabled(false);
            updateStatus("Sending: " + file.getName() + " (" + (fileSize/1024) + " KB)", Color.BLUE);

            String metadata = "FILE:" + file.getName() + "|" + fileSize;
            out.println(metadata);

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalSent = 0;

                while ((bytesRead = fis.read(buffer)) > 0) {
                    dataOut.write(buffer, 0, bytesRead);
                    totalSent += bytesRead;
                    int progress = (int) ((totalSent * 100) / fileSize);
                    updateStatus("Sending: " + file.getName() + " - " + progress + "%", Color.BLUE);
                }
                dataOut.flush();
            }

            appendToChat("[You shared: " + file.getName() + "]");
            updateStatus("Connected", Color.GREEN);
            fileButton.setEnabled(true);

        } catch (IOException e) {
            appendToChat("Error sending file: " + e.getMessage());
            updateStatus("Error", Color.RED);
            fileButton.setEnabled(true);
        }
    }

    private void disconnectFromServer() {
        try {
            appendToChat("Disconnecting...");

            if (out != null) out.close();
            if (in != null) in.close();
            if (dataIn != null) dataIn.close();
            if (dataOut != null) dataOut.close();
            if (socket != null) socket.close();

        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }

    private void appendToChat(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String formatted = "[" + timestamp + "] " + message;

        SwingUtilities.invokeLater(() -> {
            chatArea.append(formatted + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void updateStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(text);
            statusLabel.setForeground(color);
        });
    }

    private void disableInput() {
        messageField.setEnabled(false);
        sendButton.setEnabled(false);
        fileButton.setEnabled(false);
    }

    private class MessageReceiver implements Runnable {
        @Override
        public void run() {
            String message;

            try {
                while ((message = in.readLine()) != null) {
                    appendToChat(message);
                }

            } catch (IOException e) {
                appendToChat("Disconnected from server.");
                disableInput();
                updateStatus("Disconnected", Color.RED);

            } finally {
                appendToChat("Connection closed.");
                disableInput();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClient());
    }
}
