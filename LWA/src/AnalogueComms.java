import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class AnalogueComms extends Thread {
    private final int MY_PORT;
    private final LWA lwa;
    private final LinkedList<LamportRequest> lamportQueue;
    private LamportRequest myRequest;

    private DedicatedOutgoingSocket firstDedicatedOutgoing;
    private DedicatedOutgoingSocket secondDedicatedOutgoing;

    private final String process;
    private final int id;
    private Integer clock;
    private final CheckCriticalZone checkCriticalZone;

    private int connectedBrothers;
    private int answeredBrothers;
    private boolean myTurn;

    public AnalogueComms(LWA lwa, int myPort, int id, String process) {
        this.MY_PORT = myPort;
        this.lwa = lwa;
        this.id = id;
        this.connectedBrothers = 0;
        this.answeredBrothers = 0;
        this.myTurn = true;
        lamportQueue = new LinkedList<>();
        this.clock = 0;
        this.checkCriticalZone = new CheckCriticalZone(this, process);
        checkCriticalZone.start();
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
        DedicatedIncomingSocket dedicatedLWA = new DedicatedIncomingSocket(socket, this, id);
        Thread thread = new Thread(dedicatedLWA);
        thread.start();
    }

    public synchronized void addToQueue(Integer clock, String process, int id) {
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
        firstDedicatedOutgoing.releaseCS(process);
        secondDedicatedOutgoing.releaseCS(process);
        releaseRequest(process);
        myRequest = new LamportRequest(clock, this.process, id);
        firstDedicatedOutgoing.myNotify();
        secondDedicatedOutgoing.myNotify();
        lwa.waitForResume();
    }

    public void registerDedicated(DedicatedOutgoingSocket firstDedicatedOutgoing, DedicatedOutgoingSocket secondDedicatedOutgoing) {
        this.firstDedicatedOutgoing = firstDedicatedOutgoing;
        this.secondDedicatedOutgoing = secondDedicatedOutgoing;
    }

    public synchronized void releaseRequest(String releaseProcess) {
        synchronized (lamportQueue){
            lamportQueue.removeIf(lr -> lr.getProcess().equals(releaseProcess));
        }
    }

    public synchronized void checkAnswers(int clock, int id) {
        answeredBrothers++;
        myTurn = clock > this.clock || id >= this.id;

        if (answeredBrothers == connectedBrothers && myTurn){
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

    public void useScreen(){
        lwa.useScreen();
        increaseClock();
    }

    public Integer getClock() {
        synchronized (clock){
            return clock;
        }
    }

    public void increaseClock(){
        synchronized (clock){
            clock++;
        }
    }

    public void updateClock(int c) {
        synchronized (this.clock) {
            this.clock = Math.max(c, this.clock);
        }
    }

    public LamportRequest getMyRequest() {
        return myRequest;
    }
}