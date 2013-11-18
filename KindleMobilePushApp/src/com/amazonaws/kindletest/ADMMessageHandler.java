/*
 * [ADMMessenger]
 *
 * (c) 2012, Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazonaws.kindletest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.amazon.device.messaging.ADMConstants;
import com.amazon.device.messaging.ADMMessageHandlerBase;
import com.amazon.device.messaging.ADMMessageReceiver;

public class ADMMessageHandler extends ADMMessageHandlerBase {
    private static String savedMessage = null;
    private static int numberOfMissedMessages = 0;
    public static boolean inBackground = true;

    public ADMMessageHandler(){
        super(ADMMessageHandler.class.getName());
    }

    public static class MessageAlertReceiver extends ADMMessageReceiver{
        public MessageAlertReceiver(){
            super(ADMMessageHandler.class);
        }
    }

    public void onCreate(){
        super.onCreate();
    }

    protected void onMessage(final Intent intent) {
        Log.i("onMessage", "received a message");
        /* String to access message field from data JSON. */
        final String msgKey = getString(R.string.json_data_msg_key);
        /* Intent action that will be triggered in onMessage() callback. */
        final String intentAction = getString(R.string.intent_msg_action);
        final Bundle extras = intent.getExtras();
        
        verifyMD5Checksum(extras);
        
        /* Extract message from the extras in the intent. */
        String msg = "";
        for(String key : extras.keySet()){
            msg += key + "=" + extras.getString(key) + "\n";
        }
        if(inBackground){
            postNotification(msg);
        }
        else{
            /* Intent category that will be triggered in onMessage() callback. */
            final String msgCategory = getString(R.string.intent_msg_category);

            /* Broadcast an intent to update the app UI with the message. */
            /* The broadcast receiver will only catch this intent if the app is within the onResume state of its lifecycle. */
            /* User will see a notification otherwise. */
            final Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(intentAction);
            broadcastIntent.addCategory(msgCategory);
            broadcastIntent.putExtra(msgKey, msg);
            this.sendBroadcast(broadcastIntent);
        }
    }

    /**
     * This method posts a notification to notification manager.
     * @param msg Message that is included in the ADM message.
     */
    private void postNotification(final String msg){
        final Context context = getApplicationContext();
        final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final Builder notificationBuilder = new Notification.Builder(context);
        final Intent notificationIntent = new Intent(context, KindleMobilePushApp.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL);
        final Notification notification = notificationBuilder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Message(s) Received!")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .getNotification();

        /* Posting notification on notification bar. */
        final int notificationId = context.getResources().getInteger(R.integer.sample_app_notification_id);
        mNotificationManager.notify(notificationId, notification);
        savedMessage = msg;
        numberOfMissedMessages++;
    }

    public static String getMostRecentMissedMessage(){
        numberOfMissedMessages = 0;
        return savedMessage;
    }
    
    public static int getNumberOfMissedMessages(){
        return numberOfMissedMessages ;
    }
    /**
     * This method verifies the MD5 checksum of the ADM message.
     * 
     * @param extras Extra that was included with the intent.
     */
    private void verifyMD5Checksum(final Bundle extras){
        /* String to access consolidation key field from data JSON. */
        final String consolidationKey = getString(R.string.json_data_consolidation_key);
        
        final Set<String> extrasKeySet = extras.keySet();
        final Map<String, String> extrasHashMap = new HashMap<String, String>();
        for (String key : extrasKeySet){
            if (!key.equals(ADMConstants.EXTRA_MD5) && !key.equals(consolidationKey)){
                extrasHashMap.put(key, extras.getString(key));
            }            
        }
        final ADMSampleMD5ChecksumCalculator checksumCalculator = new ADMSampleMD5ChecksumCalculator();
        final String md5 = checksumCalculator.calculateChecksum(extrasHashMap);        
        final String admMd5 = extras.getString(ADMConstants.EXTRA_MD5);

        /* Data integrity check. */
        if(!admMd5.trim().equals(md5.trim())){
            Log.w("verifyMD5Checksum", "SampleADMMessageHandler:onMessage MD5 checksum verification failure. " +
            "Message received with errors");
        }
    }

    protected void onRegistrationError(final String string){
        Log.e("onRegistrationError", string);
    }

    protected void onRegistered(final String registrationId){
        Log.i("onRegistered", registrationId);
    }

    protected void onUnregistered(final String registrationId){
        Log.i("onUnregistered", registrationId);
    }
}
