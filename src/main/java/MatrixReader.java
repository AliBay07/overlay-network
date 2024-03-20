import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class MatrixReader {

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