import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Utility class for reading matrices from files.
 */
public class MatrixReader {

    /**
     * Reads nodes information from a file and returns it as a matrix.
     * Each element in the matrix represents a connection status between nodes,
     * where "0" indicates no connection and "1" indicates a connection.
     *
     * @param filePath The path to the file containing nodes information.
     * @return A matrix representing the connection status between nodes.
     */
    public static ArrayList<ArrayList<String>> getNodesInformation(String filePath) {
        ArrayList<ArrayList<String>> matrix = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                ArrayList<String> row = new ArrayList<>();
                for (int i = 0; i < line.length(); i++) {
                    row.add(String.valueOf(line.charAt(i)));
                }
                matrix.add(row);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return matrix;
    }
}