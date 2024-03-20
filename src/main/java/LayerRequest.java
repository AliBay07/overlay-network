import java.io.Serializable;

public class LayerRequest implements Serializable, Request {
    private final String message;
    private int value;
    private int senderNodeId;
    private int destinationNodeId;
    private long creationTime;

    public LayerRequest(String message) {
        this.message = message;
    }

    public LayerRequest(String message, int value) {
        this.message = message;
        this.value = value;
    }

    public LayerRequest(String message, int senderNodeId, int destinationNodeId) {
        this.message = message;
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
}
