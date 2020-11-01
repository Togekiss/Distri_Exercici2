import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class DedicatedOutgoingSocket extends Thread {
    private DataInputStream diStream;
    private DataOutputStream doStream;

    private final AnalogueComms analogueComms;
    private final String process;
    private int clock;
    private final int id;

    public DedicatedOutgoingSocket(int outgoing_port, String process, AnalogueComms analogueComms, int id) {
        this.process = process;
        this.analogueComms = analogueComms;
        this.id = id;
        clock = analogueComms.getClock();

        try {
            InetAddress iAddress = InetAddress.getLocalHost();
            String IP = iAddress.getHostAddress();

            Socket socket = new Socket(String.valueOf(IP), outgoing_port);
            doStream = new DataOutputStream(socket.getOutputStream());
            diStream = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true){
            try {
                synchronized (this){
                    this.wait();
                }
                sendRequest();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void releaseCS(String process) throws IOException {
        doStream.writeUTF("RELEASE");
        doStream.writeUTF(process);
    }


    public void sendRequest() throws IOException {
        doStream.writeUTF("LAMPORT REQUEST");
        doStream.writeUTF(process);
        clock = analogueComms.getClock();
        doStream.writeInt(clock);
        doStream.writeInt(id);
        analogueComms.addToQueue(clock, process, id);
        analogueComms.increaseClock();
        try {
            waitRequestResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void waitRequestResponse() throws IOException {
        //wait for request response
        int clock = diStream.readInt();
        int id = diStream.readInt();

        analogueComms.updateClock(clock);
        analogueComms.increaseClock();
        analogueComms.checkAnswers(clock, id);
    }

    public void myNotify() {
        synchronized (this){
            this.notify();
        }
    }
}