import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
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
        sendTo(channel, new Request(message, nodeID, nodeID, rightNodeId));
    }

    public void sendMessageLeft(String message) throws IOException {
        SwingUtilities.invokeLater(() -> this.chatArea.append("Message sent to " + leftNodeId + ": " + message + "\n"));
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
        initGUI();
        setupRabbitMQ();
    }

    private void initGUI() {
        setTitle("Virtual Node " + nodeID);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        add(chatScrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendLeftButton, BorderLayout.WEST);
        inputPanel.add(sendRightButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        pack();
        setSize(600, 400);
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

        n.createQueueBetweenLayers();

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            Request request = RequestDeserializer.deserialize(delivery.getBody());
            if (request != null) {
                if (request.getMessage().equals("networkSize")) {
                    n.setNumberOfRouters(request.getValue());
                    n.setNeighbors();

                } else {
                    System.out.println("Message received from " + request.getSenderNodeId() + ": " + request.getMessage()
                            + " - time " + (System.currentTimeMillis() - request.getCreationTime()) + " ms");
                    SwingUtilities.invokeLater(() -> n.getChatArea().append("Message received from " + request.getOriginalNodeId() + ": " + request.getMessage()
                            + " - time " + (System.currentTimeMillis() - request.getCreationTime()) + " ms\n"));
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
        System.out.println("Sending message from Virtual to Physical Node number " + request.getSenderNodeId() + " : " + request.getMessage());
        channel.basicPublish("", queue, null, bos.toByteArray());
    }

}
