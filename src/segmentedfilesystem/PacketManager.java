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
            if (idArray[i] == fileId) {
                fileIdIndex = i;
                break;
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
        switch(-1){
            case idArray[0]:
                file1.newFile(packet);
                break;
            case idArray[1]:
                file2.newFile(packet);
                break;
            case idArray[2]:
                file3.newFile(packet);
                break;
            default:
                break;
        }
    }

    public void sortPackets(DatagramPacket packet) {
        byte[] data = packet.getData();
        int length = packet.getLength();

        if (getStatus(data) % 2 == 0) {
            HeaderPacket headerPacket = new HeaderPacket(data, length);
            assignPacketToFile(headerPacket);
        } else {
            DataPacket dataPacket = new DataPacket(data, length);
            assignPacketToFile(dataPacket);
        }
    }

    public int getStatus(byte[] data) {
        return Byte.toUnsignedInt(data[0]);
    }
}
