package com.carles.jogging.jogging.first_location;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.carles.jogging.R;

/**
 * Created by carles1 on 21/04/14.
 */
public class FirstLocationFailedDialog extends DialogFragment {

    private static final String ARG_CONNECTION_TYPE = "connection_type";

    public static FirstLocationFailedDialog newInstance(Error error) {
        FirstLocationFailedDialog ret = new FirstLocationFailedDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CONNECTION_TYPE, error);
        ret.setArguments(args);
        return ret;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Error error = (Error)getArguments().getSerializable(ARG_CONNECTION_TYPE);

        Dialog dialog = new Dialog(getActivity());

        final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_custom, null);
        final TextView title = (TextView) view.findViewById(R.id.dlg_title);
        final TextView msg = (TextView) view.findViewById(R.id.dlg_msg);

        title.setText(R.string.connection_failed_title);
        if (error == Error.GOOGLE_PLAY_SERVICES_UNAVAILABLE) {
            msg.setText(R.string.connection_failed_google);
        } else if (error == Error.GPS_DISABLED) {
            msg.setText(R.string.connection_failed_gps);
        } else if (error==Error.GPS_LOST) {
            msg.setText(R.string.connection_failed_gps_lost);
        } else if (error==Error.GPS_NOT_CONNECTED) {
            msg.setText(R.string.connection_failed_gps_connection);
        } else if (error==Error.NO_LOCATIONS) {
            msg.setText(R.string.connection_failed_gps_location);
         } else {
            msg.setText(R.string.connection_failed_unknown);
        }

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(view);

        // cancel the dialog if the user touches inside it as well as outside it
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                getActivity().finish();
            }
        });
        dialog.setCanceledOnTouchOutside(true);

        dialog.getWindow().getAttributes().windowAnimations = R.style.Theme_Jogging_ZoomedDialog;
        return dialog;
    }

    @Override
    // dialog is cancelled when the user presses the back button
    public void onCancel(DialogInterface dialog) {
        getActivity().finish();
    }

}

enum Error {
    GOOGLE_PLAY_SERVICES_UNAVAILABLE, GPS_DISABLED, GPS_LOST, NO_LOCATIONS, GPS_NOT_CONNECTED;
}

