package org.blitzortung.android.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

import org.blitzortung.android.app.Main;
import org.blitzortung.android.app.R;
import org.blitzortung.android.app.view.PreferenceKey;

public class QuickSettingsDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final LayoutInflater layoutInflater = getActivity().getLayoutInflater();

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        final String[] regionValues = getResources().getStringArray(R.array.regions_values);
        final String currentRegionValue = preferences.getString(PreferenceKey.REGION.toString(), regionValues[0]);
        int selectedRegion = getSelectedIndex(regionValues, currentRegionValue);

        final String[] rasterSizeValues = getResources().getStringArray(R.array.raster_size_values);
        final String currentRasterSizeValue = preferences.getString(PreferenceKey.RASTER_SIZE.toString(), rasterSizeValues[0]);
        int selectedRasterSize = getSelectedIndex(rasterSizeValues, currentRasterSizeValue);

        final String[] queryPeriodValues = getResources().getStringArray(R.array.query_period_values);
        final String currentQueryPeriodValue = preferences.getString(PreferenceKey.QUERY_PERIOD.toString(), queryPeriodValues[0]);
        int selectedQueryPeriod = getSelectedIndex(queryPeriodValues, currentQueryPeriodValue);

        final String[] intervalDurationValues = getResources().getStringArray(R.array.interval_duration_values);
        final String currentIntervalDurationValue = preferences.getString(PreferenceKey.INTERVAL_DURATION.toString(), queryPeriodValues[0]);
        int selectedIntervalDuration = getSelectedIndex(intervalDurationValues, currentIntervalDurationValue);

        @SuppressLint("InflateParams") final View view = layoutInflater.inflate(R.layout.quick_settings_dialog, null, false);

        final Spinner selectedRegionList = (Spinner) view.findViewById(R.id.selected_region);
        selectedRegionList.setSelection(selectedRegion);

        final Spinner rasterSizeSpinner = (Spinner) view.findViewById(R.id.selected_raster_size);
        rasterSizeSpinner.setSelection(selectedRasterSize);

        final Spinner intervalDurationSpinner = (Spinner) view.findViewById(R.id.selected_interval_duration);
        intervalDurationSpinner.setSelection(selectedIntervalDuration);

        final Spinner queryPeriodSpinner = (Spinner) view.findViewById(R.id.selected_query_period);
        queryPeriodSpinner.setSelection(selectedQueryPeriod);

        builder.setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String regionValue = regionValues[selectedRegionList.getSelectedItemPosition()];
                        final String rasterSizeValue = rasterSizeValues[rasterSizeSpinner.getSelectedItemPosition()];
                        final String intervalDurationValue = intervalDurationValues[intervalDurationSpinner.getSelectedItemPosition()];
                        final String queryPeriodValue = queryPeriodValues[queryPeriodSpinner.getSelectedItemPosition()];

                        Log.i(Main.LOG_TAG, "region: " + regionValue);
                        preferences.edit()
                                .putString(PreferenceKey.REGION.toString(), regionValue)
                                .putString(PreferenceKey.RASTER_SIZE.toString(), rasterSizeValue)
                                .putString(PreferenceKey.INTERVAL_DURATION.toString(), intervalDurationValue)
                                .putString(PreferenceKey.QUERY_PERIOD.toString(), queryPeriodValue)
                                .apply();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        return builder.create();
    }

    private int getSelectedIndex(String[] regionValues, String currentRegionValue) {
        int selectedRegion = 0;
        for (String regionValue : regionValues) {
            if (regionValue.equals(currentRegionValue)) {
                break;
            }
            selectedRegion++;
        }
        return selectedRegion < regionValues.length ? selectedRegion : 0;
    }
}
