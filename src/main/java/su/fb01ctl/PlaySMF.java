// Plays standard MIDI file after resetting the synthesizer
package su.fb01ctl;

import java.io.File;
import java.io.IOException;
import org.apache.commons.cli.*;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;

public class PlaySMF {
	private static final String help=
			"PlaySMF Version 3\n"+
			"PlaySMF [-p dev] [-r gm|gs|sc88|xg|mu100|doc|mt32|fb01] [-s soundfont.sf2] [-l|-la] 1.mid 2.mid 3.mid ...";
	public static final byte[][] RESET_GM={{(byte)0xF0,0x7E,0x7F,0x09,0x01,(byte)0xF7}};
	public static final byte[][] RESET_GS={
		{(byte)0xF0,0x7E,0x7F,0x09,0x01,(byte)0xF7}, // GM
		{(byte)0xF0,0x41,0x20,0x42,0x12,0x40,0x00,0x7f,0x00,0x41,(byte)0xf7}}; // GS
	public static final byte[][] RESET_XG={
		{(byte)0xF0,0x7E,0x7F,0x09,0x01,(byte)0xF7}, // GM
		{(byte)0xF0,0x43,0x10,0x4c,0x00,0x00,0x7e,0x00,(byte)0xf7}, // XG
		{(byte)0xf0,0x43,0x10,0x49,0x00,0x00,0x12,0x00,(byte)0xf7}}; // MU Basic MAP
	public static final byte[][] RESET_MU100={
		{(byte)0xF0,0x7E,0x7F,0x09,0x01,(byte)0xF7}, // GM
		{(byte)0xF0,0x43,0x10,0x4c,0x00,0x00,0x7e,0x00,(byte)0xf7}, // XG
		{(byte)0xf0,0x43,0x10,0x49,0x00,0x00,0x12,0x01,(byte)0xf7}}; // MU Native MAP
	public static final byte[][] RESET_DOC={
		{(byte)0xF0,0x7E,0x7F,0x09,0x01,(byte)0xF7}, // GM
		{(byte)0xF0,0x43,0x73,0x01,0x14,(byte)0xf7}}; // Disk Orchestra
	public static final byte[][] RESET_MT32={
		{(byte)0xF0,0x41,0x10,0x16,0x11,0x7f,0x00,0x00,0x01,0x00,(byte)0xf7}}; // MT-32 Reset all
	public static final byte[][] RESET_FB01={
		{(byte)0xF0,0x43,0x75,0x00,0x20,0x40,0x11,(byte)0xf7}}; // Store configuration
	public static final byte[][] RESET_SC88={
		{(byte)0xF0,0x7E,0x7F,0x09,0x01,(byte)0xF7}, // GM
		{(byte)0xF0,0x41,0x10,0x42,0x12,0x00,0x00,0x7F,0x00,0x01,(byte)0xF7}}; // System mode set mode-1
	private MidiDevice outDevice;
	private int outDeviceNum;
	private byte[][] resetSequence;
	private Receiver outPort;
	private Sequencer sequencer;

