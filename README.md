# Multichat Application

A Java-based real-time chat application that allows multiple clients to communicate via a central server with file-sharing capabilities.

## Features

- **Multi-Client Chat**: Connect multiple clients to a single server for real-time messaging
- **File Sharing**: Send and receive files (up to 100MB) between clients
- **GUI Interface**: User-friendly Swing-based client interface
- **Connection Status**: Real-time status indicator showing connection state
- **Timestamped Messages**: All messages are timestamped for easy tracking
- **Automatic Broadcast**: Messages are broadcast to all connected clients

## Components

### ChatServer.java
- Runs on `localhost:1234`
- Manages multiple client connections
- Broadcasts messages between clients
- Saves shared files to a `downloads` folder
- Thread-safe client list management

### ChatClient.java
- GUI client to connect to the server
- Send text messages
- Share files via file chooser dialog
- View incoming messages with timestamps
- Connection status indicator

## How to Run

### Start the Server
```bash
javac ChatServer.java
java ChatServer
```
The server will start listening on port 1234.

### Start the Client
```bash
javac ChatClient.java
java ChatClient
```
The client GUI will launch and automatically connect to `localhost:1234`.

## Usage

1. Start the server first
2. Launch one or more client instances
3. Type messages and press Send (or Enter) to chat
4. Click "Send File" to share files with other clients
5. Files are saved to the `downloads` folder on the server

## Requirements

- Java 8 or higher
- Network connectivity between clients and server (default: localhost:1234)
