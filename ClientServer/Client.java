package ClientServer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Client {
    private String serverIP;
    private int serverPort;
    private String clientIP;
    private int clientPort;
    private String homeDirectory;
    private DatagramSocket socket;
    private ExecutorService executorService;

    private volatile String lastServerUpdate = "Waiting for updates...";

    public Client() {
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
            clientIP = config.getProperty("client_ip", "127.0.0.1"); 
            clientPort = Integer.parseInt(config.getProperty("client_port", "6000"));
            homeDirectory = config.getProperty("home_directory", "./home/");

            // Create socket
            socket = new DatagramSocket(clientPort, InetAddress.getByName(clientIP));
            executorService = Executors.newCachedThreadPool();

            System.out.println("Client " + clientIP + " running at " + clientIP + ":" + clientPort + 
                    " -> Server " + serverIP + ":" + serverPort);

            // Start independent threads
            executorService.execute(this::sendHeartbeat);
            executorService.execute(this::listenForUpdates);
            executorService.execute(this::printUpdates);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendHeartbeat() {
        Random random = new Random();
        while (true) {
            try {
                int delay = random.nextInt(30000);
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

    private void listenForUpdates() {
        try {
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                Protocol receivedProtocol = Protocol.deserialize(packet.getData());
                lastServerUpdate = receivedProtocol.getPayload();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void printUpdates() {
        while (true) {
            try {
                Thread.sleep(30000);
                System.out.println("\n🔹 [Client " + clientIP + "] Received Update from Server 🔹");
                System.out.println("--------------------------------------------------");
                System.out.println(lastServerUpdate);
                System.out.println("--------------------------------------------------\n");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private String getFileListing() {
        File folder = new File(homeDirectory);
        if (!folder.exists()) {
            System.out.println("📂 Home directory not found. Creating: " + homeDirectory);
            if (folder.mkdirs()) {
                System.out.println("✅ Home directory created successfully!");
            } else {
                return "ERROR: Could not create home directory!";
            }
        }

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
