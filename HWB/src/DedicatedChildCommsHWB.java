import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class DedicatedChildCommsHWB extends Thread{
    private Socket socket;
    private DataInputStream diStream;
    private DataOutputStream doStream;

    private ChildCommsHWB parent;


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
                System.out.println("Waiting to read request...");
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
                System.out.println("notify done in HWB from " + childName);
                parent.setChildDone(childName);
                break;
            case "RUN STATUS":
                //doStream.writeBoolean(parent.childsDoneStatus());
                boolean status = parent.childsDoneStatus();
                if (!status){
                    doStream.writeBoolean(parent.childsDoneStatus());
                }
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
            System.out.println("Sending work");
            doStream.writeUTF("WORK");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
