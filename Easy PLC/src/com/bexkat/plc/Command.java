package com.bexkat.plc;

public class Command {
	private long id;
	private long program;
	private String command;
	
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

	public String getCommand() {
		return command;
	}
	
	public void setCommand(String command) {
		this.command = command;
	}
	
	public String toString() {
		return command;
	}
}