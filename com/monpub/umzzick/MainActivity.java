package com.monpub.umzzick;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.PaintDrawable;
import android.media.MediaMetadataRetriever;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.monpub.umzzick.capture.CaptureService;
import com.monpub.umzzick.etc.Util;
import com.monpub.umzzick.video.VideoActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 1000;
    private MediaProjectionManager mProjectionManager;
    private static final int REQUEST_PERMISSIONS = 10;

    private List<File> videoList = new ArrayList<>();
    private List<File> gifList = new ArrayList<>();


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                    "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            return;
        }
//        MediaProjection mediaProjection = mProjectionManager.getMediaProjection(resultCode, data);

        ((UmZZickApplication) getApplicationContext()).storeMediaProjection(data);

        Intent intent = new Intent(this, CaptureService.class);
        intent.setAction(CaptureService.ACTION_READY);

        startService(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        initRecycler();
    }


    @Override
    protected void onStart() {
        super.onStart();

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CaptureService.ACTION_CAPTURE_STARTED);
        intentFilter.addAction(CaptureService.ACTION_CAPTURE_STOPPED);
        intentFilter.addAction(CaptureService.ACTION_CAPTURE_READY);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);

        checkCapture();

        loadGifList();
        loadVideoList();
    }

    @Override
    protected void onStop() {
        super.onStop();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    private void initRecycler() {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_video);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(videoAdapter);
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);

                int position = parent.getChildAdapterPosition(view);
                if (position < 0) {
                    return;
                }

                int margin = Util.dp2px(10f);
                outRect.top = outRect.right = outRect.bottom = margin;
                if (position == 0) {
                    outRect.left = margin;
                }
            }
        });
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                gifAdapter.notifyDataSetChanged();
            }
        });

        recyclerView = (RecyclerView) findViewById(R.id.recycler_gif);
        final StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);

                int position = parent.getChildAdapterPosition(view);
                if (position < 0) {
                    return;
                }

                StaggeredGridLayoutManager.LayoutParams lp = (StaggeredGridLayoutManager.LayoutParams)view .getLayoutParams();
                int spanIndex = lp.getSpanIndex();

                int margin = Util.dp2px(10f);
                outRect.right = outRect.bottom = margin;
                if (spanIndex == 0) {
                    outRect.left = margin;
                }
                if (position < layoutManager.getSpanCount()) {
                    outRect.top = margin;
                }
            }
        });

        recyclerView.setAdapter(gifAdapter);
    }

    private void loadVideoList() {
        videoList.clear();

        File folder = Constant.getCaptureFolder();
        File[] files = folder.listFiles();

        for (File file : files) {
            if (file.getName().endsWith(".mp4") == true && file.length() > 0) {
                videoList.add(file);
            }
        }

        Collections.sort(videoList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Long.compare(o1.lastModified(), o2.lastModified()) * -1;
            }
        });

        videoAdapter.notifyDataSetChanged();
    }

    private void loadGifList() {
        gifList.clear();

        File folder = Constant.getUmzzickFolder();
        File[] files = folder.listFiles();

        for (File file : files) {
            if (file.getName().endsWith(".gif") == true && file.length() > 0) {
                gifList.add(file);
            }
        }

        Collections.sort(gifList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Long.compare(o1.lastModified(), o2.lastModified()) * -1;
            }
        });

        gifAdapter.notifyDataSetChanged();
        checkEmpty();
    }

    public void onToggleScreenShare() {
        if (CaptureService.ACTION_CAPTURE_STOPPED.equals(lastCaptureState) == false) {
            Intent intent = new Intent(this, CaptureService.class);
            intent.setAction(CaptureService.ACTION_STOP);

            startService(intent);
        } else {
            shareScreen();
        }
    }


    private void shareScreen() {
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
        return;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                if ((grantResults.length > 0) && (grantResults[0] + grantResults[1]) == PackageManager.PERMISSION_DENIED) {
                    finish();
                }
                return;
            }
        }
    }

    private String lastCaptureState = null;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case CaptureService.ACTION_CAPTURE_READY:
                case CaptureService.ACTION_CAPTURE_STARTED :
                case CaptureService.ACTION_CAPTURE_STOPPED :
                    lastCaptureState = action;
                    videoAdapter.notifyItemChanged(0);
            }
        }
    };

    private void checkCapture() {
        Intent intent = new Intent(this, CaptureService.class);
        intent.setAction(CaptureService.ACTION_CAPTURE_CHECK);

        startService(intent);
    }

    private class VideoViewHolder extends RecyclerView.ViewHolder {
        public final ImageView imageView;
        public final TextView text0;
        public final TextView text1;
        public final View playView;
        public final View deleteView;

        public VideoViewHolder(View itemView) {
            super(itemView);

            imageView = (ImageView) itemView.findViewById(R.id.image);
            text0 = (TextView) itemView.findViewById(R.id.text0);
            text1 = (TextView) itemView.findViewById(R.id.text1);
            playView = itemView.findViewById(R.id.play);
            deleteView = itemView.findViewById(R.id.delete);
        }
    }

    private RecyclerView.Adapter<VideoViewHolder> videoAdapter = new RecyclerView.Adapter<VideoViewHolder>() {

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? 1 : 0;
        }

        @Override
        public VideoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == 1) {
                final View itemView = LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_record_item, parent, false);
                VideoViewHolder holder = new VideoViewHolder(itemView);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onToggleScreenShare();
                    }
                });
                return holder;
            }

            final View itemView = LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_video_item, parent, false);
            VideoViewHolder holder = new VideoViewHolder(itemView);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    File file = (File) v.getTag();

                    Intent intent = new Intent(MainActivity.this, VideoActivity.class);
                    intent.putExtra(VideoActivity.EXTRA_INPUT, file.getAbsolutePath());

                    startActivity(intent);
                }
            });
            holder.deleteView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    File file = (File) itemView.getTag();
                    deleteVideo(file, videoList.indexOf(file) + 1);
                }
            });
            holder.playView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    File file = (File) itemView.getTag();
                    Uri uri = FileProvider.getUriForFile(MainActivity.this, "com.monpub.umzzick.fileprovider", file);

                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                }
            });

            return holder;
        }

        @Override
        public void onBindViewHolder(VideoViewHolder holder, int position) {
            if (position == 0) {
                String text0 = null, text1 = null;
                int drawableId = 0;

                if (lastCaptureState == null) {
                    return;
                }

                switch (lastCaptureState) {
                    case CaptureService.ACTION_CAPTURE_STOPPED:
                        drawableId = R.drawable.shape_rec_none;

                        text0 = getString(R.string.rect_off_text0);
                        text1 = getString(R.string.rect_off_text1);
                        break;
                    case CaptureService.ACTION_CAPTURE_READY:
                        drawableId = R.drawable.shape_rec_ready;

                        text0 = getString(R.string.rect_ready_text0);
                        text1 = getString(R.string.rect_ready_text1);
                        break;
                    case CaptureService.ACTION_CAPTURE_STARTED :
                        drawableId = R.drawable.shape_rec_on;

                        text0 = getString(R.string.rect_on_text0);
                        text1 = getString(R.string.rect_on_text1);
                        break;

                }
                holder.imageView.setImageResource(drawableId);
                holder.text0.setText(text0);
                holder.text1.setText(text1);
                return;
            }

            int filePosition = position - 1;

            File file = videoList.get(filePosition);
            holder.itemView.setTag(file);

            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(file.getAbsolutePath());
            long duration = Long.valueOf(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

            Glide.with(MainActivity.this)
                    .load(file)
                    .centerCrop()
                    .into(holder.imageView);

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

            holder.text0.setText(simpleDateFormat.format(new Date(duration)));
            holder.text1.setText(Formatter.formatFileSize(MainActivity.this, file.length()));
        }

        @Override
        public int getItemCount() {
            return videoList.size() + 1;
        }
    };

    private class GifViewHolder extends RecyclerView.ViewHolder {
        public final ImageView imageView;
        public final TextView fileNameView;
        public final TextView fileSizeView;
        public final View deleteView;

        public GifViewHolder(View itemView) {
            super(itemView);

            imageView = (ImageView) itemView.findViewById(R.id.image);
            fileNameView = (TextView) itemView.findViewById(R.id.file_name);
            fileSizeView = (TextView) itemView.findViewById(R.id.text1);
            deleteView = itemView.findViewById(R.id.delete);
        }
    }


    private RecyclerView.Adapter<GifViewHolder> gifAdapter = new RecyclerView.Adapter<GifViewHolder>() {
        @Override
        public GifViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View itemView = LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_gif_item, parent, false);
            GifViewHolder holder = new GifViewHolder(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    File  file = (File) v.getTag();
                    Uri uri = FileProvider.getUriForFile(MainActivity.this, "com.monpub.umzzick.fileprovider", file);

                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    startActivity(intent);
                }
            });
            holder.deleteView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    File file = (File) itemView.getTag();
                    deleteGif(file);
                }
            });


            return holder;
        }

        @Override
        public void onBindViewHolder(final GifViewHolder holder, int position) {
            File file = gifList.get(position);

            holder.itemView.setTag(file);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);

            int targetWidth = Util.getWidth(MainActivity.this) / 2;
            int targetHeight = (int) (options.outHeight * ((float) targetWidth / options.outWidth));

            PaintDrawable paintDrawable = new PaintDrawable(0xFFEEEEEE);
            paintDrawable.setIntrinsicWidth(targetWidth);
            paintDrawable.setIntrinsicHeight(targetHeight);

            Glide.with(MainActivity.this)
                    .load(file)
                    .asBitmap()
                    .placeholder(paintDrawable)
                    .override(targetWidth, targetHeight)
                    .into(holder.imageView);

            holder.fileSizeView.setText(Formatter.formatFileSize(MainActivity.this, file.length()));
            holder.fileNameView.setText(file.getName());

