package ClientServer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private String serverIP;
    private int serverPort;
    private DatagramSocket socket;
    private ConcurrentHashMap<String, Protocol> clientData;
    private ConcurrentHashMap<String, InetSocketAddress> clientAddresses;
    private ExecutorService executorService;
    private static final int TIMEOUT = 30000; // 30 seconds before assuming a client is dead
    private static final int BUFFER_SIZE = 1024;

    public Server(String nodeId) {
        try {
            System.out.println("Server is starting...");

            // Load configuration using ConfigReader
            ConfigReader config = new ConfigReader(nodeId);

            if (!config.isServer()) {
                System.err.println("⚠️ ERROR: This node is not configured as a server!");
                return;
            }

            // Assign configuration values
            serverIP = config.getIP();
            serverPort = config.getPort();

            // Bind server to specific IP and Port
            socket = new DatagramSocket(serverPort, InetAddress.getByName(serverIP));

            clientData = new ConcurrentHashMap<>();
            clientAddresses = new ConcurrentHashMap<>();
            executorService = Executors.newCachedThreadPool();

            System.out.println("Server is listening on " + serverIP + ":" + serverPort + "...");

            // Start server threads
            executorService.execute(this::listenForClients);
            executorService.execute(this::monitorClients);
            executorService.execute(this::broadcastUpdates);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void listenForClients() {
        try {
            while (true) {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                InetAddress clientIP = packet.getAddress();
                int clientPort = packet.getPort();
                String nodeId = clientIP.getHostAddress();

                Protocol receivedMessage = Protocol.deserialize(packet.getData());
                clientData.put(nodeId, receivedMessage);
                clientAddresses.put(nodeId, new InetSocketAddress(clientIP, clientPort));

                System.out.println("Received heartbeat from node (" + nodeId + ")");
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void monitorClients() {
        while (true) {
            try {
                Thread.sleep(30000);
                long currentTime = System.currentTimeMillis();

                Iterator<Map.Entry<String, Protocol>> iterator = clientData.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Protocol> entry = iterator.next();
                    String nodeId = entry.getKey();
                    long lastUpdate = entry.getValue().getTimestamp();

                    if (currentTime - lastUpdate > TIMEOUT) {
                        System.out.println("Node " + nodeId + " is inactive.");
                        iterator.remove();
                        clientAddresses.remove(nodeId);
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
                Thread.sleep(30000);

                if (clientData.isEmpty()) {
                    System.out.println("No active clients to broadcast updates.");
                    continue;
                }

                StringBuilder combinedPayload = new StringBuilder();
                for (Map.Entry<String, Protocol> entry : clientData.entrySet()) {
                    combinedPayload.append(entry.getKey()).append("::").append(entry.getValue().getPayload()).append("\n");
                }

                Protocol combinedUpdate = new Protocol(1, false, "server", System.currentTimeMillis(), 0, combinedPayload.toString());
                byte[] data = combinedUpdate.serialize();

                for (InetSocketAddress clientAddress : clientAddresses.values()) {
                    DatagramPacket packet = new DatagramPacket(data, data.length, clientAddress.getAddress(), clientAddress.getPort());
                    socket.send(packet);
                }
                System.out.println("Broadcasted one packet containing all client updates.");
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Server <nodeId>");
            return;
        }
        new Server(args[0]);
    }
}
