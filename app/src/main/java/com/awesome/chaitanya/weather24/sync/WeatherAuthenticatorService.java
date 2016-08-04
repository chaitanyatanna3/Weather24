package com.awesome.chaitanya.weather24.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * The service which allows the sync adapter framework to access the authenticator
 */
public class WeatherAuthenticatorService extends Service {

    //instance field that stores the authenticator object
    private WeatherAuthenticator weatherAuthenticator;

    @Override
    public void onCreate() {
        //create a ew authenticator object
        weatherAuthenticator = new WeatherAuthenticator(this);
    }

    /**
     * When the system binds to this service to make the RPC call
     * return the autenticator's IBinder
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return weatherAuthenticator.getIBinder();
    }
}