//            holder.imageView.setImageURI(Uri.fromFile(file));
        }

        @Override
        public int getItemCount() {
            return gifList.size();
        }
    };

    private void deleteGif(final File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(R.string.delete_file_msg);
        builder.setPositiveButton(R.string.delete_file_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int position = gifList.indexOf(file);
                if (position >= 0 && gifList.remove(file) == true) {
                    if (file.exists() == true) {
                        file.delete();
                    }

                    gifAdapter.notifyItemRangeRemoved(position, 1);
                }

                checkEmpty();
            }
        });
        builder.setNegativeButton(R.string.delete_file_no, null);
        builder.show();
    }

    private void deleteVideo(final File file, final int positionInAdapter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(R.string.delete_file_msg);
        builder.setPositiveButton(R.string.delete_file_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int position = videoList.indexOf(file);
                if (position >= 0 && videoList.remove(file) == true) {
                    if (file.exists() == true) {
                        file.delete();
                    }

                    videoAdapter.notifyItemRangeRemoved(positionInAdapter, 1);
                }
            }
        });
        builder.setNegativeButton(R.string.delete_file_no, null);
        builder.show();

    }

    private void checkEmpty() {
        findViewById(R.id.empty).setVisibility(gifList == null || gifList.isEmpty() == true ? View.VISIBLE : View.GONE);
    }
}
