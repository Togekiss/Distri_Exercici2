package Sockets.LightWeight.LWA3;

import Sockets.LightWeight.LamportRequest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public class DedicatedLWA3 extends Thread {
    private final static String TMSTP_LWA1 = "TIME_STAMP_LWA1";
    private final static String TMSTP_LWA2 = "TIME_STAMP_LWA2";

    private java.util.Date date;
    private Socket socket;
    private DataInputStream diStream;
    private DataOutputStream doStream;
    private Queue<LamportRequest> lamportQueue;
    private final AnalogueCommsLWA3 analogueCommsLWA3;
    private int id;

    public DedicatedLWA3(Socket socket, AnalogueCommsLWA3 analogueCommsLWA3, int id) {
        this.socket = socket;
        this.analogueCommsLWA3 = analogueCommsLWA3;
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
        synchronized (analogueCommsLWA3){
            analogueCommsLWA3.notify();
            System.out.println("Sockets in DedicatedLWA3 created. Notifying");
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
            case TMSTP_LWA2:
                timeStamp = diStream.readLong();
                lamportQueue.add(new LamportRequest(timeStamp, TMSTP_LWA2, id));
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
