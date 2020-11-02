import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class LWB extends Thread {
    private final int OUTGOING_HWB_PORT;
    private final int OUTGOING_PORT;

    private DataInputStream diStreamHWB;
    private DataOutputStream doStreamHWB;

    private final AnalogueComms analogueComms;
    private final int id;
    private final String process;

    private final ArrayList<DedicatedOutgoingSocket> dedicatedOutgoingSocketArrayList;

    public LWB(String process, int outgoingHwbPort, int myPort, int outgoingPort, int id){
        this.OUTGOING_HWB_PORT = outgoingHwbPort;
        this.OUTGOING_PORT = outgoingPort;
        this.id = id;
        this.process = process;
        dedicatedOutgoingSocketArrayList = new ArrayList<>();
        analogueComms = new AnalogueComms(this, myPort, id, process);
        analogueComms.start();
    }

    @Override
    public synchronized void run() {
        try {
            connectToParent();
            doStreamHWB.writeUTF("ONLINE");
            doStreamHWB.writeUTF(process);
            boolean connect = diStreamHWB.readBoolean();

            if (connect){
                DedicatedOutgoingSocket dedicatedOutgoing = new DedicatedOutgoingSocket(OUTGOING_PORT, process, analogueComms, id);
                dedicatedOutgoing.start();
                dedicatedOutgoingSocketArrayList.add(dedicatedOutgoing);
                analogueComms.registerDedicated(dedicatedOutgoing);
            }
            String message = diStreamHWB.readUTF();

            if (message.equals("WORK")){
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
            System.out.println("\tSoc el procés lightweight " + process);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        System.out.println(formatter.format(calendar.getTime()));

        try {
            doStreamHWB.writeUTF("LWB DONE");
            doStreamHWB.writeUTF(process);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void connectToParent() throws IOException {
        InetAddress iAddress = InetAddress.getLocalHost();
        String IP = iAddress.getHostAddress();

        System.out.println(process + " connecting to parent...");
        Socket socketHWB = new Socket(String.valueOf(IP), OUTGOING_HWB_PORT);
        doStreamHWB = new DataOutputStream(socketHWB.getOutputStream());
        diStreamHWB = new DataInputStream(socketHWB.getInputStream());
    }

    public void waitForResume() {
        try {
            diStreamHWB.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
