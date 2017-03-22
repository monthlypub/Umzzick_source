package com.monpub.umzzick.converter;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Layout;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ThumbnailService extends FFmpegService {
    public static final String ACTION_THUMB_LEFT = "ACTION_THUMBDONE_LEFT";
    public static final String ACTION_THUMB_RIGHT = "ACTION_THUMBDONE_RIGHT";
    public static final String ACTION_THUMB_CLEAR = "ACTION_THUMBDONE_CLEAR";

    public static final String ACTION_THUMB_DONE_LEFT = "ACTION_THUMB_DONE_LEFT";
    public static final String ACTION_THUMB_DONE_RIGHT = "ACTION_THUMB_DONE_RIGHT";

    private FFmpegData pendingThumbLeft = null;
    private FFmpegData pendingThumbRight = null;

    private boolean onProgress = false;

    public ThumbnailService() {
    }

    public static void startThumb(Context context, FFmpegData ffmpegData, ThumbType type) {
        Intent intent = new Intent(context, ThumbnailService.class);
        intent.setAction(type == ThumbType.LEFT ? ACTION_THUMB_LEFT : ACTION_THUMB_RIGHT);
        intent.putExtra(EXTRA_DATA, ffmpegData);
        context.startService(intent);
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        FFmpegData ffmpegData = (FFmpegData) intent.getParcelableExtra(EXTRA_DATA);

        switch (action) {
            case ACTION_THUMB_LEFT :
                if (pendingThumbLeft != null || FFmpeg.getInstance(this).isFFmpegCommandRunning() == true || onProgress == true) {
                    pendingThumbLeft = ffmpegData;
                } else {
                    makeThumb(ffmpegData, ThumbType.LEFT);
                }
                break;
            case ACTION_THUMB_RIGHT :
                if (pendingThumbRight != null || FFmpeg.getInstance(this).isFFmpegCommandRunning() == true || onProgress == true) {
                    pendingThumbRight = ffmpegData;
                } else {
                    makeThumb(ffmpegData, ThumbType.RIGHT);
                }
                break;
            case ACTION_THUMB_CLEAR :
                // TODO clear all
                break;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void doNext(ThumbType endedType) {
        FFmpegData ffmpegData = null;
        ThumbType thumbType = null;
        if (endedType == ThumbType.LEFT) {
            if (pendingThumbRight != null) {
                ffmpegData = pendingThumbRight;
                thumbType = ThumbType.RIGHT;

                pendingThumbRight = null;
            } else {
                ffmpegData = pendingThumbLeft;
                thumbType = ThumbType.LEFT;

                pendingThumbLeft = null;
            }
        } else if (endedType == ThumbType.RIGHT) {
            if (pendingThumbLeft != null) {
                ffmpegData = pendingThumbLeft;
                thumbType = ThumbType.LEFT;

                pendingThumbLeft = null;
            } else {
                ffmpegData = pendingThumbRight;
                thumbType = ThumbType.RIGHT;

                pendingThumbRight = null;
            }
        }

        if (ffmpegData != null && thumbType != null) {
            makeThumb(ffmpegData, thumbType);
        }

    }

    public void makeThumb(final FFmpegData ffmpegData, final ThumbType thumbType) {
        onProgress = true;
        final String out = ffmpegData.pathOUT;

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String cropCmd = String.format("-ss %s -i %s -vf crop=%d:%d:%d:%d,scale=%d:-1 -vframes 1 %s -y",
                sdf.format(new Date(ffmpegData.timeFrom)),
                ffmpegData.pathIN,
                ffmpegData.cropWidth,
                ffmpegData.cropHeight,
                ffmpegData.cropX,
                ffmpegData.cropY,
                200,
                out);

        String[] cmd = cropCmd.split(" ");

        try {
            final FFmpeg ffmpeg = FFmpeg.getInstance(ThumbnailService.this);
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {
                boolean isFailed = false;
                @Override
                public void onSuccess(String message) {
                    super.onSuccess(message);
                }

                @Override
                public void onProgress(String message) {
                    super.onProgress(message);
                }

                @Override
                public void onFailure(String message) {
                    super.onFailure(message);
                    isFailed = true;
                }

                @Override
                public void onStart() {
                    super.onStart();
                }

                @Override
                public void onFinish() {
                    super.onFinish();

                    Intent intent = new Intent();
                    switch (thumbType) {
                        case LEFT:
                            intent.setAction(ACTION_THUMB_DONE_LEFT);
                            break;
                        case RIGHT:
                            intent.setAction(ACTION_THUMB_DONE_RIGHT);
                            break;
                    }
                    if (isFailed == false) {
                        intent.setData(Uri.fromFile(new File(ffmpegData.pathOUT)));
                    }
                    LocalBroadcastManager.getInstance(ThumbnailService.this).sendBroadcast(intent);

                    onProgress = false;
                    doNext(thumbType);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            return;
        }
    }

    public enum ThumbType {
        LEFT, RIGHT
    }
}
