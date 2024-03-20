import java.io.Serializable;

public class RouterRequest implements Serializable, Request {
    private final String message;
    private final int senderNodeId;
    private final int destinationNodeId;
    private long creationTime;

    public RouterRequest(String message, int senderNodeId, int destinationNodeId) {
        this.message = message;
        this.senderNodeId = senderNodeId;
        this.destinationNodeId = destinationNodeId;
        this.creationTime = System.currentTimeMillis();
    }

    public String getMessage() {
        return message;
    }

    public int getSenderNodeId() {
        return senderNodeId;
    }

    public int getDestinationNodeId() {
        return destinationNodeId;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    @Override
    public String toString() {
        return "Request{" +
                "message='" + message + '\'' +
                ", senderNodeId=" + senderNodeId +
                ", destinationNodeId=" + destinationNodeId +
                ", creationTime=" + creationTime +
                '}';
    }
}