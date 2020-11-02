import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class LWA extends Thread {
    private final int OUTGOING_HWA_PORT;
    private final int FIRST_OUTGOING_PORT;
    private final int SECOND_OUTGOING_PORT;

    private DataInputStream diStreamHWA;
    private DataOutputStream doStreamHWA;

    private final AnalogueComms analogueComms;
    private final int id;
    private final String process;

    public LWA(String process, int outgoingHwaPort, int myPort, int firstOutgoingPort, int secondOutgoingPort, int id){
        this.OUTGOING_HWA_PORT = outgoingHwaPort;
        this.FIRST_OUTGOING_PORT = firstOutgoingPort;
        this.SECOND_OUTGOING_PORT = secondOutgoingPort;
        this.id = id;
        this.process = process;
        analogueComms = new AnalogueComms(this, myPort, id, process);
        analogueComms.start();
    }

    @Override
    public synchronized void run() {
        try {
            connectToParent();
            doStreamHWA.writeUTF("ONLINE");
            doStreamHWA.writeUTF(process);
            boolean connect = diStreamHWA.readBoolean();

            if (connect){
                DedicatedOutgoingSocket firstDedicatedOutgoing = new DedicatedOutgoingSocket(FIRST_OUTGOING_PORT, process, analogueComms, id);
                firstDedicatedOutgoing.start();
                DedicatedOutgoingSocket secondDedicatedOutgoing = new DedicatedOutgoingSocket(SECOND_OUTGOING_PORT, process, analogueComms, id);
                secondDedicatedOutgoing.start();
                analogueComms.registerDedicated(firstDedicatedOutgoing, secondDedicatedOutgoing);
            }
        } catch (ConnectException ignored) {
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
            doStreamHWA.writeUTF("LWA DONE");
            doStreamHWA.writeUTF(process);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void connectToParent() throws IOException {
        InetAddress iAddress = InetAddress.getLocalHost();
        String IP = iAddress.getHostAddress();

        System.out.println(process + " connecting to parent...");
        Socket socketHWA = new Socket(String.valueOf(IP), OUTGOING_HWA_PORT);
        doStreamHWA = new DataOutputStream(socketHWA.getOutputStream());
        diStreamHWA = new DataInputStream(socketHWA.getInputStream());
    }

    public void waitForResume() {
        try {
            diStreamHWA.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
