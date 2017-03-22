package com.monpub.umzzick.converter;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.monpub.umzzick.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class FFmpegService extends Service {
    public static final String ACTION_CONVERT = "ACTION_CONVERT";

    public static final String ACTION_CONVERT_DONE = "ACTION_CONVERT_DONE";

    protected static final String EXTRA_DATA = "com.monpub.umzzick.converter.extra.data";

    private boolean converting = false;



    @Override
    public void onCreate() {
        super.onCreate();

        FFmpeg ffmpeg = null;
        try {
            ffmpeg = FFmpeg.getInstance(this);
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    super.onFailure();
                }

                @Override
                public void onSuccess() {
                    super.onSuccess();
                }

                @Override
                public void onStart() {
                    super.onStart();
                }

                @Override
                public void onFinish() {
                    super.onFinish();
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void startConvert(Context context, FFmpegData ffmpegData) {
        Intent intent = new Intent(context, FFmpegService.class);
        intent.setAction(ACTION_CONVERT);
        intent.putExtra(EXTRA_DATA, ffmpegData);
        context.startService(intent);
    }

    public class FFmpegBinder extends Binder {
        public boolean isConvertRunning() {
            return converting == true;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new FFmpegBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = intent.getAction();

        FFmpegData data = (FFmpegData) intent.getParcelableExtra(EXTRA_DATA);

        switch (action) {
            case ACTION_CONVERT :
                converting = true;
                new ConvertThread(data).start();
                break;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private class ConvertThread extends Thread {
        private FFmpegData ffmpegData;
        private String palette;
        private boolean isFailed = false;

        private ConvertThread(FFmpegData ffmpegData) {
            this.ffmpegData = ffmpegData;
        }

        @Override
        public void run() {
            converting = true;
            notiStart(ffmpegData);

            try {
                genneratePalette();
                doConvert();
            } catch (Throwable t) {
                t.printStackTrace();
            }

            if (isFailed == false) {
                notiSuccess(ffmpegData);
            } else {
                notiFail(ffmpegData);
            }
            converting = false;
        }

        private void genneratePalette() {
            String scale = "";
            if (ffmpegData.widthOUT > 0 || ffmpegData.heightOUT > 0) {
                scale = String.format(",scale=%d:%d", ffmpegData.widthOUT, ffmpegData.heightOUT);
                if (ffmpegData.widthOUT < ffmpegData.cropWidth) {
                    scale += ":flags=bicubic";
                }
            }

            final String out = ffmpegData.pathOUT + "_palette.png";

            String fps = "";
            if (ffmpegData.fps > 0) {
                fps = "-r " + ffmpegData.fps;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            String cropCmd = String.format("-ss %s -t %.3f -i %s -vf crop=%d:%d:%d:%d%s,palettegen %s %s",
                    sdf.format(new Date(ffmpegData.timeFrom)),
                    (ffmpegData.timeTo - ffmpegData.timeFrom) / 1000f,
                    ffmpegData.pathIN,
                    ffmpegData.cropWidth,
                    ffmpegData.cropHeight,
                    ffmpegData.cropX,
                    ffmpegData.cropY,
                    scale,
                    fps,
                    out);

            String[] cmd = cropCmd.split("\\s+");

            try {
                final FFmpeg ffmpeg = FFmpeg.getInstance(FFmpegService.this);
                ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {
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

                        if (isFailed == false) {
                            palette = out;
                        }

                        synchronized (ffmpegData) {
                            ffmpegData.notify();
                        }
                    }
                });
            } catch (Throwable t) {
                t.printStackTrace();
                return;
            }

            synchronized (ffmpegData) {
                try {
                    ffmpegData.wait();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        private void doConvert() {
            String scale = "";
            if (ffmpegData.widthOUT > 0 || ffmpegData.heightOUT > 0) {
                scale = String.format(",scale=%d:%d", ffmpegData.widthOUT, ffmpegData.heightOUT);
                if (ffmpegData.widthOUT < ffmpegData.cropWidth) {
                    scale += ":flags=bicubic";
                }
            }

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            String convertCmd;

            String fps = "";
            if (ffmpegData.fps > 0) {
                fps = "-r " + ffmpegData.fps;
            }


            if (TextUtils.isEmpty(palette) == false) {
                convertCmd = String.format("-ss %s -t %.3f -i %s -i %s -filter_complex crop=%d:%d:%d:%d%s[x];[x][1:v]paletteuse %s %s",
                        sdf.format(new Date(ffmpegData.timeFrom)),
                        (ffmpegData.timeTo - ffmpegData.timeFrom) / 1000f,
                        ffmpegData.pathIN,
                        palette,
                        ffmpegData.cropWidth,
                        ffmpegData.cropHeight,
                        ffmpegData.cropX,
                        ffmpegData.cropY,
                        scale,
                        fps,
                        ffmpegData.pathOUT);
            } else {
                convertCmd = String.format("-ss %s -t %.3f -i %s -i %s -filter:v crop=%d:%d:%d:%d%s %s %s",
                        sdf.format(new Date(ffmpegData.timeFrom)),
                        (ffmpegData.timeTo - ffmpegData.timeFrom / 1000f),
                        ffmpegData.pathIN,
                        ffmpegData.cropWidth,
                        ffmpegData.cropHeight,
                        ffmpegData.cropX,
                        ffmpegData.cropY,
                        scale,
                        fps,
                        ffmpegData.pathOUT);
            }

            String[] cmd = convertCmd.split("\\s+");

            try {
                FFmpeg ffmpeg = FFmpeg.getInstance(FFmpegService.this);
                ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {
                    private int lastProgress = 0;

                    @Override
                    public void onSuccess(String message) {
                        super.onSuccess(message);
                    }

                    @Override
                    public void onProgress(String message) {
                        super.onProgress(message);

                        int progress = timeTextToMills(message);
                        if (progress < 0) {
                            progress = lastProgress;
                        }

                        notiProgress(ffmpegData, progress);

                        lastProgress = progress;
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

                        synchronized (ffmpegData) {
                            ffmpegData.notify();
                        }
                    }
                });
            } catch (Throwable t) {
                isFailed = true;
                return;
            }

            synchronized (ffmpegData) {
                try {
                    ffmpegData.wait();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    private static final int NOTI_ID = 3000;

    private void notiStart(FFmpegData ffmpegData) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle(getString(R.string.noti_gif_ready_title));
        builder.setContentText(getString(R.string.noti_gif_ready_text));
        builder.setSmallIcon(R.drawable.ic_fiber_manual_record_white_24dp);
        builder.setOngoing(true);
        builder.setPriority(Notification.PRIORITY_HIGH);
        builder.setVibrate(new long[0]);
        builder.setProgress(100, 0, true);

        startForeground(NOTI_ID, builder.build());
    }

    private int timeTextToMills(String text) {
        String pattern =  "frame=.*time=([\\d]+)\\:([\\d]+)\\:([\\d]+)\\.?([\\d]+)?";
        Matcher matcher = Pattern.compile(pattern).matcher(text);

        int progressValue = -1;
        try {
            if (matcher.find() == true) {
                progressValue = Integer.valueOf(matcher.group(1)) * 60 * 60 * 1000;
                progressValue += Integer.valueOf(matcher.group(2)) * 60 * 1000;
                progressValue += Integer.valueOf(matcher.group(3)) * 1000;

                if (matcher.groupCount() > 3) {
                    String millText = matcher.group(4);

                    if (TextUtils.isEmpty(millText) == false) {
                        switch (millText.length()) {
                            case 3 :
                                progressValue += Integer.valueOf(millText);
                                break;
                            case 2 :
                                progressValue += Integer.valueOf(millText) * 10;
                                break;
                            case 1 :
                                progressValue += Integer.valueOf(millText) * 100;
                                break;
                        }

                    }

                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return progressValue;
    }
    private void notiProgress(FFmpegData ffmpegData, int progress) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle(getString(R.string.noti_gif_making_title));
        builder.setContentText(getString(R.string.noti_gif_making_text));
        builder.setSmallIcon(R.drawable.ic_fiber_manual_record_white_24dp);
        builder.setOngoing(true);
        builder.setProgress((int) (ffmpegData.timeTo - ffmpegData.timeFrom), progress, false);

        startForeground(NOTI_ID, builder.build());
    }


    private void notiFail(FFmpegData ffmpegData) {
        stopForeground(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle(getString(R.string.noti_gif_fail_title));
        builder.setContentText(getString(R.string.noti_gif_fail_text));
        builder.setPriority(Notification.PRIORITY_HIGH);
        builder.setVibrate(new long[0]);

        builder.setSmallIcon(R.drawable.ic_fiber_manual_record_white_24dp);

        notificationManager.notify(NOTI_ID, builder.build());
    }

    private void notiSuccess(FFmpegData ffmpegData) {
        MediaScannerConnection.scanFile(getApplicationContext(), new String[]{ffmpegData.pathOUT}, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String path, Uri uri) {
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                PendingIntent pendingIntent = PendingIntent.getActivity(FFmpegService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                Notification.Builder builder = new Notification.Builder(FFmpegService.this);
                builder.setContentTitle(getString(R.string.noti_gif_success_title));
                builder.setContentText(getString(R.string.noti_gif_success_text));
                builder.setSmallIcon(R.drawable.ic_fiber_manual_record_white_24dp);
                builder.setContentIntent(pendingIntent);
                builder.setAutoCancel(true);
                builder.setPriority(Notification.PRIORITY_HIGH);
                builder.setVibrate(new long[0]);

                notificationManager.notify(NOTI_ID + 2, builder.build());
            }
        });

        stopForeground(true);
    }
}
