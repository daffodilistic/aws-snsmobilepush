/*
 * Copyright 2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.androidtest;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
// Requires Android 2.2 or higher, Google Play Services on the target device, and an active google account on the device.

public class AndroidMobilePushApp extends Activity {
    private TextView tView;
    private SharedPreferences savedValues;
    private String numOfMissedMessages;

    // Since this activity is SingleTop, there can only ever be one instance. This variable corresponds to this instance.
    public static Boolean inBackground = true;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        numOfMissedMessages = getString(R.string.num_of_missed_messages);
        setContentView(R.layout.activity_main);
        tView = (TextView) findViewById(R.id.tViewId);
        tView.setMovementMethod(new ScrollingMovementMethod());
        startService(new Intent(this, MessageReceivingService.class));
    }

    public void onStop(){
        super.onStop();
        inBackground = true;
    }

    public void onRestart(){
        super.onRestart();
        tView.setText("");;
    }

    public void onResume(){
        super.onResume();
        inBackground = false;
        savedValues = MessageReceivingService.savedValues;
        int numOfMissedMessages = 0;
        if(savedValues != null){
            numOfMissedMessages = savedValues.getInt(this.numOfMissedMessages, 0);
        }
        String newMessage = getMessage(numOfMissedMessages);
        if(newMessage!=""){
            Log.i("displaying message", newMessage);
            tView.append(newMessage);
        }
    }

    public void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
    }

    // If messages have been missed, check the backlog. Otherwise check the current intent for a new message.
    private String getMessage(int numOfMissedMessages) {
        String message = "";
        String linesOfMessageCount = getString(R.string.lines_of_message_count);
        if(numOfMissedMessages > 0){
            String plural = numOfMissedMessages > 1 ? "s" : "";
            Log.i("onResume","missed " + numOfMissedMessages + " message" + plural);
            tView.append("You missed " + numOfMissedMessages +" message" + plural + ". Your most recent was:\n");
            for(int i = 0; i < savedValues.getInt(linesOfMessageCount, 0); i++){
                String line = savedValues.getString("MessageLine"+i, "");
                message+= (line + "\n");
            }
            NotificationManager mNotification = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotification.cancel(R.string.notification_number);
            SharedPreferences.Editor editor=savedValues.edit();
            editor.putInt(this.numOfMissedMessages, 0);
            editor.putInt(linesOfMessageCount, 0);
            editor.commit();
        }
        else{
            Log.i("onResume","no missed messages");
            Intent intent = getIntent();
            if(intent!=null){
                Bundle extras = intent.getExtras();
                if(extras!=null){
                    for(String key: extras.keySet()){
                        message+= key + "=" + extras.getString(key) + "\n";
                    }
                }
            }
        }
        message+="\n";
        return message;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_clear){
            tView.setText("");
            return true;
        }
        else{
            return super.onOptionsItemSelected(item);
        }
    }
}
