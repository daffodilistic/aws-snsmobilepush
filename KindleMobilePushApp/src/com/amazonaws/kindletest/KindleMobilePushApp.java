/*
 * [ADMMessenger]
 *
 * (c) 2012, Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazonaws.kindletest;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.amazon.device.messaging.ADM;

public class KindleMobilePushApp extends Activity{
    /** Catches intents sent from the onMessage() callback to update the UI. */
    private BroadcastReceiver msgReceiver;

    public void onCreate(final Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView tView = (TextView)findViewById(R.id.textMsgServer);
        tView.setMovementMethod(new ScrollingMovementMethod());
        startService(new Intent(this, ADMMessageHandler.class));
        /* Register app with ADM. */
        register();
    }

    public boolean onCreateOptionsMenu(final Menu menu){
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_clear){
            final TextView tView = (TextView)findViewById(R.id.textMsgServer);
            tView.setText("");
            return true;
        }
        else{
            return super.onOptionsItemSelected(item);
        }
    }

    public void onResume(){
        final String msgKey = getString(R.string.json_data_msg_key);
        final String intentAction = getString(R.string.intent_msg_action);
        final String msgCategory = getString(R.string.intent_msg_category);
        final TextView tView = (TextView)findViewById(R.id.textMsgServer);
        int numberOfMissedMessages = ADMMessageHandler.getNumberOfMissedMessages();
        ADMMessageHandler.inBackground = false;
        if(numberOfMissedMessages > 0){
            String msg = ADMMessageHandler.getMostRecentMissedMessage();
            Log.i("savedMsg",msg);
            tView.append("Message(s) receieved. Your most recent was:\n");
            tView.append(msg + "\n");
            final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(getResources().getInteger(R.integer.sample_app_notification_id));
        }

        /* Listen for messages coming from SampleADMMessageHandler onMessage() callback. */
        msgReceiver = createBroadcastReceiver(msgKey);
        final IntentFilter messageIntentFilter= new IntentFilter(intentAction);
        messageIntentFilter.addCategory(msgCategory);
        this.registerReceiver(msgReceiver, messageIntentFilter);
        super.onResume();
    }
    
    /**
     * Create a {@link BroadcastReceiver} for listening to messages from ADM.
     * 
     * @param msgKey String to access message field from data JSON.
     * @param timeKey String to access timeStamp field from data JSON.
     * @return {@link BroadcastReceiver} for listening to messages from ADM.
     */
    private BroadcastReceiver createBroadcastReceiver(final String msgKey) {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver(){
            public void onReceive(final Context context, final Intent broadcastIntent){
                if(broadcastIntent != null){
                    final String msg = broadcastIntent.getStringExtra(msgKey);

                    if (msg != null){
                        Log.i("broadcastReceiver", msg);
                        final TextView tView = (TextView)findViewById(R.id.textMsgServer);
                        tView.append(msg + "\n");
                    }
                    /* Clear notifications if any. */
                    final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.cancel(context.getResources().getInteger(R.integer.sample_app_notification_id));
                }
            }
        };
        return broadcastReceiver;
    }


    public void onPause(){
        ADMMessageHandler.inBackground = true;
        this.unregisterReceiver(msgReceiver);
        super.onPause();
    }

    private void register(){
        final ADM adm = new ADM(this);
        if (adm.isSupported()){
            if(adm.getRegistrationId() == null){
                adm.startRegister();
            }
        }
    }
}
