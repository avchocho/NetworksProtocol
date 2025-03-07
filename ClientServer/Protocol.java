package ClientServer;

import java.io.*;
import ClientServer.*;

public class Protocol implements Serializable {
    private static final long serialVersionUID = 1L;

    // Dynamically calculated HEADER_SIZE
    private static final int HEADER_SIZE = Integer.BYTES * 3 + Long.BYTES + 1;  // version, nodeId, reserve, timestamp, mode

    private int version; 
    private int length; // Length of payload
    private String nodeId;
    private boolean mode; // true = P2P, false = Client-Server
    private long timestamp;
    private int reserve;
    private String payload;

    public Protocol(int version, boolean mode, String nodeId, long timestamp, int reserve, String payload) {
        this.version = version; 
        this.mode = mode;
        this.nodeId = nodeId;
        this.timestamp = timestamp;
        this.reserve = reserve;
        setPayload(payload);
    }

    // Getters & Setters
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public boolean getMode() { return mode; }
    public void setMode(boolean mode) { this.mode = mode; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getReserve() { return reserve; }
    public void setReserve(int reserve) { this.reserve = reserve; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) {
        this.payload = (payload != null) ? payload : "";
        this.length = this.payload.length();
    }

    public int getLength() { return length; }

    public int getTotalSize() {
        return HEADER_SIZE + length;
    }

    // Serialization - Converts an instance of Protocol into a byte array for UDP transmission
    public byte[] serialize() throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(this);
            out.flush();
            return bos.toByteArray();
        }
    }

    // Deserialization - Converts a received UDP byte array back into a Protocol object
    public static Protocol deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream in = new ObjectInputStream(bis)) {
            return (Protocol) in.readObject();
        }
    }

    @Override
    public String toString() {
        return "Protocol{" +
                "version=" + version +
                ", mode=" + (mode ? "P2P" : "Client-Server") +
                ", nodeId=" + nodeId +
                ", timestamp=" + timestamp +
                ", reserve=" + reserve +
                ", payload='" + payload + '\'' +
                ", totalSize=" + getTotalSize() +
                '}';
    }
}
