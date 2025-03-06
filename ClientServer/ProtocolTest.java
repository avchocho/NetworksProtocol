package ClientServer;

import java.io.*;

public class ProtocolTest {
    public static void main(String[] args) {
        try {
            // Create a Protocol object
            Protocol original = new Protocol(1, false, "1001", System.currentTimeMillis(), 0, "Hello, HAC!");

            // Serialize the object
            byte[] serializedData = original.serialize();
            System.out.println("Serialized data length: " + serializedData.length);

            // Deserialize the object
            Protocol deserialized = Protocol.deserialize(serializedData);

            // Verify the deserialized object matches the original
            System.out.println("Deserialized Protocol:");
            System.out.println("Version: " + deserialized.getVersion());
            System.out.println("Mode: " + (deserialized.isP2P() ? "P2P" : "Client-Server"));
            System.out.println("Node ID: " + deserialized.getNodeId());
            System.out.println("Timestamp: " + deserialized.getTimestamp());
            System.out.println("Reserve: " + deserialized.getReserve());
            System.out.println("Payload: " + deserialized.getPayload());

            // Check if data integrity is maintained
            assert original.getVersion() == deserialized.getVersion();
            assert original.isP2P() == deserialized.isP2P();
            assert original.getNodeId() == deserialized.getNodeId();
            assert original.getTimestamp() == deserialized.getTimestamp();
            assert original.getReserve() == deserialized.getReserve();
            assert original.getPayload().equals(deserialized.getPayload());

            System.out.println("Serialization test passed!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
