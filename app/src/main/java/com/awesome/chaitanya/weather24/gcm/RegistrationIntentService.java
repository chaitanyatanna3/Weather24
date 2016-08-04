package com.awesome.chaitanya.weather24.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.awesome.chaitanya.weather24.MainActivity;
import com.awesome.chaitanya.weather24.R;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

public class RegistrationIntentService extends IntentService {

    private static final String TAG = "RegIntentService";

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            /**
             * In the unlikely event that multiple refresh operations occur simultaneously, ensure that
             * they are proceed sequentially.
             */
            synchronized (TAG) {
                //Initially this call goes out to the network to retrieve token, subsequent calls are local
                InstanceID instanceID = InstanceID.getInstance(this);

                String senderId = getString(R.string.gcm_defaultSenderId);
                if (senderId.length() != 0){
                String token = instanceID.getToken(senderId, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                sendRegistrationToServer(token);
                }
                /**
                 * You should store a boolean that indicates whether the generated toekn has been sent to your server.
                 * If the boolean is false, send the toekn to your server, otherwise your server should have already
                 * received the token.
                 */
                sharedPreferences.edit().putBoolean(MainActivity.SENT_TOKEN_TO_SERVER, true).apply();
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh" + e);
            /**
             * If an exception happens while fetching the new token or updating our Registration data on a
             * third-party server, this ensures that we'll attempt the update at a later time.
             */
        sharedPreferences.edit().putBoolean(MainActivity.SENT_TOKEN_TO_SERVER, false).apply();
        }
    }

    private void sendRegistrationToServer(String token) {
        Log.i(TAG, "GCM Registration Token: " + token);
    }
}
