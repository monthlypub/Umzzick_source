package com.monpub.umzzick;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

/**
 * Created by small-lab on 2017-03-05.
 */

public class UmZZickApplication extends Application {
    private static Context sInstance;
    private MediaProjection mediaProjection;
    private Intent mediaProjectionIntent;

    public static Context getContext() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());

        sInstance = this;
    }

    public void storeMediaProjection(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
    }

    public void storeMediaProjection(Intent intent) {
        this.mediaProjectionIntent = intent;
    }


    public MediaProjection getMediaProjection() {
        MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        return mpm.getMediaProjection(Activity.RESULT_OK, mediaProjectionIntent);
    }
}
