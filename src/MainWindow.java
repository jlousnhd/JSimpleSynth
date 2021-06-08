import java.awt.event.*;
import java.io.*;

import javax.sound.sampled.*;
import javax.swing.*;
import java.util.*;

public final class MainWindow extends JFrame implements KeyListener {
	
	private static final long serialVersionUID = 1L;

	public static final double PLAYBACK_SAMPLE_RATE = 48000.0;
	
	private static final HashMap<Integer, Integer> KEY_CODE_TO_NOTE = generateNoteMappings();
	
	private volatile boolean stop;
	public volatile Composition currentComposition;
	private volatile int instrument;
	private Thread soundThread;
	
	private final TreeSet<Integer> notesPressed;
	private final JTextArea noteDisplay;
	private double timeSlice;
	private final JLabel labelTimeSlice;
	private final JTextArea instructions;
	
	private final JRadioButton buttonSquare;
	private final JRadioButton buttonSawtooth;
	private final JRadioButton buttonTriangle;
	private final JRadioButton buttonSine;
	
	private final JButton buttonChangeTimeSlice;
	private final JButton buttonGo;
	private final JButton buttonStop;
	private final JButton buttonSave;
	private final JButton buttonSaveWav;
	private final JButton buttonLoad;
	
	public MainWindow() {
		
		super("JSimpleSynth");
		
		notesPressed = new TreeSet<Integer>();
		
		instructions = new JTextArea();
		instructions.setEditable(false);
		instructions.setText(
			"Press \"Go!\" to begin playing/recording a composition.\n" +
			"Press \"Stop\" when you're done.\n" +
			"Pressing \"Go!\" again will play back the composition and you may add to it.\n" +
			"Octave 6:  1234567890-=\n" +
			"Octave 5:  QWERTYUIOP[]\n" +
			"Octave 4:  ASDFGHJKL;' and Enter"
		);
		
		JLabel labelNotes = new JLabel("Notes:");
		
		noteDisplay = new JTextArea();
		noteDisplay.setEditable(false);
		
		buttonSquare = new JRadioButton("Square");
		buttonSawtooth = new JRadioButton("Sawtooth");
		buttonTriangle = new JRadioButton("Triangle");
		buttonSine = new JRadioButton("Sine");
		
		ButtonGroup group = new ButtonGroup();
		group.add(buttonSquare);
		group.add(buttonSawtooth);
		group.add(buttonTriangle);
		group.add(buttonSine);
		
		ItemListener il = new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) { onInstrument(); }
		};
		
		buttonSquare.addItemListener(il);
		buttonSawtooth.addItemListener(il);
		buttonTriangle.addItemListener(il);
		buttonSine.addItemListener(il);
		
		buttonSine.setSelected(true);
		
		buttonGo = new JButton("Go!");
		buttonGo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) { onGo(); }
		});
		
		buttonStop = new JButton("Stop");
		buttonStop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) { onStop(); }
		});
		
		buttonSave = new JButton("Save");
		buttonSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) { onSave(); }
		});
		
		buttonSaveWav = new JButton("Save WAV");
		buttonSaveWav.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) { onSaveWav(); }
		});
		
		buttonLoad = new JButton("Load");
		buttonLoad.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) { onLoad(); }
		});
		
		labelTimeSlice = new JLabel();
		buttonChangeTimeSlice = new JButton("Change...");
		buttonChangeTimeSlice.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) { onChangeTimeSlice(); }
		});
		
		Box timeSliceBox = Box.createHorizontalBox();
		timeSliceBox.add(labelTimeSlice);
		timeSliceBox.add(buttonChangeTimeSlice);
		
		Box controlBox = Box.createVerticalBox();
		controlBox.add(buttonSquare);
		controlBox.add(buttonSawtooth);
		controlBox.add(buttonTriangle);
		controlBox.add(buttonSine);
		controlBox.add(timeSliceBox);
		controlBox.add(buttonGo);
		controlBox.add(buttonStop);
		controlBox.add(buttonSave);
		controlBox.add(buttonSaveWav);
		controlBox.add(buttonLoad);
		controlBox.add(labelNotes);
		controlBox.add(noteDisplay);
		
		Box mainBox = Box.createHorizontalBox();
		mainBox.add(instructions);
		mainBox.add(controlBox);
		
		instructions.addKeyListener(this);
		instructions.setFocusable(true);
		
		setTimeSlice(1.0 / 60.0);
		setControlAvailability();
		setContentPane(mainBox);
		pack();
		setSize(this.getWidth(), getHeight() + 150);
		
	}
	
	private void setTimeSlice(double timeSlice) {
		
		if(!Double.isFinite(timeSlice) || timeSlice <= 0.0)
			return;
		
		timeSlice = Math.round(timeSlice * 10000.0) / 10000.0;
		
		this.timeSlice = timeSlice;
		this.labelTimeSlice.setText("Time slice length: " + (timeSlice * 1000.0) + " ms");
		this.currentComposition = new Composition(timeSlice);
		
	}
	
	private boolean isRunning() {
		return soundThread != null;
	}
	
	private void setControlAvailability() {
		
		boolean running = isRunning();
		
		buttonChangeTimeSlice.setEnabled(!running);
		buttonGo.setEnabled(!running);
		buttonStop.setEnabled(running);
		buttonSave.setEnabled(!running);
		buttonSaveWav.setEnabled(!running);
		buttonLoad.setEnabled(!running);
		
	}
	
	private void onInstrument() {
		
		if(buttonSquare.isSelected())
			instrument = Tone.SQUARE;
		
		else if(buttonSawtooth.isSelected())
			instrument = Tone.SAWTOOTH;
		
		else if(buttonTriangle.isSelected())
			instrument = Tone.TRIANGLE;
		
		else
			instrument = Tone.SINE;
		
		instructions.grabFocus();
		
	}
	
	private void onChangeTimeSlice() {
		
		if(isRunning())
			return;
		
		// We can't change an existing composition's time slice length
		if(!currentComposition.isEmpty()) {
			
			int confirm = JOptionPane.showConfirmDialog(this, "Time slice length can't be changed for existing compositions.  Lose current composition?", "Confirm", JOptionPane.OK_CANCEL_OPTION);
			
			if(confirm != JOptionPane.OK_OPTION)
				return;
			
		}
		
		// Prompt for time slice length
		String answer = JOptionPane.showInputDialog(this, "Choose a time slice length in milliseconds:", "Time Slice Length", JOptionPane.OK_CANCEL_OPTION);
		if(answer == null)
			return;
		
		double timeSliceLength;
		
		try {
			timeSliceLength = Double.parseDouble(answer) / 1000.0;
		} catch(Exception e) {
			return;
		}
		
		// Set time slice length and create new composition for that time slice length
		setTimeSlice(timeSliceLength);
		
	}
	
	private void onGo() {
		
		if(!isRunning()) {
			
			stop = false;
			
			double timeSlice = this.timeSlice;
			
			soundThread = new Thread() {
				@Override
				public void run() { runSoundThread(timeSlice); }
			};
			
			soundThread.start();
			
			setControlAvailability();
			instructions.grabFocus();
			
		}
		
	}
	
	private void onStop() {
		
		if(isRunning()) {
			
			stop = true;
			soundThread.interrupt();
			soundThread = null;
			
			setControlAvailability();
			
		}
		
	}
	
	private void onSave() {
		
		if(isRunning())
			return;
		
		JFileChooser chooser = new JFileChooser();
		if(chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return;
		
		File file = chooser.getSelectedFile();
		BufferedOutputStream os = null;
		
		try {
			
			os = new BufferedOutputStream(new FileOutputStream(file));
			currentComposition.writeTo(os);
			os.close();
			
		} catch(Exception e) {
			JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage());
		}
		
	}
	
	private void onSaveWav() {
		
		if(isRunning())
			return;
		
		JFileChooser chooser = new JFileChooser();
		if(chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return;
		
		File file = chooser.getSelectedFile();
		
		String answer = JOptionPane.showInputDialog(this, "What sample rate?  (Try 48000 if you don't know what to put)", "Sample Rate", JOptionPane.QUESTION_MESSAGE);
		if(answer == null)
			return;
		
		try {
			
			double sampleRate = Double.parseDouble(answer);
			
			currentComposition.saveWav(file, sampleRate);
			
			
		} catch(Exception e) {
			JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage());
		}
		
	}
	
	private void onLoad() {
		
		if(isRunning())
			return;
		
		JFileChooser chooser = new JFileChooser();
		if(chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
			return;
		
		File file = chooser.getSelectedFile();
		BufferedInputStream is = null;
		
		try {
			
			is = new BufferedInputStream(new FileInputStream(file));
			
			Composition composition = Composition.readFrom(is);
			is.close();
			
			setTimeSlice(composition.getSliceLength());
			currentComposition = composition;
			
		} catch(Exception e) {
			JOptionPane.showMessageDialog(this, "Error loading file: " + e.getMessage());
		}
		
	}
	
	private void runSoundThread(double timeSlice) {
		
		Composition composition = this.currentComposition;
		
		int sampleCount = composition.getMaxSliceLengthInSamples(PLAYBACK_SAMPLE_RATE);
		double[] samples = new double[sampleCount];
		byte[] bytes = new byte[samples.length * 2];
		
		AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, (float) PLAYBACK_SAMPLE_RATE, 16, 1, 2, (float) PLAYBACK_SAMPLE_RATE, true);
		SourceDataLine line;
		
		int sliceIndex = 0;
		
		try {
			
			line = AudioSystem.getSourceDataLine(format);
			line.open(format, bytes.length * 1);
			line.start();
			
		} catch (LineUnavailableException e) {
			e.printStackTrace();
			System.exit(1);
			return;
		}
		
		for(; ; ++sliceIndex) {
			
			if(stop || Thread.interrupted())
				break;
			
			// Write samples
			int end = sampleCount * 2;
			int written = 0;
			
			while(written < end)
				written += line.write(bytes, written, end - written);
			
			
			
			// Get current notes
			synchronized(notesPressed) {
				
				for(Integer note : notesPressed)
					composition.addTone(sliceIndex, new Tone(instrument, note, 64));
				
			}
			
			// Display notes
			Polyphony p = composition.getSlice(sliceIndex);
			
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() { displayPolyphony(p); }
			});
			
			// Generate notes
			sampleCount = composition.generateSamples(samples, sliceIndex, PLAYBACK_SAMPLE_RATE);
			
			for(int i = 0, j = 0; i < sampleCount; ++i) {
				
				short sample = (short) (samples[i] * Short.MAX_VALUE);
				
				bytes[j++] = (byte) ((sample >>> 8) & 0xff);
				bytes[j++] = (byte) ((sample      ) & 0xff);
				
			}
			
		}
		
		line.stop();
		line.close();
		currentComposition = composition;
		
	}
	
	private void displayPolyphony(Polyphony polyphony) {
		noteDisplay.setText(polyphony.toString());
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
		
		int code = e.getKeyCode();
		
		Integer note = KEY_CODE_TO_NOTE.get(code);
		
		if(note != null) {
			
			synchronized(notesPressed) {
				notesPressed.add(note);
			}
			
		}
		
	}

	@Override
	public void keyReleased(KeyEvent e) {
		
		int code = e.getKeyCode();
		
		Integer note = KEY_CODE_TO_NOTE.get(code);
		
		if(note != null) {
			
			synchronized(notesPressed) {
				notesPressed.remove(note);
			}
			
		}
		
	}

	@Override
	public void keyTyped(KeyEvent e) {}
	
	private static HashMap<Integer, Integer> generateNoteMappings() {
		
		HashMap<Integer, Integer> mappings = new HashMap<Integer, Integer>();
		
		mappings.put(KeyEvent.VK_1, 84);
		mappings.put(KeyEvent.VK_2, 85);
		mappings.put(KeyEvent.VK_3, 86);
		mappings.put(KeyEvent.VK_4, 87);
		mappings.put(KeyEvent.VK_5, 88);
		mappings.put(KeyEvent.VK_6, 89);
		mappings.put(KeyEvent.VK_7, 90);
		mappings.put(KeyEvent.VK_8, 91);
		mappings.put(KeyEvent.VK_9, 92);
		mappings.put(KeyEvent.VK_0, 93);
		mappings.put(KeyEvent.VK_MINUS, 94);
		mappings.put(KeyEvent.VK_EQUALS, 95);
		
		mappings.put(KeyEvent.VK_Q, 72);
		mappings.put(KeyEvent.VK_W, 73);
		mappings.put(KeyEvent.VK_E, 74);
		mappings.put(KeyEvent.VK_R, 75);
		mappings.put(KeyEvent.VK_T, 76);
		mappings.put(KeyEvent.VK_Y, 77);
		mappings.put(KeyEvent.VK_U, 78);
		mappings.put(KeyEvent.VK_I, 79);
		mappings.put(KeyEvent.VK_O, 80);
		mappings.put(KeyEvent.VK_P, 81);
		mappings.put(KeyEvent.VK_OPEN_BRACKET, 82);
		mappings.put(KeyEvent.VK_CLOSE_BRACKET, 83);
		
		mappings.put(KeyEvent.VK_A, 60);
		mappings.put(KeyEvent.VK_S, 61);
		mappings.put(KeyEvent.VK_D, 62);
		mappings.put(KeyEvent.VK_F, 63);
		mappings.put(KeyEvent.VK_G, 64);
		mappings.put(KeyEvent.VK_H, 65);
		mappings.put(KeyEvent.VK_J, 66);
		mappings.put(KeyEvent.VK_K, 67);
		mappings.put(KeyEvent.VK_L, 68);
		mappings.put(KeyEvent.VK_SEMICOLON, 69);
		mappings.put(KeyEvent.VK_QUOTE, 70);
		mappings.put(KeyEvent.VK_ENTER, 71);
		
		return mappings;
		
	}
	
}
