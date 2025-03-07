package ClientServer;

public class Node{

    private int port;
    private String nodeId, ipAddress, homeDirectory;
    

    public Node(){

    }

    public Node(String nodeId, String ipAddress, int port, String homeDirectory){
        this.nodeId = nodeId;
        this.ipAddress = ipAddress;
        this.port = port;
        this.homeDirectory = homeDirectory;
    }

    //getters

    public String getNodeId(){
        return nodeId;
    }

    public String getIpAddress(){
        return ipAddress;
    }

    public int getPort(){
        return port;
    }

    public String getHomeDirectory(){
        return homeDirectory;
    }

    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setPort(int port) { this.port = port; }
    public void setHomeDirectory(String homeDirectory) { this.homeDirectory = homeDirectory; }

    @Override
    public String toString() {
        return "Node[ID=" + nodeId + ", IP=" + ipAddress + ", Port=" + port + ", HomeDir=" + homeDirectory + "]";
    }
}

