package com.example.chaitanya.weather24.data;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import java.util.HashSet;

public class TestDb extends AndroidTestCase {

    public static final String LOG_TAG = TestDb.class.getSimpleName();

    //since we want each test to start with a clean slate
    void deleteTheDatabase() {
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
    }

    /**
     * This function gets called before each test is executed to delete the database.
     * This makes sure that we always have a clean test
     */
    public void setUp() {
        deleteTheDatabase();
    }

    public void testCreateDb() throws Throwable {
        /**
         * build a HashSet of all of the table names we widh to look for
         * Note that there will be another table in the DB that stores the
         * Andorid metadata
         */
        final HashSet<String> tableNameHashSet = new HashSet<String>();
        tableNameHashSet.add(WeatherContract.LocationEntry.TABLE_NAME);
        tableNameHashSet.add(WeatherContract.WeatherEntry.TABLE_NAME);

        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new WeatherDbHelper(this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());

        //have we created the tables we want?
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        assertTrue("Error: This means that the database has not been created correctly", c.moveToFirst());

        //verify that the tables have been created
        do {
            tableNameHashSet.remove(c.getString(0));
        } while (c.moveToNext());

        /**
         * if the fails, it means that your database doesn't contain both the location entry
         * and weather entry tables
         */
        assertTrue("Error: Your database was created without both the location entry and weather wntry tables", tableNameHashSet.isEmpty());

        //now, do our tables contain the correct columns?
        c = db.rawQuery("PRAMA table_info(" + WeatherContract.LocationEntry.TABLE_NAME + ")", null);
        assertTrue("Error: This means that we were unable to query the database for able information.", c.moveToFirst());

        //Build a Hashset of all of the column names we want to look for
        final HashSet<String> locationColumnHashSet = new HashSet<String>();
        locationColumnHashSet.add(WeatherContract.LocationEntry._ID);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_CITY_NAME);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_COORD_LAT);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_COORD_LONG);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING);
        int columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            locationColumnHashSet.remove(columnName);
        } while (c.moveToNext());

        /**
         * if this fails, it means that your database doesn't contain all of the required
         * location entry columns
         */
        assertTrue("Error: The database doesn't contain all of the required location entry columns", locationColumnHashSet.isEmpty());
        db.close();
    }

    public void testLocationTable() {
        insertLocation();
    }

    public void testWeatherTable() {
        long locationRowId = insertLocation();

        //make sure we have a valid row id.
        assertFalse("Error: Location not inserted Correctly", locationRowId == -1L);

        /**
         * First step:- Get reference to writable database
         * If there is an error in those massive SQL table creation string,
         * errors will be thrown here when you try to get a writable database.
         */
        WeatherDbHelper dbHelper = new WeatherDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        //Second step:- create weather values
        ContentValues contentValues = TestUtilities.createWeatherValues(locationRowId);

        //Third step:- Insert ContentValues into database and get a row ID back
        long weatherRowId = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, contentValues);
        assertTrue(weatherRowId != -1);

        //Fourth step:- Query the database and receive a Cursor back. A cursor is your primary interface to the query results.
        Cursor weatherCursor = db.query(WeatherContract.WeatherEntry.TABLE_NAME,
                null, null, null, null, null, null);

        //move the cursor to the first valid database row and check to see if we have any rows
        assertTrue("Error: No records returned from location query", weatherCursor.moveToFirst());

        //Fifth step:- Validate the location query
        TestUtilities.validateCurrentRecord("testInsertReadDb weatherEntry failed to validate", weatherCursor, contentValues);

        //move the cursor to demonstrate that there is only one record in the database
        assertFalse("Error: More than one record retrned from weather query", weatherCursor.moveToNext());

        //Sixth step:- Close cursor and database
        weatherCursor.close();
        dbHelper.close();
    }

    public long insertLocation() {
        //First step: Get reference to writable database
        WeatherDbHelper dbHelper = new WeatherDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        //Second Step:- Create ContentValues of what you want to insert
        ContentValues testValues = TestUtilities.createNorthPoleLocationValues();

        //Third Step:- Insert ContentValues into database and get a row ID back
        long locationRowId;
        locationRowId = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, testValues);

        //verify we get a row back
        assertTrue(locationRowId != -1);

        //Data's inserted

        //Fourth Step:-
        Cursor c = db.query(WeatherContract.LocationEntry.TABLE_NAME,
                null, null, null, null, null, null);

        //move the cursor to a valid database row and check to see if we got any records back from the query
        assertTrue("Error: No records returned from location query", c.moveToFirst());

        //Fifth Step:- Validate data in resulting cursor wiht the original ContentValues
        TestUtilities.validateCurrentRecord("Error: Location query validation failed", c, testValues);

        //move the cursor to demonstrate that there is only one record in the database
        assertFalse("Error: More than one record returned from location query", c.moveToNext());


        //Sixth Step:- Close cursor and database
        c.close();
        db.close();
        return locationRowId;
    }

}
