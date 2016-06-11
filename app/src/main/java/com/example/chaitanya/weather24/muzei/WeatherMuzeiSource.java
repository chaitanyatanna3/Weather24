package com.example.chaitanya.weather24.muzei;


import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.example.chaitanya.weather24.MainActivity;
import com.example.chaitanya.weather24.Utility;
import com.example.chaitanya.weather24.data.WeatherContract;
import com.example.chaitanya.weather24.sync.WeatherSyncAdapter;
import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.MuzeiArtSource;

/**
 * Muzei source that changes your background based on the current weather conditions
 */
public class WeatherMuzeiSource extends MuzeiArtSource {

    private static final String[] FORECAST_COLUMNS = new String[] {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.CLOUMN_SHORT_DESC
    };

    //these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_SHORT_DESC = 1;

    public WeatherMuzeiSource() {
        super("WeatherMuzeiSource");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);
        boolean dataUpdated = intent != null &&
                WeatherSyncAdapter.ACTION_DATA_UPDATED.equals(intent.getAction());
        if (dataUpdated && isEnabled()) {
            onUpdate(UPDATE_REASON_OTHER);
        }
    }

    @Override
    protected void onUpdate(int reason) {
        String location = Utility.getPreferredLocation(this);
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(location, System.currentTimeMillis());
        Cursor cursor = getContentResolver().query(weatherForLocationUri, FORECAST_COLUMNS, null, null,
                WeatherContract.WeatherEntry.COLUMN_DATE + " ASC");
        if (cursor.moveToFirst()) {
            int weatherId = cursor.getInt(INDEX_WEATHER_ID);
            String desc = cursor.getString(INDEX_SHORT_DESC);

            String imageUrl = Utility.getImageUrlForWeatherCondition(weatherId);
            //only publish a new wallpaper if we have a valid image
            if (imageUrl != null) {
                publishArtwork(new Artwork.Builder()
                .imageUri(Uri.parse(imageUrl))
                .title(desc)
                .byline(location)
                .viewIntent(new Intent(this, MainActivity.class))
                .build());
            }
        }
        cursor.close();
    }
}
