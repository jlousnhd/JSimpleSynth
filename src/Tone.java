import java.io.*;

public final class Tone {
	
	private static final double[] EQUAL_FREQS = generateEqual();
	
	public static final int SQUARE = 0;
	public static final int SAWTOOTH = 1;
	public static final int TRIANGLE = 2;
	public static final int SINE = 3;
	
	private final int type;
	private final int note;
	private final int amplitude;
	
	public Tone(int type, int note, int amplitude) {
		
		if(type < 0 || type > SINE)
			throw new IllegalArgumentException();
		
		if(note < 0 || note >= 128)
			throw new IllegalArgumentException();
		
		if(amplitude < 0 || amplitude >= 256)
			throw new IllegalArgumentException();
		
		this.type = type;
		this.note = note;
		this.amplitude = amplitude;
		
	}
	
	public void writeTo(OutputStream os) throws IOException {
		
		os.write(type);
		os.write(note);
		os.write(amplitude);
		
	}
	
	public static Tone readFrom(InputStream is) throws IOException {
		
		int type = is.read();
		if(type < 0)
			throw new IOException("End of stream reached before data could be read");
		
		if(type > SINE)
			throw new IOException("Bad value for tone type");
		
		int note = is.read();
		if(note < 0)
			throw new IOException("End of stream reached before data could be read");
		
		if(note >= 128)
			throw new IOException("Bad value for tone note");
		
		int amplitude = is.read();
		if(amplitude < 0)
			throw new IOException("End of stream reached before data could be read");
		
		return new Tone(type, note, amplitude);
		
	}
	
	public void addTo(double[] samples, double start, double sampleRate) {
		
		double amplitude = this.amplitude / 255.0;
		
		double freq = EQUAL_FREQS[note];
		
		switch(type) {
		
		case SQUARE:   addSquare  (samples, start, freq, amplitude, sampleRate); break;
		case SAWTOOTH: addSawtooth(samples, start, freq, amplitude, sampleRate); break;
		case TRIANGLE: addTriangle(samples, start, freq, amplitude, sampleRate); break;
		case SINE:     addSine    (samples, start, freq, amplitude, sampleRate); break;
		
		default:
			assert(false);
		
		}
		
	}
	
	@Override
	public String toString() {
		return "{MIDI: " + note + ", Volume: " + amplitude + ", Type: " + typeName(type) + "}";
	}
	
	public static String typeName(int type) {
		
		switch(type) {
		
		case SQUARE:   return "Square";
		case SAWTOOTH: return "Sawtooth";
		case TRIANGLE: return "Triangle";
		case SINE:     return "Sine";
		
		}
		
		throw new IllegalArgumentException();
		
	}
	
	public static void addSawtooth(double[] samples, double start, double freq, double amplitude, double sampleRate) {
		
		double cycleLength = 1.0 / freq;
		
		double min = -amplitude;
		double range = amplitude * 2.0;
		
		for(int i = 0; i < samples.length; ++i) {
			
			double time = start + (i / (double) sampleRate);
			double posInCycle = (time / cycleLength + 0.25) % 1.0;
			samples[i] += min + posInCycle * range;
			
		}
		
	}
	
	public static void addTriangle(double[] samples, double start, double freq, double amplitude, double sampleRate) {
		
		double cycleLength = 1.0 / freq;
		
		double min = -amplitude;
		double range = amplitude * 2.0;
		
		for(int i = 0; i < samples.length; ++i) {
			
			double time = start + (i / (double) sampleRate);
			double posInCycle = (time / cycleLength + 0.25) % 1.0;
			double posInHalfCycle = (posInCycle * 2.0) % 1.0;
			samples[i] += posInCycle < 0.5 ? min + posInHalfCycle * range: amplitude - posInHalfCycle * range;
			
		}
		
	}
	
	public static void addSquare(double[] samples, double start, double freq, double amplitude, double sampleRate) {
		
		double cycleLength = 1.0 / freq;
		short min = (short) -amplitude;
		
		for(int i = 0; i < samples.length; ++i) {
			
			double time = start + (i / (double) sampleRate);
			double posInCycle = (time / cycleLength) % 1.0;
			samples[i] += posInCycle < 0.5 ? amplitude : min;
			
		}
		
	}
	
	public static void addSine(double[] samples, double start, double freq, double amplitude, double sampleRate) {
		
		for(int i = 0; i < samples.length; ++i) {
			
			double time = start + (i / (double) sampleRate);
			samples[i] += sineWaveAtTime(time, freq) * amplitude;
			
		}
		
	}
	
	public static double sineWaveAtTime(double time, double freq) {
		return Math.sin((time * freq) * Math.PI * 2.0);
	}
	
	public static double[] generateEqual() {
		
		double[] freqs = new double[128]; 
		
		int offset = 0;
		
		for(int i = 69; i >= 0; --i) {
			
			freqs[i] = 440.0 * Math.pow(2, offset / 12.0);
			--offset;
			
		}
		
		offset = 0;
		
		for(int i = 69; i < freqs.length; ++i) {
			
			freqs[i] = 440.0 * Math.pow(2, offset / 12.0);
			++offset;
			
		}
		
		return freqs;
		
	}
	
}
