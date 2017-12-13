package com.upkoder.recorder.helpers;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.upkoder.recorder.wav.WavAudioFormat;

/**
 * Created by leandro on 3/17/2016.
 */
public class AudioRecordHelper {

    private static String TAG = "AudioRecordHelper";

    //http://stackoverflow.com/questions/4843739/audiorecord-object-not-initializing
    public static AudioRecord findAudioRecord() {
        int[] mSampleRates = new int[] { 44100, 22050, 11025, 8000 };
        for (int rate : mSampleRates) {
            for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_8BIT }) {
                for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) {
                    try {
                        Log.d(TAG, "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                                + channelConfig);
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                                return recorder;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, rate + "Exception, keep trying.",e);
                    }
                }
            }
        }
        return null;
    }

    public static WavAudioFormat getWavAudioFormat(AudioRecord audioRecord) {
        int sampleRate = audioRecord.getSampleRate();
        int channels = audioRecord.getChannelCount();
        int bits = -1;

        if (audioRecord.getAudioFormat() == AudioFormat.ENCODING_PCM_8BIT)
            bits = 8;
        if (audioRecord.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT)
            bits = 16;

        return new WavAudioFormat(sampleRate, bits, channels, true);
    }
}
