package com.example.chaitanya.weather24;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.ui.PlacePicker;

public class LocationEditTextPreference extends EditTextPreference {

    static final private int DEFAULT_MINIMUM_LOCATION_LENGTH = 2;
    private int mMinLength;

    public LocationEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.LocationEditTextPreference, 0, 0);
        try {
            mMinLength = a.getInteger(R.styleable.LocationEditTextPreference_minLength, DEFAULT_MINIMUM_LOCATION_LENGTH);
        }  finally {
            a.recycle();
        }

        /**
         * Check to see if Google Play Services is available. The Place Picker API is available
         * throught Google Play Services, so if this is false, we'll just carry on as thought this
         * feature does not exist. If it is true, however, we can add a widget to our preference.
         */
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(getContext());
        if (resultCode == ConnectionResult.SUCCESS) {
            // Add the current location widget to our location preference
            setWidgetLayoutResource(R.layout.pref_current_location);
        }
    }

    @Override
    protected View onCreateView(ViewGroup parent) {

        View view = super.onCreateView(parent);
        View currentLocation = view.findViewById(R.id.current_location);
        currentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getContext();

                /**
                 * Launch the plcae picker so that the user can specify their location,
                 * and then return the result to SettingsActivity
                 */
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

                /**
                 * We are in a view right now, not an activity. So we nned to get ourselves
                 * an activity that we an use to start our Place Picker intent. Bu using
                 * SettingActivity in this way, we can ensure the result of the Place Picker
                 * intent comes to the right plcae for us to process it.
                 */
                Activity settingsActivity = (SettingsActivity) context;
                try {
                    settingsActivity.startActivityForResult(builder.build(settingsActivity), SettingsActivity.PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesNotAvailableException | GooglePlayServicesRepairableException e) {

                }
            }
        });
        return view;
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        EditText et = getEditText();
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Dialog d = getDialog();
                if (d instanceof AlertDialog) {
                    AlertDialog dialog = (AlertDialog) d;
                    Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    //check if the EditText is empty
                    if (s.length() < mMinLength) {
                        //disable OK button
                        positiveButton.setEnabled(false);
                    } else {
                        //Re-enable the button
                        positiveButton.setEnabled(true);
                    }
                }
            }
        });
    }
}
