package com.upkoder.recorder;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.upkoder.recorder.views.OscillogramView;
import com.upkoder.recorder.views.PowerSpectrumView;
import com.upkoder.recorder.views.SpectrogramView;
import com.upkoder.recorder.services.SingleShotIntentService;

import static android.support.v4.content.WakefulBroadcastReceiver.startWakefulService;


public class MainActivity extends Activity {

    private static String TAG = "MainActivity";

    private Button btnRecord;
    private OscillogramView oscillogram;
    private SpectrogramView spectrogram;
    private PowerSpectrumView powerSpectrum;
    private SingleShotIntentService.ProgressBroadcast progressBroadcast;
    private SingleShotIntentService.CompletedBroadcast completedBroadcast;
    private TextView durationTextView;
    private TextView logTextView;

    public void updateButtonName() {
        if (SingleShotIntentService.getInstance() != null)
            btnRecord.setText(R.string.stop_record);
        else
            btnRecord.setText(R.string.start_record);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (progressBroadcast == null)
            progressBroadcast = new SingleShotIntentService.ProgressBroadcast(new SingleShotIntentService.ProgressBroadcast.ReceiveCallback() {
                @Override
                public void receive(int bytes, int samplingRate, short[] data) {
                    spectrogram.write(samplingRate, data);
                    oscillogram.write(samplingRate, data);
                    powerSpectrum.write(samplingRate, data);
                    durationTextView.setText(String.format("%.2f seconds", bytes / (double) samplingRate));
                }
            });

        if (completedBroadcast == null) {
            completedBroadcast = new SingleShotIntentService.CompletedBroadcast(new SingleShotIntentService.CompletedBroadcast.CompletedCallback() {
                @Override
                public void completed() {
                    updateButtonName();
                }
            });
        }

        IntentFilter progressFilter = new IntentFilter();
        progressFilter.addAction(SingleShotIntentService.ProgressBroadcast.ACTION);
        registerReceiver(progressBroadcast, progressFilter);

        IntentFilter completedFilter = new IntentFilter();
        completedFilter.addAction(SingleShotIntentService.CompletedBroadcast.ACTION);
        registerReceiver(completedBroadcast, completedFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (progressBroadcast != null)
            unregisterReceiver(progressBroadcast);
        if (completedBroadcast != null)
            unregisterReceiver(completedBroadcast);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //----------------------------------------------------------------------------------------------------
        btnRecord = (Button) findViewById(R.id.btn_start);
        oscillogram = (OscillogramView) findViewById(R.id.oscillogram_view);
        spectrogram = (SpectrogramView) findViewById(R.id.spectrogram_view);
        durationTextView = (TextView) findViewById(R.id.duration_text_view);
        powerSpectrum = (PowerSpectrumView) findViewById(R.id.power_spectrum_view);
        logTextView = (TextView) findViewById(R.id.log_text_view);
        //----------------------------------------------------------------------------------------------------
        updateButtonName();
        //----------------------------------------------------------------------------------------------------
        logTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (powerSpectrum != null) {
                    if (powerSpectrum.getLogarithmicScale())
                        powerSpectrum.setLogarithmicScale(false);
                    else powerSpectrum.setLogarithmicScale(true);
                }
                if (powerSpectrum != null && powerSpectrum.getLogarithmicScale()) {
                    logTextView.setTextColor(getResources().getColor(R.color.black));
                } else {
                    logTextView.setTextColor(getResources().getColor(R.color.color_black_alpha_10));
                }
            }
        });
        //----------------------------------------------------------------------------------------------------
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnRecord.getText().toString().compareTo(getString(R.string.start_record)) == 0) {
                    if (SingleShotIntentService.getInstance() == null) {
                        Intent service = new Intent(MainActivity.this, SingleShotIntentService.class);
                        startWakefulService(MainActivity.this, service);
                    }
                    btnRecord.setText(R.string.stop_record);
                } else {
                    if (SingleShotIntentService.getInstance() != null) {
                        try {
                            SingleShotIntentService.getInstance().cancel();
                        } catch (Exception _) {
                        }
                    }
                    btnRecord.setText(R.string.start_record);
                }
            }
        });
    }
}
