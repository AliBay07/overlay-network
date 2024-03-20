import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class VirtualNode {
    private int nodeID;
    private int leftNodeId;
    private int rightNodeId;
    private int numberOfRouters = 0;

    // RabbitMQ
    private Connection connection;
    private Channel channel;

    public int getNodeID() {
        return nodeID;
    }

    public int getLeftNodeId() {
        return leftNodeId;
    }

    public int getRightNodeId() {
        return rightNodeId;
    }

    public void setNumberOfRouters(int numberOfRouters) {
        this.numberOfRouters = numberOfRouters;
    }

    public int getNumberOfRouters() {
        return numberOfRouters;
    }

    private void setNeighbors() {
        this.rightNodeId = (nodeID + 1) % numberOfRouters;
        this.leftNodeId = (nodeID - 1 + numberOfRouters) % numberOfRouters;
    }

    public void createQueuePhyVirt() throws Exception {
        String queueName1 = "queue" + nodeID + "_v_p";
        String queueName2 = "queue" + nodeID + "_p_v";
        channel.queueDeclare(queueName1, false, false, false, null);
        channel.queueDeclare(queueName2, false, false, false, null);
    }

    public void sendMessageRight(String message) throws IOException {
        sendTo(channel, new LayerRequest(message, nodeID, rightNodeId));
    }

    public void sendMessageLeft(String message) throws IOException {
        sendTo(channel, new LayerRequest(message, nodeID, leftNodeId));
    }

    public VirtualNode(int nodeID) throws Exception {
        this.nodeID = nodeID;

        // Setup RabbitMQ Connection
        setupRabbitMQ();
    }

    private void setupRabbitMQ() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        // Set necessary connection properties, e.g., host
        factory.setHost("localhost");
        connection = factory.newConnection();
        channel = connection.createChannel();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.exit(-1);
        }
    try {
        VirtualNode n = new VirtualNode(Integer.parseInt(args[0]));
        System.out.println("Virtual Node " + n.getNodeID());

        n.createQueuePhyVirt();

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            LayerRequest request = (LayerRequest) RequestDeserializer.deserialize(delivery.getBody());
            if (request != null) {
                if (request.getMessage().equals("networkSize")) {
                    System.out.println("I GOT THE SIZE " + request.getValue());
                    n.setNumberOfRouters(request.getValue());
                    n.setNeighbors();

                    if (args.length == 3) {
                        String message = args[1];
                        String direction = args[2];
                        if (direction.equals("l")) {
                            n.sendMessageLeft(message);
                        } else if (direction.equals("r")){
                            n.sendMessageRight(message);
                        }
                    }

                } else {
                    System.out.println("Message received from " + request.getSenderNodeId() + ": " + request.getMessage()
                            + " - time " + (System.currentTimeMillis() - request.getCreationTime()) + " ms");
                }
            }
        };

        if (n.getNumberOfRouters() == 0) {
            sendTo(n.channel, new LayerRequest("getNetworkSize", n.getNodeID(), 0));
        }

        String queueName = "queue" + n.getNodeID() + "_p_v";
        n.channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});


    } catch (Exception e) {
        e.printStackTrace();
    }
    }

    private static void sendTo(Channel channel, LayerRequest request) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(request);
        oos.flush();
        String queue = "queue" + request.getSenderNodeId() + "_v_p";
        System.out.println("Sending message from Virtual to Physical Node number " + request.getSenderNodeId());
        System.out.println("Message being sent is : " + request.getMessage());
        channel.basicPublish("", queue, null, bos.toByteArray());
    }

}
