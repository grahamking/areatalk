package org.gkgk.areatalk;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * The popup which prompts for a nickname.
 */
public class NickDialog extends DialogFragment {

    Listener container;
    EditText input;

    public interface Listener {
        public void onNick(String nick);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.container = (Listener) activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.nick, null);
        this.input = (EditText) dialogView.findViewById(R.id.nick);

        builder.setView(dialogView);
        builder.setPositiveButton("OK", new DialogListener());

        return builder.create();
    }

    /* Inner class */

    class DialogListener implements DialogInterface.OnClickListener {

        public void onClick(DialogInterface dialog, int id) {

            /*
            Log.d("NickDialog", "" + input);
            Log.d("NickDialog", "" + input.getText());
            */
            String nick = NickDialog.this.input.getText().toString();
            container.onNick(nick);
        }
    }
}
