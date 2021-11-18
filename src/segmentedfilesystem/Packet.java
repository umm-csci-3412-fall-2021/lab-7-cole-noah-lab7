package segmentedfilesystem;

abstract class Packet {
    byte[] data;
    int length;

    public int getFileId(byte[] data, int length) {
        return Byte.toUnsignedInt(data[1]);
    } 

    public int getStatus() {
        return Byte.toUnsignedInt(data[0]);
    }

    public boolean isLastPacket() {
        int status = getStatus();
        return (status % 4 == 3);
    }
}
