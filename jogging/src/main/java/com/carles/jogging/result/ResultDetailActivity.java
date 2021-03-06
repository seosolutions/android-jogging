package com.carles.jogging.result;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.MenuItem;
import com.carles.jogging.BaseActivity;
import com.carles.jogging.C;
import com.carles.jogging.R;
import com.carles.jogging.feedback.FeedbackActivity;
import com.carles.jogging.feedback.MailClientNotAvailableDialog;
import com.carles.jogging.model.JoggingModel;
import com.carles.jogging.util.FormatUtil;
import com.carles.jogging.util.PrefUtil;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Session;
import com.facebook.SessionLoginBehavior;
import com.facebook.SessionState;
import com.facebook.widget.WebDialog;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by carles1 on 20/04/14.
 */
public class ResultDetailActivity extends BaseActivity implements ResultDetailFragment.OnLocationClickedListener {

    private static final String TAG = ResultDetailActivity.class.getSimpleName();
    private static final String TAG_FACEBOOK_RESPONSE = "tag_facebook_response";
    private static final String PERMISSION = "publish_actions";

    private Context ctx;
    private ResultDetailFragment detailFragment = null;
    private ResultMapFragment mapFragment = null;
    private ProgressBar progress;

    // data obtained from the intent
    private JoggingModel jogging;

