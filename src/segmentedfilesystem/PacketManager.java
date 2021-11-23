package segmentedfilesystem;

import java.net.DatagramPacket;

public class PacketManager {
    int[] idArray = {-1, -1, -1};
    int fileIdIndex = 0;
    static ReceivedFile file1 = new ReceivedFile();
    static ReceivedFile file2 = new ReceivedFile();
    static ReceivedFile file3 = new ReceivedFile();

    //Takes a packet and assigns it to the correct file based on the packets fileId
    public void assignPacketToFile(Packet packet) {
        int fileId = packet.getFileId();

        for (int i = 0; i < idArray.length; i++) {
            if (fileId == idArray[0]) {
                file1.addPacket(packet);
            } else if (fileId == idArray[1]) {
                file2.addPacket(packet);
            } else if (fileId == idArray[2]) {
                file3.addPacket(packet);
            } else {
                checkFiles(packet);
                idArray[fileIdIndex] = fileId;
                fileIdIndex++;
            }
        }
    }

    //Checks to see if an initialized ReceivedFile file has been used
    public void checkFiles(Packet packet){
        if (file1 == null){
            file1 = new ReceivedFile();
        }
        if(idArray[0] == -1){
            file1.newFile(packet);
        } else if (idArray[1] == -1){
            file2.newFile(packet);
        } else if (idArray[2] == -1) {
            file3.newFile(packet);
        }
    }

    public void sortPackets(byte[] data, int length) {
        if (getStatus(data) % 2 == 0) {
            HeaderPacket headerPacket = new HeaderPacket(data, length);
            assignPacketToFile(headerPacket);
        } else {
            DataPacket dataPacket = new DataPacket(data, length);
            assignPacketToFile(dataPacket);
        }
    }

    public static boolean allPacketsReceived(){
        //if file1 & file2 & file3 are all done,
        //then return true. otherwise return false
        if (file1.allPacketsReceived() && file2.allPacketsReceived() && file3.allPacketsReceived()){
            return true;
        } else {
            return false;
        }
    }

    public int getStatus(byte[] data) {
        return Byte.toUnsignedInt(data[0]);
    }
}
