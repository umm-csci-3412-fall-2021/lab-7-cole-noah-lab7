package segmentedfilesystem;

public abstract class Packet {
    public int fileId;
    public int status;

    public Packet(int fileId, int status) {
        this.fileId = fileId;
        this.status = status;
    }

    public int getFileId() {
        return fileId;
    }

    public int getStatus() {
        return status;
    }
}
