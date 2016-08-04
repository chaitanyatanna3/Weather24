package com.awesome.chaitanya.weather24.gcm;

import android.content.Intent;
import com.google.android.gms.iid.InstanceIDListenerService;

public class MyInstanceIdListenerService extends InstanceIDListenerService {

    private static final String TAG = "MyInstanceIDLS";

    /**
     * Called if InstanceId token is updated. This may occur if the security of the previous token
     * has been compromised. This call is initiated by the InstanceId provider
     */
    @Override
    public void onTokenRefresh() {
        //Retch updated Instance ID token
        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);
    }
}
