package segmentedfilesystem;

abstract class Packet {
    byte[] data;
    int length;

    public int getFileId(byte[] data, int length) {
        return Byte.toUnsignedInt(data[1]);
    } 
}
