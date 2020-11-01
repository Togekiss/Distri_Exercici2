import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;


public class DedicatedIncomingSocket implements Runnable {
    private final Socket socket;
    private DataInputStream diStream;
    private DataOutputStream doStream;

    private final AnalogueComms analogueComms;
    private final int id;

    public DedicatedIncomingSocket(Socket socket, AnalogueComms analogueComms, int id) {
        this.socket = socket;
        this.analogueComms = analogueComms;
        this.id = id;
    }

    @Override
    public void run() {
        try {
            diStream = new DataInputStream(socket.getInputStream());
            doStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true){
            try {
                String request = diStream.readUTF();
                actOnRequest(request);
            } catch (SocketException se){
                se.printStackTrace();
                System.exit(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void actOnRequest(String request) throws IOException {
        int clock;
        int id;
        switch (request){
            case "LAMPORT REQUEST":
                //Get petition
                String process = diStream.readUTF();
                clock = diStream.readInt();
                id = diStream.readInt();
                analogueComms.addToQueue(clock, process, id);
                analogueComms.updateClock(clock);
                analogueComms.increaseClock();

                //Answer petition
                doStream.writeInt(analogueComms.getClock());
                doStream.writeInt(this.id);
                analogueComms.increaseClock();
                break;

            case "RELEASE":
                String releaseProcess = diStream.readUTF();
                analogueComms.releaseRequest(releaseProcess);
                analogueComms.myNotify();
                break;
        }
    }

    private synchronized int getMyRequestedClock(String process) {
        LinkedList<LamportRequest> lamportRequest = analogueComms.getLamportQueue();
        for (LamportRequest l : lamportRequest){
            if (l.getProcess().equals(process)){
                return l.getClock();
            }
        }
        return 0;
    }
}
