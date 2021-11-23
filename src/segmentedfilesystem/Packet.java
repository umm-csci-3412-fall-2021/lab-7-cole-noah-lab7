package segmentedfilesystem;

abstract class Packet {
    protected byte[] data;
    int length;

    public int getFileId() {
        return Byte.toUnsignedInt(data[1]);
    } 

    public int getStatus() {
        return Byte.toUnsignedInt(data[0]);
    }

    public boolean isLastPacket() {
        return (getStatus() % 4 == 3);
    }
}
