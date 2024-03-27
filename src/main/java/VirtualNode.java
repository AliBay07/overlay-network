import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Represents a virtual node, extending JFrame to provide a graphical user interface.
 */
public class VirtualNode extends JFrame {
    private int nodeID;
    private int leftNodeId;
    private int rightNodeId;
    private int numberOfRouters = 0;
    private final JTextArea chatArea;
    private final JTextField inputField;
    private final JButton sendLeftButton;
    private final JButton sendRightButton;
    private final JButton sendToButton;
    private JLabel imageLabel = new JLabel();
    private boolean withGraph = false;

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

    public boolean isWithGraph() {
        return withGraph;
    }

    public void setWithGraph(boolean withGraph) {
        this.withGraph = withGraph;
    }

    public JTextArea getChatArea() {
        return chatArea;
    }

    /**
     * Sets the neighboring node IDs based on the current node ID.
     */
    private void setNeighbors() {
        this.rightNodeId = (nodeID + 1) % numberOfRouters;
        this.leftNodeId = (nodeID - 1 + numberOfRouters) % numberOfRouters;
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

    /**
     * Sends a message to the right neighbor.
     *
     * @param senderId The ID of the sender.
     * @param message The message to be sent.
     * @param counter The remaining number of hops for the message.
     * @throws IOException If an I/O error occurs while sending the message.
     */
    public void sendMessageRight(int senderId, String message, int counter) throws IOException {
        SwingUtilities.invokeLater(() -> this.chatArea.append("Message sent right to " + rightNodeId + ": " + message + "\n"));
        Logger.log("\n========SENDING NEW MESSAGE RIGHT " + "'" + message + "' FROM " + this.nodeID + " TO " +  rightNodeId + "========");
        counter = (counter > 0) ? counter - 1 : counter;
        sendTo(channel, new Request(message, senderId, nodeID, rightNodeId, "R", counter));
    }

    /**
     * Sends a message to the left neighbor.
     *
     * @param senderId The ID of the sender.
     * @param message The message to be sent.
     * @param counter The remaining number of hops for the message.
     * @throws IOException If an I/O error occurs while sending the message.
     */
    public void sendMessageLeft(int senderId, String message, int counter) throws IOException {
        SwingUtilities.invokeLater(() -> this.chatArea.append("Message sent left to " + leftNodeId + ": " + message + "\n"));
        Logger.log("\n========SENDING NEW MESSAGE LEFT " + "'" + message + "' FROM " + this.nodeID + " TO " +  leftNodeId + "========");
        counter = (counter > 0) ? counter - 1 : counter;
        sendTo(channel, new Request(message, senderId, nodeID, leftNodeId, "L", counter));
    }

    /**
     * Sends a message to a virtual node.
     *
     * @param message The message to be sent.
     * @param destinationId The ID of the destination virtual node.
     * @throws IOException If an I/O error occurs while sending the message.
     */
    public void sendToVirtual(String message, int destinationId) throws IOException {
        SwingUtilities.invokeLater(() -> this.chatArea.append("Message sent to " + destinationId + ": " + message + "\n"));

        int counterRight = (destinationId - this.nodeID + this.numberOfRouters) % this.numberOfRouters;
        int counterLeft = (this.nodeID - destinationId + this.numberOfRouters) % this.numberOfRouters;

        Logger.log("\n!!!!!========SENDING NEW MESSAGE " + "'" + message + "' FROM " + this.nodeID + " TO " +  destinationId + "========!!!!!");

        if (counterRight <= counterLeft) {
            sendMessageRight(nodeID, message, counterRight);
        } else {
            sendMessageLeft(nodeID, message, counterLeft);
        }
    }

    public VirtualNode(int nodeID) throws Exception {
        this.nodeID = nodeID;

        chatArea = new JTextArea();
        inputField = new JTextField();
        sendLeftButton = new JButton("Send Left");
        sendRightButton = new JButton("Send Right");
        sendToButton = new JButton("Send To");

        sendRightButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = inputField.getText().trim();
                if (!message.isEmpty()) {
                    try {
                        inputField.setText("");
                        sendMessageRight(nodeID, message, 0);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });

        sendLeftButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = inputField.getText().trim();
                if (!message.isEmpty()) {
                    try {
                        inputField.setText("");
                        sendMessageLeft(nodeID, message, 0);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });

        sendToButton.addActionListener(e -> {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                JDialog dialog = new JDialog(this, "Send To", true);
                JPanel panel = new JPanel(new BorderLayout());

                JComboBox<Integer> routerList = new JComboBox<>();
                for (int i = 0; i <= numberOfRouters; i++) {
                    if (i != nodeID) {
                        routerList.addItem(i);
                    }
                }

                JButton sendButton = new JButton("Send");
                sendButton.addActionListener(sendEvent -> {
                    if (routerList.getSelectedItem() != null) {
                        int selectedRouter = (Integer) routerList.getSelectedItem();
                        try {
                            inputField.setText("");
                            sendToVirtual(message, selectedRouter);
                            dialog.dispose();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                });

                JButton closeButton = new JButton("Close");
                closeButton.addActionListener(closeEvent -> {
                    dialog.dispose();
                });

                panel.add(new JLabel("Select Router: "), BorderLayout.WEST);
                panel.add(routerList, BorderLayout.CENTER);
                JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
                buttonPanel.add(closeButton);
                buttonPanel.add(sendButton);
                panel.add(buttonPanel, BorderLayout.SOUTH);

                dialog.add(panel);
                dialog.pack();
                dialog.setLocationRelativeTo(this);
                dialog.setVisible(true);
            }
        });

        setupRabbitMQ();
    }

    /**
     * Initializes the graphical user interface for the virtual node.
     * Sets up the window title, layout, chat area, input field, and buttons.
     */
    private void initGUI() {
        setTitle("Virtual Node " + nodeID);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);

        if (withGraph) {
            BufferedImage image = null;
            try {
                image = ImageIO.read(new File("graphs/graph.png"));
                imageLabel.setIcon(new ImageIcon(image));
            } catch (IOException e) {
                e.printStackTrace();
            }

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, imageLabel, chatScrollPane);
            splitPane.setResizeWeight(0.1);

            add(splitPane, BorderLayout.CENTER);
        } else {
            add(chatScrollPane, BorderLayout.CENTER);
        }

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendLeftButton, BorderLayout.WEST);
        inputPanel.add(sendRightButton, BorderLayout.EAST);
        inputPanel.add(sendToButton, BorderLayout.SOUTH);
        add(inputPanel, BorderLayout.SOUTH);

