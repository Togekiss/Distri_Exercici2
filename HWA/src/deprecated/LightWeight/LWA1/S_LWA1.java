package Sockets.LightWeight.LWA1;

import Sockets.LightWeight.LamportRequest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

public class S_LWA1 extends Thread {
    private final static int OUTGOING_HWA_PORT = 44444;
    private final static int OUTGOING_LWA2_PORT = 55556;
    private final static int OUTGOING_LWA3_PORT = 55557;
    private final static String TMSTP = "TIME_STAMP_LWA1";

    private Socket socketHWA;
    private DataInputStream diStreamHWA;
    private DataOutputStream doStreamHWA;

    private Socket socketLWA2;
    private ObjectInputStream oiStreamLWA2;
    private DataOutputStream doStreamLWA2;

    private Socket socketLWA3;
    private ObjectInputStream oiStreamLWA3;
    private DataOutputStream doStreamLWA3;

    private AnalogueCommsLWA1 analogueCommsLWA1;
    private int id;

    public S_LWA1(){
        id = 1;
        analogueCommsLWA1 = new AnalogueCommsLWA1(this, id);
        analogueCommsLWA1.start();
    }

    @Override
    public synchronized void run() {
        try {
            createSockets();
            wait();

            while (true){
                doStreamLWA2.writeUTF(TMSTP);
                doStreamLWA3.writeUTF(TMSTP);

                long time = new java.util.Date().getTime();
                doStreamLWA2.writeLong(time);
                analogueCommsLWA1.addToQueue(time, TMSTP, id);
                doStreamLWA3.writeLong(time);
                analogueCommsLWA1.addToQueue(time, TMSTP, id);

                System.out.println("Wrote to LWA2 and LWA3 from LWA1");

                Object obj = oiStreamLWA2.readObject();
                LamportRequest LWA2Response = (LamportRequest) obj;
                obj = oiStreamLWA3.readObject();
                LamportRequest LWA3Response = (LamportRequest) obj;

                System.out.println("Received in LWA1: " + LWA2Response.getTimeStamp());
                System.out.println("Received in LWA1: " + LWA3Response.getTimeStamp());

                if (analogueCommsLWA1.peekQueue().getProcess().equals(TMSTP)){
                    if (LWA2Response.getTimeStamp() > time && LWA3Response.getTimeStamp() > time){
                        //work
                    }else if (LWA2Response.getTimeStamp() == time && LWA3Response.getTimeStamp() == time && analogueCommsLWA1.peekQueue().getId() < LWA2Response.getId() && analogueCommsLWA1.peekQueue().getId() < LWA3Response.getId()){
                        //work
                    }
                }
            }

        } catch (ConnectException ignored) {
        } catch (IOException | InterruptedException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void createSockets() throws IOException {
        InetAddress iAddress = InetAddress.getLocalHost();
        String IP = iAddress.getHostAddress();

        socketHWA = new Socket(String.valueOf(IP), OUTGOING_HWA_PORT);
        doStreamHWA = new DataOutputStream(socketHWA.getOutputStream());
        diStreamHWA = new DataInputStream(socketHWA.getInputStream());

        socketLWA2 = new Socket(String.valueOf(IP), OUTGOING_LWA2_PORT);
        doStreamLWA2 = new DataOutputStream(socketLWA2.getOutputStream());
        oiStreamLWA2 = new ObjectInputStream(socketLWA2.getInputStream());

        socketLWA3 = new Socket(String.valueOf(IP), OUTGOING_LWA3_PORT);
        doStreamLWA3 = new DataOutputStream(socketLWA3.getOutputStream());
        oiStreamLWA3 = new ObjectInputStream(socketLWA3.getInputStream());

    }

    public void notifyDone() throws IOException {
        doStreamHWA.writeUTF("LWA1_DONE");
    }

}
