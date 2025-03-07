package PeerToPeer;

import java.io.*;
import java.util.*;

public class ConfigReaderP2P {
    public static List<Node> readConfig(String filePath) throws IOException {
        System.out.println("Reading P2P configuration file: " + filePath);
        List<Node> nodes = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty() || line.startsWith("#")) {
                continue; // Skip empty lines and comments
            }
            String[] parts = line.split(",");
            int nodeID = Integer.parseInt(parts[0].trim());
            String ipAddress = parts[1].trim();
            int port = Integer.parseInt(parts[2].trim());
            String homeDir = parts[3].trim();

            Node nodeInfo = new Node(nodeID, ipAddress, port, homeDir);
            nodes.add(nodeInfo);
        }

        reader.close();
        return nodes;
    }
}
