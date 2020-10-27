import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;

public class CheckCriticalZone extends Thread {
    private AnalogueCommsLWA analogueCommsLWA;

    public CheckCriticalZone(AnalogueCommsLWA analogueCommsLWA){
        this.analogueCommsLWA = analogueCommsLWA;
    }

    @Override
    public void run() {
        System.out.println("CheckCS runs with thread: " + Thread.currentThread().getName());
        while (true) {
            try {
                synchronized (this) {
                    this.wait();
                }
                String tmstp = analogueCommsLWA.getProcess();

                if (checkQueue(tmstp)) {
                    analogueCommsLWA.useScreen();
                    try {
                        analogueCommsLWA.releaseProcess(tmstp);
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
        int clock = analogueCommsLWA.getClock();
        int id = analogueCommsLWA.getTheId();
        LinkedList<LamportRequest> lamportQueue = analogueCommsLWA.getLamportQueue();

        System.out.println("in: " + Thread.currentThread().getName());

//        synchronized (lamportQueue){

        try{
           /* for (LamportRequest lr : lamportQueue) {
                System.out.println("[LAMPORT (query)]" + lr.toString());
            }
*/

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


  //      }
        return available;
    }

    public void myNotify() {
        synchronized (this){
            this.notify();
        }
    }
}