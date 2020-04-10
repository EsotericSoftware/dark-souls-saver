
package com.esotericsoftware.darksoulssaver;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public enum Sound {
	save("/save.wav"), //
	replaceSave("/replace-save.wav"), //
	replaceBackup("/replace-backup.wav"), //
	error("/error.wav"), //
	;

	final byte[] bytes;

	Sound (String path) {
		try (InputStream input = Sound.class.getResourceAsStream(path)) {
			if (input == null) throw new FileNotFoundException(path);
			if (input.read() != 'R' || input.read() != 'I' || input.read() != 'F' || input.read() != 'F')
				throw new IOException("RIFF header not found: " + path);

			skipFully(input, 4);

			if (input.read() != 'W' || input.read() != 'A' || input.read() != 'V' || input.read() != 'E')
				throw new IOException("Invalid wave file header: " + path);

			int fmtChunkLength = seekToChunk(input, 'f', 'm', 't', ' ');

			int type = input.read() & 0xff | (input.read() & 0xff) << 8;
			if (type != 1) throw new IOException("WAV files must be PCM, unsupported format: " + type);

			int channels = input.read() & 0xff | (input.read() & 0xff) << 8;
			if (channels != 1 && channels != 2) throw new IOException("WAV files must have 1 or 2 channels: " + channels);

			int sampleRate = input.read() & 0xff | (input.read() & 0xff) << 8 | (input.read() & 0xff) << 16
				| (input.read() & 0xff) << 24;
			if (sampleRate != Audio.sampleRate) throw new IOException("Invalid sample rate: " + sampleRate);

			skipFully(input, 6);

			int bitsPerSample = input.read() & 0xff | (input.read() & 0xff) << 8;
			if (bitsPerSample != 16) throw new IOException("WAV files must have 16 bits per sample: " + bitsPerSample);

			skipFully(input, fmtChunkLength - 16);

			int remaining = seekToChunk(input, 'd', 'a', 't', 'a');
			bytes = new byte[remaining];
			int offset = 0;
			while (remaining > 0) {
				int count = input.read(bytes, offset, remaining);
				if (count == -1) throw new EOFException();
				remaining -= count;
				offset += count;
			}
		} catch (IOException ex) {
			throw new RuntimeException("Error reading WAV file: " + path, ex);
		}
	}

	static private int seekToChunk (InputStream input, char c1, char c2, char c3, char c4) throws IOException {
		while (true) {
			boolean found = input.read() == c1;
			found &= input.read() == c2;
			found &= input.read() == c3;
			found &= input.read() == c4;
			int chunkLength = input.read() & 0xff | (input.read() & 0xff) << 8 | (input.read() & 0xff) << 16
				| (input.read() & 0xff) << 24;
			if (chunkLength == -1) throw new IOException("Chunk not found: " + c1 + c2 + c3 + c4);
			if (found) return chunkLength;
			skipFully(input, chunkLength);
		}
	}

	static private void skipFully (InputStream input, int count) throws IOException {
		while (count > 0) {
			long skipped = input.skip(count);
			if (skipped <= 0) throw new EOFException("Unable to skip.");
			count -= skipped;
		}
	}
}
