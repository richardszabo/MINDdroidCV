package com.lego.minddroid;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public abstract class SampleViewBase extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = "Sample::SurfaceView";

    private Camera              mCamera;
    private SurfaceHolder       mHolder;
    private int                 mFrameWidth;
    private int                 mFrameHeight;
    private byte[]              mFrame;
    private boolean             mThreadRun;
    protected MINDdroidCV mActivity;    
    protected double[]buffer;
    protected int left, right;

    public SampleViewBase(Context context, MINDdroidCV uiActivity) {
        super(context);
        mActivity = uiActivity;
        
        mHolder = getHolder();
        mHolder.addCallback(this);
		buffer = new double[10];
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    public int getFrameWidth() {
        return mFrameWidth;
    }

    public int getFrameHeight() {
        return mFrameHeight;
    }

    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
        Log.i(TAG, "surfaceCreated");
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();            
            List<Camera.Size> sizes = params.getSupportedPreviewSizes();
            mFrameWidth = width;
            mFrameHeight = height;

            // selecting optimal camera preview size
            {
                double minDiff = Double.MAX_VALUE;
                for (Camera.Size size : sizes) {
                    if (Math.abs(size.height - height) < minDiff) {
                        mFrameWidth = size.width;
                        mFrameHeight = size.height;
                        minDiff = Math.abs(size.height - height);
                    }
                }
            }

            params.setPreviewSize(getFrameWidth(), getFrameHeight());
            mCamera.setParameters(params);
            //mCamera.setDisplayOrientation(180);
            
            try {
				mCamera.setPreviewDisplay(null);
			} catch (IOException e) {
				Log.e(TAG, "mCamera.setPreviewDisplay fails: " + e);
			}
            mCamera.startPreview();
        }
    }       

    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
        mCamera = Camera.open();
        mCamera.setPreviewCallback(new PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera camera) {
                synchronized (SampleViewBase.this) {
                    mFrame = data;
                    SampleViewBase.this.notify();
                }
            }
        });
        (new Thread(this)).start();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        mThreadRun = false;
        if (mCamera != null) {
            synchronized (this) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;
            }
        }
    }

    protected abstract Bitmap processFrame(byte[] data);

    public void run() {
        mThreadRun = true;
        Log.i(TAG, "Starting processing thread");
        while (mThreadRun) {
            Bitmap bmp = null;

            synchronized (this) {
                try {
                    this.wait();
                    bmp = processFrame(mFrame);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (bmp != null) {
                Canvas canvas = mHolder.lockCanvas();
                calculateMove();
                if (canvas != null) {
                    canvas.drawBitmap(bmp,0.0f,0.0f,null); //(canvas.getWidth() - getFrameWidth()) / 2, (canvas.getHeight() - getFrameHeight()) / 2, null);
                	
                    //drawText(canvas,buffer);
                    mHolder.unlockCanvasAndPost(canvas);
                }
                bmp.recycle();
            }
        }
    }
    
    void drawText(Canvas canvas,double[] buffer) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        DecimalFormat twoPlaces = new DecimalFormat("0.00");
        String todraw = ":" + twoPlaces.format(buffer[0]) + ":" + 
        							twoPlaces.format(buffer[1]) + ":" + 
        							twoPlaces.format(buffer[2]) + ":" +
        							left + ":" + right + ":";
        Paint bpaint = new Paint();
        bpaint.setStyle(Paint.Style.FILL);
        bpaint.setColor(Color.BLUE);
        Rect rect = new Rect(0,0,250,50);
        canvas.drawRect(rect , bpaint);
        canvas.drawText(todraw, 0, todraw.length(), 10.0f, 10.0f, paint);    	
    }
    
    void calculateMove() {
    	// buffer[1] holds the light direction info if the phone is in landscape format
    	// small values -> turn left
    	// large values -> turn right
    	// in portrait mode buffer[2] should be used 
    	// large values -> turn right 
    	// small values -> turn left
    	if( buffer[0] > 100 ) { // light is visible
    		int forwardSpeed = 50;
    		double upScale = 40; 
    		//double direction = (buffer[1] - getFrameWidth()/2)/getFrameWidth();
    		double direction = -1.0 * (buffer[2] - getFrameHeight()/2)/getFrameHeight();
    		left = (int)(upScale * direction) + forwardSpeed;
    		right = (int)(-1.0 * upScale * direction) + forwardSpeed;
    	} else {
    		left = 0;
    		right = 0;
    	}
    	left = Math.min(Math.max(left,0),100);
    	right = Math.min(Math.max(right,0),100);
    	
    	mActivity.updateMotorControl(left,right);
    }

}