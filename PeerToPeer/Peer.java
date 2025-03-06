package PeerToPeer;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Peer {
    private static final int PORT = 5000; // Port for communication
    private static final int TIMEOUT = 30000; // 30 seconds timeout for node inactivity
    private DatagramSocket socket;
    private int nodeId;
    private boolean mode = true; // mode=true (1) for Peer-to-Peer, false (0) for Client-Server
    private ConcurrentHashMap<Integer, Long> activePeers; // Stores active peers & last seen time

    public Peer(int nodeId) throws SocketException {
        this.socket = new DatagramSocket(PORT);
        this.nodeId = nodeId;
        this.activePeers = new ConcurrentHashMap<>();
    }

    // Sends an "I am alive" message to all known peers
    public void sendHeartbeat(List<String> peerIPs) throws IOException {
        long currentTime = System.currentTimeMillis();
        Protocol msg = new Protocol(1, mode, nodeId, currentTime, 0, "Alive-Peer");
        byte[] data = msg.serialize();

        for (String peerIP : peerIPs) {
            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(peerIP), PORT);
            socket.send(packet);
            System.out.println("Sent heartbeat to " + peerIP);
        }
    }

    // Listens for incoming messages from other peers
    public void listen() throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (true) {
            socket.receive(packet);
            try {
                Protocol msg = Protocol.deserialize(packet.getData());

                // Update peer status
                activePeers.put(msg.getNodeId(), System.currentTimeMillis());
                System.out.println("Received from Node " + msg.getNodeId() + ": " + msg.getPayload());

            } catch (ClassNotFoundException e) {
                e.printStackTrace(); // Handle the error appropriately
            } catch (IOException e) {
                e.printStackTrace(); // Handle I/O exceptions
            }
        }
    }

    // Checks if peers are still active every 30 seconds
    public void checkPeerStatus() {
        while (true) {
            long currentTime = System.currentTimeMillis();
            activePeers.forEach((peerId, lastSeen) -> {
                if (currentTime - lastSeen > TIMEOUT) {
                    System.out.println("Peer " + peerId + " is inactive!");
                    activePeers.remove(peerId);
                }
            });

            try {
                Thread.sleep(5000); // Check every 5 seconds
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Peer peer = new Peer(1); // Assign node ID

        List<String> peerIPs = Arrays.asList("127.0.0.1"); // List of known peers (replace with real IPs)

        // Start listening in a separate thread
        new Thread(() -> {
            try {
                peer.listen();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // Start monitoring peer activity
        new Thread(peer::checkPeerStatus).start();

        // Send heartbeats at random intervals (0-30s)
        while (true) {
            peer.sendHeartbeat(peerIPs);
            Thread.sleep((long) (Math.random() * 30000)); // Sleep between 0-30 sec
        }
    }
}