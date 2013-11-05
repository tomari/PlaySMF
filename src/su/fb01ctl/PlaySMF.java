// Plays standard MIDI file after resetting the synthesizer
package su.fb01ctl;

import java.io.File;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;

public class PlaySMF {
	private static final String help=
			"PlaySMF [-p dev] [-r gm|gs|xg|mu100|doc|cm32|fb01] 1.mid 2.mid 3.mid ...";
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
	public static final byte[][] RESET_CM32={
		{(byte)0xF0,0x41,0x10,0x16,0x11,0x7f,0x00,0x00,0x01,0x00,(byte)0xf7}}; // MT-32 Reset all
	public static final byte[][] RESET_FB01={
		{(byte)0xF0,0x43,0x75,0x00,0x20,0x40,0x11,(byte)0xf7}}; // Store configuration
	
	public static void main(String[] args) {
		byte[][] resetSequence={};
		MidiDevice.Info[] devInfo=MidiSystem.getMidiDeviceInfo();
		int argptr=0;
		if(args.length==0) {
			System.err.println(help);
			for(int i=0; i<devInfo.length; i++) {
				System.out.println("Dev "+String.valueOf(i)+" "+devInfo[i].toString());
				System.out.println("    "+devInfo[i].getDescription());
			}
			System.exit(1);
			return;
		}
		MidiDevice outPort;
		try {
			outPort=MidiSystem.getSynthesizer();
			while(argptr<args.length) {
				if(args[argptr].equals("-p")) {
					int x=Integer.valueOf(args[argptr+1]);
					outPort=MidiSystem.getMidiDevice(devInfo[x]);
					argptr+=2;
				} else if(args[argptr].equals("-r")) {
					final String resetType=args[argptr+1];
					resetSequence=chooseResetSequenceByName(resetType);
					argptr+=2;
				} else {
					break;
				}
			}
			outPort.open();
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println(help);
			System.exit(1);
			return;
		} catch (MidiUnavailableException e) {
			System.err.println(e.toString());
			System.exit(1);
			return;
		}
		// connect sequencer to the synthesizer
		Sequencer seq;
		Receiver synRx;
		try {
			seq=MidiSystem.getSequencer();
			Transmitter seqTx=seq.getTransmitter();
			synRx=outPort.getReceiver();
			seqTx.setReceiver(synRx);
		} catch (MidiUnavailableException e) {
			System.err.println(e.toString());
			System.exit(1);
			return;
		}
		for(int i=argptr; i<args.length; i++) {
			try{
				File midFile=new File(args[i]);
				Sequence midSeq=MidiSystem.getSequence(midFile);
				sendReset(synRx,resetSequence);
				seq.open();
				seq.setSequence(midSeq);
				long us=seq.getMicrosecondLength();
				seq.start();
				Thread.sleep(us/1000);
				seq.stop();
				seq.close();
			} catch (Exception e) {
				System.err.println(e.toString());
			}
		}
		outPort.close();
	}
	static private void sendReset(Receiver rcv, byte[][] resetSequence) throws InvalidMidiDataException {
		for(int i=0; i<resetSequence.length; i++) {
			SysexMessage sm=new SysexMessage(resetSequence[i],resetSequence[i].length);
			rcv.send(sm,-1);
		}
	}
	static private byte[][] chooseResetSequenceByName(String seqName) {
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
		} else if(seqName.equalsIgnoreCase("cm32")) {
			return RESET_CM32;
		} else if(seqName.equalsIgnoreCase("fb01")) {
			return RESET_FB01;
		} else {
			System.err.println("Unknown reset sequence name specified.");
			return RESET_GM;
		}
	}
}
