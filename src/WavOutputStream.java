import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public class WavOutputStream implements Closeable {

	private static final int HEADER_LENGTH = 44;
	private static final int FMT_SIZE = 16;
	
	private final FileChannel channel; 
	private final long sampleRate;
	private final int numChannels;
	private final WavSampleFormat sampleFormat;
	private long samplesWritten;
	
	public WavOutputStream(String name, long sampleRate, int numChannels, WavSampleFormat sampleFormat) throws IOException {
		
		this(new RandomAccessFile(name, "rw"), sampleRate, numChannels, sampleFormat);
		
	}
	
	public WavOutputStream(File file, long sampleRate, int numChannels, WavSampleFormat sampleFormat) throws IOException {
		
		this(new RandomAccessFile(file, "rw"), sampleRate, numChannels, sampleFormat);
		
	}
	
	private WavOutputStream(RandomAccessFile file, long sampleRate, int numChannels, WavSampleFormat sampleFormat) throws IOException {
		
		if(sampleRate < 0 || sampleRate >= (1L << 32))
			throw new IllegalArgumentException("Sample rate must fit within 32 bit unsigned integer.");
		
		if(numChannels < 0 || numChannels >= (1 << 16))
			throw new IllegalArgumentException("Number of channels must fit within 16 bit unsigned integer.");
		
		this.channel = file.getChannel();
		this.sampleRate = sampleRate;
		this.numChannels = numChannels;
		this.sampleFormat = sampleFormat;
		
		samplesWritten = 0;
		
		// Seek to where samples will begin being written
		file.seek(HEADER_LENGTH);
		
	}
	
	public void write(int[] samples, int offset, int length) throws IOException {
		
		if(!sampleFormat.isCorrectArrayType(samples))
			throw new IllegalArgumentException("Underlying sample type does not match input array.");
		
		if(length % numChannels != 0)
			throw new IllegalArgumentException("All channels' samples must be written at once.");
		
		ByteBuffer buffer = ByteBuffer.allocateDirect(length * sampleFormat.BYTES_PER_SAMPLE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		IntBuffer asInt = buffer.asIntBuffer();
		asInt.put(samples, offset, length);
		
		buffer.rewind();
		
		samplesWritten += length;
		
		while(length > 0)
			length -= channel.write(buffer);
		
	}
	
	public void write(short[] samples, int offset, int length) throws IOException {
		
		if(!sampleFormat.isCorrectArrayType(samples))
			throw new IllegalArgumentException("Underlying sample type does not match input array.");
		
		if(length % numChannels != 0)
			throw new IllegalArgumentException("All channels' samples must be written at once.");
		
		ByteBuffer buffer = ByteBuffer.allocateDirect(length * sampleFormat.BYTES_PER_SAMPLE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		ShortBuffer asShort = buffer.asShortBuffer();
		asShort.put(samples, offset, length);
		
		buffer.rewind();
		
		samplesWritten += length;
		
		while(length > 0)
			length -= channel.write(buffer);
		
	}
	
	public void write(float[] samples, int offset, int length) throws IOException {
		
		if(!sampleFormat.isCorrectArrayType(samples))
			throw new IllegalArgumentException("Underlying sample type does not match input array.");
		
		if(length % numChannels != 0)
			throw new IllegalArgumentException("All channels' samples must be written at once.");
		
		ByteBuffer buffer = ByteBuffer.allocateDirect(length * sampleFormat.BYTES_PER_SAMPLE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		FloatBuffer asFloat = buffer.asFloatBuffer();
		asFloat.put(samples, offset, length);
		
		buffer.rewind();
		
		samplesWritten += length;
		
		while(length > 0)
			length -= channel.write(buffer);
		
	}
	
	public void write(byte[] samples, int offset, int length) throws IOException {
		
		if(!sampleFormat.isCorrectArrayType(samples))
			throw new IllegalArgumentException("Underlying sample type does not match input array.");
		
		if(length % numChannels != 0)
			throw new IllegalArgumentException("All channels' samples must be written at once.");
		
		ByteBuffer buffer = ByteBuffer.wrap(samples, offset, length);
		
		buffer.rewind();
		
		samplesWritten += length;
		
		while(length > 0)
			length -= channel.write(buffer);
		
	}
	
	public long getSamplesWritten() {
		return samplesWritten;
	}
	
	@Override
	public void close() throws IOException {
		
		byte[] header = new byte[HEADER_LENGTH];
		int offset = 0;
		
		long rawBytes = samplesWritten * sampleFormat.BYTES_PER_SAMPLE;
		long riffChunkSize = rawBytes + HEADER_LENGTH - 8;
		
		long byteRate = sampleRate * numChannels * sampleFormat.BYTES_PER_SAMPLE;
		int blockAlign = numChannels * sampleFormat.BYTES_PER_SAMPLE;
		
		header[offset++] = 'R';
		header[offset++] = 'I';
		header[offset++] = 'F';
		header[offset++] = 'F';
		
		header[offset++] = (byte) ((riffChunkSize      ) & 0xff);
		header[offset++] = (byte) ((riffChunkSize >>  8) & 0xff);
		header[offset++] = (byte) ((riffChunkSize >> 16) & 0xff);
		header[offset++] = (byte) ((riffChunkSize >> 24) & 0xff);
		
		header[offset++] = 'W';
		header[offset++] = 'A';
		header[offset++] = 'V';
		header[offset++] = 'E';
		
		header[offset++] = 'f';
		header[offset++] = 'm';
		header[offset++] = 't';
		header[offset++] = ' ';
		
		header[offset++] = (byte) ((FMT_SIZE      ) & 0xff);
		header[offset++] = (byte) ((FMT_SIZE >>  8) & 0xff);
		header[offset++] = (byte) ((FMT_SIZE >> 16) & 0xff);
		header[offset++] = (byte) ((FMT_SIZE >> 24) & 0xff);
		
		header[offset++] = (byte) ((sampleFormat.FORMAT_CODE     ) & 0xff);
		header[offset++] = (byte) ((sampleFormat.FORMAT_CODE >> 8) & 0xff);
		
		header[offset++] = (byte) ((numChannels     ) & 0xff);
		header[offset++] = (byte) ((numChannels >> 8) & 0xff);
		
		header[offset++] = (byte) ((sampleRate      ) & 0xff);
		header[offset++] = (byte) ((sampleRate >>  8) & 0xff);
		header[offset++] = (byte) ((sampleRate >> 16) & 0xff);
		header[offset++] = (byte) ((sampleRate >> 24) & 0xff);
		
		header[offset++] = (byte) ((byteRate      ) & 0xff);
		header[offset++] = (byte) ((byteRate >>  8) & 0xff);
		header[offset++] = (byte) ((byteRate >> 16) & 0xff);
		header[offset++] = (byte) ((byteRate >> 24) & 0xff);
		
		header[offset++] = (byte) ((blockAlign     ) & 0xff);
		header[offset++] = (byte) ((blockAlign >> 8) & 0xff);
		
		header[offset++] = (byte) ((sampleFormat.BITS_PER_SAMPLE     ) & 0xff);
		header[offset++] = (byte) ((sampleFormat.BITS_PER_SAMPLE >> 8) & 0xff);
		
		header[offset++] = 'd';
		header[offset++] = 'a';
		header[offset++] = 't';
		header[offset++] = 'a';
		
		header[offset++] = (byte) ((rawBytes      ) & 0xff);
		header[offset++] = (byte) ((rawBytes >>  8) & 0xff);
		header[offset++] = (byte) ((rawBytes >> 16) & 0xff);
		header[offset++] = (byte) ((rawBytes >> 24) & 0xff);
		
		channel.position(0);
		
		ByteBuffer buffer = ByteBuffer.wrap(header);
		buffer.rewind();
		
		int toWrite = header.length;
		
		while(toWrite > 0)
			toWrite -= channel.write(buffer);
		
		channel.close();
		
	}
	
	
	
}