        pack();
        setSize(800 + (withGraph ? 400 : 0), 600);
        setLocationRelativeTo(null);

        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    /**
     * Sets up a connection to RabbitMQ.
     *
     * @throws Exception If an error occurs during RabbitMQ setup.
     */
    private void setupRabbitMQ() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
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

        if (args.length == 3) {
            n.setWithGraph(true);
        }

        n.initGUI();

        n.createQueueBetweenLayers();

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            Request request = RequestDeserializer.deserialize(delivery.getBody());
            if (request != null) {
                if (request.getMessage().equals("networkSize")) {
                    n.setNumberOfRouters(request.getValue());
                    n.setNeighbors();
                } else if (request.getCounter() <= 0) {
                    SwingUtilities.invokeLater(() -> n.getChatArea().append("Message received from " + request.getOriginalNodeId() + ": " + request.getMessage()
                            + " - time " + (System.currentTimeMillis() - request.getCreationTime()) + " ms\n"));
                    Logger.log("Virtual Node " + request.getSenderNodeId() + " : Message received from node " + request.getOriginalNodeId() + ": " + request.getMessage());
                    Logger.log("========================================================");
                } else if (request.getOption().equals("L")) {
                    n.sendMessageLeft(request.getOriginalNodeId(), request.getMessage(), request.getCounter());
                } else {
                    n.sendMessageRight(request.getOriginalNodeId(), request.getMessage(), request.getCounter());
                }
            }
        };

        if (n.getNumberOfRouters() == 0) {
            sendTo(n.channel, new Request("getNetworkSize", n.getNodeID(), n.getNodeID(), 0));
        }

        String queueName = "queue" + n.getNodeID() + "_p_v";
        n.channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});


    } catch (Exception e) {
        e.printStackTrace();
    }
    }

    /**
     * Sends a request to the physical layer.
     *
     * @param channel   The channel used for communication.
     * @param request   The request to be sent.
     * @throws IOException If an I/O error occurs while sending the request.
     */
    private static void sendTo(Channel channel, Request request) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(request);
        oos.flush();
        String queue = "queue" + request.getSenderNodeId() + "_v_p";
        Logger.log("Virtual Node " + request.getSenderNodeId() + " : Sending message from Virtual to Physical Layer");
        channel.basicPublish("", queue, null, bos.toByteArray());
    }

}
