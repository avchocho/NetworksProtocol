package ClientServer;

public class Node {
    private int port;
    private String nodeId, ipAddress, homeDirectory;

    public Node(String nodeId, String ipAddress, int port, String homeDirectory) {
        this.nodeId = nodeId;
        this.ipAddress = ipAddress;
        this.port = port;
        this.homeDirectory = homeDirectory;
    }

    public Node() {
        // Empty constructor for flexibility (can set values later using setters)
    }

    // Getters and Setters
    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHomeDirectory() {
        return homeDirectory;
    }

    public void setHomeDirectory(String homeDirectory) {
        this.homeDirectory = homeDirectory;
    }

    @Override
    public String toString() {
        return "Node[ID=" + nodeId + ", IP=" + ipAddress + ", Port=" + port + ", HomeDir=" + homeDirectory + "]";
    }
}
