package com.upkoder.recorder.views;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.upkoder.recorder.fft.FFT;
import com.upkoder.recorder.fft.Windows;
import com.upkoder.recorder.R;
import com.upkoder.recorder.helpers.ArrayHelper;

/**
 * Created by leandro on 4/9/2016.
 */
public class SpectrogramView extends SurfaceView implements SurfaceHolder.Callback {

    private static final int DEFAULT_WINDOW_SIZE = 256;
    private static final int DEFAULT_OVERLAP = 128;
    private static final String DEFAULT_WINDOW_TYPE = "hanning";
    private static final int DEFAULT_COLUMNS = 512;
    private static final boolean DEFAULT_WRAP = true;

    private Bitmap mBitmap;
    private int mWindowSize;
    private int mOverlap;
    private int mRows;
    private int mCols;
    private String mWindowType;
    private boolean mWorking;
    private short[] mBuffer;
    private int mOffset;
    private double[] x;
    private double[] y;
    private double[] p;
    private FFT mFft;
    private int mAbsoluteCurrentColumn;
    private Paint mSpectrogramPainter;
    private double[] mWindowValues;
    private double mWindowWeight;
    private boolean mWrap;

    private SurfaceHolder mHolder = null;

    private int[] colors;
    private int[] limits;

    public SpectrogramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attributes =
                context.obtainStyledAttributes(attrs, R.styleable.SpectrogramView);
        mOverlap = attributes.getInteger(R.styleable.SpectrogramView_overlap, DEFAULT_OVERLAP);
        mWindowSize = attributes.getInteger(R.styleable.SpectrogramView_window_size, DEFAULT_WINDOW_SIZE);
        mWindowType = attributes.getString(R.styleable.SpectrogramView_window_type);

        if (mWindowType == null)
            mWindowType = DEFAULT_WINDOW_TYPE;
        mWrap = attributes.getBoolean(R.styleable.SpectrogramView_wrap, DEFAULT_WRAP);
        mRows = mWindowSize / 2 + 1;
        mCols = attributes.getInt(R.styleable.SpectrogramView_columns, DEFAULT_COLUMNS);
        mBitmap = Bitmap.createBitmap(mCols, mRows, Bitmap.Config.ARGB_8888);
        mBuffer = null;
        p = new double[mWindowSize];
        x = new double[mWindowSize];
        y = new double[mWindowSize];
        mFft = new FFT(mWindowSize);
        mAbsoluteCurrentColumn = 0;
        mWindowValues = Windows.byName(mWindowType, mWindowSize);
        mWindowWeight = Windows.weight(mWindowValues);

        mSpectrogramPainter = new Paint();
        mSpectrogramPainter.setAntiAlias(true);

        attributes.recycle();

        // Get Holder From Surface View
        mHolder = getHolder();

