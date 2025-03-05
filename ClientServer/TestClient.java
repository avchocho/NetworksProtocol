package ClientServer;

import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.*;
import ClientServer.Protocol;

public class TestClient {
    private String serverIP;
    private int serverPort;
    private int nodeId;
    private DatagramSocket socket;
    private ExecutorService executorService;

    public TestClient(int nodeId) {
        try {
            System.out.println("Client " + nodeId + " is starting...");

            // Load config
            Properties config = new Properties();
            InputStream input = getClass().getClassLoader().getResourceAsStream("ClientServer/client_config.properties");

            if (input == null) {
                throw new FileNotFoundException("client_config.properties not found!");
            }
            config.load(input);

            // Read server IP & Port from config
            serverIP = config.getProperty("server_ip", "127.0.0.1");
            serverPort = Integer.parseInt(config.getProperty("server_port", "5000"));

            // Initialize socket
            socket = new DatagramSocket();
            this.nodeId = nodeId;

            executorService = Executors.newCachedThreadPool();

            System.out.println("Client " + nodeId + " is running, connecting to " + serverIP + ":" + serverPort);

            // Start sender and receiver threads
            executorService.execute(this::sendUpdates);
            executorService.execute(this::listenForServer);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends periodic heartbeat messages to the server.
     */
    private void sendUpdates() {
        Random random = new Random();
        while (true) {
            try {
                int delay = random.nextInt(30000); // Random delay (0-30s)
                Thread.sleep(delay);

                Protocol heartbeat = new Protocol(1, false, nodeId, System.currentTimeMillis(), 0, "Hello from node " + nodeId);
                byte[] data = heartbeat.serialize();

                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(serverIP), serverPort);
                socket.send(packet);

                System.out.println("Client " + nodeId + " sent heartbeat to server.");
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Listens for responses from the server.
     */
    private void listenForServer() {
        try {
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                Protocol receivedProtocol = Protocol.deserialize(packet.getData());

                System.out.println("Client " + nodeId + " received update from server: " + receivedProtocol.getPayload());
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int nodeId = 1; // Default node ID
        if (args.length >= 1) {
            try {
                nodeId = Integer.parseInt(args[0]); // Read from command-line if provided
            } catch (NumberFormatException e) {
                System.out.println("Invalid nodeId, using default ID: " + nodeId);
            }
        }
        new TestClient(nodeId);
        //new TestClient(2);

    }
}