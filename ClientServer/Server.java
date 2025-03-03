package ClientServer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private DatagramSocket socket;
    private ConcurrentHashMap<Integer, Long> clientStatus;
    private ConcurrentHashMap<Integer, String> fileListings; // Maps nodeId to file listing
    private int serverPort;

    public Server() {
        try {
            System.out.println("Server is starting..");

            // Load configuration
            Properties config = new Properties();
            InputStream input = getClass().getClassLoader().getResourceAsStream("ClientServer/server_config.properties");

            if (input == null) {
                throw new FileNotFoundException("server_config.properties not found in resources!");
            }

            config.load(input);
            serverPort = Integer.parseInt(config.getProperty("server_port", "9876"));

            // Initialize server socket
            socket = new DatagramSocket(serverPort);
            clientStatus = new ConcurrentHashMap<>();
            fileListings = new ConcurrentHashMap<>(); // Fix NullPointerException

            System.out.println("Server is listening on port " + serverPort + "...");

            new Thread(this::monitorClients).start(); // Monitor for dead clients
            new Thread(this::broadcastUpdates).start(); // Periodically send updates
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void listenForClients() {
        try {
            byte[] incomingData = new byte[1024];

            while (true) {
                DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                socket.receive(incomingPacket);

                Protocol receivedMessage = Protocol.deserialize(incomingPacket.getData());

                int nodeId = receivedMessage.getNodeId();
                clientStatus.put(nodeId, System.currentTimeMillis());

                if (receivedMessage.getFlag() == 1) { // Heartbeat Message
                    System.out.println("SERVER: Received heartbeat from Node " + nodeId);
                } else if (receivedMessage.getFlag() == 2) { // File Listing Update
                    fileListings.put(nodeId, receivedMessage.getPayload());
                    System.out.println("SERVER: Updated file list for Node " + nodeId);
                }

                sendAcknowledgment(incomingPacket.getAddress(), incomingPacket.getPort(), nodeId);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void sendAcknowledgment(InetAddress clientAddress, int clientPort, int nodeId) {
        try {
            Protocol ackMessage = new Protocol(1, 3, nodeId, System.currentTimeMillis(), 0, "ACK");
            byte[] ackData = ackMessage.serialize();
            DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, clientAddress, clientPort);
            socket.send(ackPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void monitorClients() {
        while (true) {
            try {
                Thread.sleep(10000); // Check every 10s
                long currentTime = System.currentTimeMillis();
    
                for (Integer nodeId : new HashSet<>(clientStatus.keySet())) {
                    if (currentTime - clientStatus.get(nodeId) > 30000) { // 30s threshold
                        System.out.println("SERVER: Node " + nodeId + " is offline.");
                        clientStatus.remove(nodeId);
                        fileListings.remove(nodeId);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcastUpdates() {
        while (true) {
            try {
                Thread.sleep(30000); // Every 30s, send updates to all clients
                if (clientStatus.isEmpty()) continue;

                // Build the availability and file listing message
                StringBuilder updatePayload = new StringBuilder();
                for (Integer nodeId : clientStatus.keySet()) {
                    updatePayload.append("Node ").append(nodeId).append(" is alive. Files: ")
                            .append(fileListings.getOrDefault(nodeId, "No files")).append("\n");
                }

                Protocol updateMessage = new Protocol(1, 4, -1, System.currentTimeMillis(), 0, updatePayload.toString());
                byte[] updateData = updateMessage.serialize();

                // Send to all active clients
                for (Integer nodeId : clientStatus.keySet()) {
                    InetAddress clientAddress = InetAddress.getByName("127.0.0.1"); // Loopback for local testing
                    DatagramPacket updatePacket = new DatagramPacket(updateData, updateData.length, clientAddress, serverPort + nodeId);
                    socket.send(updatePacket);
                }

                System.out.println("SERVER: Sent availability update to all clients.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.listenForClients();
    }
}
