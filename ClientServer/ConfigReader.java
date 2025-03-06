package ClientServer;

import java.io.*;

public class ConfigReader {
    private String nodeId;
    private String role;
    private String ip;
    private int port;
    private String homeDir;

    private static final String CONFIG_FILE = "client_server_config.txt"; 

    public ConfigReader(String nodeId) throws IOException {
        this.nodeId = nodeId;
        loadConfig();
    }

    private void loadConfig() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) continue; // Skip comments and empty lines
                String[] parts = line.split(",\\s*"); // Split by comma and optional spaces
                
                if (parts.length < 4) {
                    System.err.println("⚠️ ERROR: Invalid config format in line: " + line);
                    continue;
                }

                // Check if this line matches the node ID
                if (parts[0].equals(nodeId)) {
                    role = parts[1].toLowerCase(); // Convert to lowercase
                    ip = parts[2];
                    port = Integer.parseInt(parts[3]);
                    
                    if (isClient() && parts.length >= 5) {
                        homeDir = parts[4];
                    } else {
                        homeDir = null; // Home directory is only needed for clients
                    }
                    break;
                }
            }
        }

        if (role == null || ip == null) {
            throw new FileNotFoundException("⚠️ ERROR: Node ID " + nodeId + " not found in config file.");
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
        return "Node " + nodeId + " [" + role.toUpperCase() + "] - IP: " + ip + ", Port: " + port +
               (isClient() ? ", Home: " + homeDir : "");
    }

    public static void main(String[] args) {
        try {
            // Test reading configurations for a specific node
            ConfigReader config = new ConfigReader("1"); // Change node ID for testing
            System.out.println(config);

            if (config.isServer()) {
                System.out.println("✅ This node is a SERVER.");
            } else {
                System.out.println("✅ This node is a CLIENT.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
