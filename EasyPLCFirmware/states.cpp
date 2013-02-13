/*
 * states.c
 *
 *  Created on: Jan 12, 2013
 *      Author: stock
 */

#include "Arduino.h"
#include "easy_plc_firmware.h"
#include "states.h"
#include "modbus.h"
#include "relays.h"

boolean sdready, isexecute, y_online, z_online;
const uint8_t MSG_POSITION = 0x00;

void runState() {
	uint16_t scratch_size;
	byte scratch[20];

	switch (state) {
	case STATE_READY:
		if (isexecute) {
			state = STATE_EXECUTE;
			break;
		}
		if (acc.isConnected()) {
			state = STATE_READCMD;
			break;
		}
		if (digitalRead(execute_button) == LOW) {
			state = STATE_INIT_EXECUTE;
			break;
		}
		break;
	case STATE_CMD_ALARM_RESET:
		sendReset();
		clearDrive();
		state = STATE_READY;
		break;
	case STATE_CMD_INIT:
		sendSvon();
		state = STATE_CMD_HOME;
		break;
	case STATE_CMD_HOME:
		sendSetup();
		state = STATE_READY;
		break;
	case STATE_CMD_ECHO:
		sendAck();
		state = STATE_READY;
		break;
	case STATE_READCMD:
		if (!acc.isConnected()) {
			state = STATE_READY;
			break;
		}
		checkPosition();
		state = readCommand();
		break;
	case STATE_INIT_UPLOAD:
		if (!sdready && !SD.begin(sd_select)) {
			state = STATE_SD_ERROR;
			break;
		}
		sdready = true;
		SD.remove("active.txt"); // Without this, it appends the to existing file
		dataFile = SD.open("active.txt", FILE_WRITE);
		if (!dataFile) {
			state = STATE_SD_ERROR;
			break;
		}
		status_led(0, 0, 1);
		state = STATE_UPLOAD;
		break;
	case STATE_UPLOAD:
		if (upload_count <= 0) {
			dataFile.close();
			status_led(0, 0, 0);
			state = STATE_READY;
			break;
		}
		scratch_size = 20;
		scratch_size = acc.read(scratch, scratch_size, 1);
		if (scratch_size > 0) {
			dataFile.write(scratch, scratch_size);
			upload_count -= scratch_size;
			break;
		}
		break;
	case STATE_SD_ERROR:
		status_led(1, 0, 0);
		delay(3000);
		status_led(0, 1, 0);
		state = STATE_READY;
		break;
	case STATE_INIT_EXECUTE:
		if (!sdready && !SD.begin(sd_select)) {
			state = STATE_SD_ERROR;
			break;
		}
		sdready = true;
		dataFile = SD.open("active.txt");
		if (!dataFile) {
			Serial.println("failed to open datafile");
			state = STATE_SD_ERROR;
			break;
		}
		status_led(0, 0, 1);
		isexecute = true;
		state = STATE_EXECUTE;
		break;
	case STATE_EXECUTE:
		if (!dataFile.available()) {
			dataFile.close();
			status_led(0, 0, 0);
			isexecute = false;
			state = STATE_READY;
			break;
		}
		cmd[1] = dataFile.read();
		state = STATE_PROCESS_COMMAND;
		break;
	case STATE_PROCESS_COMMAND:
		// Two high order bits determine axis or relay control:
		// 00xxxxxx - x axis step
		// 01xxxxxx - y axis step
		// 10xxxxxx - z axis step
		// 11xxxxxx - relay trigger (runs until linked limit switch trips)
		switch ((cmd[1] >> 6) & 0x03) {
		case 0: // x
			Serial.print("X axis step: ");
			Serial.println(cmd[1] & 0x3f, DEC);
			active_node = nodex;
			if (sendMoveCommand(active_node, cmd[1] & 0x3f))
				state = STATE_MOVE_WAIT_BUSY_ON;
			else
				state = STATE_READY;
			break;
		case 1: // y
			if (!y_online) {
				state = STATE_READY;
				break;
			}
			Serial.print("Y axis step: ");
			Serial.println(cmd[1] & 0x3f, DEC);
			active_node = nodey;
			if (sendMoveCommand(active_node, cmd[1] & 0x3f))
				state = STATE_MOVE_WAIT_BUSY_ON;
			else
				state = STATE_READY;
			break;
		case 2: // z
			if (!z_online) {
				state = STATE_READY;
				break;
			}
			Serial.print("Z axis step: ");
			Serial.println(cmd[1] & 0x3f, DEC);
			active_node = nodez;
			if (sendMoveCommand(active_node, cmd[1] & 0x3f))
				state = STATE_MOVE_WAIT_BUSY_ON;
			else
				state = STATE_READY;
			break;
		case 3: // relay
			Serial.print("Relay command: ");
			Serial.println(cmd[1] & 0x3f, DEC);
			state = STATE_RELAY_COMMAND;
			break;
		default:
			Serial.print("Unknown command: ");
			Serial.println(cmd[1], HEX);
			state = STATE_READY;
			break;
		}
		break;
	case STATE_MOVE_WAIT_BUSY_ON:
		if (millis() - timein_state > 5000) {
			state = STATE_TIMEOUT;
			break;
		}
		if (!checkBusy(active_node))
			break;
		active_node.writeSingleCoil(0x001a, 0x00);
		state = STATE_MOVE_WAIT_INP_ON;
		break;
	case STATE_MOVE_WAIT_INP_ON:
		if (millis() - timein_state > 5000) {
			state = STATE_TIMEOUT;
			break;
		}
		if (!checkInp(active_node))
			break;
		state = STATE_MOVE_WAIT_BUSY_OFF;
		break;
	case STATE_MOVE_WAIT_BUSY_OFF:
		if (checkBusy(active_node))
			break;
		state = STATE_READY;
	case STATE_RELAY_COMMAND:
		relayCommand();
		break;
	case STATE_TIMEOUT:
		clearDrive();
		reportStatus(active_node);
		state = STATE_READY;
		break;
	case STATE_RELAY_EXTEND_WAIT:
		relayExtendWait();
		break;
	case STATE_RELAY_RETRACT_WAIT:
		relayRetractWait();
		break;
	case STATE_RELAY_STOP:
		relayStop();
		break;
	case STATE_RELAY_ERROR:
		relayError();
		break;
	}
}

/*
 * Axis - specific helper functions.
 */
int sendPosition(uint8_t axis, int oldpos, int newpos) {
	uint8_t scratch[6];

	if (oldpos != newpos) {
		scratch[5] = newpos & 0xff;
		newpos = newpos >> 8;
		scratch[4] = newpos & 0xff;
		newpos = newpos >> 8;
		scratch[3] = newpos & 0xff;
		newpos = newpos >> 8;
		scratch[2] = newpos & 0xff;
		scratch[1] = axis;
		scratch[0] = MSG_POSITION;
		acc.write(scratch, 6);
		return newpos;
	}
	return oldpos;
}

// Just send an empty command reply - used as a keepalive
void sendAck() {
	uint8_t scratch[1] = { 'e' };
	acc.write(scratch, 1);
}

void checkPosition() {
	if (millis() - timein_status > 500) {
		x = sendPosition(0, x, getPosition(nodex));
		if (y_online)
			y = sendPosition(1, y, getPosition(nodey));
		if (z_online)
			z = sendPosition(2, z, getPosition(nodez));
		timein_status = millis();
	}
}

