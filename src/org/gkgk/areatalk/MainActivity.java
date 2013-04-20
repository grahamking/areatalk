package org.gkgk.areatalk;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.InterfaceAddress;
import java.util.Enumeration;
import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.os.AsyncTask;
import android.widget.TextView;
import android.widget.EditText;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.text.InputType;
import android.content.Context;

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
        this.input.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_AUTO_CORRECT |
                InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE);
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

        this.packet.setAddress(getBroadcast());
        this.packet.setPort(5311);
    }

    InetAddress getBroadcast() {

        System.setProperty("java.net.preferIPv4Stack", "true");
        Enumeration<NetworkInterface> niEnum;
        try {
            niEnum = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException exc) {
            Log.e(TAG, "Error NetworkInterface.getNetworkInterfaces()", exc);
            return null;
        }

        InetAddress result = null;

        while (niEnum.hasMoreElements()) {

            NetworkInterface ni = niEnum.nextElement();

            String name = ni.getDisplayName();
            Log.d(TAG, "getDisplayName: " + name);
            boolean isEthOrWifi = name.startsWith("wlan") || name.startsWith("eth");

            boolean isLoopback;
            try {
                isLoopback = ni.isLoopback();
            } catch (SocketException exc) {
                Log.e(TAG, "Error isLoopback()", exc);
                return null;
            }

            if(!isLoopback && isEthOrWifi){
                for (InterfaceAddress addr : ni.getInterfaceAddresses()) {
                    result = addr.getBroadcast();
                    if (result != null) {
                        break;
                    }
                }
            }
        }

        Log.d(TAG, "Broadcast: " + result.getHostAddress());
        return result;
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

            view.setText("");
            InputMethodManager imm = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(MainActivity.this.input.getWindowToken(), 0);

            return true;
        }
    }

    /** Keep network out of UI thread. */
    class SendTask extends AsyncTask<String, Void, Void> {

        protected Void doInBackground(String... msgs) {

            String msg = msgs[0];
            DatagramPacket pk = MainActivity.this.packet;
            pk.setData(msg.getBytes());
            try {
                MainActivity.this.sock.send(pk);
            }
            catch (IOException exc) {
                Log.e(TAG, "IOException sending packet", exc);
            }

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

                packet.setData(new byte[256]);
            }

        }

        protected void onProgressUpdate(String... msgs) {
            for (String m : msgs) {
                MainActivity.this.content.append(m + "\n");
            }
        }
    }
}

