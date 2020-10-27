package Sockets.LightWeight.LWA1;

import Sockets.LightWeight.LWA3.AnalogueCommsLWA3;
import Sockets.LightWeight.LamportRequest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public class DedicatedLWA1 extends Thread {
    public final static String TMSTP_LWA2 = "TIME_STAMP_LWA2";
    public final static String TMSTP_LWA3 = "TIME_STAMP_LWA3";

    private Socket socket;
    private DataInputStream diStream;
    private DataOutputStream doStream;
    private Queue<LamportRequest> lamportQueue;

    private Date date;
    private final AnalogueCommsLWA1 analogueCommsLWA1;
    private int id;

    public DedicatedLWA1(Socket socket, AnalogueCommsLWA1 analogueCommsLWA1, int id) {
        this.socket = socket;
        this.analogueCommsLWA1 = analogueCommsLWA1;
        lamportQueue = new LinkedList<>();
        date = new Date();
        this.id = id;
    }

    @Override
    public void run() {
        try {
            diStream = new DataInputStream(socket.getInputStream());
            doStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        synchronized (analogueCommsLWA1){
            analogueCommsLWA1.notify();
            System.out.println("Sockets in DedicatedLWA1 created. Notifying");
        }
        while (true){
            try {
                String request = diStream.readUTF();
                actOnRequest(request);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void actOnRequest(String request) throws IOException {
        long timeStamp;
        switch (request){
            case TMSTP_LWA2:
                System.out.println("Received LWA2 timestamp in LWA1. Answering...");
                timeStamp = diStream.readLong();
                lamportQueue.add(new LamportRequest(timeStamp, TMSTP_LWA2, id));
                timeStamp = date.getTime();
                doStream.writeLong(timeStamp);
                System.out.println("Answer to LWA2 from LWA1 done.");
                break;
            case TMSTP_LWA3:
                System.out.println("Received LWA3 timestamp in LWA1. Answering...");
                timeStamp = diStream.readLong();
                lamportQueue.add(new LamportRequest(timeStamp, TMSTP_LWA3, id));
                timeStamp = date.getTime();
                doStream.writeLong(timeStamp);
                System.out.println("Answer to LWA3 from LWA1 done.");
                break;
        }
    }

    public void addToQueue(long time, String tmstp, int id) {
        lamportQueue.add(new LamportRequest(time, tmstp, id));
    }

    public LamportRequest peekQueue() {
        return lamportQueue.peek();
    }
}
