package ClientServer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import ClientServer.*;  

public class Client extends Node { 
    private String serverIP;
    private int serverPort;
    private DatagramSocket socket;
    private ExecutorService executorService;
    private volatile String lastServerUpdate = "Waiting for updates...";

    public Client() {
        try {
            System.out.println("Client is starting...");

            // Load config file using ConfigReader
            ConfigReader config = new ConfigReader("ClientServer/client_config.properties");

            // Set Node attributes using inherited setters
            setIpAddress(config.getProperty("client_ip", InetAddress.getLocalHost().getHostAddress())); // Get actual IP
            setNodeId(getIpAddress()); // Node ID is now the IP address
            setPort(config.getIntProperty("client_port", 6001));
            setHomeDirectory(config.getProperty("home_directory", "./home/"));

            // Read server details from config
            serverIP = config.getProperty("server_ip", "127.0.0.1");
            serverPort = config.getIntProperty("server_port", 5000);

            // Create socket for communication
            socket = new DatagramSocket(getPort(), InetAddress.getByName(getIpAddress()));
            executorService = Executors.newCachedThreadPool();

            System.out.println("Client " + getIpAddress() + " running at " +
                getIpAddress() + ":" + getPort() +
                " -> Server " + serverIP + ":" + serverPort);

            // Start threads for communication
            executorService.execute(this::sendHeartbeat);
            executorService.execute(this::listenForUpdates);
            executorService.execute(this::printUpdates);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Thread 1: Sends periodic heartbeat messages with file listings.
     */
    private void sendHeartbeat() {
        Random random = new Random();
        while (true) {
            try {
                int delay = random.nextInt(30000); // Random delay (0-30s)
                Thread.sleep(delay);

                String fileListing = getFileListing();
                Protocol heartbeat = new Protocol(1, false, getNodeId(), System.currentTimeMillis(), 0, fileListing);
                byte[] data = heartbeat.serialize();

                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(serverIP), serverPort);
                socket.send(packet);

                System.out.println("[Client " + getIpAddress() + "] Sent heartbeat with file listing.");

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Thread 2: Listens for updates from the server.
     */
    private void listenForUpdates() {
        try {
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                Protocol receivedProtocol = Protocol.deserialize(packet.getData());
                lastServerUpdate = receivedProtocol.getPayload(); // Store received update for printing
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Thread 3: Prints the latest server update every 30 seconds.
     */
    private void printUpdates() {
        while (true) {
            try {
                Thread.sleep(30000); // Print every 30 seconds
                System.out.println("\n[Client " + getIpAddress() + "] Received Update from Server ");
                System.out.println("--------------------------------------------------");
                System.out.println(lastServerUpdate);
                System.out.println("--------------------------------------------------\n");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Retrieves a file listing from the home directory.
     */
    private String getFileListing() {
        File folder = new File(getHomeDirectory());

        // If the directory doesn't exist, create it
        if (!folder.exists()) {
            System.out.println("Home directory not found. Creating: " + getHomeDirectory());
            boolean created = folder.mkdirs();
            if (created) {
                System.out.println("Home directory created successfully!");
            } else {
                return "ERROR: Could not create home directory!";
            }
        }

        // List files in the directory
        StringBuilder fileList = new StringBuilder();
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                fileList.append(file.getName()).append(",");
            }
        }
        return fileList.length() > 0 ? fileList.toString() : "No files available.";
    }

    public static void main(String[] args) {
        new Client();
    }
}
