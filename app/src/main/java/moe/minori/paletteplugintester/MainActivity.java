package moe.minori.paletteplugintester;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Calendar;

/**
 * MainActivity, where it all starts.
 *
 * This activity is based on pre-API 1 (Adorable Chihaya)
 *
 * Author: Minori Hiraoka (https://keybase.io/minori)
 * License: Apache License 2
 */
public class MainActivity extends AppCompatActivity {

    TextView statusTextView;
    TextView receiverStatusTextView;
    Button multiFunctionalButton;

    AvailabilityCallbackListener cbl = new AvailabilityCallbackListener();
    BroadcastReceiver waitUntilAPIResponds;
    BroadcastReceiver allAPIReceiver;

    boolean isReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = (TextView) findViewById(R.id.serviceAPIStatusTextView);
        receiverStatusTextView = (TextView) findViewById(R.id.serviceAPIreceivedTextView);
        multiFunctionalButton = (Button) findViewById(R.id.multiFunctionalButton);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // It is a good idea to ensure that support API service is running in target's system
        checkAPIAvailability(cbl);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // try to stop all receiver when terminating a lifecycle.
        // Service might still not be started until user terminates a lifecycle

        if (waitUntilAPIResponds != null)
            unregisterReceiver(waitUntilAPIResponds);

        if (allAPIReceiver != null)
            unregisterReceiver(allAPIReceiver);
    }

    public void onClick(View v) {

        // If service is not ready, ignore input
        if ( !isReady )
            return;

        if (v == multiFunctionalButton) {
            /*
            sendBroadcast(new Intent(Consts.REQ_WRITE_TWEET)
                    .putExtra("DATA", "Testing Tweet on Palette API\n" +
                            "Current time is: " + Calendar.getInstance().getTime().toString() + "\n" +
                            "#PaletteAPI" ),
                    Consts.EXTERNAL_BROADCAST_API);

                    */

            sendBroadcast(new Intent("palette.twitter.externalBroadcast.follow.user")
            .putExtra("USERLONG", 3248397295L),
                    Consts.EXTERNAL_BROADCAST_API);
        }
    }

    /**
     * After communication with API service, listener will be called
     *
     * @param listener
     */
    private void checkAPIAvailability(final SimpleCallbackListener listener) {

        // API_AVAIL_NOTIFY_POS and API_AVAIL_NOTIFY_NEG will be called if service is alive.
        // If not, there will be no reply. Consider using a timer to handle that event.

        final android.os.Handler timerHandler = new android.os.Handler();

        IntentFilter filter = new IntentFilter(Consts.API_AVAIL_NOTIFY_POS);
        filter.addAction(Consts.API_AVAIL_NOTIFY_NEG);

        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                // It is important to release BroadcastReceiver after it's job done.
                // It it is not released, it can cause leak and crash on some system.
                unregisterReceiver(this);

                // Also if timer is running, stop the timer
                timerHandler.removeCallbacksAndMessages(null);

                // This block is running in UI thread unless handler is specified in registerReceiver( ... ) method.
                // Prevent running excessive task in UI thread to avoid ANR.

                if (intent.getAction().equals(Consts.API_AVAIL_NOTIFY_POS)) {
                    // Service on green

                    listener.onEvent("GREEN");
                } else if (intent.getAction().equals(Consts.API_AVAIL_NOTIFY_NEG)) {
                    // Service on red

                    listener.onEvent("RED");
                } else {
                    // Not likely to happen
                    Log.e("PalettePluginTester", "Unknown response from API service, maybe your API documentation is outdated?");

                    statusTextView.setText("Status: Unknown error!");

                    // API documentation might be outdated, or maybe some bugs at API. Report any bug to Issue Tracker of API service.
                }
            }
        };

        // For performance, you may register receiver on separate thread (handler)
        registerReceiver(receiver, filter, Consts.EXTERNAL_BROADCAST_API, null);

        // You must supply EXTERNAL_BROADCAST_API data in order to communicate successfully
        // OS may require user to manually agree on such permission when installing
        sendBroadcast(new Intent(Consts.API_AVAIL_QUERY), Consts.EXTERNAL_BROADCAST_API);

        // start timer
        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                // unregister receiver to prevent leak
                unregisterReceiver(receiver);

                // fire event to listener that API service is not responding.
                listener.onEvent("RED");
            }
        }, 5000); // Wait for 5 seconds until reply

        // update UI component
        statusTextView.setText("Status: Waiting for service reply...");

    }

    /**
     * If successful, GREEN / else, RED
     * GREEN will call registerAllAPIReceiver();
     * RED will wait until service is available
     */
    class AvailabilityCallbackListener implements SimpleCallbackListener {
        @Override
        public void onEvent(String data) {
            if (data.equals("GREEN")) {
                // success
                statusTextView.setText("Status: SUCCESS");
                isReady = true;

                registerAllAPIReceiver();

            } else if (data.equals("RED")) {

                statusTextView.setText("Status: FAILED, waiting");

                // if failed, try to wait until API broadcasts API_AVAIL_NOTIFY_POS

                IntentFilter filter = new IntentFilter(Consts.API_AVAIL_NOTIFY_POS);

                waitUntilAPIResponds = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {

                        unregisterReceiver(this);

                        if (intent.getAction().equals(Consts.API_AVAIL_NOTIFY_POS)) {
                            // check again for API Availability

                            checkAPIAvailability(cbl);
                        }
                    }
                };

                registerReceiver(waitUntilAPIResponds, filter);

            } else {
                // Also not likely to happen. Ignore this block.
            }

        }
    }

    /**
     * In pre-API 1, the only communication possible is Writing, Receiving tweet on timeline.
     */
    private void registerAllAPIReceiver() {
        IntentFilter filter = new IntentFilter();

        filter.addAction(Consts.NOTIFY_NEW_STATUS);
        filter.addAction("palette.twitter.externalBroadcast.notify.follower");

        registerReceiver(allAPIReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.getAction().equals(Consts.NOTIFY_NEW_STATUS)) {
                    StringBuilder builder = new StringBuilder();

                    builder.append("Tweet From: ");
                    builder.append(intent.getStringExtra("FROMTEXT"));
                    builder.append("(");
                    builder.append(intent.getStringExtra("FROMTEXTSCREEN"));
                    builder.append((") "));
                    builder.append(intent.getLongExtra("FROMLONG", -1));
                    builder.append("\n");
                    builder.append("Content: ");
                    builder.append(intent.getStringExtra("TEXT"));
                    builder.append("\n");
                    builder.append("In reply to: ");
                    builder.append(intent.getLongExtra("REPLY_TO", -1));

                    try {
                        receiverStatusTextView.setText(
                                builder.toString()
                        );
                    } catch (Exception e) {
                        // ignore
                    }
                }
                else
                {
                    receiverStatusTextView.setText(intent.getLongExtra("USERLONG", -1) + " followed you");
                }


            }
        }, filter, Consts.EXTERNAL_BROADCAST_API, null);
    }

}
