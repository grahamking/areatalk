package org.gkgk.areatalk;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.InterfaceAddress;
import java.util.Enumeration;
import java.io.IOException;

import android.util.Log;
import android.app.IntentService;
import android.content.Intent;

/** Receive UDP broadcast messages. */
public class ReceiveService extends IntentService {

    private static final String TAG = ReceiveService.class.getSimpleName();

    public ReceiveService() {
        super("ReceiveService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        this.listen();
    }

    void listen() {

        DatagramSocket sock = null;
        try {
            sock = new DatagramSocket(5311);
        }
        catch (SocketException exc) {
            Log.e(TAG, "SocketException creating DatagramSocket", exc);
        }

        byte[] buf = new byte[256];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        while (true) {
            try {
                sock.receive(packet);
            }
            catch (IOException exc) {
                Log.e(TAG, "IOException receive", exc);
            }
            String msg = new String(packet.getData(), 0, packet.getLength());
            Log.d(TAG, msg);

            this.broadcast(msg);

            packet.setData(new byte[256]);
        }
    }

    /** Send the message to listeners */
    void broadcast(String msg) {
        Intent intent = new Intent(MainActivity.MESSAGE_RECEIVED);
        intent.putExtra("msg", msg);
        sendBroadcast(intent);
    }

}
