package com.monpub.umzzick.converter;

import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;

/**
 * Created by small-lab on 2017-03-05.
 */

public class FFmpegData implements Parcelable {
    private static final int MAX_SIZE = 800;
    public String pathIN;
    public String pathOUT;

    public long timeFrom;
    public long timeTo;

    public int cropX;
    public int cropY;
    public int cropWidth;
    public int cropHeight;

    public int widthOUT = 300;
    public int heightOUT = 300;

    public int fps = -1;

    public FFmpegData(String pathIN) {
        this.pathIN = pathIN;
    }

    public void setCrop(int videoWidth, int videoHeight, RectF cropRect) {
        cropX = (int) (videoWidth * cropRect.left);
        cropY = (int) (videoHeight * cropRect.top);
        cropWidth = (int) (videoWidth * cropRect.width());
        cropHeight = (int) (videoHeight * cropRect.height());

        if (cropWidth < MAX_SIZE && cropHeight < MAX_SIZE) {
            widthOUT = cropWidth;
            heightOUT = cropHeight;
        } else {
            if (cropWidth > cropHeight) {
                widthOUT = MAX_SIZE;
                heightOUT = -1;
            } else {
                widthOUT = -1;
                heightOUT = MAX_SIZE;
            }
        }
    }

    public void setSizeOUT(int width, int height) {
        widthOUT = width;
        heightOUT = height;
    }

    public void setPahtOUT(String pathOUT) {
        this.pathOUT = pathOUT;
    }

    public void setTime(long timeFrom, long timeTo) {
        this.timeFrom = timeFrom;
        this.timeTo = timeTo;
    }

    public void setFPS(int fps) {
        this.fps = fps;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(pathIN);
        out.writeString(pathOUT);

        out.writeLong(timeFrom);
        out.writeLong(timeTo);

        out.writeInt(cropX);
        out.writeInt(cropY);
        out.writeInt(cropWidth);
        out.writeInt(cropHeight);

        out.writeInt(widthOUT);
        out.writeInt(heightOUT);

        out.writeInt(fps);
    }

    public static final Parcelable.Creator<FFmpegData> CREATOR
            = new Parcelable.Creator<FFmpegData>() {
        public FFmpegData createFromParcel(Parcel in) {
            return new FFmpegData(in);
        }

        public FFmpegData[] newArray(int size) {
            return new FFmpegData[size];
        }
    };

    private FFmpegData(Parcel in) {
        pathIN = in.readString();
        pathOUT =  in.readString();

        timeFrom = in.readLong();
        timeTo = in.readLong();

        cropX = in.readInt();
        cropY = in.readInt();
        cropWidth = in.readInt();
        cropHeight = in.readInt();

        widthOUT = in.readInt();
        heightOUT = in.readInt();

        fps = in.readInt();
    }

}
