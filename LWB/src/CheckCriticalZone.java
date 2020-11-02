import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;

public class CheckCriticalZone extends Thread {
    private final AnalogueComms analogueComms;
    private final String process;

    public CheckCriticalZone(AnalogueComms analogueComms, String process){
        this.analogueComms = analogueComms;
        this.process = process;
    }

    @Override
    public void run() {
        while (true) {
            try {
                synchronized (this) {
                    this.wait();
                }

                if (checkQueue()) {
                    analogueComms.useScreen();
                    try {
                        analogueComms.releaseProcess(process);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized boolean checkQueue() {
        boolean available = true;

        LinkedList<LamportRequest> lamportQueue = analogueComms.getLamportQueue();
        synchronized (lamportQueue){
            for (LamportRequest lr : lamportQueue) {
                if (lr.getClock() < analogueComms.getMyRequest().getClock()) {
                    available = false;
                } else if (lr.getClock() == analogueComms.getMyRequest().getClock() && lr.getId() < analogueComms.getMyRequest().getId()) {
                    available = false;
                }
            }
        }

        return available;
    }

    public void myNotify() {
        synchronized (this){
            this.notify();
        }
    }
}