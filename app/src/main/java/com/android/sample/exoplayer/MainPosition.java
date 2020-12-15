package com.android.sample.exoplayer;

import android.os.Parcel;
import android.os.Parcelable;

class MainPosition implements Parcelable {

    private final int mCurrentWindowIndex;
    private final long mCurrentPosition;

    public MainPosition(int currentWindowIndex, long currentPosition) {
        mCurrentWindowIndex = currentWindowIndex;
        mCurrentPosition = currentPosition;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mCurrentWindowIndex);
        out.writeLong(mCurrentPosition);
    }

    public static final Parcelable.Creator<MainPosition> CREATOR
            = new Parcelable.Creator<MainPosition>() {
        public MainPosition createFromParcel(Parcel in) {
            return new MainPosition(in);
        }

        public MainPosition[] newArray(int size) {
            return new MainPosition[size];
        }
    };

    private MainPosition(Parcel in) {
        mCurrentWindowIndex = in.readInt();
        mCurrentPosition = in.readLong();
    }

    public int getCurrentWindowIndex() {
        return mCurrentWindowIndex;
    }

    public long getCurrentPosition() {
        return mCurrentPosition;
    }
}
