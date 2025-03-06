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
    private static final int TIMEOUT = 30000;
    private static final int BUFFER_SIZE = 1024;

    public Server() {
        try {
            System.out.println("🔹 Server is starting...");

            // Load configuration
            Properties config = new Properties();
            InputStream input = getClass().getClassLoader().getResourceAsStream("ClientServer/server_config.properties");

            if (input == null) {
                throw new FileNotFoundException("server_config.properties not found in resources!");
            }
            config.load(input);

            // Read IP and Port from config
            serverIP = config.getProperty("server_ip", "127.0.0.1");
            serverPort = Integer.parseInt(config.getProperty("server_port", "5000"));

            // Bind server to specific IP and Port
            socket = new DatagramSocket(serverPort, InetAddress.getByName(serverIP));

            clientData = new ConcurrentHashMap<>();
            clientAddresses = new ConcurrentHashMap<>();
            executorService = Executors.newCachedThreadPool();

            System.out.println("✅ Server is listening on " + serverIP + ":" + serverPort + "...");

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
        new Server();
    }
}
