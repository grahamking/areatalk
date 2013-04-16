package org.gkgk.areatalk;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.os.AsyncTask;
import android.widget.TextView;
import android.util.Log;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    TextView content;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        this.content = (TextView) findViewById(R.id.content);
        Log.d(TAG, this.content.toString());

        new Receiver().execute();
    }

    /** Inner classes **/

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

