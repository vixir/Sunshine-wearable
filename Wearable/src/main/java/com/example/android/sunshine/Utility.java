package com.example.android.sunshine;

import android.util.Log;

import java.util.Calendar;

/**
 * Created by DELL on 01-02-2017.
 */

public class Utility {

    private static final String LOG_TAG = "Utility";

    public static int getSmallArtResourceIdForWeatherCondition(int weatherId) {



        /*
         * Based on weather code data for Open Weather Map.
         */
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 771 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        } else if (weatherId >= 900 && weatherId <= 906) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 958 && weatherId <= 962) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 951 && weatherId <= 957) {
            return R.drawable.ic_clear;
        }

        Log.e(LOG_TAG, "Unknown Weather: " + weatherId);
        return R.drawable.ic_storm;
    }


    public static String getDay(int dayOfWeek) {
        String day;
        switch (dayOfWeek) {
            case Calendar.SUNDAY: {
                day = "SUNDAY";
                break;
            }
            case Calendar.MONDAY: {
                day = "MONDAY";
                break;
            }
            case Calendar.TUESDAY: {
                day = "TUESDAY";
                break;
            }
            case Calendar.WEDNESDAY: {
                day = "WEDNESDAY";
                break;
            }
            case Calendar.THURSDAY: {
                day = "THURSDAY";
                break;
            }
            case Calendar.FRIDAY: {
                day = "FRIDAY";
                break;
            }
            case Calendar.SATURDAY: {
                day = "SATURDAY";
                break;
            }

            default:
                day = "MONDAY";
        }
        return day;
    }

}
