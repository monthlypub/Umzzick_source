package com.monpub.umzzick.video;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.monpub.umzzick.Constant;
import com.monpub.umzzick.R;
import com.monpub.umzzick.converter.FFmpegData;
import com.monpub.umzzick.converter.FFmpegService;
import com.monpub.umzzick.converter.ThumbnailService;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class VideoActivity extends AppCompatActivity {
    public static final String EXTRA_INPUT = "extra_input";

    private int videoWidth, videoHeight;
    private ImageView thumbStart, thumbEnd;

    private MediaPlayer mediaPlayer;
    private TimeRangeSeekBar rangeSeekBar;

    private VideoView videoView;
    private MediaController mediaController;

    private String pathIn;
    private SelectionView selectionView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_MODE_OVERLAY);

        if (savedInstanceState == null) {
            clearThumb();
        }

        setContentView(R.layout.activity_video);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pathIn = getIntent().getStringExtra(EXTRA_INPUT);

        videoView = (VideoView) findViewById(R.id.video);
        rangeSeekBar = (TimeRangeSeekBar) findViewById(R.id.seekbar_range);


        thumbStart = (ImageView) findViewById(R.id.thumb_start);
        thumbEnd = (ImageView) findViewById(R.id.thumb_end);

        final View frame = findViewById(R.id.frame_controller);

        videoView.setVideoPath(pathIn);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                rangeSeekBar.init(
                        mediaPlayer.getDuration() / 2 - 2500,
                        mediaPlayer.getDuration() / 2 + 2500,
                        mediaPlayer.getDuration()
                );

                videoWidth = mediaPlayer.getVideoWidth();
                videoHeight = mediaPlayer.getVideoHeight();

                VideoActivity.this.mediaPlayer = mediaPlayer;

                mediaController = new MediaController(VideoActivity.this);
                mediaController.setBackgroundResource(R.drawable.shape_video_controller_bg);
                mediaController.setAlpha(0.7f);
                videoView.setMediaController(mediaController);
                mediaController.setAnchorView(findViewById(R.id.frame_controller));

                selectionView.videoReady();
            }
        });
        videoView.start();

        selectionView = (SelectionView) findViewById(R.id.selection);
        selectionView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (selectionView.getBoxType() == SelectionView.BoxType.FREE) {
                    selectionView.setBoxType(SelectionView.BoxType.SQUARE);
                } else {
                    selectionView.setBoxType(SelectionView.BoxType.FREE);
                }

                return true;
            }
        });
        selectionView.setOnSelectionChangedListener(new SelectionView.OnSelectionChangedListener() {
            @Override
            public void onSelectionChanged() {
                clearThumb();
                rangeSeekBar.notifyRangeChanged();
            }
        });
        selectionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaController == null) {
                    return;
                }

                if (mediaController.isShowing() == true) {
                    mediaController.hide();
                } else {
                    mediaController.show(3000);
                }
            }
        });

        final TimeRangeSeekBar.OnRangeChangedListner onRangeChangedListner = new TimeRangeSeekBar.OnRangeChangedListner() {

            @Override
            public void onRangeChanged(TimeRangeSeekBar.RangeType type) {
                if (mediaPlayer == null) {
                    return;
                }

                SelectionView selectionView = (SelectionView) findViewById(R.id.selection);
                RectF rectF = selectionView.getSelection();

                long startMills = rangeSeekBar.getProgressFrom();
                long endMills = rangeSeekBar.getProgressTo();

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss.SS");
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

                ((TextView) findViewById(R.id.text_from)).setText(simpleDateFormat.format(new Date(startMills)));
                ((TextView) findViewById(R.id.text_to)).setText(simpleDateFormat.format(new Date(endMills)));

                File startPathOUT = null;
                File endPathOUT = null;
                boolean needStartThumb = false, needEndThumb = false;
                String scaleString = rectF.toString().replaceAll("[^\\d]", "");
                switch (type) {
                    case FROM:
                        startPathOUT = new File(getThumbDir(), "start_" + scaleString + "_" + startMills + ".png");
                        needStartThumb = true;
                        break;
                    case TO:
                        endPathOUT = new File(getThumbDir(), "end_" + scaleString + "_" + endMills + ".png");
                        needEndThumb = true;
                        break;
                    case BOTH:
                        startPathOUT = new File(getThumbDir(), "start_" + scaleString + "_" + startMills + ".png");
                        endPathOUT = new File(getThumbDir(), "end_" + scaleString + "_" + endMills + ".png");
                        needStartThumb = needEndThumb = true;
                        break;
                }


                if (startPathOUT != null && startPathOUT.exists() == true) {
                    thumbStart.setImageURI(Uri.fromFile(startPathOUT));
                    needStartThumb = false;
                }

                if (endPathOUT != null && endPathOUT.exists() == true) {
                    thumbEnd.setImageURI(Uri.fromFile(endPathOUT));
                    needEndThumb = false;
                }

                loopHandler.removeMessages(0);
                loopHandler.sendEmptyMessageDelayed(0, 100);

                if (needStartThumb == false && needEndThumb == false) {
                    return;
                }

                if (needStartThumb == true) {
                    FFmpegData startFFmpegData = new FFmpegData(pathIn);
                    startFFmpegData.setCrop(
                            mediaPlayer.getVideoWidth(),
                            mediaPlayer.getVideoHeight(),
                            rectF);
                    startFFmpegData.setTime(startMills, 0);
                    startFFmpegData.setPahtOUT(startPathOUT.getAbsolutePath());

                    ThumbnailService.startThumb(VideoActivity.this, startFFmpegData, ThumbnailService.ThumbType.LEFT);
                }

                if (needEndThumb == true) {
                    FFmpegData endFFmpegData = new FFmpegData(pathIn);
                    endFFmpegData.setCrop(
                            mediaPlayer.getVideoWidth(),
                            mediaPlayer.getVideoHeight(),
                            rectF);
                    endFFmpegData.setTime(endMills, 0);
                    endFFmpegData.setPahtOUT(endPathOUT.getAbsolutePath());

                    ThumbnailService.startThumb(VideoActivity.this, endFFmpegData, ThumbnailService.ThumbType.RIGHT);
                }
            }
        };
        rangeSeekBar.setOnRangeChangedListner(onRangeChangedListner);

        View.OnClickListener naviOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.prev:
                        rangeSeekBar.stepPrev(50);
                        break;
                    case R.id.next:
                        rangeSeekBar.stepNext(50);
                        break;
                }
            }
        };

        findViewById(R.id.prev).setOnClickListener(naviOnClickListener);
        findViewById(R.id.next).setOnClickListener(naviOnClickListener);
