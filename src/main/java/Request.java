import java.io.Serializable;

public class Request implements Serializable {
    private final String message;
    private final int originalNodeId;
    private int senderNodeId;
    private int destinationNodeId;
    private long creationTime;
    private int value;

    public Request(String message, int originalNodeId, int senderNodeId, int destinationNodeId, int value) {
        this.message = message;
        this.originalNodeId = originalNodeId;
        this.senderNodeId = senderNodeId;
        this.destinationNodeId = destinationNodeId;
        this.value = value;
    }

    public Request(String message, int originalNodeId, int senderNodeId, int destinationNodeId) {
        this.message = message;
        this.originalNodeId = originalNodeId;
        this.senderNodeId = senderNodeId;
        this.destinationNodeId = destinationNodeId;
        this.creationTime = System.currentTimeMillis();
    }

    public String getMessage() {
        return message;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getSenderNodeId() {
        return senderNodeId;
    }

    public void setSenderNodeId(int senderNodeId) {
        this.senderNodeId = senderNodeId;
    }

    public int getDestinationNodeId() {
        return destinationNodeId;
    }

    public void setDestinationNodeId(int destinationNodeId) {
        this.destinationNodeId = destinationNodeId;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public int getOriginalNodeId() {
        return originalNodeId;
    }

    @Override
    public String toString() {
        return "Request{" +
                "message='" + message + '\'' +
                ", originalNodeId=" + originalNodeId +
                ", senderNodeId=" + senderNodeId +
                ", destinationNodeId=" + destinationNodeId +
                ", creationTime=" + creationTime +
                '}';
    }
}
