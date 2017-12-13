package com.upkoder.recorder.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.upkoder.recorder.R;

/**
 * Created by leandro on 6/12/2016.
 */
public class OscillogramView extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder mHolder = null;
    private Bitmap mBitmap = null;
    private boolean mWorking = false;

    public OscillogramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attributes =
                context.obtainStyledAttributes(attrs, R.styleable.OscillogramView);
        attributes.recycle();
        mHolder = getHolder();
    }

    private void writeData(final int samplingRate, final short[] data) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mWorking = true;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                mWorking = false;
                OscillogramView.this.invalidate();
            }

            @Override
            protected Void doInBackground(Void... params) {
                // Canvas initialization
                Canvas canvas = mHolder.lockCanvas();
                if (canvas == null)
                    return null;
                canvas.drawColor(Color.WHITE);

                float W = canvas.getWidth(),
                      H = canvas.getHeight();

                Paint paint = new Paint();
                paint.setColor(Color.BLUE);

                float minValue = Short.MIN_VALUE;
                float maxValue = Short.MAX_VALUE;

                for (int i = 1; i < data.length; i++) {
                    float x1 = (i - 1) * W / (float)data.length;
                    float y1 = data[i-1] / (maxValue - minValue);
                    float x2 = i * W / (float)data.length;
                    float y2 = data[i] / (maxValue - minValue);
                    y1 = H / 2f - y1 * H / 2f ;
                    y2 = H / 2f - y2 * H / 2f ;
                    canvas.drawLine(x1, y1, x2, y2, paint);
                }

                mHolder.unlockCanvasAndPost(canvas);
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void write(int samplingRate, short[] data) {
        if (!mWorking) {
            writeData(samplingRate, data);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
