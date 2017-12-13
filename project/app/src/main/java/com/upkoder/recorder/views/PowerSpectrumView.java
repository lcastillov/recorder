package com.upkoder.recorder.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.upkoder.recorder.fft.FFT;
import com.upkoder.recorder.R;

/**
 * Created by leandro on 6/12/2016.
 */
public class PowerSpectrumView extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder mHolder = null;
    private boolean mWorking;
    private FFT mfft;
    private double[] x;
    private double[] y;
    private double[] p;
    private int mWindowSize;
    private int mWindowOverlap;
    private boolean log;

    public PowerSpectrumView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attributes =
                context.obtainStyledAttributes(attrs, R.styleable.PowerSpectrumView);
        mHolder = getHolder();
        mWindowSize = attributes.getInteger(R.styleable.PowerSpectrumView_ws, 256);
        mWindowOverlap = attributes.getInteger(R.styleable.PowerSpectrumView_wo, mWindowSize * 3 / 4);
        mfft = new FFT(mWindowSize);
        x = new double[mWindowSize];
        y = new double[mWindowSize];
        p = new double[mWindowSize];
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
                PowerSpectrumView.this.invalidate();
            }

            @Override
            protected Void doInBackground(Void... params) {
                // Canvas initialization
                Canvas canvas = mHolder.lockCanvas();
                if (canvas == null)
                    return null;
                canvas.drawColor(Color.WHITE);

                // Set 0 to every value in the power spectrum
                for (int i = 0; i < mWindowSize; i++)
                    p[i] = 0;

                // Calculate power spectrum
                int number_of_windows = 0;
                for (int offset = 0; offset < data.length; offset += (mWindowSize - mWindowOverlap)) {
                    for (int i = 0; i < mWindowSize; i++) {
                        x[i] = y[i] = 0;
                        if (offset + i < data.length)
                            x[i] = data[i];
                    }
                    mfft.fft(x, y);
                    for (int i = 0; i < mWindowSize; i++)
                        p[i] += 2 * (x[i] * x[i] + y[i] * y[i]) / samplingRate;
                    number_of_windows++;
                }

                // Average & normalization
                double maxValue = Double.MIN_VALUE;

                for (int i = 0; i < mWindowSize; i++) {
                    p[i] /= number_of_windows;
                    if (p[i] > maxValue) maxValue = p[i];
                }

                if (maxValue > 0) {
                    for (int i = 0; i < mWindowSize; i++)
                        p[i] /= maxValue;
                }

                if (log) {
                    // Log scale
                    double minValue = Double.MAX_VALUE;
                    for (int i = 0; i < mWindowSize; i++) {
                        if (p[i] < 1e-8)
                            p[i] = Double.MIN_VALUE;
                        else {
                            p[i] = 20 * Math.log10(p[i]);
                            minValue = Math.min(minValue, p[i]);
                        }
                    }

                    for (int i = 0; i < mWindowSize; i++)
                        if (p[i] < minValue) p[i] = minValue;

                    if (minValue < -1e-8) {
                        for (int i = 0; i < mWindowSize; i++)
                            p[i] = 1 - p[i] / minValue;
                    } else {
                        for (int i = 0; i < mWindowSize; i++)
                            p[i] = 1;
                    }
                }

                // Draw
                float W = canvas.getWidth(),
                      H = canvas.getHeight();

                Paint painter = new Paint();
                painter.setColor(Color.BLACK);
                painter.setAntiAlias(true);
                painter.setStyle(Paint.Style.FILL_AND_STROKE);

                int n = mWindowSize / 2 + 1;
                for (int i = 0; i < n; i++) {
                    canvas.drawRect(i * W / n, (float)(H * (1 - p[i])), (i + 1) * W / n, H, painter);
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

    public boolean getLogarithmicScale() {
        return log;
    }

    public void setLogarithmicScale(boolean value) {
        log = value;
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
