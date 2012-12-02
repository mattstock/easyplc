package com.bexkat.plc.compiler;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.bexkat.plc.Command;

public class ByteCompiler {
	public static final String TAG = "ByteCompiler";
	public static final byte MASK_X = 0x00;
	public static final byte MASK_Y = 0x40;
	public static final byte MASK_Z = (byte) 0x80;
	public static final byte MASK_RELAY = (byte) 0xc0;
	public static final byte END_BYTE = (byte) 0xff;
	private static final float[] STEPS = { 
		0, 50, -0.5f, -1, -2, -3, -5, -7,
		-10, -20, 0.5f, 1, 2, 3, 5, 7,
		10, 20 };
	
	public static List<MoveResult> compile(List<Command> cmds) {
		ArrayList<MoveResult> stepSeq = new ArrayList<MoveResult>();
		float x = 0, y = 0, z = 0;
		// ABS move to origin (step 0) starts all command sequences.
		stepSeq.add(new MoveResult(MASK_X, 0));
		stepSeq.add(new MoveResult(MASK_Y, 0));
		stepSeq.add(new MoveResult(MASK_Z, 0));
		for (Command c: cmds) {
			Log.d(TAG, c.toString());
			switch (c.getCommand()) {
			case Command.TYPE_POS:
				stepSeq.addAll(generateSteps(MASK_Z, c.getZ()-z));
				stepSeq.addAll(generateSteps(MASK_Y, c.getY()-y));
				stepSeq.addAll(generateSteps(MASK_X, c.getX()-x));
				x = c.getX();
				y = c.getY();
				z = c.getZ();
				break;
			case Command.TYPE_RELAY:
				stepSeq.add(generateRelay(c.getState(), c.getRelay()));
				break;
			default:
				return null;
			}
		}
		
		return stepSeq;
	}

	public static List<MoveResult> moveStep(byte axis, float delta) {
		return generateSteps(axis, delta);
	}
	
	public static Byte relayByte(int state, int relay) {
		return generateRelay(state, relay).getByte();
	}
	
	private static MoveResult generateRelay(int state, int relay) {
		Log.d(TAG, "generateRelay("+state+","+relay+")");
		if (relay == Command.RELAY_AIR) {
			if (state == 1)
				return new MoveResult(MASK_RELAY, 0);
			else
				return new MoveResult(MASK_RELAY, 1);
		}
		if (relay == Command.RELAY_MOULD) {
			if (state == 1)
				return new MoveResult(MASK_RELAY, 2);
			else
				return new MoveResult(MASK_RELAY, 3);			
		}
		return new MoveResult(MASK_RELAY, 1); // kind of a nop
	}
	
	// A little tail recursion
	private static List<MoveResult> generateSteps(byte axis, float delta) {
		ArrayList<MoveResult> scratch = new ArrayList<MoveResult>();
		int best = 2; // Start at step 2
		float rem = delta-STEPS[best];
		
		if (delta == 0)
			return scratch;
		
		for (int i=3; i < STEPS.length; i++) {
			if (Math.abs(delta-STEPS[i]) < Math.abs(rem)) {
				rem = delta-STEPS[i];
				best = i;
			}
		}
		
		if (Math.abs(rem) >= Math.abs(delta)) {
			Log.d(TAG, "failed to find an better increment - close enough!");
			return scratch;
		}
		
		Log.d(TAG, "best is now " + best + " rem = " + rem);
		scratch.add(new MoveResult(axis, best));
		scratch.addAll(generateSteps(axis, rem));
		return scratch;
	}
}
