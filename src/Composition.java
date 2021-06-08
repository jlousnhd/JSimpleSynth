import java.io.*;
import java.util.*;

public final class Composition {
	
	private static final Polyphony EMPTY_POLYPHONY = new Polyphony(Collections.emptySet());
	
	private final double sliceLength;
	private final TreeMap<Integer, Polyphony> slices;
	
	public Composition(double sliceLength) {
		
		if(!Double.isFinite(sliceLength) || sliceLength <= 0.0)
			throw new IllegalArgumentException("Slice length must be positive and finite");
		
		this.sliceLength = sliceLength;
		this.slices = new TreeMap<Integer, Polyphony>();
		
	}
	
	private Composition(double sliceLength, TreeMap<Integer, Polyphony> slices) {
		
		this.sliceLength = sliceLength;
		this.slices = slices;
		
	}
	
	public Polyphony getSlice(int index) {
		
		Polyphony p = slices.get(index);
		
		return p == null ? EMPTY_POLYPHONY : p;
		
	}
	
	public boolean isEmpty() {
		return slices.isEmpty();
	}
	
	public int generateSamples(double[] samples, int sliceIndex, double sampleRate) {
		
		long start = getSliceStartInSamples(sliceIndex, sampleRate);
		int sampleLength = (int) (getSliceEndInSamples(sliceIndex, sampleRate) - start);
		
		if(samples.length < sampleLength)
			throw new IllegalArgumentException();
		
		Arrays.fill(samples, 0.0);
		
		Polyphony polyphony = slices.get(sliceIndex);
		
		if(polyphony == null)
			return sampleLength;
		
		polyphony.renderTo(samples, start / sampleRate, sampleRate);
		
		return sampleLength;
		
	}
	
	public void writeTo(OutputStream os) throws IOException {
		
		// Slice length
		long bits = Double.doubleToLongBits(sliceLength);
		
		os.write((int) (bits >>> 56) & 0xff);
		os.write((int) (bits >>> 48) & 0xff);
		os.write((int) (bits >>> 40) & 0xff);
		os.write((int) (bits >>> 32) & 0xff);
		os.write((int) (bits >>> 24) & 0xff);
		os.write((int) (bits >>> 16) & 0xff);
		os.write((int) (bits >>>  8) & 0xff);
		os.write((int) (bits       ) & 0xff);
		
		// Write number of slices
		int sliceCount = getSliceCount();
		
		os.write((sliceCount >>> 24) & 0xff);
		os.write((sliceCount >>> 16) & 0xff);
		os.write((sliceCount >>>  8) & 0xff);
		os.write((sliceCount       ) & 0xff);
		
		// Write slices
		for(int i = 0; i < sliceCount; ++i) {
			
			Polyphony slice = slices.get(i);
			slice = (slice == null) ? EMPTY_POLYPHONY : slice;
			
			slice.writeTo(os);
			
		}
		
	}
	
	public static Composition readFrom(InputStream is) throws IOException {
		
		long bits = 0L;
		
		for(int i = 0; i < 8; ++i) {
			
			bits <<= 8;
			
			int byt = is.read();
			
			if(byt < 0)
				throw new IOException("End of stream reached before data could be read");
			
			bits |= byt;
			
		}
		
		double sliceLength = Double.longBitsToDouble(bits);
		if(!Double.isFinite(sliceLength) || sliceLength <= 0.0)
			throw new IOException("Bad slice length");
		
		int sliceCount = 0;
		
		for(int i = 0; i < 4; ++i) {
			
			sliceCount <<= 8;
			
			int byt = is.read();
			
			if(byt < 0)
				throw new IOException("End of stream reached before data could be read");
			
			sliceCount |= byt;
			
		}
		
		TreeMap<Integer, Polyphony> slices = new TreeMap<Integer, Polyphony>();
		
		for(int i = 0; i < sliceCount; ++i) {
			
			Polyphony slice = Polyphony.readFrom(is);
			
			if(slice.polyphony() > 0)
				slices.put(i, slice);
			
		}
		
		return new Composition(sliceLength, slices);
		
	}
	
	public void addTone(int sliceIndex, Tone tone) {
		
		if(sliceIndex < 0)
			throw new IllegalArgumentException();
		
		if(tone == null)
			throw new NullPointerException();
		
		Polyphony existing = slices.get(sliceIndex);
		slices.put(sliceIndex, existing == null ? new Polyphony(tone) : new Polyphony(existing, tone));
		
	}
	
	public long getSliceStartInSamples(int sliceIndex, double sampleRate) {
		
		double start = sliceIndex * sliceLength;
		return (long) Math.floor(start * sampleRate);
		
	}
	
	public long getSliceEndInSamples(int sliceIndex, double sampleRate) {
		
		double start = (sliceIndex + 1) * sliceLength;
		return (long) Math.floor(start * sampleRate);
		
	}
	
	public int getSliceLengthInSamples(int sliceIndex, double sampleRate) {
		return (int) (getSliceEndInSamples(sliceIndex, sampleRate) - getSliceStartInSamples(sliceIndex, sampleRate));
	}
	
	public int getMaxSliceLengthInSamples(double sampleRate) {
		return (int) Math.ceil(sliceLength * sampleRate);
	}
	
	public double getSliceLength() {
		return sliceLength;
	}
	
	public int getSliceCount() {
		
		Integer lastSliceIndex = slices.lastKey();
		
		if(lastSliceIndex == null)
			return 0;
		
		return lastSliceIndex + 1;
		
	}
	
	public double getLength() {
		return getSliceLength() * getSliceCount();
	}
	
	public void saveWav(File file, double sampleRate) throws IOException {
		
		double[] samples = new double[getMaxSliceLengthInSamples(sampleRate)];
		short[] shorts = new short[samples.length];
		
		WavOutputStream wos = new WavOutputStream(file.getAbsolutePath(), (int) sampleRate, 1, WavSampleFormat.PCM_INT_16);
		
		int sliceCount = getSliceCount();
		
		for(int i = 0; i < sliceCount; ++i) {
			
			int sampleCount = generateSamples(samples, i, sampleRate);
			
			for(int j = 0; j < sampleCount; ++j)
				shorts[j] = (short) (samples[j] * Short.MAX_VALUE);
			
			wos.write(shorts, 0, sampleCount);
			
		}
		
		wos.close();
		
	}
	
}
