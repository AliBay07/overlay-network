import java.util.ArrayList;

public class PhysicalNode {

    private int nodeID;
    private ArrayList<Integer> neighbors;
    private ArrayList<String> nodeInformation;

    public PhysicalNode(int nodeID) {
        this.nodeID = nodeID;
        neighbors = new ArrayList<Integer>();
        nodeInformation = new ArrayList<String>();
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

    public ArrayList<String> getNodeInformation() {
        return nodeInformation;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.exit(-1);
        }

        PhysicalNode n = new PhysicalNode(Integer.parseInt(args[0]));
        ArrayList<String> nodeInformation = MatrixReader.getNodeInformation(n.getNodeID());
        n.setNodeInformation(nodeInformation);

        for (int i = 0; i < nodeInformation.size(); i++) {
            if (Integer.parseInt(nodeInformation.get(i)) == 1) {
                n.addNeighbor(i);
            }
        }

        System.out.println(n.getNodeInformation());
        System.out.println(n.getNeighbors());

        // for each neighbor, we will create a queue dynamically,
        // using the format (queue_smallerNumber_greaterNumber)


    }
}
