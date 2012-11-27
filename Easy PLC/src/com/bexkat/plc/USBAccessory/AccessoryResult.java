package com.bexkat.plc.USBAccessory;

public class AccessoryResult {
	private AccessoryCommandType type;
	private boolean success;
	
	public AccessoryResult(AccessoryCommandType type, boolean success) {
		this.type = type;
		this.success = success;
	}
	
	public AccessoryCommandType getType() {
		return type;
	}
	public void setType(AccessoryCommandType type) {
		this.type = type;
	}
	public boolean isSuccess() {
		return success;
	}
	public void setSuccess(boolean success) {
		this.success = success;
	}
}
