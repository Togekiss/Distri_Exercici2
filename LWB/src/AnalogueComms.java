import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class AnalogueComms extends Thread {
    private int MY_PORT;
    private final LWB LWB;
    private String time_stamp_lwb;
    private LinkedList<LamportRequest> lamportQueue;
    private boolean gotAnswer;

    private DedicatedOutgoingSocket dedicatedOutgoing;
    private DedicatedIncomingSocket dedicatedLWB;

    private String process;
    private int id;
    private CheckCriticalZone checkCriticalZone;
    private int clock;

    public AnalogueComms(LWB LWB, int myPort, String time_stamp_lwb, int id) {
        this.MY_PORT = myPort;
        this.LWB = LWB;
        this.time_stamp_lwb = time_stamp_lwb;
        this.id = id;
        lamportQueue = new LinkedList<>();
        gotAnswer = false;
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

    public synchronized void checkBothAnswers(String process, int clock, int OUTGOING_PORT) {
        addToQueue(clock, process, id);
        if (!gotAnswer){
            if (OUTGOING_PORT == 55556){
                System.out.println("\tRECEIVING first response");
                System.out.println("\tRECEIVING request response from TIME_STAMP_LWA2");
            }else  if (OUTGOING_PORT == 55557){
                System.out.println("\tRECEIVING request response from TIME_STAMP_LWA3");
            }else  if (OUTGOING_PORT == 55555){
                System.out.println("\tRECEIVING request response from TIME_STAMP_LWA1");
            }

            //first answer. change flag
            gotAnswer = true;
        }else {
            System.out.println("\tRECEIVING second response");
            if (OUTGOING_PORT == 55556){
                System.out.println("\tRECEIVING request response from TIME_STAMP_LWA2");
            }else  if (OUTGOING_PORT == 55557){
                System.out.println("\tRECEIVING request response from TIME_STAMP_LWA3");
            }else  if (OUTGOING_PORT == 55555){
                System.out.println("\tRECEIVING request response from TIME_STAMP_LWA1");
            }

            //second answer. Must check queue
            System.out.println("\tGot both answers. Checking queue");
            //setRequestData(TMSTP, requestTime);
            this.process = process;
            this.clock = clock;
            //checkCSAvailability();
            myNotify();
            //reset answer flag
            gotAnswer = false;
        }
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
        LWB.useScreen();
        clock++;
    }
}
