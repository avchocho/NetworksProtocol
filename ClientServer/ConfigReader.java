package ClientServer;

import java.io.*;

public class ConfigReader {
    private String nodeId;
    private String role;
    private String ip;
    private int port;
    private String homeDir;

    public ConfigReader(String nodeId) throws IOException {
        this.nodeId = nodeId;
        loadConfig();
    }

    private void loadConfig() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("client_server_config.txt"));
        String line;
        
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("#") || line.trim().isEmpty()) continue; // Skip comments and empty lines
            String[] parts = line.split(", ");
            
            if (parts[0].equals(nodeId)) {
                role = parts[1];
                ip = parts[2];
                port = Integer.parseInt(parts[3]);
                homeDir = parts[4];
                break;
            }
        }
        
        reader.close();

        if (role == null || ip == null || homeDir == null) {
            throw new FileNotFoundException("Node ID " + nodeId + " not found in config file.");
        }
    }

    public String getNodeId() { return nodeId; }
    public String getRole() { return role; }
    public String getIP() { return ip; }
    public int getPort() { return port; }
    public String getHomeDir() { return homeDir; }
    public boolean isServer() { return role.equalsIgnoreCase("server"); }
    public boolean isClient() { return role.equalsIgnoreCase("client"); }

    @Override
    public String toString() {
        return "Node " + nodeId + " [" + role.toUpperCase() + "] - IP: " + ip + ", Port: " + port + ", Home: " + homeDir;
    }

    public static void main(String[] args) {
        try {
            // Example: Read config for node 2 (change this for different nodes)
            ConfigReader config = new ConfigReader("2");
            System.out.println(config);

            // Example Usage
            if (config.isServer()) {
                System.out.println("This node is the SERVER.");
            } else {
                System.out.println("This node is a CLIENT.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
