
public enum WavSampleFormat {
	
	
	PCM_INT_16(16, 1, true, false, false, false),
	PCM_UINT_8( 8, 1, false, true, false, false),
	PCM_INT_32(32, 1, false, false, false, true),
	PCM_FLOAT (32, 3, false, false, true, false);
	
	public final int BITS_PER_SAMPLE;
	public final int BYTES_PER_SAMPLE;
	public final int FORMAT_CODE;
	public final boolean IS_SHORT;
	public final boolean IS_BYTE;
	public final boolean IS_FLOAT;
	public final boolean IS_INT;
	
	WavSampleFormat(int bitsPerSample, int formatCode, boolean isShort, boolean isByte, boolean isFloat, boolean isInt) {
		
		this.BITS_PER_SAMPLE = bitsPerSample;
		this.BYTES_PER_SAMPLE = bitsPerSample / 8;
		this.FORMAT_CODE = formatCode;
		
		this.IS_SHORT = isShort;
		this.IS_BYTE = isByte;
		this.IS_FLOAT = isFloat;
		this.IS_INT = isInt;
		
	}
	
	public boolean isCorrectArrayType(int[] array) {
		return IS_INT;
	}
	
	public boolean isCorrectArrayType(byte[] array) {
		return IS_BYTE;
	}
	
	public boolean isCorrectArrayType(float[] array) {
		return IS_FLOAT;
	}
	
	public boolean isCorrectArrayType(short[] array) {
		return IS_SHORT;
	}
	
}
