import java.io.FileInputStream;
import java.io.InputStream;

public class Java {
    public void doStuff() throws Exception {
        try (InputStream is = new FileInputStream("test")) {
        }
    }
}