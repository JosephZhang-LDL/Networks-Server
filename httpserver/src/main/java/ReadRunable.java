import java.util.Hashtable;
import java.util.List;

public class ReadRunable implements Runnable{
    Hashtable<String, String> fields;
    List<Byte> responseBuffer;
    public ReadRunable() {

    }

    public void run() {
        handler.readRequest(fields, new String(buffer.array(), "UTF-8"),
                                    responseBuffer, client);
    }
}
