package ClientServer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import ClientServer.*;


public class Server extends Node {
    private DatagramSocket socket;
    private ExecutorService executorService;
    private static final int TIMEOUT = 30000; // 30s before considering a client inactive
    private static final int BUFFER_SIZE = 1024;

    private ConcurrentHashMap<String, Protocol> clientData; // Active client data (heartbeat + files)
    private ConcurrentHashMap<String, InetSocketAddress> clientAddresses; // Client IPs & Ports

    public Server() {
        super("server", "127.0.0.1", 5000, ""); // Initialize Node with server details

        try {
            System.out.println("Server is starting...");

            // Load configuration
            ConfigReader config = new ConfigReader("ClientServer/server_config.properties");
            setIpAddress(config.getProperty("server_ip", "127.0.0.1"));
            setPort(config.getIntProperty("server_port", 5000));

            // Bind socket to server IP and port
            InetAddress serverAddress = InetAddress.getByName(getIpAddress());
            socket = new DatagramSocket(getPort(), serverAddress);

            clientData = new ConcurrentHashMap<>();
            clientAddresses = new ConcurrentHashMap<>();

            executorService = Executors.newCachedThreadPool();

            System.out.println("Server is listening on " + getIpAddress() + ":" + getPort());

            // Start server threads
            executorService.execute(this::listenForClients);
            executorService.execute(this::monitorClients);
            executorService.execute(this::broadcastUpdates);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Thread 1: Listens for incoming heartbeats from clients.
     */
    private void listenForClients() {
        try {
            while (true) {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Extract client IP and port
                InetAddress clientIP = packet.getAddress();
                int clientPort = packet.getPort();
                String nodeId = clientIP.getHostAddress(); // Use actual IP as nodeId

                // Deserialize received data
                Protocol receivedMessage = Protocol.deserialize(packet.getData());

                // Store client's latest data (file list + timestamp)
                clientData.put(nodeId, receivedMessage);
                clientAddresses.put(nodeId, new InetSocketAddress(clientIP, clientPort));

                System.out.println("[Server] Received heartbeat from node (" + nodeId + ")");
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Thread 2: Monitors clients and marks inactive ones.
     */
    private void monitorClients() {
        while (true) {
            try {
                Thread.sleep(30000); // Check every 30 seconds
                long currentTime = System.currentTimeMillis();
    
                // Track inactive clients
                List<String> inactiveNodes = new ArrayList<>();
    
                Iterator<Map.Entry<String, Protocol>> iterator = clientData.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Protocol> entry = iterator.next();
                    String nodeId = entry.getKey();
                    long lastUpdate = entry.getValue().getTimestamp();
    
                    // Check if client is inactive
                    if (currentTime - lastUpdate > TIMEOUT) {
                        if (clientData.containsKey(nodeId)) {
                            System.out.println("[Server] Node " + nodeId + " is now inactive.");
                        }
                        inactiveNodes.add(nodeId);
                        iterator.remove(); // Removes from clientData
                        clientAddresses.remove(nodeId);
                    }
                }
    
                // Print inactive nodes if any were found
                if (!inactiveNodes.isEmpty()) {
                    System.out.println("[Server] Inactive clients: " + String.join(", ", inactiveNodes));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Thread 3: Broadcasts availability & file listings to all clients.
     */
    private void broadcastUpdates() {
        while (true) {
            try {
                Thread.sleep(30000); // Send updates every 30 seconds

                if (clientData.isEmpty()) {
                    System.out.println("[Server] No active clients to broadcast updates.");
                    continue;
                }

                // Build a message containing active clients & their files
                StringBuilder combinedPayload = new StringBuilder();
                for (Map.Entry<String, Protocol> entry : clientData.entrySet()) {
                    String nodeId = entry.getKey();
                    String fileList = entry.getValue().getPayload();
                    combinedPayload.append(nodeId).append("::").append(fileList).append("\n");
                }

                // Create a Protocol object containing all updates
                Protocol combinedUpdate = new Protocol(1, false, "server", System.currentTimeMillis(), 0, combinedPayload.toString());
                byte[] data = combinedUpdate.serialize();

                // Send ONE packet to each active client
                for (InetSocketAddress clientAddress : clientAddresses.values()) {
                    DatagramPacket packet = new DatagramPacket(data, data.length,
                            clientAddress.getAddress(), clientAddress.getPort());
                    socket.send(packet);
                }

                System.out.println("[Server] Sent updated client availability.");
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new Server();
    }
}