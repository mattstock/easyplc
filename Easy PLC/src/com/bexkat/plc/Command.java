package com.bexkat.plc;

public class Command {
	public static final int RELAY_AIR = 0;
	public static final int RELAY_MOULD = 1;
	public static final int TYPE_POS = 0;
	public static final int TYPE_RELAY = 1;
	private long id, program;
	private int command;
	private int state;
	private int relay;
	private float x, y, z;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getProgram() {
		return program;
	}

	public void setProgram(long program) {
		this.program = program;
	}

	public int getCommand() {
		return command;
	}

	public void setCommand(int command) {
		this.command = command;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public int getRelay() {
		return relay;
	}

	public void setRelay(int relay) {
		this.relay = relay;
	}

	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}

	public float getZ() {
		return z;
	}

	public void setZ(float z) {
		this.z = z;
	}

	public String toString() {
		String s, r;
		
		switch (command) {
		case TYPE_POS:
			return String.format("Move %.4f, %.4f, %.4f", x, y, z);
		case TYPE_RELAY:
			if (state == 1)
				s = "forward";
			else
				s = "reverse";
			switch (relay) {
			case RELAY_AIR:
				r = "Air";
				break;
			case RELAY_MOULD:
				r = "Mould";
				break;
			default:
				r = "err";
			}	
			return r + " relay " + s;
		default:
			return "Invalid command";
		}
	}	
}