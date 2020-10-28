import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;

public class AnalogueComms extends Thread {
    private int MY_PORT;
    private final LWA lwa;
    private String time_stamp_lwa;
    private final ArrayList<Thread> dedicatedThreadList;
    private LinkedList<LamportRequest> lamportQueue;
    private boolean gotAnswer;

    private DedicatedOutgoingSocket firstDedicatedOutgoing;
    private DedicatedOutgoingSocket secondDedicatedOutgoing;
    private DedicatedIncomingSocket dedicatedLWA;

    private String process;
    private int id;
    private int clock;
    private CheckCriticalZone checkCriticalZone;

    public AnalogueComms(LWA lwa, int myPort, String time_stamp_lwa, int id) {
        this.MY_PORT = myPort;
        this.lwa = lwa;
        this.time_stamp_lwa = time_stamp_lwa;
        this.id = id;
        dedicatedThreadList = new ArrayList<>();
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
            /*
            for (int i = 0; i <= 2; i++){
                Socket socket = serverSocket.accept();
                newDedicatedAnalogueComms(socket);
            }

            while (true){
                synchronized (this){
                    this.wait();
                }
                checkCSAvailability();
            }
*/
            while (true){
                Socket socket = serverSocket.accept();
                newDedicatedAnalogueComms(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void newDedicatedAnalogueComms(Socket socket) {
        dedicatedLWA = new DedicatedIncomingSocket(socket, this, id);
        Thread thread = new Thread(dedicatedLWA);
        dedicatedThreadList.add(thread);
        thread.start();
    }
/*
    public synchronized void checkCSAvailability(){
        boolean available = true;
        System.out.println();
        for (LamportRequest lr : lamportQueue){
            System.out.println("[LAMPORT (query)]" + lr.toString());
        }

        for (LamportRequest lr : lamportQueue){
  //          System.out.println("[LAMPORT (query conditionals)]" + lr.toString());
            if (!lr.getProcess().equals(tmstp)){
                System.out.println("Checking one");
                if (lr.getTimeStamp() < requestTime){
                    System.out.println("Checked has lesser requestTime");
                    available = false;
                }else if (lr.getTimeStamp() == requestTime && lr.getId() < id){
                    System.out.println("Checked has equal requestTime and checked ID is lesser than my ID");
                    available = false;
                }
            }
        }

        if (available){
            s_lwa.useScreen();
            try {
                releaseProcess(tmstp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            for (int i = 0; i <= 10; i++){
                System.out.println("...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
*/
    public synchronized void addToQueue(int clock, String process, int id) {
        LamportRequest request = new LamportRequest(clock, process, id);
        System.out.println("\t\t\t\t adding: " + request);
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

    public void waitForFreeCS() {
        for (int i = 0; i < dedicatedThreadList.size(); i++){
            try {
                synchronized (dedicatedThreadList.get(i)){
                    dedicatedThreadList.get(i).wait();
                }
                //dedicatedThreadList.get(i).wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public synchronized void releaseProcess(String tmstp) throws IOException {
        System.out.println("### sending release msg to both dedicatedOutgoings ### " + tmstp);
        firstDedicatedOutgoing.releaseCS(tmstp);
        secondDedicatedOutgoing.releaseCS(tmstp);
        releaseRequest(tmstp);
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
        System.out.println();
        for (LamportRequest lr : lamportQueue){
            System.out.println("[LAMPORT (remove)]" + lr.toString());
        }
    }

    public synchronized void checkBothAnswers(String process, int clock, int OUTGOING_PORT) {
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
            this.process = process;
            this.clock = clock;
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
        lwa.useScreen();
        clock++;
    }
}
