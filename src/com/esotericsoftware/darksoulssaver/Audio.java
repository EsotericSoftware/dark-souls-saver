
package com.esotericsoftware.darksoulssaver;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public class Audio {
	static final int sampleRate = 16000;

	private SourceDataLine line;

	public Audio () {
		AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
		try {
			line = (SourceDataLine)AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
			line.open(format);
			line.start();
		} catch (Throwable ex) {
			System.out.println("Error opening playback" + (line == null ? "." : ": " + line.getFormat()));
			ex.printStackTrace(System.out);
			line = null;
		}
	}

	public boolean play (Sound sound) {
		SourceDataLine line = this.line;
		if (line == null) return false;
		try {
			int count = sound.bytes.length;
			int written = 0;
			while (written < count) {
				int result = line.write(sound.bytes, written, count - written);
				if (result == -1) throw new IOException("Error writing audio: stream closed");
				written += result;
			}
			return true;
		} catch (IOException ex) {
			System.out.println("Error writing audio.");
			ex.printStackTrace(System.out);
			return false;
		}
	}
}
