import java.io.*;
import java.util.*;

import com.rabbitmq.client.*;

/**
 * Represents a physical node in the network.
 */
public class PhysicalNode {

    private int nodeID;
    private ArrayList<Integer> neighbors;
    private ArrayList<String> nodeInformation;

    private Hashtable<String, ArrayList<Integer>> shortestPaths = new Hashtable<>();

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

    public Hashtable<String, ArrayList<Integer>> getShortestPaths() {
        return shortestPaths;
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

    /**
     * Generates shortest paths from the current physical node to all other physical nodes in the intranet.
     * Uses Dijkstra's algorithm to find the shortest paths.
     * Uses the neighborTable containing information about neighbors of each node.
     * Populates the shortestPaths map with the shortest paths found.
     */
    public void generateNodePaths() {
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

        for (int node : neighborTable.keySet()) {
            if (node != this.getNodeID()) {
                int currentNode = node;
                ArrayList<Integer> path = new ArrayList<>();
                while (currentNode != this.getNodeID()) {
                    path.add(0, currentNode);
                    currentNode = parentMap.get(currentNode);
                }
                shortestPaths.put(this.getNodeID() + "-" + node, path);
            }
        }
    }

    /**
     * Retrieves the next node to traverse towards the given destination router.
     *
     * @param destinationRouter The ID of the destination router.
     * @return The ID of the next node to traverse towards the destination router,
     *         or null if there is no path to the destination or if the destination is unreachable.
     */
    public Integer getNextNode(int destinationRouter) {
        String key = this.nodeID + "-" + destinationRouter;
        if (!shortestPaths.containsKey(key)) {
            return null;
        }
        ArrayList<Integer> path = shortestPaths.get(key);
        if (path.isEmpty()) {
            return null;
        }
        return path.get(0);
    }

    /**
     * Creates a neighbor table based on the provided nodes information.
     *
     * @param nodesInformation A list containing information about the connections between nodes.
     *                         Each element represents a node and contains a list of 0s and 1s indicating
     *                         whether there is a connection to other nodes (0 for no connection, 1 for connection).
     */
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

    /**
     * Sets up a connection to RabbitMQ.
     *
     * @throws Exception If an error occurs during RabbitMQ setup.
     */
    private void setupRabbitMQ() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        // Set necessary connection properties, e.g., host
        factory.setHost("localhost");
        connection = factory.newConnection();
        channel = connection.createChannel();
    }

    /**
     * Creates message queues for communicating with the neighboring routers.
     *
     * @throws Exception If an error occurs during queue creation.
     */
    public void createQueuesForNeighbors() throws Exception {
        for (Integer neighborID : neighbors) {
            String queueName1 = "queue_" + nodeID + "_" + neighborID;
            String queueName2 = "queue_" + neighborID + "_" + nodeID;
            channel.queueDeclare(queueName1, false, false, false, null);
            channel.queueDeclare(queueName2, false, false, false, null);
        }
    }

    /**
     * Creates a message queue for communication between the physical and virtual layers.
     *
     * @throws Exception If an error occurs during queue creation.
     */
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
            n.generateNodePaths();
            n.createQueuesForNeighbors();
            n.createQueueBetweenLayers();

            DeliverCallback deliverCallbackPhysicalNodes = (consumerTag, delivery) -> {
                Request request = RequestDeserializer.deserialize(delivery.getBody());
                if (request != null) {
                    if (request.getDestinationNodeId() == n.getNodeID()) {
                        sendToVirtualNode(n.channel, new Request(request.getMessage(), request.getOriginalNodeId(), n.getNodeID(),
                                request.getDestinationNodeId(), request.getOption(), request.getCounter()));
                    } else {
                        Integer nextNode = n.getNextNode(request.getDestinationNodeId());
                        if (nextNode != null) {
                            sendToPhysicalNode(n.channel, new Request(request.getMessage(), request.getOriginalNodeId(), n.getNodeID(),
                                    request.getDestinationNodeId(), request.getOption(), request.getCounter()), nextNode);
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

                    if (request.getMessage().equals("getNetworkSize") && request.getDestinationNodeId() == -1) {
                        sendToVirtualNode(n.channel, new Request("networkSize", request.getOriginalNodeId(), n.getNodeID(),
                                request.getDestinationNodeId(), n.getNeighborTable().size()));
                    } else {
                        Integer nextNode = n.getNextNode(request.getDestinationNodeId());
                        if (nextNode != null) {
                            sendToPhysicalNode(n.channel, new Request(request.getMessage(), request.getOriginalNodeId(), n.getNodeID(),
                                    request.getDestinationNodeId(), request.getOption(), request.getCounter()), nextNode);
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

    /**
     * Sends a request to a physical node.
     *
     * @param channel   The channel used for communication.
     * @param request   The request to be sent.
     * @param nextNode  The ID of the next node to which the request will be sent.
     * @throws IOException If an I/O error occurs while sending the request.
     */
    private static void sendToPhysicalNode(Channel channel, Request request, int nextNode) throws IOException {
        request.setCreationTime(System.currentTimeMillis());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(request);
        oos.flush();
        String queue = "queue_" + request.getSenderNodeId() + "_" + nextNode;
        Logger.log("Physical Node " + request.getSenderNodeId() + " : Sending message in the Physical Layer from " + request.getSenderNodeId() + " to " + nextNode);
        channel.basicPublish("", queue, null, bos.toByteArray());
    }

    /**
     * Sends a request to a virtual node.
     *
     * @param channel   The channel used for communication.
     * @param request   The request to be sent.
     * @throws IOException If an I/O error occurs while sending the request.
     */
    private static void sendToVirtualNode(Channel channel, Request request) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(request);
        oos.flush();
        String queue = "queue" + request.getSenderNodeId() + "_p_v";
        Logger.log("Physical Node " + request.getSenderNodeId() + " : Sending message from Physical to Virtual Layer");
        channel.basicPublish("", queue, null, bos.toByteArray());
    }
}
