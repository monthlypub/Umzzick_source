package com.monpub.umzzick.etc;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.monpub.umzzick.UmZZickApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by small-lab on 2015-06-15.
 */
public final class Util {
    private Util() {}

    public static int dp2px(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, UmZZickApplication.getContext().getResources().getDisplayMetrics());
    }

    public static float aspectRatio(Activity activity) {
        View view = activity.getWindow().getDecorView();
        int width = view.getWidth();
        int height = view.getHeight();

        return (float) height / width;
    }


    public static int dp2px(DisplayMetrics displayMetrics, float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics);
    }


    public static float px2dp(float px) {
        float scale = UmZZickApplication.getContext().getResources().getDisplayMetrics().density;
        return px / scale;
    }

    public static int getHeight(Activity context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);

        return displayMetrics.heightPixels;
    }

    public static int getWidth(Activity context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);

        return displayMetrics.widthPixels;
    }

    public static Bitmap getBitmapFromAsset(Context context, String filePath) {
        AssetManager assetManager = context.getAssets();

        InputStream istr;
        Bitmap bitmap = null;
        try {
            istr = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
            // handle exception
            e.printStackTrace();
        }

        return bitmap;
    }

    public static void unzipTo(InputStream inputStream, File directory) {
        ZipInputStream zis = null;
        int bufferLength = 1024;
        try {
            zis= new ZipInputStream(inputStream);
            ZipEntry zipEntry = null;
            while ((zipEntry = zis.getNextEntry()) != null) {
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(new File(directory, zipEntry.getName()));
                    byte[] buffer = new byte[bufferLength];

                    int readCount;
                    while (true) {
                        readCount = zis.read(buffer, 0, bufferLength);

                        if (readCount > 0) {
                            fos.write(buffer, 0, readCount);
                        }
                        if (readCount <= 0) {
                            break;
                        }
                    }

                    zis.closeEntry();
                    fos.close();
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (zis != null) {
                try {
                    zis.close();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static boolean deleteDir(File file){
        if(file.exists()){
            File[] childFileList = file.listFiles();
            for(File childFile : childFileList){
                if(childFile.isDirectory()){
                    deleteDir(childFile);
                }
                else{
                    childFile.delete();
                }
            }
            file.delete();
            return true;
        }else{
            return false;
        }
    }

    public static void fixBackgroundRepeat(View view) {
        Drawable bg = view.getBackground();
        if (bg != null) {
            if (bg instanceof BitmapDrawable) {
                BitmapDrawable bmp = (BitmapDrawable) bg;
                bmp.mutate(); // make sure that we aren't sharing state anymore
                bmp.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
            }
        }
    }

}
