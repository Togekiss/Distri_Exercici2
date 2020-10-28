import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class AnalogueComms extends Thread {
    private int MY_PORT;
    private final LWB lwb;
    private String time_stamp_lwb;
    private LinkedList<LamportRequest> lamportQueue;

    private DedicatedOutgoingSocket dedicatedOutgoing;
    private DedicatedIncomingSocket dedicatedLWB;

    private String process;
    private int id;
    private CheckCriticalZone checkCriticalZone;
    private int clock;

    public AnalogueComms(LWB lwb, int myPort, String time_stamp_lwb, int id) {
        this.MY_PORT = myPort;
        this.lwb = lwb;
        this.time_stamp_lwb = time_stamp_lwb;
        this.id = id;
        lamportQueue = new LinkedList<>();
        this.checkCriticalZone = new CheckCriticalZone(this);
        checkCriticalZone.start();
        this.clock = 0;
    }

    @Override
    public void run() {
        try {
            //creem el nostre socket
            ServerSocket serverSocket = new ServerSocket(MY_PORT);

            while (true){
                Socket socket = serverSocket.accept();
                newDedicatedAnalogueComms(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void newDedicatedAnalogueComms(Socket socket) {
        dedicatedLWB = new DedicatedIncomingSocket(socket, this, id);
        Thread thread = new Thread(dedicatedLWB);
        thread.start();
    }

    public synchronized void addToQueue(int clock, String process, int id) {
        LamportRequest request = new LamportRequest(clock, process, id);
        boolean found = false;
        for (LamportRequest lr : lamportQueue){
            if (lr.getProcess().equals(process)){
                found = true;
                break;
            }
        }

        if (!found){
            System.out.println("Adding request: " + request.toString());
            lamportQueue.add(request);

            System.out.println();
            for (LamportRequest lr : lamportQueue){
                System.out.println("[LAMPORT (add)]" + lr.toString());
            }
        }
    }

    public synchronized void releaseProcess(String tmstp) throws IOException {
        System.out.println("### sending release msg to dedicatedOutgoing ### " + tmstp);
        dedicatedOutgoing.releaseCS(tmstp);
        releaseRequest(tmstp);
        dedicatedOutgoing.myNotify();
        lwb.waitForResume();
    }

    public void registerDedicated(DedicatedOutgoingSocket dedicatedOutgoing) {
        this.dedicatedOutgoing = dedicatedOutgoing;
    }

    public synchronized void releaseRequest(String releaseProcess) {
        synchronized (lamportQueue){
            lamportQueue.removeIf(lr -> lr.getProcess().equals(releaseProcess));
        }
        System.out.println();
        for (LamportRequest lr : lamportQueue){
            System.out.println("[LAMPORT (remove)]" + lr.toString());
        }
    }

    public synchronized void gotAnswer(String process, int clock) {
        addToQueue(clock, process, id);
        this.process = process;
        this.clock = clock;
        System.out.println("\tGot answer. Checking queue");
        myNotify();
    }

    public void myNotify() {
        checkCriticalZone.myNotify();
    }

    public LinkedList<LamportRequest> getLamportQueue() {
        return lamportQueue;
    }

    public String getProcess() {
        return process;
    }

    public int getClock() {
        return clock;
    }

    public int getTheId() {
        return id;
    }

    public void useScreen(){
        lwb.useScreen();
        clock++;
    }
}