	protected PlaySMF(int deviceNum,String resetSeqLabel) {
		outDeviceNum=deviceNum;
		if(resetSeqLabel==null) {
			resetSequence=null;
		} else {
			resetSequence=chooseResetSequenceByName(resetSeqLabel);
		}
	}
	public void prepare(String soundBankPath) throws MidiUnavailableException, IOException, InvalidMidiDataException {
		if(outDeviceNum>0) {
			MidiDevice.Info[] devInfo=MidiSystem.getMidiDeviceInfo();
			outDevice=MidiSystem.getMidiDevice(devInfo[outDeviceNum]);
		} else {
			outDevice=MidiSystem.getSynthesizer();
		}
		outDevice.open();
		if(soundBankPath!=null && outDevice instanceof Synthesizer) {
			Synthesizer synth=(Synthesizer)outDevice;
			Soundbank sb=MidiSystem.getSoundbank(new File(soundBankPath));
			synth.loadAllInstruments(sb);
			//try { Thread.sleep(500); } catch(InterruptedException e) {} // Wait for the system to stabilize
		}
		// connect sequencer to the synthesizer
		sequencer=MidiSystem.getSequencer();
		// detach default transmitter if there are any
		java.util.List<Transmitter> default_txs=sequencer.getTransmitters();
		default_txs.forEach((t)->{
			t.close();
		});
		Transmitter seqTx=sequencer.getTransmitter();
		outPort=outDevice.getReceiver();
		seqTx.setReceiver(outPort);
		sequencer.open();
	}
	public void play(String path) throws InvalidMidiDataException, IOException, MidiUnavailableException {
		File midFile=new File(path);
		Sequence midSeq=MidiSystem.getSequence(midFile);
		if(resetSequence!=null) {
			sendReset();
		}
		sequencer.setSequence(midSeq);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) { }
		long us=sequencer.getMicrosecondLength();
		sequencer.start();
		try {
			Thread.sleep(us/1000);
		} catch (InterruptedException e) {
		} finally {
			while(sequencer.isRunning()) {
				try { Thread.sleep(1000); } catch (InterruptedException e) {}
			}
			sequencer.stop();
		}
	}
	public void close() {
		sequencer.close();
		outDevice.close();
	}
	private static String stringOfMidDevPorts(int x) {
		if(x==-1) { return "Unlimited"; } else return String.valueOf(x);
	}
	public static void dumpMidiDevices(boolean dumpAll) {
		MidiDevice.Info[] devInfo=MidiSystem.getMidiDeviceInfo();
		for(int i=0; i<devInfo.length; i++) {
			MidiDevice.Info nfo=devInfo[i];
			try {
				MidiDevice dev = MidiSystem.getMidiDevice(nfo);
				int maxRx=dev.getMaxReceivers();
				int maxTx=dev.getMaxTransmitters();
				if(dumpAll || maxRx!=0) {
					System.out.println("Dev "+String.valueOf(i)+" "+nfo.getVendor()+" "+nfo.getName()+" "+nfo.getVersion());
					System.out.println("    "+nfo.getDescription());
					System.out.println("    Source: "+stringOfMidDevPorts(maxTx)+" Sink: "+stringOfMidDevPorts(maxRx));
				}
			} catch (MidiUnavailableException e) {
				System.out.println("Dev "+String.valueOf(i)+" unavailable");
				continue;
			}
		}
		return;
	}
	/** parse options and invoke real main loop */
	public static void main(String[] args) {
		Options options=new Options();
		options.addOption("l",false,"List MIDI devices");
		options.addOption("a",false,"when supplied with -l, list all MIDI devices");
		options.addOption(Option.builder("p").hasArg(true).type(Number.class)
			.desc("Output port (index shown with -l option)").build());
		options.addOption(Option.builder("s").hasArg(true).type(String.class)
			.desc("Soundfont (path to .sf2 file)").build());
		options.addOption(Option.builder("r").hasArg(true).type(String.class)
			.desc("Reset method: one of (gm gs sc88 xg mu100 doc mt32 fb01)").build());
		CommandLineParser parser=new DefaultParser();
		try{
			CommandLine line=parser.parse(options,args);
			// List MIDI devices
			if(line.hasOption('l')){
				dumpMidiDevices(line.hasOption('a'));
			}
			// Get output MIDI port number
			int outPortID=-1;
			if(line.hasOption('p')){
				outPortID=((Number)line.getParsedOptionValue("p")).intValue();
			}
			// Get SoundFont path
			String soundBankPath=null;
			if(line.hasOption('s')){
				soundBankPath=(String)line.getParsedOptionValue("s");
			}
			// Get Reset Type
			String resetType=null;
			if(line.hasOption('r')){
				resetType=(String)line.getParsedOptionValue("r");
			}
			mainLoop(outPortID,resetType,soundBankPath,line.getArgs());
		}catch(ParseException e){
			System.err.println("Parsing failed. Reason: "+e.getMessage());
			HelpFormatter formatter=new HelpFormatter();
			formatter.printHelp("PlaySMF",options);
			System.exit(1);
		}
		System.exit(0);
	}
	/** play smf files */
	static void mainLoop(int outPortId,String resetType,String soundBankPath,String[] smfFiles){
		PlaySMF ps=new PlaySMF(outPortId,resetType);
		try {
			ps.prepare(soundBankPath);
		} catch (Exception e) {
			System.err.println(e.toString());
		}
		for(String smf:smfFiles){
			try {
				ps.play(smf);
			} catch (Exception e) {
				System.err.println(e.toString());
			}
		}
		ps.close();
	}
	private void sendReset() {
		for(byte[] sysex: resetSequence) {
			SysexMessage sm;
			try {
				sm=new SysexMessage(sysex,sysex.length);
				outPort.send(sm,-1);
				Thread.sleep(50);
			} catch (InvalidMidiDataException e) {
				e.printStackTrace();
				return;
			} catch (InterruptedException e) {}
		}
	}
	public static byte[][] chooseResetSequenceByName(String seqName) {
		if(seqName.equalsIgnoreCase("gm")) {
			return RESET_GM;
		} else if(seqName.equalsIgnoreCase("gs")) {
			return RESET_GS;
		} else if(seqName.equalsIgnoreCase("xg")) {
			return RESET_XG;
		} else if(seqName.equalsIgnoreCase("mu100")) {
			return RESET_MU100;
		} else if(seqName.equalsIgnoreCase("doc")) {
			return RESET_DOC;
		} else if(seqName.equalsIgnoreCase("mt32")) {
			return RESET_MT32;
		} else if(seqName.equalsIgnoreCase("fb01")) {
			return RESET_FB01;
		} else if(seqName.equalsIgnoreCase("sc88")) {
			return RESET_SC88;
		} else {
			throw new RuntimeException("Unknown reset sequence specified");
		}
	}
}
