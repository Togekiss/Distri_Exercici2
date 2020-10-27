package Sockets.LightWeight.LWA3;

import Sockets.LightWeight.LamportRequest;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AnalogueCommsLWA3 extends Thread {

    private final static int INCOME_PORT = 55557;

    private DedicatedLWA3 dedicatedLWA3;
    private final S_LWA3 s_lwa3;
    private int id;

    public AnalogueCommsLWA3(S_LWA3 s_lwa3, int id) {
        this.s_lwa3 = s_lwa3;
        this.id = id;
    }

    @Override
    public void run() {
        try {
            //creem el nostre socket
            ServerSocket serverSocket = new ServerSocket(INCOME_PORT);
            //esperem a la conexio del HeavyWeight_B
            Socket socket = serverSocket.accept();
            newDedicatedAnalogueComms(socket);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void newDedicatedAnalogueComms(Socket socket) {
        dedicatedLWA3 = new DedicatedLWA3(socket, this, id);
        dedicatedLWA3.start();
        try {
            wait();
            synchronized (s_lwa3){
                s_lwa3.notify();
                System.out.println("Sockets in AnalogueCommsLWA3 created. Notifying");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void addToQueue(long time, String tmstp, int id) {
        dedicatedLWA3.addToQueue(time, tmstp, id);
    }

    public LamportRequest peekQueue() {
        return dedicatedLWA3.peekQueue();
    }
}
