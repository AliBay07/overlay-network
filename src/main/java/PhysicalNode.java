import java.util.ArrayList;
import com.rabbitmq.client.*;

public class PhysicalNode {

    private int nodeID;
    private ArrayList<Integer> neighbors;
    private ArrayList<String> nodeInformation;

    // RabbitMQ
    private Connection connection;
    private Channel channel;

    public PhysicalNode(int nodeID) throws Exception{
        this.nodeID = nodeID;
        neighbors = new ArrayList<Integer>();
        nodeInformation = new ArrayList<String>();

        // Setup RabbitMQ Connection
        setupRabbitMQ();
    }

    public int getNodeID() {
        return nodeID;
    }

    public void setNodeID(int nodeID) {
        this.nodeID = nodeID;
    }

    public void addNeighbor(int index) {
        this.neighbors.add(index);
    }

    public ArrayList<Integer> getNeighbors() {
        return neighbors;
    }

    public void setNodeInformation(ArrayList<String> nodeInformation) {
        this.nodeInformation = nodeInformation;
    }

        private void setupRabbitMQ() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        // Set necessary connection properties, e.g., host
        factory.setHost("localhost");
        connection = factory.newConnection();
        channel = connection.createChannel();
    }

    public void createQueuesForNeighbors() throws Exception {
        for (Integer neighborID : neighbors) {
            String queueName = formatQueueName(nodeID, neighborID);
            channel.queueDeclare(queueName, false, false, false, null);
            System.out.println("Queue Created: " + queueName);
        }
    }

    private String formatQueueName(int node1, int node2) {
        if (node1 < node2) {
            return "queue_" + node1 + "_" + node2;
        } else {
            return "queue_" + node2 + "_" + node1;
        }
    }

    public ArrayList<String> getNodeInformation() {
        return nodeInformation;
    }

   
    public static void main(String[] args) {
        if (args.length == 0) {
            System.exit(-1);
        }

        try {
            PhysicalNode n = new PhysicalNode(Integer.parseInt(args[0]));
            ArrayList<String> nodeInformation = MatrixReader.getNodeInformation(n.getNodeID());
            n.setNodeInformation(nodeInformation);

            for (int i = 0; i < nodeInformation.size(); i++) {
                if (Integer.parseInt(nodeInformation.get(i)) == 1) {
                    n.addNeighbor(i);
                }
            }

            n.createQueuesForNeighbors();

            System.out.println(n.getNodeInformation());
            System.out.println(n.getNeighbors());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
