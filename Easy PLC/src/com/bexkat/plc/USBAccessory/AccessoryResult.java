package com.bexkat.plc.USBAccessory;

public class AccessoryResult {
	private int position;
	private int axis;
	
	public AccessoryResult(byte[] res) {
		axis = res[0];
		
		position = 0;
		for (int i=1; i < 5; i++) {
			position = position << 8;
			position = position | (res[i] & 0xff);
		}
	}

	public int getAxis() {
		return axis;
	}
	
	public int getPosition() {
		return position;
	}
	
	public void setPosition(int position) {
		this.position = position;
	}
}
