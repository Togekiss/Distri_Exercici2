import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class DedicatedOutgoingSocket extends Thread {
    private int OUTGOING_PORT;

    private Socket socket;
    private DataInputStream diStream;
    private DataOutputStream doStream;

    private AnalogueComms analogueComms;
    private String process;
    private int clock;
    private int id;

    public DedicatedOutgoingSocket(LWB LWB, int outgoing_port, String tmstp, AnalogueComms analogueComms, int id) {
        OUTGOING_PORT = outgoing_port;
        process = tmstp;
        this.analogueComms = analogueComms;
        this.id = id;
        clock = analogueComms.getClock();

        try {
            InetAddress iAddress = InetAddress.getLocalHost();
            String IP = iAddress.getHostAddress();

            socket = new Socket(String.valueOf(IP), OUTGOING_PORT);
            doStream = new DataOutputStream(socket.getOutputStream());
            diStream = new DataInputStream(socket.getInputStream());

            //long requestTime = sendRequest();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true){
            try {
                synchronized (this){
                    System.out.println("Pre waited in dedicatedOutgoing");
                    this.wait();
                    System.out.println("Post waited in dedicatedOutgoing");
                }
                sendRequest();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void releaseCS(String tmstp) throws IOException {
        System.out.println("\tRealeasing " + tmstp + " in releaseCS");
        doStream.writeUTF("RELEASE");
        doStream.writeUTF(tmstp);
    }


    public void sendRequest() throws IOException {
        doStream.writeUTF("LAMPORT REQUEST");
        doStream.writeUTF(process);
        clock = analogueComms.getClock();
        doStream.writeInt(clock);
        doStream.writeInt(id);
        analogueComms.addToQueue(clock, process, id);

        try {
            waitRequestResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void waitRequestResponse() throws IOException {
        //wait for request response
        int clock = diStream.readInt();

        int firstId = diStream.readInt();
        //System.out.println("\t[SENDER - RECEIVED] Timestamp[" + responseTime + "] and ID[" + firstId + "]");
        //System.out.println("\t[SENDER - RECEIVED] ID: " + firstId);

        analogueComms.gotAnswer(process, clock);
    }

    public void myNotify() {
        synchronized (this){
            this.notify();
        }
    }
}