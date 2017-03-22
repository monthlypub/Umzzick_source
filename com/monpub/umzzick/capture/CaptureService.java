package com.monpub.umzzick.capture;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.CamcorderProfile;
import android.media.CameraProfile;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.ToggleButton;

import com.monpub.umzzick.Constant;
import com.monpub.umzzick.MainActivity;
import com.monpub.umzzick.R;
import com.monpub.umzzick.UmZZickApplication;

import java.io.IOException;

public class CaptureService extends Service {
    public static final String ACTION_READY = "ACTION_READY";
    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";

    public static final String ACTION_CAPTURE_CHECK = "ACTION_CAPTURE_CHECK";
    public static final String ACTION_CAPTURE_READY = "ACTION_CAPTURE_READY";
    public static final String ACTION_CAPTURE_STARTED = "ACTION_CAPTURE_STARTED";
    public static final String ACTION_CAPTURE_STOPPED = "ACTION_CAPTURE_STOPPED";

    private int mScreenDensity;
    private static final int DISPLAY_SIZE = 1280;

    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;

    private MediaRecorder mediaRecorder;
    private MediaProjectionManager projectionManager;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String outputPath;

    public CaptureService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        try {
            switch (action) {
                case ACTION_READY:
                    if (mMediaProjection == null) {
                        mMediaProjection = ((UmZZickApplication) getApplicationContext()).getMediaProjection();
                    }
                    mMediaProjection.registerCallback(mMediaProjectionCallback, null);
                    readyCaptrue();
                    break;
                case ACTION_START:
                    mediaRecorder = new MediaRecorder();
                    projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

                    startRecord();
                    break;
                case ACTION_STOP:
                    stopRecord();
                    break;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        if (mVirtualDisplay != null) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_CAPTURE_STARTED));
        } else  if (mMediaProjection != null) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_CAPTURE_READY));
        } else {
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_CAPTURE_STOPPED));
        }

        return START_NOT_STICKY;
    }

    private void readyCaptrue() {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_fiber_manual_record_white_24dp);
        builder.setContentTitle(getString(R.string.noti_rec_ready_title));
        builder.setContentText(getString(R.string.noti_rec_ready_text));
        builder.setPriority(Notification.PRIORITY_MAX);
        builder.setVibrate(new long[]{0});

        Intent intent = new Intent(this, this.getClass());
        intent.setAction(ACTION_START);

        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(pendingIntent);

        startForeground(3000, builder.build());
    }

    private void startRecord() {
        int width = 0;
        int height = 0;

        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();

        Point size = new Point();

        windowManager.getDefaultDisplay().getRealSize(size);

        width = size.x;
        height = size.y;

        if (width > height) {
            if (width > DISPLAY_SIZE) {
                height = (int) (DISPLAY_SIZE * ((float) height / width));
                width = DISPLAY_SIZE;
            }
        } else {
            if (height > DISPLAY_SIZE) {
                width = (int) (DISPLAY_SIZE * ((float) width / height));
                height = DISPLAY_SIZE;
            }
        }

        initRecorder(width, height, rotation);

        mVirtualDisplay = createVirtualDisplay(width, height);

        mediaRecorder.start();

        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_fiber_manual_record_white_24dp);
        builder.setContentTitle(getString(R.string.noti_rec_title));
        builder.setContentText(getString(R.string.noti_rec_text));
        builder.setPriority(Notification.PRIORITY_MAX);

        Intent intent = new Intent(this, this.getClass());
        intent.setAction(ACTION_STOP);

        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(3000, builder.build());
    }

    private void stopRecord() {
        try {
            try {
                mediaRecorder.stop();
            } catch (Throwable t) {
                t.printStackTrace();
            }
            try {
                mediaRecorder.reset();
            } catch (Throwable t) {
                t.printStackTrace();
            }
            mediaRecorder = null;

            MediaScannerConnection.scanFile(getApplicationContext(), new String[]{outputPath}, null, new MediaScannerConnection.OnScanCompletedListener() {
                @Override
                public void onScanCompleted(String path, Uri uri) {
                    LocalBroadcastManager.getInstance(CaptureService.this).sendBroadcast(new Intent(ACTION_CAPTURE_STOPPED));

                    Notification.Builder builder = new Notification.Builder(CaptureService.this);
                    builder.setSmallIcon(R.drawable.ic_fiber_manual_record_white_24dp);
                    builder.setContentTitle(getString(R.string.noti_rec_finish_title));
                    builder.setContentText(getString(R.string.noti_rec_finish_text));
                    builder.setPriority(Notification.PRIORITY_MAX);
                    builder.setVibrate(new long[]{0});

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(uri);
                    PendingIntent pendingIntent = PendingIntent.getActivity(CaptureService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                    builder.setContentIntent(pendingIntent);
                    builder.setAutoCancel(true);

                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.notify(3001, builder.build());
                }
            });

        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }

        stopScreenSharing();

        stopForeground(true);

    }

    private void initRecorder(int width, int height, int rotation) {
        try {
            String outputPath = Constant.getCaptureFolder() + "/capture_" + System.currentTimeMillis() + ".mp4";

            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setOutputFile(outputPath);
            mediaRecorder.setVideoSize(width, height);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
//            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setVideoEncodingBitRate((int) (CamcorderProfile.get(CamcorderProfile.QUALITY_720P).videoBitRate * 0.8));
            mediaRecorder.setVideoFrameRate(60);

            int orientation = ORIENTATIONS.get(rotation + 90);
            mediaRecorder.setOrientationHint(orientation);
            mediaRecorder.prepare();

            this.outputPath = outputPath;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private VirtualDisplay createVirtualDisplay(int width, int height) {
        return mMediaProjection.createVirtualDisplay("MainActivity",
                width,
                height,
                mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(),
                null /*Callbacks*/,
                null /*Handler*/);
    }

    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;

        destroyMediaProjection();
    }

    private void destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    private MediaProjection.Callback mMediaProjectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            stopRecord();
        }
    };
}
