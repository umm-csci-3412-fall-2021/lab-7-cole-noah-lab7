package segmentedfilesystem;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.TreeMap;

public class ReceivedFile {
    
    TreeMap<Integer, DataPacket> file;
    String fileName;
    int lastPacketNum = -2;
    byte[] finalByteArray;

    public void newFile(Packet packet) {
        TreeMap<Integer, DataPacket> file = new TreeMap<>();
        if (packet instanceof HeaderPacket) {
            HeaderPacket headerPacket = (HeaderPacket) packet;
            fileName = headerPacket.getFileName();
        } else {
            DataPacket dataPacket = (DataPacket) packet;
            int packetNum = dataPacket.getPacketNumber();
            file.put(packetNum, dataPacket);
        }

        if(packet.isLastPacket()) {
            lastPacketNum = packet.getFileId();
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
            file.put(packetNum, dataPacket);
        }

        if(packet.isLastPacket()) {
            lastPacketNum = packet.getFileId();
        }

        if(allPacketsReceived()) {
            writeFile();
        }
    }

    // Writes all fo the values in the treemap to a byte array
    public void writeToByteArray(){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            for(DataPacket packet : file.values()) {
                baos.write(packet.getData());
            }
            finalByteArray = baos.toByteArray();
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
