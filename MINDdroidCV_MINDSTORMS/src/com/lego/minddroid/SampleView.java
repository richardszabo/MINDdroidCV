package com.lego.minddroid;

import android.content.Context;
import android.graphics.Bitmap;

class SampleView extends SampleViewBase {
	
	public SampleView(Context context, MINDdroidCV uiActivity) {
		super(context,uiActivity);
	}
	
	@Override
	protected Bitmap processFrame(byte[] data) {
        int frameSize = getFrameWidth() * getFrameHeight();
        int[] rgba = new int[frameSize];

 		FindLight(getFrameWidth(), getFrameHeight(), data, rgba,buffer);

        Bitmap bmp = Bitmap.createBitmap(getFrameWidth(), getFrameHeight(), Bitmap.Config.ARGB_8888);
        bmp.setPixels(rgba, 0/* offset */, getFrameWidth() /* stride */, 0, 0, getFrameWidth(), getFrameHeight());
        return bmp;
	}
	
	public native void FindLight(int width, int height, byte yuv[], int[] rgba,double[] array);

	static {
		System.loadLibrary("mixed_sample");
	}
}
