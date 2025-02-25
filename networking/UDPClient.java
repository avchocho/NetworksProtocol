package networking;

import java.io.IOException;
import java.net.*;
import java.util.Random;
/**
 * 
 * @author cjaiswal
 *
 *  
 * 
 */

public class UDPClient {
    DatagramSocket Socket;
    private InetAddress serverAddress;
    private final int SERVER_PORT = 9876;
    private final String NODE_ID = "Node1";

    public UDPClient() {
        try {
            Socket = new DatagramSocket();
            serverAddress = InetAddress.getByName("localhost");
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void sendHeartbeat() {
        new Thread(() -> {
            Random rand = new Random();
            while (true) {
                try {
                    int sleepTime = rand.nextInt(31) * 1000;  // Random interval (0-30s)
                    System.out.println("CLIENT waiting " + (sleepTime / 1000) + " sec before next heartbeat...");

                    // Create the heartbeat message
                    String heartbeatMessage = "HEARTBEAT|" + NODE_ID;
                    byte[] data = heartbeatMessage.getBytes();

                    // Send the heartbeat packet
                    DatagramPacket sendPacket = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
                    Socket.send(sendPacket);
                    System.out.println("CLIENT sent heartbeat: " + heartbeatMessage);

                    // Wait before sending the next heartbeat
                    Thread.sleep(sleepTime);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void createAndListenSocket() {
        try {
            sendHeartbeat(); // Start heartbeat messages

            byte[] incomingData = new byte[1024];
            InetAddress IPAddress = InetAddress.getByName("localhost");
            Random rand = new Random();

            while (true) {  // Continuous loop

                System.out.println("");

                int sleepTime = rand.nextInt(31) * 1000;  // Random interval (0-30s)

                String sentence = "Viehmann";
                byte[] data = sentence.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPAddress, 9876);
                Socket.send(sendPacket);
                System.out.println("SERVER message sent from client");

                DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                Socket.receive(incomingPacket);
                String response = new String(incomingPacket.getData(), 0, incomingPacket.getLength());
                System.out.println("SERVER response from server: " + response);

                Thread.sleep(sleepTime);
            }
        } 
        catch (UnknownHostException e) {
            e.printStackTrace();
        } 
        catch (SocketException e) {
            e.printStackTrace();
        } 
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } 
        finally {
            if (Socket != null && !Socket.isClosed()) {
                Socket.close();
            }
        }
    }

    public static void main(String[] args) {
        UDPClient client = new UDPClient();
        client.createAndListenSocket();
    }
}