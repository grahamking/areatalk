package org.gkgk.areatalk;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.os.AsyncTask;
import android.widget.TextView;
import android.widget.EditText;
import android.util.Log;
import android.view.KeyEvent;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    DatagramSocket sock;
    DatagramPacket packet;

    TextView content;
    EditText input;
    TextView.OnEditorActionListener sendListener;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.initSender();

        setContentView(R.layout.main);

        this.content = (TextView) findViewById(R.id.content);
        new Receiver().execute();

        this.sendListener = new SendListener();
        this.input = (EditText) findViewById(R.id.input);
        this.input.setOnEditorActionListener(this.sendListener);
    }

    void initSender() {

        try {
            this.sock = new DatagramSocket();
            this.sock.setBroadcast(true);
        }
        catch (SocketException exc) {
            System.err.println(exc);
        }

        byte[] buf = new byte[100];
        this.packet = new DatagramPacket(buf, buf.length);

        byte[] addr = new byte[]{ (byte)192, (byte)168, (byte)11, (byte)255 };
        try {
            this.packet.setAddress(InetAddress.getByAddress(addr));
        }
        catch (UnknownHostException exc) {
            System.err.println(exc);
        }
        this.packet.setPort(5311);
    }

    /** Inner classes **/

    class SendListener implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(
                TextView view,
                int actionId,
                KeyEvent event)
        {
            CharSequence msg = view.getText();
            Log.d(TAG, "Send: " + msg);

            // Use THREAD_POOL_EXECUTOR so runs in parallel with Receiver
            new SendTask().executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR,
                    msg.toString());

            Log.d(TAG, "SendTask started");

            return true;
        }
    }

    /** Keep network out of UI thread. */
    class SendTask extends AsyncTask<String, Void, Void> {

        protected void onPreExecute() {
            Log.d(TAG, "SendTask.onPreExecute");
        }

        protected void onProgressUpdate(Void... progress) {
            Log.d(TAG, "SendTask.onProgressUpdate");
        }

        protected void onPostExecute(Void result) {
            Log.d(TAG, "SendTask.onPostExecute");
        }

        protected Void doInBackground(String... msgs) {
            Log.d(TAG, "SendTask.doInBackground start");

            String msg = msgs[0];
            DatagramPacket pk = MainActivity.this.packet;
            pk.setData(msg.getBytes());
            try {
                Log.d(TAG, "SendTask calling sock.send");
                MainActivity.this.sock.send(pk);
            }
            catch (IOException exc) {
                Log.e(TAG, "IOException sending packet", exc);
            }

            Log.d(TAG, "SendTask.doInBackground end");

            return null;
        }
    }

    class Receiver extends AsyncTask<String, String, String> {

        protected String doInBackground(String... args) {

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
                publishProgress(msg);
            }

        }

        protected void onProgressUpdate(String... msgs) {
            for (String m : msgs) {
                MainActivity.this.content.append(m);
            }
        }
    }
}

