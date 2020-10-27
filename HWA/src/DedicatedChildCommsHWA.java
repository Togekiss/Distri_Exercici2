import sun.net.ConnectionResetException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class DedicatedChildCommsHWA extends Thread{
    private Socket socket;
    private DataInputStream diStream;
    private DataOutputStream doStream;
    //private final S_HWA s_hwa;

    private ChildCommsHWA parent;


    public DedicatedChildCommsHWA(Socket socket, ChildCommsHWA childCommsHWA) {
        this.socket = socket;
        this.parent = childCommsHWA;
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
            } catch (ConnectionResetException ignored){

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
                interconnectChilds(childName);
                break;
            case "LWA DONE":
                System.out.println("notify done in HWA from LWA.");
                parent.childsDone();

                break;
        }
    }

    private void interconnectChilds(String childName) {
        switch (childName) {
            case "LWA1":
                parent.LWA1Online = true;
                System.out.println("LWA1 to true");
                break;

            case "LWA2":
                parent.LWA2Online = true;
                System.out.println("LWA2 to true");
                break;

            case "LWA3":
                parent.LWA3Online = true;
                System.out.println("LWA3 to true");
                break;

        }
        if (parent.LWA1Online && parent.LWA2Online && parent.LWA3Online){
            parent.notifyChildrensToConnect();
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
