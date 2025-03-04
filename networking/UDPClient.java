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
    DatagramSocket Socket; //declares UDP socket and server info
    private InetAddress serverAddress;
    private final int SERVER_PORT = 9876;
    private final String NODE_ID = "Node1"; //uniquely identifies the client 
    //the server address is localhost (same machine)

    public UDPClient() {
        try {
            Socket = new DatagramSocket(); //creates a client socket
            serverAddress = InetAddress.getByName("localhost");//finds the server's IP (localhost)
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void sendHeartbeat() {
        //runs a separate thread to send heartbeat messages every 0-30 secs
        new Thread(() -> {
            Random rand = new Random();
            while (true) {
                try {
                    int sleepTime = rand.nextInt(31) * 1000;  // Random interval (0-30s)
                    System.out.println("CLIENT waiting " + (sleepTime / 1000) + " sec before next heartbeat...");

                    // Create the heartbeat message
                    String heartbeatMessage = "HEARTBEAT|" + NODE_ID;
                    byte[] data = heartbeatMessage.getBytes(); //formats heartbeat data and converts it to bytes

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

            byte[] incomingData = new byte[1024]; //creates a buffer for responses
            InetAddress IPAddress = InetAddress.getByName("localhost"); //resolves a server IP address
            Random rand = new Random(); //random number generator

            while (true) {  // Continuous loop that sends messages every 0-30 seconds

                System.out.println("");

                int sleepTime = rand.nextInt(31) * 1000;  // Random interval (0-30s)

                String sentence = "Viehmann";
                byte[] data = sentence.getBytes(); //converts message into bytes and sends it to the server 
                DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPAddress, 9876);
                Socket.send(sendPacket);
                System.out.println("SERVER message sent from client");

                //waits for the server's reponse
                DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                Socket.receive(incomingPacket);
                String response = new String(incomingPacket.getData(), 0, incomingPacket.getLength());
                System.out.println("SERVER response from server: " + response);

                Thread.sleep(sleepTime); //waits randomly 0-30s before sending the next message
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