package segmentedfilesystem;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.TreeMap;

public class ReceivedFile {
    
    TreeMap<Integer, byte[]> file;
    String fileName;
    int lastPacketNum = -2;
    byte[] finalByteArray;

    public void newFile(Packet packet) {
        TreeMap<Integer, byte[]> file = new TreeMap<>();
        if (packet instanceof HeaderPacket) {
            HeaderPacket headerPacket = (HeaderPacket) packet;
            fileName = headerPacket.getFileName();
        } else {
            DataPacket dataPacket = (DataPacket) packet;
            int packetNum = dataPacket.getPacketNumber();
            byte[] packetData = dataPacket.getData();
            file.put(packetNum, packetData);
        }

        if(packet.isLastPacket()) {
            DataPacket dataPacket = (DataPacket) packet;
            lastPacketNum = dataPacket.getPacketNumber();
        }

        this.file = file;
    }

    // Collects the packets and puts them in the TreeMap or collects the filename if the packet is a header
    public void addPacket(Packet packet) {
        if (packet instanceof HeaderPacket) {
            HeaderPacket headerPacket = (HeaderPacket) packet;
            fileName = headerPacket.getFileName();
        } else {
            DataPacket dataPacket = (DataPacket) packet;
            int packetNum = dataPacket.getPacketNumber();
            byte[] packetData = dataPacket.getData();
            file.put(packetNum, packetData);
        }

        if(packet.isLastPacket()) {
            DataPacket dataPacket = (DataPacket) packet;
            lastPacketNum = dataPacket.getPacketNumber();
        }

        if(allPacketsReceived()) {
            writeFile();
        }
    }

    // Writes all fo the values in the treemap to a byte array
    public void writeToByteArray(){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            for (int i = 0; i <= lastPacketNum; i++) {
                if (fileName.equals("small.txt")) {
                    System.out.println("Packet " + i + " of file " + fileName + ": <" + new String(file.get(i)) + ">");
                    System.out.println("Packet length = " + file.get(i).length);
                }
                baos.write(file.get(i));
            }
            finalByteArray = baos.toByteArray();
            if (fileName.equals("small.txt")) {
                System.out.println("Final byte array : " + new String(finalByteArray));
                System.out.println("Length = " + finalByteArray.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Writes the byte array to a file
    public void writeFile() {
        writeToByteArray();
        try (FileOutputStream fos = new FileOutputStream(fileName)) { //Throws an error if we don't handle error
            fos.write(finalByteArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Checks if all packets have been received
    public boolean allPacketsReceived() {
        return file.size() == lastPacketNum+1 && fileName != null;
    }

}
