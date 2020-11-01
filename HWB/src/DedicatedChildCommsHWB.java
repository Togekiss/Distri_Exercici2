import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class DedicatedChildCommsHWB extends Thread{
    private final Socket socket;
    private DataInputStream diStream;
    private DataOutputStream doStream;

    private final ChildCommsHWB parent;


    public DedicatedChildCommsHWB(Socket socket, ChildCommsHWB childCommsHWB) {
        this.socket = socket;
        this.parent = childCommsHWB;
    }

    @Override
    public void run() {
        try {
            diStream = new DataInputStream(socket.getInputStream());
            doStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true){
            try {
                String request = diStream.readUTF();
                actOnRequest(request);
            } catch (SocketException se){
                se.printStackTrace();
                System.err.println("Exiting...");
                System.exit(-1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void actOnRequest(String request) throws IOException {
        switch (request){
            case "ONLINE":
                String childName = diStream.readUTF();
                System.out.println("Got ONLINE call from: " + childName);
                parent.interconnectChilds(childName);
                break;

            case "LWB DONE":
                childName = diStream.readUTF();
                parent.setChildDone(childName);
                break;
        }
    }


    public void connectToAnalogues() {
        try {
            doStream.writeBoolean(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void work() {
        try {
            System.out.println("Sending work to childs.");
            doStream.writeUTF("WORK");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
