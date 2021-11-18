package segmentedfilesystem;

public class DataPacket extends Packet {
    // Get the data from the data packet    
    public byte[] getData() {
        byte[] data = new byte[length-4];
        for (int i = 0; i < length-4; i++) 
            data[i] = data[i+4]; //Start at index 4 because of the 4 byte header
        return data;
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
}
