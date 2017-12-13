package com.upkoder.recorder.services;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioRecord;
import android.os.Environment;
import android.text.format.Time;

import com.upkoder.recorder.helpers.ArrayHelper;
import com.upkoder.recorder.helpers.AudioRecordHelper;
import com.upkoder.recorder.models.Record;
import com.upkoder.recorder.wav.WavAudioFormat;
import com.upkoder.recorder.wav.WavFileWriter;

import java.io.File;
import java.io.IOException;

/**
 * Created by leandro on 6/12/2016.
 */
public class SingleShotIntentService extends IntentService {

    //----------------------------------------------------------------------------------------------------
    // Broadcasts
    //----------------------------------------------------------------------------------------------------
    public static class CompletedBroadcast extends BroadcastReceiver {
        public static final String ACTION = "CompletedBroadcast.ACTION";

        public interface CompletedCallback {
            void completed();
        }

        CompletedCallback callback;

        public CompletedBroadcast(CompletedCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            this.callback.completed();
        }
    }

    public static class ProgressBroadcast extends BroadcastReceiver {

        public static final String ACTION = "ProgressBroadcast.ACTION";
        public static final String EXTRA_DATA = "DATA";
        public static final String EXTRA_BYTES = "BYTES";
        public static final String EXTRA_SAMPLING_RATE = "SAMPLING_RATE";

        public interface ReceiveCallback {
            void receive(int bytes, int samplingRate, short[] data);
        }

        private ProgressBroadcast.ReceiveCallback callback;

        public ProgressBroadcast(ProgressBroadcast.ReceiveCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            int samplingRate = intent.getIntExtra(ProgressBroadcast.EXTRA_SAMPLING_RATE, -1);
            short[] data = intent.getShortArrayExtra(ProgressBroadcast.EXTRA_DATA);
            int bytes = intent.getIntExtra(ProgressBroadcast.EXTRA_BYTES, -1);
            this.callback.receive(bytes, samplingRate, data);
        }
    }

    //----------------------------------------------------------------------------------------------------
    // ...
    //----------------------------------------------------------------------------------------------------
    public static final String EXTRA_DURATION = "duration";

    int duration;
    boolean canceled;
    static SingleShotIntentService instance;

    public static SingleShotIntentService getInstance() {
        return SingleShotIntentService.instance;
    }

    public SingleShotIntentService() {
        super("SingleShotIntentService");
        SingleShotIntentService.instance = this;
    }

    public void cancel() {
        this.canceled = true;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        canceled = false;
        duration = intent.getIntExtra(EXTRA_DURATION, -1);

        //----------------------------------------------------------------------------------------------------
        // Metadata
        //----------------------------------------------------------------------------------------------------
        Time time = new Time();
        time.setToNow();
        String name = time.format("%Y%m%d_%H%M%S").concat(".wav");
        String date = time.format("%Y-%m-%d %H:%M");

        //----------------------------------------------------------------------------------------------------
        // Create file
        //----------------------------------------------------------------------------------------------------
        final File file = new File(this.getExternalFilesDir(Environment.DIRECTORY_MUSIC), name);

        try {
            if (!file.createNewFile()) {
                stop();
                return;
            }
        } catch (IOException e) {
            stop();
            return;
        }

        //----------------------------------------------------------------------------------------------------
        // Start recording
        //----------------------------------------------------------------------------------------------------
        AudioRecord recorder = AudioRecordHelper.findAudioRecord();
        WavAudioFormat waf = AudioRecordHelper.getWavAudioFormat(recorder);
        WavFileWriter wavWriter = null;

        try {
            wavWriter = new WavFileWriter(waf, file);

            boolean emptyPrefix = true;
            int readBytes = 0;
            int stopBytes = duration < 0 ? Integer.MAX_VALUE : waf.getSampleRate() * duration;

            recorder.startRecording();

            while (!canceled && readBytes < stopBytes) {
                short[] buffer = new short[1024];
                int length = recorder.read(buffer, 0, 1024);

                if (emptyPrefix) {
                    int offset = 0;
                    while (offset < length && emptyPrefix && buffer[offset] == 0)
                        offset++;
                    if (offset >= length)
                        continue;
                    buffer = ArrayHelper.segment(buffer, offset, length);
                    emptyPrefix = false;
                }

                if (readBytes + buffer.length > stopBytes)
                    buffer = ArrayHelper.segment(buffer, 0, readBytes + buffer.length - stopBytes);

                wavWriter.write(buffer);
                readBytes += buffer.length;

                //--------------------------------------------------
                // Progress broadcast
                //--------------------------------------------------
                Intent broadcast = new Intent(ProgressBroadcast.ACTION);
                broadcast.putExtra(ProgressBroadcast.EXTRA_SAMPLING_RATE, waf.getSampleRate());
                broadcast.putExtra(ProgressBroadcast.EXTRA_DATA, buffer);
                broadcast.putExtra(ProgressBroadcast.EXTRA_BYTES, readBytes);
                sendBroadcast(broadcast);
            }
        } catch (IOException e) {
            e.printStackTrace();
            name = date = null;
        } finally {
            if (recorder != null)
                recorder.stop();
            try {
                if (wavWriter != null)
                    wavWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //--------------------------------------------------
        // TODO: Save file here!!!
        //--------------------------------------------------
        if (name != null) {
            Record record = Record.create(this, name, date);
            if (record != null) {
                record.send();
            }
        }

        stop();
    }

    private void stop() {
        SingleShotIntentService.instance = null;
        Intent broadcast = new Intent(CompletedBroadcast.ACTION);
        sendBroadcast(broadcast);
    }
}
