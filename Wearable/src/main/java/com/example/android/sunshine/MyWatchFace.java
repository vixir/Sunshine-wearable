/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class MyWatchFace extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
        private static final String TAG = "Engine";
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        private GoogleApiClient googleApiClient;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;
        Bitmap mBitmap;
        Paint mBackgroundPaint;
        Paint mTextPaint;
        Paint mDay;
        Paint mTempHi;
        Paint mTempLo;
        Paint mMoodPic;

        private final DataApi.DataListener onDataChangedListener = new DataApi.DataListener() {
            @Override
            public void onDataChanged(DataEventBuffer dataEvents) {
                Log.e(TAG, "SOMETHING HAPPENED");
                for (DataEvent event : dataEvents) {
                    if (event.getType() == DataEvent.TYPE_CHANGED) {
                        DataItem item = event.getDataItem();
                        processConfigurationFor(item);
                    }
                }
                dataEvents.release();
                invalidateIfNecessary();
            }
        };
        private float mVerticalPadding = 10f;
        private float mHorizontalPadding = 10f;

        private void invalidateIfNecessary() {
            if (isVisible() && !isInAmbientMode()) {
                invalidate();
            }
        }

        private void processConfigurationFor(DataItem item) {
            Log.e(TAG, "Datamap read method called" + item.getUri());

            if ("/simple_watch_face_config".equals(item.getUri().getPath())) {
                Log.e(TAG, item.getUri().toString() + " ");
                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                Log.e(TAG, dataMap + " ");
                if (dataMap.containsKey("WEATHER_DATA")) {
                    String[] response = dataMap.getString("WEATHER_DATA").split(" ");
                    Log.e(TAG, "WEATHER_DATA" + dataMap.getString("WEATHER_DATA"));
                    int weatherId = Integer.parseInt(response[0]);
                    mMoodDrawable = mResources.getDrawable(R.drawable.ic_clear, null);
                    mBitmap = ((BitmapDrawable) mMoodDrawable).getBitmap();
                    mMaxTemp = (int) Double.parseDouble(response[1]) + "\u00b0";
                    mMinTemp = (int) Double.parseDouble(response[2]) + "\u00b0";
                }

            }
        }

        private final ResultCallback<DataItemBuffer> onConnectedResultCallback = new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(DataItemBuffer dataItems) {
                for (DataItem item : dataItems) {
                    processConfigurationFor(item);
                }
                dataItems.release();
                invalidateIfNecessary();
            }
        };

        boolean mAmbient;
        Calendar mCalendar;
        Resources mResources = MyWatchFace.this.getResources();
        float mXOffset;
        float mYOffset;
        boolean mIsRound;
        Drawable mMoodDrawable;
        Bitmap mBackgroundBitmap;
        String mMaxTemp;
        Paint mPaint;

        String mMinTemp;
        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            googleApiClient = new GoogleApiClient.Builder(MyWatchFace.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .setHotwordIndicatorGravity(Gravity.BOTTOM)
                    .build());
            mYOffset = mResources.getDimension(R.dimen.digital_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mResources.getColor(R.color.blue));

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(mResources.getColor(R.color.digital_text));

            mTempHi = new Paint();
            mTempHi = createTextPaint(mResources.getColor(R.color.digital_text));

            mTempLo = new Paint();
            mTempLo = createTextPaint(mResources.getColor(R.color.digital_text));

            mDay = new Paint();
            mDay = createTextPaint(mResources.getColor(R.color.digital_text));
            Drawable backgroundDrawable = mResources.getDrawable(R.drawable.bg, null);
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();
            mPaint = new Paint();
            mPaint.setColor(getResources().getColor(R.color.translucent, null));
            mPaint.setStyle(Paint.Style.FILL);
            //Default Values
            mMoodDrawable = mResources.getDrawable(R.drawable.ic_clear, null);
            mBitmap = ((BitmapDrawable) mMoodDrawable).getBitmap();
            mMaxTemp = "31" + "\u00b0";
            mMinTemp = "12" + "\u00b0";

            mCalendar = Calendar.getInstance();
        }

        @Override
        public void onDestroy() {
            releaseGoogleApiClient();
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                googleApiClient.connect();
                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                releaseGoogleApiClient();
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void releaseGoogleApiClient() {
            if (googleApiClient != null && googleApiClient.isConnected()) {
                Wearable.DataApi.removeListener(googleApiClient, onDataChangedListener);
                googleApiClient.disconnect();
            }
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = MyWatchFace.this.getResources();
            mIsRound = insets.isRound();
            mXOffset = resources.getDimension(mIsRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(mIsRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);
            float maxTempHi = resources.getDimension(mIsRound
                    ? R.dimen.digital_max_size_round : R.dimen.digital_max_size_square);

            mTextPaint.setTextSize(textSize);
            mTempHi.setTextSize(maxTempHi);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.
                    /*Toast.makeText(getApplicationContext(), R.string.message, Toast.LENGTH_SHORT)
                            .show();*/
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
                canvas.drawBitmap(mBackgroundBitmap, 0, 0, null);
            }


            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);
            int flags = DateUtils.FORMAT_SHOW_YEAR;
            String date = DateUtils.formatDateTime(getApplicationContext(), now, flags);
            String text = String.format("%d:%02d", mCalendar.get(Calendar.HOUR), mCalendar.get(Calendar.MINUTE));
            mXOffset = bounds.width() / 2 - mTextPaint.measureText(text) / 2;
            canvas.drawText(text, mXOffset, mYOffset, mTextPaint);
            String amPm = (mCalendar.get(Calendar.AM_PM) == Calendar.AM) ? "AM" : "PM";
            canvas.drawText(amPm, mTextPaint.measureText(text) + mXOffset, mYOffset, mTempLo);
            int dayOfWeek = mCalendar.get(Calendar.DAY_OF_WEEK);
            String day = Utility.getDay(dayOfWeek);
            if (mIsRound) {
                RectF rectF = new RectF();
                rectF.set(-bounds.width() / 2, bounds.height() / 2 + mVerticalPadding * 2, bounds.width() + bounds.width() / 2, 2 * bounds.height());
                canvas.drawArc(rectF, -180, 180, true, mPaint);
            } else {
                canvas.drawRect(0, bounds.height(), bounds.width(), bounds.height() / 2 + 3 * mVerticalPadding, mPaint);
            }
            canvas.drawText(mMinTemp, mXOffset + 2 * mHorizontalPadding, mYOffset + mTempLo.getTextSize() * 6 + mTextPaint.getTextSize(), mTempLo);
            canvas.drawText(mMaxTemp, mXOffset + mHorizontalPadding, mYOffset + mTempLo.getTextSize() * 4 + mTextPaint.getTextSize(), mTempHi);
            canvas.drawBitmap(mBitmap, mXOffset + mTempLo.measureText(mMinTemp) + mTempHi.measureText(mMaxTemp), mYOffset + mTextPaint.getTextSize() + mTempLo.getTextSize(), mTextPaint);
            canvas.drawText(day, bounds.width() / 2 - mTempLo.measureText(day) / 2, mYOffset + 2 * mTempLo.getTextSize(), mDay);
            canvas.drawText(date, bounds.width() / 2 - mTempLo.measureText(date) / 2, mYOffset + 2 * mTempLo.getTextSize() + 2 * mVerticalPadding, mDay);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.d(TAG, "connected GoogleAPI");
            Wearable.DataApi.addListener(googleApiClient, onDataChangedListener);
            Wearable.DataApi.getDataItems(googleApiClient).setResultCallback(onConnectedResultCallback);
        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        }
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFace.Engine> mWeakReference;

        public EngineHandler(MyWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }
}
