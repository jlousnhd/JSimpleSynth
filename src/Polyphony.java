import java.io.*;
import java.util.*;

public final class Polyphony {
	
	private final Tone[] tones;
	
	private Polyphony(Tone[] tones) {
		this.tones = tones;
	}
	
	public Polyphony(Tone tone) {
		
		if(tone == null)
			throw new NullPointerException();
		
		tones = new Tone[] { tone };
		
	}
	
	public Polyphony(Polyphony existing, Tone newTone) {
		
		if(newTone == null)
			throw new NullPointerException();
		
		if(existing.tones.length >= 255)
			throw new IllegalArgumentException("Polyphony is limited to 255 tones");
		
		this.tones = new Tone[existing.tones.length + 1];
		
		System.arraycopy(existing.tones, 0, this.tones, 0, existing.tones.length);
		this.tones[existing.tones.length] = newTone;
		
	}
	
	public Polyphony(Collection<Tone> tones) {
		
		this.tones = new Tone[tones.size()];
		
		if(this.tones.length >= 256)
			throw new IllegalArgumentException("Polyphony is limited to 255 tones");
		
		int i = 0;
		for(Tone tone : tones) {
			
			if(tone == null)
				throw new NullPointerException();
			
			this.tones[i] = tone;
			
		}
		
	}
	
	public int polyphony() {
		return tones.length;
	}
	
	public void writeTo(OutputStream os) throws IOException {
		
		os.write(tones.length);
		
		for(Tone tone : tones)
			tone.writeTo(os);
		
	}
	
	public static Polyphony readFrom(InputStream is) throws IOException {
		
		int length = is.read();
		if(length < 0)
			throw new IOException("End of stream reached before data could be read");
		
		Tone[] tones = new Tone[length];
		
		for(int i = 0; i < tones.length; ++i)
			tones[i] = Tone.readFrom(is);
		
		return new Polyphony(tones);
		
	}
	
	public void renderTo(double[] samples, double start, double sampleRate) {
		
		for(Tone tone : tones)
			tone.addTo(samples, start, sampleRate);
		
	}
	
	@Override
	public String toString() {
		
		String str = "";
		
		for(Tone tone : tones)
			str += tone + "\n";
		
		return str;
		
	}
	
}
