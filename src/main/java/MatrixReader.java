import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class MatrixReader {

    public static ArrayList<String> getNodeInformation(int nodeIndex) {
        String filePath = "input.txt";
        int counter = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (counter == nodeIndex) {
                    ArrayList<String> row = new ArrayList<>();
                    for (int i = 0; i < line.length(); i++) {
                        row.add(String.valueOf(line.charAt(i)));
                    }
                    return row;
                }
                counter++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<String>();
    }
}