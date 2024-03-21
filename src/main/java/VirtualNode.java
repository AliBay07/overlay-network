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

public class VirtualNode extends JFrame {
    private int nodeID;
    private int leftNodeId;
    private int rightNodeId;
    private int numberOfRouters = 0;
    private final JTextArea chatArea;
    private final JTextField inputField;
    private final JButton sendLeftButton;
    private final JButton sendRightButton;
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

    private void setNeighbors() {
        this.rightNodeId = (nodeID + 1) % numberOfRouters;
        this.leftNodeId = (nodeID - 1 + numberOfRouters) % numberOfRouters;
    }

    public JTextArea getChatArea() {
        return chatArea;
    }

    public void createQueueBetweenLayers() throws Exception {
        String queueName1 = "queue" + nodeID + "_v_p";
        String queueName2 = "queue" + nodeID + "_p_v";
        channel.queueDeclare(queueName1, false, false, false, null);
        channel.queueDeclare(queueName2, false, false, false, null);
    }

    public void sendMessageRight(String message) throws IOException {
        SwingUtilities.invokeLater(() -> this.chatArea.append("Message sent to " + rightNodeId + ": " + message + "\n"));
        Logger.log("\n========SENDING NEW MESSAGE " + "'" + message + "' FROM " + this.nodeID + " TO " +  rightNodeId + "========");
        sendTo(channel, new Request(message, nodeID, nodeID, rightNodeId));
    }

    public void sendMessageLeft(String message) throws IOException {
        SwingUtilities.invokeLater(() -> this.chatArea.append("Message sent to " + leftNodeId + ": " + message + "\n"));
        Logger.log("\n========SENDING NEW MESSAGE " + "'" + message + "' FROM " + this.nodeID + " TO " +  leftNodeId + "========");
        sendTo(channel, new Request(message, nodeID, nodeID, leftNodeId));
    }

    public VirtualNode(int nodeID) throws Exception {
        this.nodeID = nodeID;

        chatArea = new JTextArea();
        inputField = new JTextField();
        sendLeftButton = new JButton("Send Left");
        sendRightButton = new JButton("Send Right");

        sendRightButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = inputField.getText().trim();
                if (!message.isEmpty()) {
                    try {
                        inputField.setText("");
                        sendMessageRight(message);
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
                        sendMessageLeft(message);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });

        // Setup RabbitMQ Connection
        setupRabbitMQ();
    }

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
            } catch (IOException e) {
                e.printStackTrace();
            }

            imageLabel.setIcon(new ImageIcon(image));

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
        add(inputPanel, BorderLayout.SOUTH);

        pack();
        setSize(800 + (withGraph ? 400 : 0), 600);
        setLocationRelativeTo(null);

        SwingUtilities.invokeLater(() -> setVisible(true));
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
                } else {
                    SwingUtilities.invokeLater(() -> n.getChatArea().append("Message received from " + request.getOriginalNodeId() + ": " + request.getMessage()
                            + " - time " + (System.currentTimeMillis() - request.getCreationTime()) + " ms\n"));
                    Logger.log("Virtual Node " + request.getSenderNodeId() + " : Message received from node " + request.getOriginalNodeId() + ": " + request.getMessage());
                    Logger.log("========================================================");
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