    // share with facebook
    private Session.StatusCallback sessionCallback = new Session.StatusCallback() {
        @Override
        public void call(final Session session, final SessionState state, final Exception exception) {

            Log.i(TAG, "facebook session state after callback = " + state.name());
            if (session != null && session.isOpened()) {
                switch (state) {
                    case CREATED_TOKEN_LOADED:
                        Log.i(TAG, "facebook session is CREATE_TOKEN_LOADED: openForPublish");
                        openForPublish(session);
                        break;

                    case OPENED:
                    case OPENED_TOKEN_UPDATED:
                        Log.i(TAG, "facebook session is OPENED: publishFeedDialog");
                        publishFeedDialog();
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_detail);
        ctx = this;

        jogging = getIntent().getParcelableExtra(C.EXTRA_JOGGING_TOTAL);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.title_result);

        // load views
        progress = (ProgressBar) findViewById(R.id.progress);

        // show detail fragment as is the ResultDetailActivity's initial fragment
        if (savedInstanceState == null) {
            if (detailFragment == null) {
                detailFragment = ResultDetailFragment.newInstance();
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, detailFragment).commit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // returns from ACTION_SENT intent
        if (requestCode == C.REQUEST_FEEDBACK) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), getString(R.string.feedback_sent), Toast.LENGTH_LONG).show();
            } else if (resultCode == C.RESULT_NO_MAIL_CLIENT) {
                MailClientNotAvailableDialog.newInstance().show(getSupportFragmentManager(),
                        C.TAG_MAIL_CLIENT_NOT_AVAILABLE);
            }
            return;
        }

        // returns from a facebook intent
        if (Session.getActiveSession() != null) {
            Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
        }
    }

    @Override
    public void onLocationClicked(int position) {
        addResultMapFragment(position);
    }

    private void addResultMapFragment(int position) {
        if (mapFragment == null) {
            mapFragment = ResultMapFragment.newInstance(position, jogging);
        } else {
            mapFragment.setPosition(position);
        }
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (mapFragment.isAdded()) {
            ft.show(mapFragment);
        } else {
            ft.add(R.id.fragment_container, mapFragment);
        }
        ft.hide(detailFragment);
        ft.commit();
    }

    private void shareWithFacebook() {
        // safe check
        if (jogging == null) {
            return;
        }

        Session session = Session.getActiveSession();
        if (session == null) {
            session = new Session(this);
            Session.setActiveSession(session);
        }

        if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
            openForPublish(session);
        } else {
            Session.openActiveSession(this, true, sessionCallback);
        }
    }

    private void openForPublish(Session session) {
        if (session != null) {
            List<String> permissions = new ArrayList<String>();
            permissions.add(PERMISSION);

            Session.OpenRequest openRequest = new Session.OpenRequest(this).setCallback(sessionCallback);
            openRequest.setLoginBehavior(SessionLoginBehavior.SUPPRESS_SSO);
            openRequest.setRequestCode(Session.DEFAULT_AUTHORIZE_ACTIVITY_CODE);
            openRequest.setPermissions(permissions);
            session.openForPublish(openRequest);
        }
    }

    private void publishFeedDialog() {
        if (Session.getActiveSession() == null) {
            return;
        }

        progress.setVisibility(View.VISIBLE);

        Bundle params = new Bundle();
        String user = PrefUtil.getLoggedUser(ctx).getName();
        String appName = getString(R.string.app_name);
        params.putString("name", getString(R.string.share_feed_dialog_name, appName));
        params.putString("caption", getString(R.string.app_name));
        params.putString("description", getString(R.string.share_feed_dialog_desc,
                (int) jogging.getGoalDistance(), FormatUtil.time(jogging.getGoalTime())));
        params.putString("link", getString(R.string.play_store_url));

        WebDialog feedDialog = (new WebDialog.FeedDialogBuilder(ctx, Session.getActiveSession(), params)).
                setOnCompleteListener(new WebDialog.OnCompleteListener() {

                    @Override
                    public void onComplete(Bundle values, FacebookException error) {

                        progress.setVisibility(View.GONE);

                        if (error == null) {

                            final String postId = values.getString("post_id");
                            if (postId != null) {
                                // When the story is posted
                                Log.i(TAG, "Shared with facebook via FeedDialog. id=" + postId);
                                trackShareWithFacebook();
                                showSuccessResponse();

                            } else {
                                // User clicked the Cancel button
                                Log.i(TAG, "NOT shared with facebook. User cancelled");
                            }

                        } else if (error instanceof FacebookOperationCanceledException) {
                            // User clicked the "x" button
                            Log.i(TAG, "NOT shared with facebook. User closed dialog");

                        } else {
                            // Generic, ex: network error
                            Log.e(TAG, "NOT shared with facebook. Error:" + error.getMessage());
                            showFailureResponse(error.getMessage());
                        }
                    }
                }).build();
        feedDialog.show();
    }

    private void trackShareWithFacebook() {
        EasyTracker.getInstance(this).send(MapBuilder.createSocial("Facebook", "Share",
                PrefUtil.getLoggedUser(ctx).getName() + " running").build());
    }

    private void showSuccessResponse() {
        showSuccessResponse(null);
    }

    private void showSuccessResponse(String response) {
        FacebookCallbackDialog.newInstance(response, false).show(getSupportFragmentManager(), TAG_FACEBOOK_RESPONSE);
    }

    private void showFailureResponse(String errMsg) {
        FacebookCallbackDialog.newInstance(errMsg, true).show(getSupportFragmentManager(), TAG_FACEBOOK_RESPONSE);
    }

    private void shareWithWhatsApp() {
        // populate the share intent with data
        Intent waIntent = new Intent(Intent.ACTION_SEND);
        waIntent.setType("text/plain");
        waIntent.setPackage("com.whatsapp");
        waIntent.putExtra(Intent.EXTRA_TEXT, new StringBuilder()
                .append(getString(R.string.share_whatsapp_text, (int) jogging.getGoalDistance(), FormatUtil.time(jogging.getGoalTime())))
                .append(getString(R.string.app_name))
                .append("' : ")
                .append(getString(R.string.play_store_url)).toString());
        waIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.app_name));
        startActivity(waIntent);

        trackShareWithWhatsApp();
    }

    private void trackShareWithWhatsApp() {
        EasyTracker.getInstance(this).send(MapBuilder.createSocial("WhatsApp", "Share",
                PrefUtil.getLoggedUser(ctx).getName() + " running").build());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.action_facebook:
                shareWithFacebook();
                return true;

            case R.id.action_map:
                addResultMapFragment(-1);
                return true;

            case R.id.action_whatsapp:
                shareWithWhatsApp();
                return true;

            case R.id.action_feedback:
                startActivityForResult(new Intent(this, FeedbackActivity.class), C.REQUEST_FEEDBACK);
                overridePendingTransition(R.anim.zoom_in, 0);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (mapFragment != null && mapFragment.isAdded() && mapFragment.isVisible()) {
            if (detailFragment != null) {
                // if we are showing the map, re-show results detail fragment
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.show(detailFragment).hide(mapFragment).commit();
                return;
            }
        }
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_activity_to_right_in, R.anim.slide_activity_to_right_out);
    }

    /*- ************************************************************* */
    /*- ************************************************************* */
    @SuppressLint("ValidFragment")
    private static class FacebookCallbackDialog extends DialogFragment {

        private static final String ARG_RESPONSE = "arg_response";
        private static final String ARG_IS_ERROR = "arg_is_error";

        public static FacebookCallbackDialog newInstance(String response, boolean isError) {
            FacebookCallbackDialog ret = new FacebookCallbackDialog();
            Bundle args = new Bundle();
            args.putString(ARG_RESPONSE, response);
            args.putBoolean(ARG_IS_ERROR, isError);
            ret.setArguments(args);
            return ret;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog dialog = new Dialog(getActivity());

            final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_custom, null);
            final TextView title = (TextView) view.findViewById(R.id.dlg_title);
            final TextView msg = (TextView) view.findViewById(R.id.dlg_msg);

            title.setText(R.string.facebook_response_title);
            if (getArguments() == null || getArguments().getString(ARG_RESPONSE) == null) {
                msg.setText(R.string.facebook_post_success);
            } else if (getArguments().getBoolean(ARG_IS_ERROR, false)) {
                msg.setText(R.string.facebook_post_failure);
            } else {
                msg.setText(getArguments().getString(ARG_RESPONSE));
            }

            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(view);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismiss();
                }
            });

            dialog.setCanceledOnTouchOutside(true);
            dialog.getWindow().getAttributes().windowAnimations = R.style.Theme_Jogging_ZoomedDialog;
            return dialog;
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            dismiss();
        }
    }
}