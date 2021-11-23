package segmentedfilesystem;

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


}
