/* CSC340: Project 1
 * Authors: Adelina Chocho, Reeya Patel, Megan Mohr
 * 
 * This class defines the structure of the messages between the nodes in the HAC. It's
 * the standards for communication, ensuring that all the nodes follow a consistent 
 * message format when sending and receiving data over UDP.
*/
package PeerToPeer;

import java.io.*;

public class Protocol implements Serializable {
    private static final long serialVersionUID = 1L; //first version, long data type 
    private static final int HEADER_SIZE = Integer.BYTES * 4 + Long.BYTES + 1;

    private int version; 
    private int length; //length of payload
    private String nodeId;
    private boolean mode; //true = P2P, false = client-server
    private long timestamp;
    private int reserve;
    private String payload;
    
    public Protocol(int version, boolean mode, int nodeId2, long timestamp, int reserve, String payload) {
        this.version = version; 
        this.mode = mode;
        this.nodeId = nodeId2;
        this.timestamp = timestamp;
        this.reserve = reserve;
        this.payload = payload;
        this.length = (payload != null) ? payload.length() : 0;

    }
    public Protocol(int version2, boolean mode2, int nodeId2, long currentTime, int reserve2, String payload2) {
        //TODO Auto-generated constructor stub
    }
    //getters and setters
    public int getVersion() { return version; }
    public void setVersion(int version) {this.version = version;}

    public boolean isP2P() { return mode; }
    public void setMode(boolean mode) { this.mode = mode; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId;}

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) {this.timestamp = timestamp;}

    public int getReserve() { return reserve; }
    public void setReserve (int reserve) {this.reserve = reserve;}

    public String getPayload() { return payload; }
    public void setPayload(String payload ) {
        this.payload = payload;
        this.length = (payload != null) ? payload.length() : 0; //updates the length when payload changes 
    }

    public int getLength() { return length; }

    public int getTotalSize() {
        return HEADER_SIZE + length;
    }
    
    
    //serialization - convers an instance of protoocl into a byte array, allows it to be sent over UDP
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(this);
        out.flush();
        return bos.toByteArray();
    }

    //deserialization - needed when a node receives a UDP packet as the receiving node must convert it back into a protocol object to understand its contents 
    public static Protocol deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream in = new ObjectInputStream(bis);
        return (Protocol) in.readObject();
    }
    

}

