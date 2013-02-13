package com.bexkat.plc.USBAccessory;

public class AccessoryResult {
	private int position = 0;
	private int axis = -1;
	private int type = 0;

	public AccessoryResult(byte[] res) {
		type = res[0];

		switch (type) {
		case 0: // MSG_POSITION
			axis = res[1];

			position = 0;
			for (int i = 2; i < 6; i++) {
				position = position << 8;
				position = position | (res[i] & 0xff);
			}
			break;
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
