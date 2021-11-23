package segmentedfilesystem;

import java.util.Arrays;

public class DataPacket extends Packet {

    public DataPacket(byte[] data, int length) {
        this.data = data;
        this.length = length;
    }

    // Get the data from the data packet    
    public byte[] getData() {
        byte[] packetData = new byte[length-4];
        for (int i = 4; i < length; i++) {
            packetData[i-4] = data[i]; //Start at index 4 because of the 4 byte header
        }
        return packetData;
    }

    // Get the packet number from the packet
    public int getPacketNumber() {
        byte firstByte = data[2];
        byte secondByte = data[3];
        int firstInt = Byte.toUnsignedInt(firstByte);
        int secondInt = Byte.toUnsignedInt(secondByte);
        int packetNumber = firstInt * 256 + secondInt; //Convert to one long int
        return packetNumber;
    }

    public byte[] trimNulls(byte[] bytes, int length) {
        return Arrays.copyOfRange(bytes, 0, length);
    }
}
