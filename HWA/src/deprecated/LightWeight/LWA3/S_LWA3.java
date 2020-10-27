package Sockets.LightWeight.LWA3;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

public class S_LWA3 extends Thread {
    private final static int OUTGOING_HWA_PORT = 44444;
    private final static int OUTGOING_LWA1_PORT = 55555;
    private final static int OUTGOING_LWA2_PORT = 55556;
    public final static String TMSTP = "TIME_STAMP_LWA3";

    private Socket socketHWA;
    private DataInputStream diStreamHWA;
    private DataOutputStream doStreamHWA;

    private Socket socketLWA1;
    private DataInputStream diStreamLWA1;
    private DataOutputStream doStreamLWA1;

    private Socket socketLWA2;
    private DataInputStream diStreamLWA2;
    private DataOutputStream doStreamLWA2;

    private AnalogueCommsLWA3 analogueCommsLWA3;
    private int id;

    public S_LWA3(){
        id = 3;
        analogueCommsLWA3 = new AnalogueCommsLWA3(this, id);
        analogueCommsLWA3.start();
    }

    @Override
    public synchronized void run() {
        try {
            createSockets();
            wait();

            while (true){
                doStreamLWA1.writeUTF(TMSTP);
                doStreamLWA2.writeUTF(TMSTP);

                long time = new java.util.Date().getTime();
                doStreamLWA1.writeLong(time);
                analogueCommsLWA3.addToQueue(time, TMSTP, id);
                doStreamLWA2.writeLong(time);
                analogueCommsLWA3.addToQueue(time, TMSTP, id);

                System.out.println("Wrote to LWA1 and LWA2 from LWA3");

                long LWA1Timestamp = diStreamLWA1.readLong();
                long LWA2Timestamp = diStreamLWA2.readLong();

                System.out.println("Received in LWA3: " + LWA1Timestamp);
                System.out.println("Received in LWA3: " + LWA2Timestamp);

                if (LWA1Timestamp > time && LWA2Timestamp > time && analogueCommsLWA3.peekQueue().getProcess().equals(TMSTP)){
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

        socketLWA2 = new Socket(String.valueOf(IP), OUTGOING_LWA2_PORT);
        doStreamLWA2 = new DataOutputStream(socketLWA2.getOutputStream());
        diStreamLWA2 = new DataInputStream(socketLWA2.getInputStream());
    }

    public void notifyDone() throws IOException {
        doStreamHWA.writeUTF("LWA3_DONE");
    }

}
