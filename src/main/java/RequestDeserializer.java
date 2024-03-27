import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Utility class for deserializing Request objects from byte arrays.
 */
public class RequestDeserializer {

    /**
     * Deserializes a byte array into a Request object.
     *
     * @param bytes The byte array to deserialize.
     * @return The deserialized Request object.
     */
    public static Request deserialize(byte[] bytes) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (Request) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

}
