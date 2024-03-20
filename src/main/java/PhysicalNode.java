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

    public void createQueueBetweenLayers() throws Exception {
        String queueName1 = "queue" + nodeID + "_v_p";
        String queueName2 = "queue" + nodeID + "_p_v";
        channel.queueDeclare(queueName1, false, false, false, null);
        channel.queueDeclare(queueName2, false, false, false, null);
    }
   
    public static void main(String[] args) {

        if (args.length < 2) {
            System.exit(-1);
        }

        try {
            PhysicalNode n = new PhysicalNode(Integer.parseInt(args[0]));
            System.out.println("Physical Node " + n.getNodeID());
            String filePath = args[1];
            ArrayList<ArrayList<String>> nodesInformation = MatrixReader.getNodesInformation(filePath);

            for (int i = 0; i < nodesInformation.size(); i++) {
                if (i == n.getNodeID()) {
                    n.setNodeInformation(nodesInformation.get(i));
                    break;
                }
            }

            n.createNeighborTable(nodesInformation);

            n.setNeighbors(n.neighborTable.get(n.getNodeID()));
            n.createQueuesForNeighbors();
            n.createQueueBetweenLayers();

            DeliverCallback deliverCallbackPhysicalNodes = (consumerTag, delivery) -> {
                Request request = RequestDeserializer.deserialize(delivery.getBody());
                if (request != null) {
                    if (request.getDestinationNodeId() == n.getNodeID()) {
                        sendToVirtualNode(n.channel, new Request(request.getMessage(), request.getOriginalNodeId(), n.getNodeID(), request.getDestinationNodeId()));
                    } else {
                        Integer nextNode = n.getNextNode(request.getDestinationNodeId(), n.getNeighborTable());
                        if (nextNode != null) {
                            sendToPhysicalNode(n.channel, new Request(request.getMessage(), request.getOriginalNodeId(), n.getNodeID(),
                                    request.getDestinationNodeId()), nextNode);
                        } else {
                            sendToVirtualNode(n.channel, new Request("Can't find path to router!", request.getOriginalNodeId(),
                                    n.getNodeID(), request.getDestinationNodeId()));
                        }
                    }
                }
            };

            for (Integer neighborID : n.getNeighbors()) {
                String queueName = "queue_" + neighborID + "_" + n.getNodeID();
                n.channel.basicConsume(queueName, true, deliverCallbackPhysicalNodes, consumerTag -> {});
            }

            DeliverCallback deliverCallbackBetweenLayers = (consumerTag, delivery) -> {
                Request request = RequestDeserializer.deserialize(delivery.getBody());
                if (request != null) {

                    if (request.getMessage().equals("getNetworkSize")) {
                        System.out.println(request.getMessage());
                        sendToVirtualNode(n.channel, new Request("networkSize", request.getOriginalNodeId(), n.getNodeID(),
                                request.getDestinationNodeId(), n.getNeighborTable().size()));
                    } else {
                        Integer nextNode = n.getNextNode(request.getDestinationNodeId(), n.getNeighborTable());
                        if (nextNode != null) {
                            sendToPhysicalNode(n.channel, new Request(request.getMessage(), request.getOriginalNodeId(), n.getNodeID(),
                                    request.getDestinationNodeId()), nextNode);
                        } else {
                            sendToVirtualNode(n.channel, new Request("Can't find path to router!", request.getOriginalNodeId(),
                                    n.getNodeID(), request.getDestinationNodeId()));
                        }
                    }
                }
            };

            String queueName = "queue" + n.getNodeID() + "_v_p";
            n.channel.basicConsume(queueName, true, deliverCallbackBetweenLayers, consumerTag -> {});

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendToPhysicalNode(Channel channel, Request request, int nextNode) throws IOException {
        request.setCreationTime(System.currentTimeMillis());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(request);
        oos.flush();
        String queue = "queue_" + request.getSenderNodeId() + "_" + nextNode;
        System.out.println("Sending message from " + request.getSenderNodeId() + " to " + nextNode + " : " + request.getMessage());
        channel.basicPublish("", queue, null, bos.toByteArray());
    }

    private static void sendToVirtualNode(Channel channel, Request request) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(request);
        oos.flush();
        String queue = "queue" + request.getSenderNodeId() + "_p_v";
        System.out.println("Sending message from Physical to Virtual Node number " + request.getSenderNodeId() + " : " + request.getMessage());
        channel.basicPublish("", queue, null, bos.toByteArray());
    }
}
