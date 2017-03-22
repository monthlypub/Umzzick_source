package com.monpub.umzzick;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Created by small-lab on 2017-03-07.
 */

public final class Constant {
    public final static String FOLDER_UMZZICK = "Umzzick";
    public final static String FOLDER_CAPTURE = "capture";
    public final static String FOLDER_INTERMEDIATE = "intermediate";


    public static File getUmzzickFolder() {
        makeFolders();
        return Environment.getExternalStoragePublicDirectory(FOLDER_UMZZICK);
    }

    public static File getCaptureFolder() {
        makeFolders();
        return Environment.getExternalStoragePublicDirectory(FOLDER_UMZZICK + File.separator + FOLDER_CAPTURE);
    }

    public static File getIntermediateFolder() {
        makeFolders();
        return Environment.getExternalStoragePublicDirectory(FOLDER_UMZZICK + File.separator + FOLDER_INTERMEDIATE);
    }

    private static void makeFolders() {
        File root = Environment.getExternalStoragePublicDirectory(FOLDER_UMZZICK);
        if (root.exists() == false) {
            root.mkdir();
        }

        File capture = new File(root, FOLDER_CAPTURE);
        if (capture.exists() == false) {
            capture.mkdir();
        }

        File intermediate = new File(root, FOLDER_INTERMEDIATE);
        if (intermediate.exists() == false) {
            intermediate.mkdir();
        }
    }
}
