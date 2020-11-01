import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class AnalogueComms extends Thread {
    private final int MY_PORT;
    private final LWB lwb;
    private final LinkedList<LamportRequest> lamportQueue;
    private LamportRequest myRequest;

    private DedicatedOutgoingSocket dedicatedOutgoing;

    private final String process;
    private final int id;
    private final CheckCriticalZone checkCriticalZone;
    private int clock;

    private int connectedBrothers;
    private int answeredBrothers;
    private boolean myTurn;

    public AnalogueComms(LWB lwb, int myPort, int id, String process) {
        this.MY_PORT = myPort;
        this.lwb = lwb;
        this.id = id;
        this.connectedBrothers = 0;
        this.answeredBrothers = 0;
        this.myTurn = true;
        lamportQueue = new LinkedList<>();
        this.checkCriticalZone = new CheckCriticalZone(this, process);
        checkCriticalZone.start();
        this.clock = 0;
        this.process = process;
        myRequest = new LamportRequest(clock, process, id);
    }

    @Override
    public void run() {
        try {
            //creem el nostre socket
            ServerSocket serverSocket = new ServerSocket(MY_PORT);

            while (true){
                Socket socket = serverSocket.accept();
                newDedicatedAnalogueComms(socket);
                connectedBrothers++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void newDedicatedAnalogueComms(Socket socket) {
        DedicatedIncomingSocket dedicatedLWB = new DedicatedIncomingSocket(socket, this, id);
        Thread thread = new Thread(dedicatedLWB);
        thread.start();
    }

    public synchronized void addToQueue(int clock, String process, int id) {
        LamportRequest request = new LamportRequest(clock, process, id);
        boolean found = false;
        synchronized (lamportQueue){
            for (LamportRequest lr : lamportQueue){
                if (lr.getProcess().equals(process)){
                    found = true;
                    break;
                }
            }

            if (!found){
                lamportQueue.add(request);
            }
        }
    }

    public synchronized void releaseProcess(String process) throws IOException {
        dedicatedOutgoing.releaseCS(process);
        releaseRequest(process);
        myRequest = new LamportRequest(clock, this.process, id);
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
    }

    public synchronized void checkAnswers(int clock, int id) {
        answeredBrothers++;
        if (clock <= this.clock  && id < this.id){
            myTurn = false;
        }

        if (answeredBrothers == connectedBrothers && myTurn){
            System.out.println("answer true");
            myTurn = true;
            myNotify();
        }
    }

    public void myNotify() {
        checkCriticalZone.myNotify();
    }

    public LinkedList<LamportRequest> getLamportQueue() {
        synchronized (lamportQueue){
            return lamportQueue;
        }
    }

    public int getClock() {
        return clock;
    }

    public void useScreen(){
        lwb.useScreen();
        increaseClock();
    }

    public void increaseClock(){
        clock++;
    }

    public void updateClock(int clock) {
        this.clock = Math.max(clock, this.clock);
    }

    public LamportRequest getMyRequest() {
        return myRequest;
    }
}
