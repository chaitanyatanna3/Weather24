package com.example.chaitanya.weather24.data;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.test.AndroidTestCase;
import android.util.Log;

public class TestProvider extends AndroidTestCase {

    public static final String LOG_TAG = TestProvider.class.getSimpleName();

    public void deleteAllRecordesFromProvider() {
        mContext.getContentResolver().delete(WeatherContract.WeatherEntry.CONTENT_URI, null, null);
        mContext.getContentResolver().delete(WeatherContract.LocationEntry.COTENT_URI, null, null);

        Cursor cursor = mContext.getContentResolver().query(WeatherContract.WeatherEntry.CONTENT_URI, null, null, null, null);
        assertEquals("Error: Records not deleted from Weather table duirng delete", 0, cursor.getCount());
        cursor.close();

        cursor = mContext.getContentResolver().query(WeatherContract.LocationEntry.COTENT_URI, null, null, null, null);
        assertEquals("Error: Records not deleted from location table during delete", 0, cursor.getCount());
        cursor.close();
    }

    public void deleteAllRecords() {
        deleteAllRecordesFromProvider();
    }

    /**
     * Since we want each test to start with a clean slate, run deleteAllRecords in setUp
     */

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteAllRecords();
    }

    /**
     * This test checks to make sure that the content provider is registered correctly.
     */
    public void testProviderRegistry() {
        PackageManager pm = mContext.getPackageManager();
        /**
         * We define the component name based on the package name from the context
         * and the WeatherProvider class
         */
        ComponentName componentName = new ComponentName(mContext.getPackageName(), WeatherProvider.class.getName());
        try {
            /**
             * Fetch the provider info using the component name from the PackageManager
             */
            ProviderInfo providerInfo = pm.getProviderInfo(componentName, 0);

            //make sure that the registered authority matches the authority from the contract
            assertEquals("Error: WeatherProvider registered with authority:" + providerInfo.authority
            + "instead of authority: " + WeatherContract.CONTENT_AUTHORITY, providerInfo.authority, WeatherContract.CONTENT_AUTHORITY);
        } catch (PackageManager.NameNotFoundException e) {
            assertTrue("Error: WeatherProvider not registered at " + mContext.getPackageName(), false);
            e.printStackTrace();
        }
    }

    //It verifies that the ContentProvider returns the correct type for each type of URI that it can handle
    public void testGetType() {
        //content://com.example.chaitanya.weather24/weather
        String type = mContext.getContentResolver().getType(WeatherContract.WeatherEntry.CONTENT_URI);
        assertEquals("Error: the WeatherEntry CONTENT_URI should return WeatheEntry.CONTENT_TYPE", WeatherContract.WeatherEntry.CONTENT_TYPE, type);

        String testLocation = "95132";
        //content://com.example.chaitanya.weather24/weather/95132
        type = mContext.getContentResolver().getType(WeatherContract.WeatherEntry.buildWeatherLocation(testLocation));
        assertEquals("Error: the WeatherEntry CONTENT_URI with location should return WeatheEntry.CONTENT_TYPE", WeatherContract.WeatherEntry.CONTENT_TYPE, type);

        long testDate = 1419120000L; //Dcember 21st, 2014
        //content://com.example.chaitanya.weather24/weather/95132/20140612
        type = mContext.getContentResolver().getType(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(testLocation, testDate));
        assertEquals("Error: the WeatherEntry CONTENT_URI with location and date should return WeatheEntry.CONTENT_ITEM_TYPE", WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE, type);

        //content://com.example.chaitanya.weather24/location
        type = mContext.getContentResolver().getType(WeatherContract.LocationEntry.COTENT_URI);
        assertEquals("Error: the LocationEntry CONTENT_URI should return LocationEntry.CONTENT_TYPE", WeatherContract.LocationEntry.CONTENT_TYPE, type);
    }

    /**
     * This test uses the database directly to insert and then uses the ContentProvider
     * to read out the data
     */
    public void testBasicWeatherQuery() {
        //insert our test records into the database
        WeatherDbHelper dbHelper = new WeatherDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues testValues = TestUtilities.createNorthPoleLocationValues();
        long locationRowId = TestUtilities.insertNorthPoleLocationValues(mContext);

        //Fantastic! Now that we have a location, add some weather!
        ContentValues weatherValues = TestUtilities.createWeatherValues(locationRowId);

        long weatherRowId = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, weatherValues);
        assertTrue("Unable to insert WeatherEntry into the database", weatherRowId != -1);

        db.close();

        //test the basic content provider query
        Cursor weatherCursor = mContext.getContentResolver().query(WeatherContract.WeatherEntry.CONTENT_URI,
                null, null, null, null);

        //make sure we get the correct cursor out of the database
        TestUtilities.validateCursor("testBasicWeatherQuery", weatherCursor, weatherValues);
    }

    /**
     * This test uses the database directly to insert and then uses ContentProvider to read out the data.
     */
    public void testBasicLocationQueries() {
        //insert our test records into the database
        WeatherDbHelper dbHelper = new WeatherDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues testValues = TestUtilities.createNorthPoleLocationValues();
        long locationRowId = TestUtilities.insertNorthPoleLocationValues(mContext);

        //test the basix content provider query
        Cursor locationCursor = mContext.getContentResolver().query(WeatherContract.LocationEntry.COTENT_URI,
                null, null, null, null);

        //make sure we get the correct cursor out of the database
        TestUtilities.validateCursor("testBasicLocationQueries", locationCursor, testValues);

        /**
         * has notification uri been set correctly?
         */
        if (Build.VERSION.SDK_INT >= 19) {
            assertEquals("Error: Location Query did not properly set NotificationUri", locationCursor.getNotificationUri(),
                    WeatherContract.LocationEntry.COTENT_URI);
        }
    }

    /**
     * This test uses the provider to insert and then update the data.
     */
    public void testUpdateLocation() {
        //create a new map of values, where column names are the keys
        ContentValues values = TestUtilities.createNorthPoleLocationValues();

        Uri locationUri = mContext.getContentResolver().insert(WeatherContract.LocationEntry.COTENT_URI, values);
        long locationRowId = ContentUris.parseId(locationUri);

        //verify we get a row back.
        assertTrue(locationRowId != -1);
        Log.d(LOG_TAG, "New row id: " + locationRowId);

        ContentValues updatedValues = new ContentValues(values);
        updatedValues.put(WeatherContract.LocationEntry._ID, locationRowId);
        updatedValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, "Santa's Village");

        /**
         * Create a cursor with observer to make sure that the content provider
         * is notifying the observers as expected
         */
        Cursor locatinCursor = mContext.getContentResolver().query(WeatherContract.LocationEntry.COTENT_URI,
                null, null, null, null);

        TestUtilities.TestContentObserver tco = TestUtilities.getTestContentObserver();
        locatinCursor.registerContentObserver(tco);

        int count = mContext.getContentResolver().update(WeatherContract.LocationEntry.COTENT_URI, updatedValues,
                WeatherContract.LocationEntry._ID + "= ?", new String[] {Long.toString(locationRowId)});
        assertEquals(count, 1);

        /**
         * Test to make sure our observer is called. If not, we throw an assertion.
         */
        tco.waitForNotificationOrFail();

        locatinCursor.unregisterContentObserver(tco);
        locatinCursor.close();

        //A cursor is your primary interface to the query results.
        Cursor cursor = mContext.getContentResolver().query(WeatherContract.LocationEntry.COTENT_URI, null,
                WeatherContract.LocationEntry._ID + " = " + locationRowId, null, null);

        TestUtilities.validateCursor("testUpdateLocation. Error validating location entry update.", cursor, updatedValues);
        cursor.close();
    }

    /**
     * Make sure we can still delete after adding/updating stuff
     */
    public void testInsertReadProvider() {
        ContentValues testValues = TestUtilities.createNorthPoleLocationValues();

        //Register a content observer for our insert. This time, directly with the content resolver.
        TestUtilities.TestContentObserver tco = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(WeatherContract.LocationEntry.COTENT_URI, true, tco);
        Uri locationUri = mContext.getContentResolver().insert(WeatherContract.LocationEntry.COTENT_URI, testValues);

        /**
         * Did our content observer get called?
         */
        tco.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(tco);

        long locationRowId = ContentUris.parseId(locationUri);

        //verify we get a row back
        assertTrue(locationRowId != -1);

        /**
         * Data's inserted. Now pull some out to stare at it and verify ti made the round trip.
         *
         * A cursor is your primary interface to the query results.
         */
        Cursor cursor = mContext.getContentResolver().query(WeatherContract.LocationEntry.COTENT_URI,
                null, null, null, null);

        TestUtilities.validateCursor("testInsertReadProvider. Error validating LocationEntry.", cursor, testValues);

        //Now that we have a location, add some weather.
        ContentValues weatherValues = TestUtilities.createWeatherValues(locationRowId);
        //The tco is one-shot class
        tco = TestUtilities.getTestContentObserver();

        mContext.getContentResolver().registerContentObserver(WeatherContract.WeatherEntry.CONTENT_URI, true, tco);

        Uri weatherInsertUri = mContext.getContentResolver().insert(WeatherContract.WeatherEntry.CONTENT_URI, weatherValues);
        assertTrue(weatherInsertUri != null);

        /**
         * Did our content observer get called?
         */
        tco.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(tco);

        //A cursor is your primary interface to the query results.
        Cursor weatherCursor = mContext.getContentResolver().query(WeatherContract.WeatherEntry.CONTENT_URI,
                null, null, null, null);

        TestUtilities.validateCursor("testInsertReadProvider. Error validating WeatherEntry insert.", weatherCursor, weatherValues);

        /**
         * Add the location values in with the weather data so that we can make sure
         * that the join worked and we actually get all the values back.
         */
        weatherValues.putAll(testValues);

        //Get the joined weather and location data
        weatherCursor = mContext.getContentResolver().query(WeatherContract.WeatherEntry.buildWeatherLocation(TestUtilities.TEST_LOCATION),
                null, null, null, null);
        TestUtilities.validateCursor("Error validating joined weather and location data.", weatherCursor, weatherValues);

        //Get the joined weather and location with a start date
        weatherCursor = mContext.getContentResolver().query(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(TestUtilities.TEST_LOCATION,
                TestUtilities.TEST_DATE), null, null, null, null);
        TestUtilities.validateCursor("Error validating joined weather and location data with a start date.", weatherCursor, weatherValues);

        //Get the joined weather data for a specific date
        weatherCursor = mContext.getContentResolver().query(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(TestUtilities.TEST_LOCATION,
                TestUtilities.TEST_DATE), null, null, null, null);
        TestUtilities.validateCursor("Error validating joined weather data for a specific date.", weatherCursor, weatherValues);
    }

    /**
     * Make sure we can still delete after adding/updating stuff
     */

    public void testDeleteRecord() {
        testInsertReadProvider();

        //Register a content observer for our location delete
        TestUtilities.TestContentObserver locationObserver = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(WeatherContract.LocationEntry.COTENT_URI, true, locationObserver);

        //register a content observer for our weather delete
        TestUtilities.TestContentObserver weatherObserver = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(WeatherContract.WeatherEntry.CONTENT_URI, true, weatherObserver);

        deleteAllRecordesFromProvider();

        locationObserver.waitForNotificationOrFail();
        weatherObserver.waitForNotificationOrFail();

        mContext.getContentResolver().unregisterContentObserver(locationObserver);
        mContext.getContentResolver().unregisterContentObserver(weatherObserver);
    }

    static private final int BULK_INSERT_RECORDS_TO_INSERT = 10;

    static ContentValues[] createBulkInsertWeatherValued(long locationRowId) {
        long currentTestDate = TestUtilities.TEST_DATE;
        long millisecondsInADay = 1000 * 60 * 60 * 24;
        ContentValues[] returnContentValues = new ContentValues[BULK_INSERT_RECORDS_TO_INSERT];

        for (int i = 0; i < BULK_INSERT_RECORDS_TO_INSERT; i++, currentTestDate += millisecondsInADay) {
            ContentValues weatherValues = new ContentValues();
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationRowId);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, currentTestDate);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, 1.1);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, 1.2 + 0.01 * (float) i);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, 1.3 - 0.01 * (float) i);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, 75 + i);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, 65 - i);
            weatherValues.put(WeatherContract.WeatherEntry.CLOUMN_SHORT_DESC, "Asteroids");
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, 5.5 + 0.2 * (float) i);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, 321);
            returnContentValues[i] = weatherValues;
        }
        return returnContentValues;
    }

    //BulkInsert ContentProvider function
    public void testBulkInsert() {
        //first, let's create a location value
        ContentValues testValues = TestUtilities.createNorthPoleLocationValues();
        Uri locationUri = mContext.getContentResolver().insert(WeatherContract.LocationEntry.COTENT_URI, testValues);
        long locationRowId = ContentUris.parseId(locationUri);

        //verify we get a row back
        assertTrue(locationRowId != -1);

        //Data's inserted. Now pull some out to stare at it and verify it we made a round trip
        //A cursor is your primary interface to the query results.
        Cursor cursor = mContext.getContentResolver().query(WeatherContract.LocationEntry.COTENT_URI,
                null, null, null, null);

        TestUtilities.validateCursor("Error while validating LocationEntry", cursor, testValues);

        /**
         * Noew we can bulkInsert some weather. In fact, we only implement BulkInsert for
         * weather entries. With ContentProviders, you really only have to implement the
         * features use, after all.
         */
        ContentValues[] bulkInsertContentValues = createBulkInsertWeatherValued(locationRowId);

        //Register a content observer or our bulk insert.
        TestUtilities.TestContentObserver weatherObserver = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(WeatherContract.WeatherEntry.CONTENT_URI, true, weatherObserver);

        int insertCount = mContext.getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, bulkInsertContentValues);

        weatherObserver.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(weatherObserver);

        assertEquals(insertCount, BULK_INSERT_RECORDS_TO_INSERT);

        //A cursor is your primary interface to the query results
        cursor = mContext.getContentResolver().query(WeatherContract.WeatherEntry.CONTENT_URI, null, null, null, null);

        //we should have as many records in the database as we've inserted.
        assertEquals(cursor.getCount(), BULK_INSERT_RECORDS_TO_INSERT);

        //and let's make sure they match the ones we created.
        cursor.moveToFirst();

        for (int i = 0; i < BULK_INSERT_RECORDS_TO_INSERT; i++, cursor.moveToNext()) {
            TestUtilities.validateCursor("Error while calidating WeatherEntry" + 1, cursor, bulkInsertContentValues[i]);
        }
        cursor.close();
    }

}
