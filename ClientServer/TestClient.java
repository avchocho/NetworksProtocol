package ClientServer;
import java.net.*;
import java.io.*;
import java.util.Random;

public class TestClient {
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private int nodeId;
    private Random random;

    public TestClient(int nodeId) {
        try {
            this.serverPort = 5000; // Server port
            this.serverAddress = InetAddress.getByName("127.0.0.1"); // Localhost
            this.socket = new DatagramSocket();
            this.nodeId = nodeId;
            this.random = new Random();

            System.out.println("CLIENT " + nodeId + " started.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startClient() {
        new Thread(() -> {
            while (true) {
                try {
                    int waitTime = random.nextInt(31); // Random wait time (0-30s)
                    Thread.sleep(waitTime * 1000);

                    sendHeartbeat();
                    sendFileUpdate();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void sendHeartbeat() {
        try {
            Protocol heartbeat = new Protocol(1, 1, nodeId, System.currentTimeMillis(), 0, ""); 
            byte[] data = heartbeat.serialize();
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
            socket.send(packet);
            System.out.println("CLIENT " + nodeId + ": Sent heartbeat.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendFileUpdate() {
        try {
            String fakeFileList = "file1.txt, file2.txt, file3.txt";
            Protocol fileUpdate = new Protocol(1, 2, nodeId, System.currentTimeMillis(), 0, fakeFileList);
            byte[] data = fileUpdate.serialize();
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
            socket.send(packet);
            System.out.println("CLIENT " + nodeId + ": Sent file listing update.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receiveUpdates() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    
                while (true) {
                    System.out.println("CLIENT " + nodeId + ": Waiting for availability update...");
                    
                    socket.receive(packet); // Blocking call
                    System.out.println("CLIENT " + nodeId + ": Packet received!");
    
                    Protocol updateMessage = Protocol.deserialize(packet.getData());
    
                    System.out.println("CLIENT " + nodeId + ": Deserialized update - Flag: " + updateMessage.getFlag());
    
                    if (updateMessage.getFlag() == 4) { // Availability update
                        System.out.println("CLIENT " + nodeId + ": Received availability update:\n" + updateMessage.getPayload());
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) {
        TestClient client = new TestClient(1001);

        client.receiveUpdates(); // Start listening for availability updates
        client.startClient(); // Start sending updates at random intervals
    }
}
