// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import android.view.View;

import com.mopub.common.CreativeOrientation;
import com.mopub.common.IntentActions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.DeviceUtils;
import com.mopub.common.util.Intents;
import com.mopub.mraid.MraidVideoViewController;

import java.io.Serializable;

import static com.mopub.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static com.mopub.common.DataKeys.CREATIVE_ORIENTATION_KEY;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.mobileads.BaseBroadcastReceiver.broadcastAction;

public class MraidVideoPlayerActivity extends BaseVideoPlayerActivity implements BaseVideoViewController.BaseVideoViewControllerListener {
    @Nullable private BaseVideoViewController mBaseVideoController;
    private long mBroadcastIdentifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBroadcastIdentifier = getBroadcastIdentifierFromIntent(getIntent());

        try {
            mBaseVideoController = createVideoViewController(savedInstanceState);
        } catch (IllegalStateException e) {
            // This can happen if the activity was started without valid intent extras. We leave
            // mBaseVideoController set to null, and finish the activity immediately.

            broadcastAction(this, mBroadcastIdentifier, IntentActions.ACTION_FULLSCREEN_FAIL);
            finish();
            return;
        }

        final Serializable orientationExtra = getIntent().getSerializableExtra(
                CREATIVE_ORIENTATION_KEY);
        CreativeOrientation requestedOrientation = CreativeOrientation.DEVICE;
        if (orientationExtra instanceof CreativeOrientation) {
            requestedOrientation = (CreativeOrientation) orientationExtra;
        }
        DeviceUtils.lockOrientation(this, requestedOrientation);

        mBaseVideoController.onCreate();
    }

    @Override
    protected void onPause() {
        if (mBaseVideoController != null) {
            mBaseVideoController.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBaseVideoController != null) {
            mBaseVideoController.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        if (mBaseVideoController != null) {
            mBaseVideoController.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mBaseVideoController != null) {
            mBaseVideoController.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mBaseVideoController != null) {
            mBaseVideoController.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onBackPressed() {
        if (mBaseVideoController != null && mBaseVideoController.backButtonEnabled()) {
            super.onBackPressed();
            mBaseVideoController.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (mBaseVideoController != null) {
            mBaseVideoController.onActivityResult(requestCode, resultCode, data);
        }
    }

    private BaseVideoViewController createVideoViewController(Bundle savedInstanceState) throws IllegalStateException {
        String clazz = getIntent().getStringExtra(VIDEO_CLASS_EXTRAS_KEY);

        if ("vast".equals(clazz)) {
            return new VastVideoViewController(this, getIntent().getExtras(), savedInstanceState, mBroadcastIdentifier, this);
        } else if ("mraid".equals(clazz)) {
            return new MraidVideoViewController(this, getIntent().getExtras(), savedInstanceState, this);
        } else {
            throw new IllegalStateException("Unsupported video type: " + clazz);
        }
    }

    /**
     * Implementation of BaseVideoViewControllerListener
     */

    @Override
    public void onSetContentView(final View view) {
        setContentView(view);
    }

    @Override
    public void onSetRequestedOrientation(final int requestedOrientation) {
        setRequestedOrientation(requestedOrientation);
    }

    @Override
    public void onFinish() {
        finish();
    }

    @Override
    public void onStartActivityForResult(final Class<? extends Activity> clazz,
            final int requestCode,
            final Bundle extras) {
        if (clazz == null) {
            return;
        }

        final Intent intent = Intents.getStartActivityIntent(this, clazz, extras);

        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            MoPubLog.log(CUSTOM, "Activity " + clazz.getName() + " not found. Did you declare it in your AndroidManifest.xml?");
        }
    }

    protected static long getBroadcastIdentifierFromIntent(Intent intent) {
        return intent.getLongExtra(BROADCAST_IDENTIFIER_KEY, -1);
    }

    @Deprecated
    @VisibleForTesting
    BaseVideoViewController getBaseVideoViewController() {
        return mBaseVideoController;
    }

    @Deprecated
    @VisibleForTesting
    void setBaseVideoViewController(final BaseVideoViewController baseVideoViewController) {
        mBaseVideoController = baseVideoViewController;
    }
}
