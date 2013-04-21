package org.gkgk.areatalk;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.InterfaceAddress;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.os.AsyncTask;
import android.widget.TextView;
import android.widget.EditText;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.text.InputType;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.graphics.Typeface;

public class MainActivity extends Activity implements NickDialog.Listener {

    private static final String TAG = MainActivity.class.getSimpleName();

    DatagramSocket sock;
    DatagramPacket packet;

    TextView content;
    TextView userList;
    EditText input;

    TextView.OnEditorActionListener sendListener;
    String nick;
    Set<String> users;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        this.users = new TreeSet<String>();
        this.userList = (TextView) this.findViewById(R.id.users);

        this.startReceiver();
        this.startListener();

        this.loadNick();
        if (this.nick == null) {
            this.promptNick();
        } else {
            this.sendRaw("/ON " + nick);
        }
    }

    /** Start the thread which listens for socket messages and displays them. */
    void startReceiver() {

        this.content = (TextView) findViewById(R.id.content);

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

        new Receiver().execute();
    }

    /** Initialize the part which responds to user input, and sends to socket */
    void startListener() {

        this.sendListener = new SendListener();
        this.input = (EditText) findViewById(R.id.input);
        this.input.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_AUTO_CORRECT |
                InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE);
        this.input.setOnEditorActionListener(this.sendListener);
    }

    /** Called by NickDialog when a nick is set */
    public void onNick(String nick) {
        this.nick = nick;
        Log.d(TAG, "Nick: " + this.nick);

        SharedPreferences prefs = this.getSharedPreferences("areatalk", 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("nick", nick);
        editor.commit();

        this.sendRaw("/ON " + nick);
    }

    /** Load nick from shared preferences into this.nick */
    void loadNick() {
        SharedPreferences prefs = getSharedPreferences("areatalk", 0);
        this.nick = prefs.getString("nick", null);
    }

    /** Prompt user for a nickname, save it to shared prefs. */
    void promptNick() {
        new NickDialog().show(getFragmentManager(), "NICK");
    }

    /** Send a message down the socket, with no extra formatting. */
    void sendRaw(String msg) {
        new SendTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, msg);
    }

    /** A new user has joined the chat */
    void addUser(String nick) {
        this.sendRaw("/NICK "+ this.nick);
        this.users.add(nick);
        this.displayUsers();
    }

    /** A user is still in the chat */
    void updateUser(String nick) {
        this.users.add(nick);
        this.displayUsers();
    }

    /** Display a regular message */
    void display(String nick, String message) {

        nick = this.lpad(nick, 15);
        SpannableString span = new SpannableString(nick);
        span.setSpan(new StyleSpan(Typeface.BOLD), 0, span.length(), 0);
        this.content.append(span);

        this.content.append(" " + message + "\n");
    }

    /** Refresh the display of active users. */
    void displayUsers() {

        StringBuilder u = new StringBuilder(this.users.size());
        for (String user : this.users) {
            u.append(user);
            u.append(", ");
        }
        u.delete(u.length()-2, u.length()); // Remove last ", "
        this.userList.setText("Talking now: " + u.toString());
    }

    /** Left-pad a string with spaces */
    String lpad(String msg, int len) {

        if (msg.length() >= len) {
            return msg;
        }

        int needed = len - msg.length();
        StringBuilder result = new StringBuilder();
        while (needed-- > 0) {
            result.append(" ");
        }
        result.append(msg);
        return result.toString();
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
                    MainActivity.this.nick +": " + msg.toString());

            view.setText("");
            InputMethodManager imm = (InputMethodManager) MainActivity.this.
                getSystemService(Context.INPUT_METHOD_SERVICE);
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
                if (m.startsWith("/")) {
                    actSysMsg(m);
                } else if (m.contains(": ")) {
                    actUserMsg(m);
                } else {
                    Log.d(TAG, "Unknown message: " + m);
                }
            }
        }

        /** Regular message from a user - split off nick and display */
        void actUserMsg(String m) {

            TextView content = MainActivity.this.content;
            String[] parts = m.split(":", 2);
            MainActivity.this.display(parts[0], parts[1]);
        }

        /** System message, not intended for display */
        void actSysMsg(String m) {

            if (m.startsWith("/ON ")) {
                String joined = m.substring(3);
                MainActivity.this.addUser(joined);

            } else if (m.startsWith("/NICK ")) {
                String current = m.substring(5);
                MainActivity.this.updateUser(current);
            }
        }

    }
}

