import java.io.Serializable;
import java.util.Date;

public class Request implements Serializable {
    private final String message;
    private final int senderNodeId;
    private final int destinationNodeId;
    private long creationTime;

    public Request(String message, int senderNodeId, int destinationNodeId) {
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