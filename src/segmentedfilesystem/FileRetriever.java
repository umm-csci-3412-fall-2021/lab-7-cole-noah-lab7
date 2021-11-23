package segmentedfilesystem;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.InetAddress;

public class FileRetriever {
        private String server;
        private int port;

        public FileRetriever(String server, int port) {
                // Save the server and port for use in `downloadFiles()`
                this.server = server;
                this.port = port;
        }

        public void downloadFiles() throws IOException {
                // Do all the heavy lifting here.
                // This should
                // * Connect to the server
                // * Download packets in some sort of loop
                // * Handle the packets as they come in by, e.g.,
                // handing them to some PacketManager class
                // Your loop will need to be able to ask someone
                // if you've received all the packets, and can thus
                // terminate. You might have a method like
                // PacketManager.allPacketsReceived() that you could
                // call for that, but there are a bunch of possible
                // ways.

                try {
                        boolean done = false;
                        PacketManager packetManager = new PacketManager();
                        DatagramSocket socket = new DatagramSocket();
                        byte[] buf = new byte[1028];
                        InetAddress ip = InetAddress.getByName(server);
                        DatagramPacket helloPacket = new DatagramPacket(buf, buf.length, ip, port);
                        socket.send(helloPacket);

                        while (!done) {
                                buf = new byte[1028];
                                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                                socket.receive(packet);

                                byte[] data = packet.getData();
                                int length = packet.getLength();

                                packetManager.sortPackets(data, length);

                                if (packetManager.allPacketsReceived()) {
                                        done = true;
                                        socket.close();
                                }
                        }
                } catch (SocketException e) {
                        System.out.println("Socket: " + e.getMessage());
                } catch (IOException e) {
                        System.out.println("IO: " + e.getMessage());
                }

                
        }

}
