package com.awesome.chaitanya.weather24.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class WeatherDbHelper extends SQLiteOpenHelper {

    //if you change the database schema, you must increment the database version
    private static final int DATABASE_VERSION = 1;

    //name of the database
    static final String DATABASE_NAME = "weather.db";

    //constructor
    public WeatherDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //create a table to hold locations. A location consists of the string supplied in the
        //location setting, the city name, and the latitude and longitude
        final String SQL_CREATE_LOCATION_TABLE =
                "CREATE TABLE IF NOT EXISTS " + WeatherContract.LocationEntry.TABLE_NAME + " (" +
                        WeatherContract.LocationEntry._ID + " INTEGER PRIMARY KEY," +
                        WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " TEXT UNIQUE NOT NULL, " +
                        WeatherContract.LocationEntry.COLUMN_CITY_NAME + " TEXT NOT NULL, " +
                        WeatherContract.LocationEntry.COLUMN_COORD_LAT + " REAL NOT NULL, " +
                        WeatherContract.LocationEntry.COLUMN_COORD_LONG + " REAL NOT NULL " +
                        " );";

        final String SQL_CREATE_WEATHER_TABLE =
                "CREATE TABLE IF NOT EXISTS " + WeatherContract.WeatherEntry.TABLE_NAME + " (" +
                        //WHY AUTOINCREMENT HERE, AND NOT ABOVE?
                        //UNIQUE KEYS WIL  BE AUTO-GENERATED IN EITHER CASE. BUT FOR WEATHER FORECASTING,
                        //IT'S REASONABLE TO ASSUME THE USER WILL WANT INFORMATION FOR A CERTAIN DATE AND ALL
                        //DATES, SO THE FORECAST DATA SHOULD BE SORTED ACCORDINGLY
                        WeatherContract.WeatherEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        //THE ID OF LOCATION ENTRY ASSOCIATED WITH THIS WEATHER DATA
                        WeatherContract.WeatherEntry.COLUMN_LOC_KEY + " INTEGER NOT NULL, " +
                        WeatherContract.WeatherEntry.COLUMN_DATE + " INTEGER NOT NULL, " +
                        WeatherContract.WeatherEntry.CLOUMN_SHORT_DESC + " TEXT NOT NULL, " +
                        WeatherContract.WeatherEntry.COLUMN_WEATHER_ID + " INTEGER NOT NULL, " +
                        WeatherContract.WeatherEntry.COLUMN_MIN_TEMP + " REAL NOT NULL, " +
                        WeatherContract.WeatherEntry.COLUMN_MAX_TEMP + " REAL NOT NULL, " +
                        WeatherContract.WeatherEntry.COLUMN_HUMIDITY + " REAL NOT NULL, " +
                        WeatherContract.WeatherEntry.COLUMN_PRESSURE + " REAL NOT NULL, " +
                        WeatherContract.WeatherEntry.COLUMN_WIND_SPEED + " REAL NOT NULL, " +
                        WeatherContract.WeatherEntry.COLUMN_DEGREES + " REAL NOT NULL, " +
                        //SET UP THE LOCATION COLUMN AS A FOREIGN KEY TO LOCATION TABLE
                        " FOREIGN KEY (" + WeatherContract.WeatherEntry.COLUMN_LOC_KEY + ") REFERENCES " +
                        WeatherContract.LocationEntry.TABLE_NAME + " (" + WeatherContract.LocationEntry._ID + "), " +
                        //TO ASSURE THE APPLICATION HAVE JUST ONE WEATHER WNRTY PER DAY PER LOCATION
                        //IT'S CREATED A UNIQUE CONSTRAINT WITH REPLACE STRATEGY
                        " UNIQUE (" + WeatherContract.WeatherEntry.COLUMN_DATE + ", " +
                        WeatherContract.WeatherEntry.COLUMN_LOC_KEY + ") ON CONFLICT REPLACE);";


        db.execSQL(SQL_CREATE_LOCATION_TABLE);
        db.execSQL(SQL_CREATE_WEATHER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /**
         * this is database is only cache for online data, so its upgrade policy is to simply to discard the
         * data and start over. note that this only fires if you change the version for your database.
         * it does not depend on the version number for your application.
         * if you want to update the schema without wiping data, commenting out next 2 lines should be your
         * top priority before modifying this method.
         */
        db.execSQL("DROP TABLE IF EXISTS "+ WeatherContract.LocationEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + WeatherContract.WeatherEntry.TABLE_NAME);
        onCreate(db);
    }
}