//        findViewById(R.id.prev_thumb_end).setOnClickListener(naviOnClickListener);
//        findViewById(R.id.next_thumb_end).setOnClickListener(naviOnClickListener);
    }

    @Override
    protected void onStart() {
        super.onStart();

        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ThumbnailService.ACTION_THUMB_DONE_LEFT);
            intentFilter.addAction(ThumbnailService.ACTION_THUMB_DONE_RIGHT);
            intentFilter.addDataScheme("file");
            LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_video_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save :
                startSupportActionMode(confirmCallback);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private File getThumbDir() {
        File thumbDir = new File(getCacheDir(), "thumb");
        if (thumbDir.exists() == false) {
            thumbDir.mkdirs();
        }

        return thumbDir;
    }

    private void clearThumb() {
        File thumbDir = getThumbDir();

        File[] files = thumbDir.listFiles();
        for (File file : files) {
            file.delete();
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Uri data = intent.getData();

            switch (action) {
                case ThumbnailService.ACTION_THUMB_DONE_LEFT :
                    thumbStart.setImageURI(data);
                    break;
                case ThumbnailService.ACTION_THUMB_DONE_RIGHT :
                    thumbEnd.setImageURI(data);
                    break;
            }
        }
    };

    public Handler loopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                if (mediaPlayer.isPlaying() == true) {
                    long start = rangeSeekBar.getProgressFrom();
                    long end = rangeSeekBar.getProgressTo();

                    long curr = mediaPlayer.getCurrentPosition();

                    if (curr + 100 > end) {
                        mediaPlayer.seekTo((int) start);
                    }

                    sendEmptyMessageDelayed(0, 100);
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    };

    private SeekBar scaleSeekBar;
    private TextView scaledTextView;
    private Spinner fpsSpinner ;
    private View confirmPanel;



    private ActionMode.Callback confirmCallback = new ActionMode.Callback() {
        private void initConfirm(final ActionMode mode) {
            scaleSeekBar = (SeekBar) findViewById(R.id.seek_size);
            fpsSpinner = (Spinner) findViewById(R.id.spinner_fps);
            scaledTextView = (TextView) findViewById(R.id.text_size);
            confirmPanel = findViewById(R.id.confirm_panel);

            confirmPanel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mode.finish();
                }
            });

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(VideoActivity.this, android.R.layout.simple_spinner_item, new String[]{"10", "15", "20", "25", "원본"});
            fpsSpinner.setAdapter(adapter);
            fpsSpinner.setSelection(2);

            scaleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    String type = (String) seekBar.getTag();
                    RectF selection = selectionView.getSelection();
                    Rect outRect = new Rect();

                    calcurateScaledRect(type, progress, selection, outRect);
                    scaledTextView.setText(String.format(" - %d x %d", outRect.width(), outRect.height()));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }

        private void calcurateScaledRect(String type, int progress, RectF selection, Rect outRect) {
            int width = 0, height = 0;

            float cropWidth = videoWidth * selection.width();
            float cropHeight = videoHeight * selection.height();

            switch (type) {
                case "LONG" :
                    width = progress + 100;
                    height = (int) (width * (cropHeight / cropWidth));
                    break;
                case "WIDE" :
                    height = progress + 100;
                    width = (int) (height * (cropWidth / cropHeight));;
                    break;
                case "SQUARE" :
                    width = progress + 100;
                    height = progress + 100;
                    break;
            }

            outRect.left = outRect.top = 0;
            outRect.right = width;
            outRect.bottom = height;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            new MenuInflater(VideoActivity.this).inflate(R.menu.menu_video_confirm, menu);
            mode.setTitle(getString(R.string.action_mode_save));

            initConfirm(mode);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            confirmPanel.setVisibility(View.VISIBLE);

            RectF rectF = selectionView.getSelection();

            int cropWidth = (int) (videoWidth * rectF.width());
            int cropHeight = (int) (videoHeight * rectF.height());

            int max = -100;
            if (cropWidth > cropHeight) {
                scaleSeekBar.setTag("WIDE");
                max += cropHeight;
            } else if (cropWidth < cropHeight) {
                scaleSeekBar.setTag("LONG");
                max += cropWidth;
            } else {
                scaleSeekBar.setTag("SQUARE");
                max += cropHeight;
            }

            scaleSeekBar.setMax(max);
            scaleSeekBar.setProgress(max);

            try {
                mediaController.hide();
                videoView.pause();
            } catch (Throwable t) {
                t.printStackTrace();
            }
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_save_confirm) {
                String type = (String) scaleSeekBar.getTag();
                RectF selection = selectionView.getSelection();
                Rect outRect = new Rect();

                calcurateScaledRect(type, scaleSeekBar.getProgress(), selection, outRect);

                Integer fps = null;
                String fpsString = (String) fpsSpinner.getSelectedItem();
                if (TextUtils.isDigitsOnly(fpsString) == true) {
                    fps = Integer.valueOf(fpsString);
                }
                convert(outRect, fps);
                finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // TODO hide control
            findViewById(R.id.confirm_panel).setVisibility(View.GONE);
        }
    };

    private void convert(Rect scaledRect, Integer fps) {
        FFmpegData ffmpegData = new FFmpegData(
                pathIn
        );

        RectF rectF = selectionView.getSelection();

        File in = new File(pathIn);
        File out = new File(Constant.getUmzzickFolder(), in.getName() + "_" + System.currentTimeMillis() + ".gif");

        ffmpegData.setCrop(videoWidth, videoHeight, rectF);
        ffmpegData.setPahtOUT(out.getAbsolutePath());
        ffmpegData.setTime(rangeSeekBar.getProgressFrom(), rangeSeekBar.getProgressTo());

        if (fps != null) {
            ffmpegData.setFPS(fps);
        }

        if (scaledRect != null) {
            ffmpegData.setSizeOUT(scaledRect.width(), scaledRect.height());
        }

        FFmpegService.startConvert(VideoActivity.this, ffmpegData);
    }
}
