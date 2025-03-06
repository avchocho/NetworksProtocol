package networking;

/**
* 
* @author cjaiswal
*
*/

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UDPServer {
    DatagramSocket socket = null;

    public UDPServer() {
        // No changes here
    }

    public void createAndListenSocket() {
        try {
            socket = new DatagramSocket(9876); //binds server socket to port number so clients must send data to port 
            byte[] incomingData = new byte[1024]; //creates a buffer to store incoming data

            while (true) {
                DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length); //waits for data from a client and stores it in incomingpacket
                socket.receive(incomingPacket);
                String message = new String(incomingPacket.getData(), 0, incomingPacket.getLength()); // Trim message
                InetAddress IPAddress = incomingPacket.getAddress(); //gets client's IP
                int port = incomingPacket.getPort(); //gets client's port so know where to send the response

                System.out.println("Received message from client: " + message);
                System.out.println("Client IP: " + IPAddress.getHostAddress());
                System.out.println("Client Port: " + port);

                String reply = "Thank you for the message"; 
                byte[] data = reply.getBytes(); //creates a response message in bytes

                DatagramPacket replyPacket = new DatagramPacket(data, data.length, IPAddress, port);
                socket.send(replyPacket); //sends respinse packet back to the client 
                
                System.out.println("Sent response to client");
                
                Thread.sleep(2000); // Keep this if you need a delay; pauses for 2 secs before processing the next request
            }
        } 
        catch (SocketException e) {
            e.printStackTrace();
        } 
        catch (IOException i) {
            i.printStackTrace();
        } 
        catch (InterruptedException e) {
            e.printStackTrace();
        } 
        finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Server socket closed.");
            }
        }
    }

    public static void main(String[] args) {
        UDPServer server = new UDPServer();
        server.createAndListenSocket();
    }
}


