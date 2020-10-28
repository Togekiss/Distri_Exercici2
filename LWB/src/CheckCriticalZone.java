import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;

public class CheckCriticalZone extends Thread {
    private AnalogueComms analogueComms;

    public CheckCriticalZone(AnalogueComms analogueComms){
        this.analogueComms = analogueComms;
    }

    @Override
    public void run() {
        System.out.println("CheckCS runs with thread: " + Thread.currentThread().getName());
        while (true) {
            try {
                synchronized (this) {
                    this.wait();
                }
                String tmstp = analogueComms.getProcess();

                if (checkQueue(tmstp)) {
                    analogueComms.useScreen();
                    try {
                        analogueComms.releaseProcess(tmstp);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("...");
               }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized boolean checkQueue(String process) {
        boolean available = true;
        int clock = analogueComms.getClock();
        int id = analogueComms.getTheId();
        LinkedList<LamportRequest> lamportQueue = analogueComms.getLamportQueue();

        System.out.println("in: " + Thread.currentThread().getName());

        try{

            System.out.println("Cheking access to CS. My process: " + process + "; My clock: " + clock + "; My id: " + id);
            for (LamportRequest lr : lamportQueue) {
           //     System.out.println("[LAMPORT (query conditionals)]" + lr.toString());
                if (!lr.getProcess().equals(process)) {
                    if (lr.getClock() < clock) {
                        available = false;
                    } else if (lr.getClock() == clock && lr.getId() < id) {
                        available = false;
                    }
                }
            }
        }catch (ConcurrentModificationException e){
            System.err.println("in catch: " + Thread.currentThread().getName());
            e.printStackTrace();
        }
        return available;
    }

    public void myNotify() {
        synchronized (this){
            this.notify();
        }
    }
}