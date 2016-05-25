package com.example.chaitanya.weather24;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.chaitanya.weather24.data.WeatherContract;
import com.example.chaitanya.weather24.sync.WeatherSyncAdapter;

/**
 * Encapsulates fetching the forecast and displaying it as a (@Link Listview) layout
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String LOG_TAG = ForecastFragment.class.getSimpleName();
    private ForecastAdapter mForecastAdapter;

    private ListView mListView;
    private int mPosition = ListView.INVALID_POSITION;
    private boolean mUseTodayLayout;

    private static final String SELECTED_KEY = "selected_position";
    private static final int FORECAST_LOADER = 0;

    /**
     * For the forecast view we're showing only a small subset of the stored data.
     * Specify the columns need.
     */
    private static final String[] FORECAST_COLUMNS = {
            /**
             * In this case the id needs to be fully qualified with a table name, since
             * the content provider joins the location and weather tables in the background.
             * On one hand, that's annoying. On other hand, you can search the weather table
             * using the location set by the user, which is only in the location table.
             * So the convenience is worth it.
             */
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.CLOUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    /**
     * These are indices are tied to FORECAST_COLUMNS. If FORECAST_COLUMNSchanges, these
     * must change.
     */
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;

    /**
     * A callback interface that all activates containing this fragment
     * must implement. This mechanism allows activities to be notified
     * of item selections
     */
    public interface Callback {
        //DetailFragmentCallback for when an item has been selected
        public void onItemSelected(Uri dateUri);
    }

    public ForecastFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        /**
         * Handle action bar item clicks here. The action bar
         * will automactically handle clicks on the Home/Up
         * button, so long as you specify a parent activity in AndroidManifest.xml
         */
        int id = item.getItemId();
//        if (id == R.id.action_refresh) {
//            updateWeather();
//            return true;
//        }
        if (id == R.id.action_map) {
            openPreferredLocationInMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        /**
         * The ForecastAdapter will take data from a source and use
         * it to populate the ListView it's attached to.
         */
        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        //Get the reference to the ListView, and attached this adapter to it.
        mListView = (ListView) rootView.findViewById(R.id.listview_forecast);
        mListView.setAdapter(mForecastAdapter);

        //We'll call our MainActivity
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /**
                 * CursorAdapter returns a cursor at the correct position for getItem(), or
                 * null if it cannot seek to that position
                 */
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                if (cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                    ((Callback) getActivity()).onItemSelected(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationSetting, cursor.getLong(COL_WEATHER_DATE)));
                }
                mPosition = position;
            }
        });

        /**
         * If there's instance state, mine it for useful information. The end-goal here is that
         * user never knows that turning their devices sideways does crazy lifecycle related
         * things. It should feel like some stuff stretched out, or magically appeared to take advantage of room,
         * but data or place in app was never actually "lost".
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            /**
             * The ListView probably hasn't even been populated yet. Actually perform the swapout
             * in onLoadFinished.
             */
        mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }
        mForecastAdapter.setUseTodayLayout(mUseTodayLayout);

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    //Since we read the location when we create the loader, all we need to do is to restart things
    void onLocationChanged() {
        updateWeather();
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
    }

    private void updateWeather() {
        WeatherSyncAdapter.syncImmediately(getActivity());
    }

    private void openPreferredLocationInMap() {
        /**
         * Using URI scheme for showing a location found on a map. This super-handy
         * intent can is detailed in the "Common Intents" page of Android's developer site:-
         * https://developer.android.com/guide/components/intents-common.html#Maps
         */
        if (null != mForecastAdapter) {
            Cursor cursor = mForecastAdapter.getCursor();
            if (null != cursor) {
                cursor.moveToPosition(0);
                String posLat = cursor.getString(COL_COORD_LAT);
                String posLong = cursor.getString(COL_COORD_LONG);
                Uri geoLocation = Uri.parse("Geo:" + posLat + "," + posLong);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Log.d(LOG_TAG, "Couldn't call " + geoLocation.toString() + " no receiving apps installed!!");
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        /**
         * When tablets rotate, the currently selected list item needs to be saved.
         * When no item is selected, mPosition will be set to ListView.INVALID_POSITION,
         * so chack for that before storing.
         */
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        /**
         * This is called when a new Loader needs to be created. This fragment only uses one
         * loader, so we don't care about checking the id.
         * To only show current and future dates, filter the query to return weather only for dates after on including today.
         */
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        String locationSetting = Utility.getPreferredLocation(getActivity());
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(locationSetting, System.currentTimeMillis());
        return new CursorLoader(getActivity(), weatherForLocationUri, FORECAST_COLUMNS, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mForecastAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION) {
            /**
             * If we don't need to restart the loader, and thers's a desired position to restore to,
             * do so now.
             */
        mListView.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mForecastAdapter.swapCursor(null);
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
        if (mForecastAdapter != null) {
            mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }
}