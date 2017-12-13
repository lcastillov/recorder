package com.upkoder.recorder.wav;

public class AudioChunk {
 	public short[] data;
	public int length = 0;
	public int used = 0;
	
	public AudioChunk(int size) {
		data = new short[size];
	}
}