        // Create Color Bar
        colors = new int[]{
                Color.BLACK, Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, Color.RED, Color.MAGENTA, Color.WHITE
        };
        limits = new int[] {
                -70, -60, -50, -30, -20, -10,  -10, 0
        };
    }

    /**
     * @param samplingRate Number of samples per second.
     * @param data Array representing the signal.
     */
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
                SpectrogramView.this.invalidate();
            }

            @Override
            protected Void doInBackground(Void... params) {
                if (mBuffer == null)
                    mBuffer = data;
                else {
                    mBuffer = ArrayHelper.concat(mBuffer, data);
                }

                while (mWindowSize <= mBuffer.length - mOffset) {
                    for (int i = 0; i < mWindowSize; i++) {
                        x[i] = mBuffer[mOffset + i] * mWindowValues[i];
                        y[i] = 0;
                    }

                    mFft.fft(x, y);

                    double maxValue = 100000000, minDB = -120;
                    for (int i = 0; i < mRows; i++) {
                        p[i] = 2 * (x[i] * x[i] + y[i] * y[i]) / samplingRate / mWindowWeight;
                        if (p[i] > maxValue) maxValue = p[i];
                    }

                    if (maxValue > 0) {
                        for (int i = 0; i < mRows; i++) {
                            if (p[i] > 0) {
                                p[i] = 10 * Math.log10(p[i] / maxValue);
                            } else {
                                p[i] = minDB;
                            }
                            if (p[i] < minDB)
                                p[i] = minDB;
                        }
                    }

                    int relativeCurrentColumn = mAbsoluteCurrentColumn % mCols;

                    // Plot Current Column
                    if (minDB < 0) {
                        for (int i = 0; i < mRows; i++) {
                            int j = 0;
                            while (j < colors.length && p[i] >= limits[j])
                                j++;

                            if (j == 0) {
                                mBitmap.setPixel(relativeCurrentColumn, mRows - i - 1, colors[0]);
                            } else if (j == colors.length) {
                                mBitmap.setPixel(relativeCurrentColumn, mRows - i - 1, colors[colors.length - 1]);
                            } else {
                                mBitmap.setPixel(relativeCurrentColumn, mRows - i - 1,
                                        (int)new ArgbEvaluator().evaluate(
                                                (float) ((p[i] - limits[j - 1]) / (limits[j] - limits[j - 1])),
                                                colors[j - 1], colors[j]
                                        )
                                );
                            }
                        }
                    } else {
                        for (int i = 0; i < mRows; i++)
                            mBitmap.setPixel(relativeCurrentColumn, i, Color.WHITE);
                    }

                    mOffset += (mWindowSize - mOverlap);
                    mAbsoluteCurrentColumn = mAbsoluteCurrentColumn + 1;
                }

                if (mOffset != 0) {
                    mBuffer = ArrayHelper.segment(mBuffer, mOffset, data.length);
                    mOffset = 0;
                }

                // Draw
                if (mHolder != null) {
                    Canvas canvas = mHolder.lockCanvas();

                    if (canvas == null)
                        return null;

                    canvas.drawColor(Color.WHITE);

                    int relativeCurrentColumn = mAbsoluteCurrentColumn % mCols;

                    if (mWrap || mAbsoluteCurrentColumn < mCols) {
                        /**
                         * If visual element is wrapped then, draw bitmap normally.
                         */
                        canvas.drawBitmap(mBitmap, null,
                                new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), mSpectrogramPainter);
                    } else {
                        /**
                         * Divide the bitmap into two pieces:
                         * 1st Part -> From (0, 0) to (relativeCurrentColumn, H)
                         * 2nd Part -> From (relativeCurrentColumn, H) to (W, H)
                         *
                         * First part must to be putted in the last
                         * part of the canvas:
                         * -> From (P, 0) to (canvas.Width(), canvas.Height())
                         *
                         * Second part must to be putted in the first
                         * part of the canvas:
                         * -> From (0, 0) to (P, canvas.Height())
                         *
                         * where:
                         * W = bitmap.Width()
                         * H = bitmap.Height()
                         *
                         * P = canvas.Width() * (1 - relativeCurrentColumn / W)
                         *
                         * relativeCurrentColumn = mAbsoluteCurrentColumn % mCols;
                         */
                        float P = canvas.getWidth() * (1 - relativeCurrentColumn / (float) mBitmap.getWidth());

                        canvas.drawBitmap(mBitmap, new Rect(relativeCurrentColumn, 0, mBitmap.getWidth(), mBitmap.getHeight()),
                                new RectF(0, 0, P, canvas.getHeight()), mSpectrogramPainter
                        );

                        canvas.drawBitmap(mBitmap, new Rect(0, 0, relativeCurrentColumn, mBitmap.getHeight()),
                                new RectF(P, 0, canvas.getWidth(), canvas.getHeight()), mSpectrogramPainter
                        );
                    }
                    mHolder.unlockCanvasAndPost(canvas);
                }

                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Write signal to the visualizer. If the element is not
     * busy, then it will take this chunk of data. Otherwise
     * it will be ignored.
     *
     * @param samplingRate Number of samples per second.
     * @param data Array representing the signal.
     */
    public void write(int samplingRate, short[] data) {
        if (!mWorking) {
            writeData(samplingRate, data);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setWrap(boolean value) {
        mWrap = value;
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
