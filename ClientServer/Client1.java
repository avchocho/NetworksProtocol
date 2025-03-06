package ClientServer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Client1 {
    private String serverIP;
    private int serverPort;
    private String clientIP;
    private int clientPort;
    private String homeDirectory;
    private DatagramSocket socket;
    private ExecutorService executorService;

    private volatile String lastServerUpdate = "Waiting for updates...";

    public Client1() {
        try {
            System.out.println("Client is starting...");

            // Load config file
            Properties config = new Properties();
            InputStream input = getClass().getClassLoader().getResourceAsStream("ClientServer/client_config.properties");

            if (input == null) {
                throw new FileNotFoundException("client_config.properties not found!");
            }
            config.load(input);

            // Read server and client details
            serverIP = config.getProperty("server_ip", "127.0.0.1");
            serverPort = Integer.parseInt(config.getProperty("server_port", "5000"));
            clientIP = config.getProperty("client_ip", "127.0.0.2"); // Now used as nodeId
            clientPort = Integer.parseInt(config.getProperty("client_port", "6001"));
            homeDirectory = config.getProperty("home_directory", "./home/");

            // Create socket
            socket = new DatagramSocket(clientPort, InetAddress.getByName(clientIP));
            executorService = Executors.newCachedThreadPool();

            System.out.println("Client " + clientIP + " running at " + clientIP + ":" + clientPort + " -> Server " + serverIP + ":" + serverPort);

            // Start three independent threads
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
                Protocol heartbeat = new Protocol(1, false, clientIP, System.currentTimeMillis(), 0, fileListing);
                byte[] data = heartbeat.serialize();

                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(serverIP), serverPort);
                socket.send(packet);

                System.out.println("[Client " + clientIP + "] ✅ Sent heartbeat with file listing.");

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
                System.out.println("\n🔹 [Client " + clientIP + "] Received Update from Server 🔹");
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
        File folder = new File(homeDirectory);
        
        // If the directory doesn't exist, create it
        if (!folder.exists()) {
            System.out.println("📂 Home directory not found. Creating: " + homeDirectory);
            boolean created = folder.mkdirs();
            if (created) {
                System.out.println("✅ Home directory created successfully!");
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
        new Client1(); // No need to pass nodeId manually
    }
}
