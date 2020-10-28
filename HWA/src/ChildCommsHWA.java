import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ChildCommsHWA extends Thread {
    private final static int INCOME_PORT = 44444;

    public boolean LWA1Online;
    public boolean LWA2Online;
    public boolean LWA3Online;
    private boolean LWA1Executed;
    private boolean LWA2Executed;
    private boolean LWA3Executed;

    private S_HWA parent;
    private ArrayList<DedicatedChildCommsHWA> dedicatedChildCommsList;

    private final static String LWA1 = "LWA1";
    private final static String LWA2 = "LWA2";
    private final static String LWA3 = "LWA3";

    public ChildCommsHWA(S_HWA s_hwa) {
        LWA1Online = false;
        LWA2Online = false;
        LWA3Online = false;
        LWA1Executed = LWA2Executed = LWA3Executed = false;
        dedicatedChildCommsList = new ArrayList<>();
        this.parent = s_hwa;
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
        DedicatedChildCommsHWA dedicatedChildCommsHWA = new DedicatedChildCommsHWA(socket/*, s_hwa*/, this);
        dedicatedChildCommsHWA.start();
        dedicatedChildCommsList.add(dedicatedChildCommsHWA);
    }

    public void notifyChildrensToConnect() {
        for (int i = 0; i < dedicatedChildCommsList.size(); i++){
            dedicatedChildCommsList.get(i).connectToAnalogues();
        }
    }

    public void setChildDone(String childName) {
        switch (childName) {
            case LWA1:
                LWA1Executed = true;
                System.out.println("LWA1 executed to true");
                break;

            case LWA2:
                LWA2Executed = true;
                System.out.println("LWA2 executed to true");
                break;

            case LWA3:
                LWA3Executed = true;
                System.out.println("LWA3 executed to true");
                break;
        }
        if (LWA1Executed && LWA2Executed && LWA3Executed){
            childsDone();
            LWA1Executed = LWA2Executed = LWA3Executed = false;
        }
    }

    public void childsDone() {
        parent.myNotify();
    }

    public void childsWork() {
        for (DedicatedChildCommsHWA dedicatedChild : dedicatedChildCommsList) {
            dedicatedChild.work();
        }
    }
}
