package segmentedfilesystem;

public class HeaderPacket extends Packet {
    public String getFileName() {
        byte[] byteName = new byte[length-2];
        for (int i = 0; i < length-2; i++) 
            byteName[i] = data[i+2]; //Start at index 2 because of the 2 byte header
        return new String(byteName);
    }
}
