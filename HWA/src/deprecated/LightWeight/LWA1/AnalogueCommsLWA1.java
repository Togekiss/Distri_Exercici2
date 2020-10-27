package Sockets.LightWeight.LWA1;

import Sockets.LightWeight.LamportRequest;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AnalogueCommsLWA1 extends Thread {
    private final static int INCOME_PORT = 55555;
    private DedicatedLWA1 dedicatedLWA1;
    private final S_LWA1 s_lwa1;
    private int id;

    public AnalogueCommsLWA1(S_LWA1 s_lwa1, int id) {
        this.s_lwa1 = s_lwa1;
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
        dedicatedLWA1 = new DedicatedLWA1(socket, this, id);
        dedicatedLWA1.start();
        try {
            wait();
            synchronized (s_lwa1){
                s_lwa1.notify();
                System.out.println("Sockets in AnalogueCommsLWA1 created. Notifying");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void addToQueue(long time, String tmstp, int id) {
        dedicatedLWA1.addToQueue(time, tmstp, id);
    }

    public LamportRequest peekQueue() {
        return dedicatedLWA1.peekQueue();
    }
}
