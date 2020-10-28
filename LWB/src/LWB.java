import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class LWB extends Thread {
    private int OUTGOING_HWB_PORT;
    private int OUTGOING_PORT;
    private String TMSTP;

    private Socket socketHWB;
    private DataInputStream diStreamHWB;
    private DataOutputStream doStreamHWB;

    private AnalogueComms analogueComms;
    private int id;
    private String className;

    private ArrayList<DedicatedOutgoingSocket> dedicatedOutgoingSocketArrayList;

    public LWB(String className, int outgoingHwbPort, int myPort, int outgoingPort, String time_stamp_lwb, int id){
        this.OUTGOING_HWB_PORT = outgoingHwbPort;
        this.OUTGOING_PORT = outgoingPort;
        this.id = id;
        this.className = className;
        this.TMSTP = time_stamp_lwb;
        dedicatedOutgoingSocketArrayList = new ArrayList<>();
        analogueComms = new AnalogueComms(this, myPort, time_stamp_lwb, id);
        analogueComms.start();
    }

    @Override
    public synchronized void run() {
        try {
            connectToParent();
            doStreamHWB.writeUTF("ONLINE");
            doStreamHWB.writeUTF(className);
            boolean connect = diStreamHWB.readBoolean();

            if (connect){
                DedicatedOutgoingSocket dedicatedOutgoing = new DedicatedOutgoingSocket(this, OUTGOING_PORT, TMSTP, analogueComms, id);
                dedicatedOutgoing.start();
                dedicatedOutgoingSocketArrayList.add(dedicatedOutgoing);
                analogueComms.registerDedicated(dedicatedOutgoing);
            }
            String message = diStreamHWB.readUTF();

            if (message.equals("WORK")){
                System.out.println("Got work");
                for (DedicatedOutgoingSocket dedicatedOut : dedicatedOutgoingSocketArrayList) {
                    dedicatedOut.myNotify();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void useScreen() {
        for (int i = 0; i < 10; i++){
            System.out.println("\tSoc el procés lightweight " + TMSTP);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            doStreamHWB.writeUTF("LWB DONE");
            doStreamHWB.writeUTF(className);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("finished useScreen?");
    }


    private void connectToParent() throws IOException {
        InetAddress iAddress = InetAddress.getLocalHost();
        String IP = iAddress.getHostAddress();

        System.out.println(className + " connecting to parent");
        socketHWB = new Socket(String.valueOf(IP), OUTGOING_HWB_PORT);
        doStreamHWB = new DataOutputStream(socketHWB.getOutputStream());
        diStreamHWB = new DataInputStream(socketHWB.getInputStream());
    }

    public void waitForResume() {
        try {
            System.out.println("waiting for HWB's response to resume");
            String aux = diStreamHWB.readUTF();
            System.out.println("I read: " + aux);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
