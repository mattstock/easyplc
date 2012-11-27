package com.bexkat.plc.compiler;

public class MoveResult {
	private byte axis;
	private byte stepval;
	
	public MoveResult(byte axis) {
		this.axis = axis;
	}
	
	public MoveResult(byte axis, int stepval) {
		this.axis = axis;
		this.stepval = (byte) (stepval & 0xff);
	}

	public void setAxis(byte axis) {
		this.axis = axis;
	}

	public void setStepval(byte stepval) {
		this.stepval = stepval;
	}

	public Byte getByte() {
		return new Byte((byte) (axis|stepval));
	}
}
