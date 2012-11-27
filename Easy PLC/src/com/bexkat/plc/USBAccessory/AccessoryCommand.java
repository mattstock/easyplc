package com.bexkat.plc.USBAccessory;

import java.util.List;

import com.bexkat.plc.compiler.MoveResult;

public class AccessoryCommand {
	private AccessoryCommandType type;
	private byte[] data;
	
	public AccessoryCommand(AccessoryCommandType type, List<MoveResult> data) {
		int i = 0;
		this.type = type;
		this.data = new byte[data.size()];
		for (MoveResult r: data)
			this.data[i++] = r.getByte();
	}

	public AccessoryCommand(AccessoryCommandType type, byte data) {
		this.type = type;
		this.data = new byte[1];
		this.data[0] = data;
	}
	
	public AccessoryCommand(AccessoryCommandType type) {
		this.type = type;
		this.data = null;
	}

	public AccessoryCommandType getType() {
		return type;
	}

	public void setType(AccessoryCommandType type) {
		this.type = type;
	}

	public byte[] getData() {
		return data;
	}
}
