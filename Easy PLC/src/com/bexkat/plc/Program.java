package com.bexkat.plc;

import java.util.ArrayList;
import java.util.List;

public class Program {
	private long id;
	private String name;
	private String description;
	private ArrayList<Command> commands;
	private byte[] compile;
	
	public Program() {
		this.commands = new ArrayList<Command>();
	}
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public List<Command> getCommands() {
		return commands;
	}

	public void addCommand(Command command) {
		command.setProgram(this.id);
		this.commands.add(command);
	}
	
	public String toString() {
		return name + ": " + description;
	}
	
	// TODO need to convert from command to a sequence of byte codes using the step values available
	public void compile() {
		ArrayList<Byte> scratch = new ArrayList<Byte>();
		boolean firstPoint = true;
		float x,y,z;
		
		for (Command c: commands) {
			switch (c.getCommand()) {
			case Command.TYPE_POS:
				if (firstPoint) { // 
					
				}
			}
		}
		
		
	}
}
