package Sockets.LightWeight.LWA2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

public class S_LWA2 extends Thread {
    private final static int OUTGOING_HWA_PORT = 44444;
    private final static int OUTGOING_LWA1_PORT = 55555;
    private final static int OUTGOING_LWA3_PORT = 55557;
    private final static String TMSTP = "TIME_STAMP_LWA2";

    private Socket socketHWA;
    private DataInputStream diStreamHWA;
    private DataOutputStream doStreamHWA;

    private Socket socketLWA1;
    private DataInputStream diStreamLWA1;
    private DataOutputStream doStreamLWA1;

    private Socket socketLWA3;
    private DataInputStream diStreamLWA3;
    private DataOutputStream doStreamLWA3;

    private AnalogueCommsLWA2 analogueCommsLWA2;
    private int id;

    public S_LWA2(){
        id = 2;
        analogueCommsLWA2 = new AnalogueCommsLWA2(this, id);
        analogueCommsLWA2.start();
    }

    @Override
    public synchronized void run() {
        try {
            createSockets();
            wait();

            while (true){
                doStreamLWA1.writeUTF(TMSTP);
                doStreamLWA3.writeUTF(TMSTP);

                long time = new java.util.Date().getTime();
                doStreamLWA1.writeLong(time);
                analogueCommsLWA2.addToQueue(time, TMSTP, id);
                doStreamLWA3.writeLong(time);
                analogueCommsLWA2.addToQueue(time, TMSTP, id);

                System.out.println("Wrote to LWA1 and LWA3 from LWA2");

                long LWA1Timestamp = diStreamLWA1.readLong();
                long LWA3Timestamp = diStreamLWA3.readLong();

                System.out.println("Received in LWA2: " + LWA1Timestamp);
                System.out.println("Received in LWA2: " + LWA3Timestamp);

                if (LWA1Timestamp > time && LWA3Timestamp > time && analogueCommsLWA2.peekQueue().getProcess().equals(TMSTP)){
                    System.out.println("Got both ACKs in LWA1");
                    break;
                }
            }

        } catch (ConnectException ignored) {
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createSockets() throws IOException {
        InetAddress iAddress = InetAddress.getLocalHost();
        String IP = iAddress.getHostAddress();

        socketHWA = new Socket(String.valueOf(IP), OUTGOING_HWA_PORT);
        doStreamHWA = new DataOutputStream(socketHWA.getOutputStream());
        diStreamHWA = new DataInputStream(socketHWA.getInputStream());

        socketLWA1 = new Socket(String.valueOf(IP), OUTGOING_LWA1_PORT);
        doStreamLWA1 = new DataOutputStream(socketLWA1.getOutputStream());
        diStreamLWA1 = new DataInputStream(socketLWA1.getInputStream());

        socketLWA3 = new Socket(String.valueOf(IP), OUTGOING_LWA3_PORT);
        doStreamLWA3 = new DataOutputStream(socketLWA3.getOutputStream());
        diStreamLWA3 = new DataInputStream(socketLWA3.getInputStream());
    }

    public void notifyDone() throws IOException {
        doStreamHWA.writeUTF("LWA2_DONE");
    }

}
