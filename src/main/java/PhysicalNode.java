import java.io.*;
import java.util.*;

import com.rabbitmq.client.*;

public class PhysicalNode {

    private int nodeID;
    private ArrayList<Integer> neighbors;
    private ArrayList<String> nodeInformation;

    // RabbitMQ
    private Connection connection;
    private Channel channel;
    private Hashtable<Integer, ArrayList<Integer>> neighborTable;

    public PhysicalNode(int nodeID) throws Exception{
        this.nodeID = nodeID;
        neighbors = new ArrayList<Integer>();
        nodeInformation = new ArrayList<String>();
        neighborTable = new Hashtable<>();

        // Setup RabbitMQ Connection
        setupRabbitMQ();
    }

    public int getNodeID() {
        return nodeID;
    }

    public void setNodeInformation(ArrayList<String> nodeInformation) {
        this.nodeInformation = nodeInformation;
    }

    public ArrayList<Integer> getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(ArrayList<Integer> neighbors) {
        this.neighbors = neighbors;
    }

    public Hashtable<Integer, ArrayList<Integer>> getNeighborTable() {
        return neighborTable;
    }

    public Integer getNextNode(int destinationRouter, Hashtable<Integer, ArrayList<Integer>> neighborTable) {
        if (!neighborTable.containsKey(destinationRouter)) {
            return null;
        }
        HashMap<Integer, Integer> parentMap = new HashMap<>();
        HashMap<Integer, Integer> distance = new HashMap<>();
        PriorityQueue<Integer> pq = new PriorityQueue<>(Comparator.comparingInt(distance::get));

        for (int node : neighborTable.keySet()) {
            if (node == this.getNodeID()) {
                distance.put(node, 0);
            } else {
                distance.put(node, Integer.MAX_VALUE);
            }
            pq.add(node);
        }

        while (!pq.isEmpty()) {
            int currentRouter = pq.poll();
            if (currentRouter == destinationRouter) {
                break;
            }

            for (int neighbor : neighborTable.get(currentRouter)) {
                int newDistance = distance.get(currentRouter) + 1;
                if (newDistance < distance.get(neighbor)) {
                    distance.put(neighbor, newDistance);
                    parentMap.put(neighbor, currentRouter);
                    pq.remove(neighbor);
                    pq.add(neighbor);
                }
            }
        }

        ArrayList<Integer> path = new ArrayList<>();
        int node = destinationRouter;
        while (node != this.getNodeID()) {
            path.add(0, node);
            node = parentMap.get(node);
        }

        if (path.isEmpty()) {
            return null;
        }

        return path.get(0);
    }

    public void createNeighborTable(ArrayList<ArrayList<String>> nodesInformation) {

        for (int i = 0; i < nodesInformation.size(); i++) {
            ArrayList<Integer> neighbors = new ArrayList<>();
            for (int j = 0; j < nodesInformation.get(i).size(); j++) {
                if (Integer.parseInt(nodesInformation.get(i).get(j)) == 1) {
                    neighbors.add(j);
                }
            }
            this.neighborTable.put(i, neighbors);
        }
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
            String queueName1 = "queue_" + nodeID + "_" + neighborID;
            String queueName2 = "queue_" + neighborID + "_" + nodeID;
            channel.queueDeclare(queueName1, false, false, false, null);
            channel.queueDeclare(queueName2, false, false, false, null);
        }
    }
   
    public static void main(String[] args) {
        if (args.length == 0) {
            System.exit(-1);
        }

        try {
            PhysicalNode n = new PhysicalNode(Integer.parseInt(args[0]));
            System.out.println("I'm node " + n.getNodeID() + " !!!!!");
            ArrayList<ArrayList<String>> nodesInformation = MatrixReader.getNodesInformation();

            for (int i = 0; i < nodesInformation.size(); i++) {
                if (i == n.getNodeID()) {
                    n.setNodeInformation(nodesInformation.get(i));
                    break;
                }
            }

            n.createNeighborTable(nodesInformation);

            n.setNeighbors(n.neighborTable.get(n.getNodeID()));
            n.createQueuesForNeighbors();

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                Request request = deserialize(delivery.getBody());
                if (request != null) {
                    if (request.getDestinationNodeId() == n.getNodeID()) {
                        System.out.println("Message received from " + request.getSenderNodeId() + ": " + request.getMessage()
                                + " - time " + (System.currentTimeMillis() - request.getCreationTime()) + " ms");
                    } else {
                        Integer nextNode = n.getNextNode(request.getDestinationNodeId(), n.getNeighborTable());
                        if (nextNode != null) {
                            System.out.println("Next node after " + n.getNodeID()
                                    + " to reach " + request.getDestinationNodeId() + ": " + nextNode);
                            sendTo(n.channel, new Request("Hello", n.getNodeID(),
                                    request.getDestinationNodeId()), nextNode);
                        }
                    }
                }
            };

            for (Integer neighborID : n.getNeighbors()) {
                String queueName = "queue_" + neighborID + "_" + n.getNodeID();
                n.channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
            }

            if (args.length == 2) {
                Integer nextNode = n.getNextNode(Integer.parseInt(args[1]), n.getNeighborTable());
                sendTo(n.channel, new Request("Hello", n.getNodeID(), Integer.parseInt(args[1])), nextNode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendTo(Channel channel, Request request, int nextNode) throws IOException {
        request.setCreationTime(System.currentTimeMillis());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(request);
        oos.flush();
        String queue = "queue_" + request.getSenderNodeId() + "_" + nextNode;
        System.out.println("Sending message from " + request.getSenderNodeId() + " to " + nextNode);
        channel.basicPublish("", queue, null, bos.toByteArray());
    }

    private static Request deserialize(byte[] bytes) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (Request) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
