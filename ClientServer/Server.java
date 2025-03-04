package ClientServer;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;

public class Server {
    private String serverIP;
    private int serverPort;
    private DatagramSocket socket;
    private ConcurrentHashMap<Integer, Protocol> clientData; // Thread-safe storage
    private ExecutorService executorService;

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
    
            // Read IP and Port from config
            serverIP = config.getProperty("server_ip", "127.0.0.1");
            serverPort = Integer.parseInt(config.getProperty("server_port", "9876"));
            
            // Bind server to specific IP and Port
            InetAddress serverAddress = InetAddress.getByName(serverIP);
            socket = new DatagramSocket(serverPort, serverAddress);
    
            clientData = new ConcurrentHashMap<>();
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
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Deserialize the received data
                Protocol receivedMessage = Protocol.deserialize(packet.getData());
                int nodeId = receivedMessage.getNodeId();
                clientData.put(nodeId, receivedMessage); // Update client's latest data

                System.out.println("Received heartbeat from Node " + nodeId);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void monitorClients() {
        while (true) {
            try {
                Thread.sleep(30000); // Check every 30 seconds
                long currentTime = System.currentTimeMillis();

                for (Map.Entry<Integer, Protocol> entry : clientData.entrySet()) {
                    long lastUpdate = entry.getValue().getTimestamp();
                    if (currentTime - lastUpdate > 30000) { // Mark as dead if silent for 30s
                        System.out.println("Node " + entry.getKey() + " is inactive.");
                        clientData.remove(entry.getKey());
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
                Thread.sleep(30000); // Send updates every 30 seconds

                for (Map.Entry<Integer, Protocol> entry : clientData.entrySet()) {
                    int nodeId = entry.getKey();
                    Protocol update = entry.getValue();
                    byte[] data = update.serialize();
                    DatagramPacket packet = new DatagramPacket(data, data.length,
                            InetAddress.getByName(serverIP), serverPort);
                    socket.send(packet);
                }
                System.out.println("Broadcasted updates to all active nodes.");
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new Server();
    }
}
