import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ChildCommsHWB extends Thread {
    private final static int INCOME_PORT = 22222;

    private boolean LWB1Online;
    private boolean LWB2Online;
    private boolean LWB1Executed;
    private boolean LWB2Executed;
    private final static String LWB1 = "LWB1";
    private final static String LWB2 = "LWB2";

    private final S_HWB parent;
    private final ArrayList<DedicatedChildCommsHWB> dedicatedChildCommsList;


    public ChildCommsHWB(S_HWB s_hwb) {
        LWB1Online = LWB2Online = false;
        LWB1Executed = LWB2Executed = false;
        dedicatedChildCommsList = new ArrayList<>();
        this.parent = s_hwb;
    }

    @Override
    public void run() {
        try {
            //creem el nostre socket
            ServerSocket serverSocket = new ServerSocket(INCOME_PORT);
            while (true){
                Socket socket = serverSocket.accept();
                newDedicatedChildComms(socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void newDedicatedChildComms(Socket socket) {
        DedicatedChildCommsHWB dedicatedChildCommsHWB = new DedicatedChildCommsHWB(socket, this);
        dedicatedChildCommsHWB.start();
        dedicatedChildCommsList.add(dedicatedChildCommsHWB);
    }

    public void interconnectChilds(String childName) {
        switch (childName) {
            case LWB1:
                LWB1Online = true;
                break;

            case LWB2:
                LWB2Online = true;
                break;

        }
        if (LWB1Online && LWB2Online){
            notifyChildrensToConnect();
        }
    }

    public void notifyChildrensToConnect() {
        for (int i = 0; i < dedicatedChildCommsList.size(); i++){
            dedicatedChildCommsList.get(i).connectToAnalogues();
        }
    }

    private void childsDone() {
        parent.myNotify();
    }

    public void childsWork() {
        for (DedicatedChildCommsHWB dedicatedChild : dedicatedChildCommsList) {
            dedicatedChild.work();
        }
    }

    public void setChildDone(String childName) {
        switch (childName) {
            case LWB1:
                LWB1Executed = true;
                System.out.println("LWB1 executed.");
                break;

            case LWB2:
                LWB2Executed = true;
                System.out.println("LWB2 executed.");
                break;

        }
        if (LWB1Executed && LWB2Executed){
            childsDone();
            LWB1Executed = LWB2Executed = false;
        }
    }
}