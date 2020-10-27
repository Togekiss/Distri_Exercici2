package Sockets.LightWeight.LWA2;

import Sockets.LightWeight.LamportRequest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public class DedicatedLWA2 extends Thread {
    private final static String TMSTP_LWA1 = "TIME_STAMP_LWA1";
    private final static String TMSTP_LWA3 = "TIME_STAMP_LWA3";

    private Socket socket;
    private DataInputStream diStream;
    private DataOutputStream doStream;
    private Queue<LamportRequest> lamportQueue;
    private Date date;
    private final AnalogueCommsLWA2 analogueCommsLWA2;
    private int id;

    public DedicatedLWA2(Socket socket, AnalogueCommsLWA2 analogueCommsLWA2, int id) {
        this.socket = socket;
        this.analogueCommsLWA2 = analogueCommsLWA2;
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
        synchronized (analogueCommsLWA2){
            analogueCommsLWA2.notify();
            System.out.println("Sockets in DedicatedLWA2 created. Notifying");

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
            case TMSTP_LWA1:
                timeStamp = diStream.readLong();
                lamportQueue.add(new LamportRequest(timeStamp, TMSTP_LWA1, id));
                timeStamp = date.getTime();
                doStream.writeLong(timeStamp);
                break;
            case TMSTP_LWA3:
                timeStamp = diStream.readLong();
                lamportQueue.add(new LamportRequest(timeStamp, TMSTP_LWA3, id));
                timeStamp = date.getTime();
                doStream.writeLong(timeStamp);
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
