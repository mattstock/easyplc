#include "easy_plc_firmware.h"

void status_led(int r, int g, int b);

AndroidAccessory acc("Bexkat Systems LLC", "PLC Operator",
		"PLC Operator Control Module", "1.0", "http://www.bexkat.com/",
		"00337366235678");

File dataFile;

uint16_t result;
char cmd[20];
int cmd_size = 0;
enum states state_prev, state;
int upload_count;
long timein_state, timein_status;
int x, y, z;
uint8_t buttons;

void setup() {
	// Debugging serial
	Serial.begin(115200);
	nodex.begin(38400);
	nodey.begin(38400);
	nodez.begin(38400);

	// Needed for SD card
	pinMode(sd_select, OUTPUT);
	digitalWrite(sd_select, HIGH);
	pinMode(53, OUTPUT);

	// Relays
	pinMode(relay_air, OUTPUT);
	pinMode(relay_extend_mould, OUTPUT);
	pinMode(relay_retract_mould, OUTPUT);
	digitalWrite(relay_air, LOW);
	digitalWrite(relay_extend_mould, LOW);
	digitalWrite(relay_retract_mould, LOW);

	// Relay endstops (normally open, with internal pullup)
	pinMode(relay_extend_stop, INPUT);
	pinMode(relay_retract_stop, INPUT);
	digitalWrite(relay_extend_stop, HIGH);
	digitalWrite(relay_retract_stop, HIGH);

	// Execute button
	pinMode(execute_button, INPUT);
	digitalWrite(execute_button, HIGH);

	// RGB LED
	pinMode(led_red, OUTPUT);
	pinMode(led_green, OUTPUT);
	pinMode(led_blue, OUTPUT);
	status_led(0, 0, 0);

	// Pay attention to USB events.
	acc.powerOn();

	x_online = true;
	y_online = true;
	z_online = true;

	enableSerial();
	state = STATE_CMD_INIT;
	state_prev = STATE_READY;
	sdready = false;
	isexecute = false;
}

void loop() {

	// Print out the current state
	if (state != state_prev) {
		Serial.print(timein_state, DEC);
		Serial.print(" : ");
		Serial.println(state);
		state_prev = state;
		timein_state = millis();
	}

	if (x_online && checkAlarm(nodex)) {
		Serial.print("X ");
		printAlarm(nodex);
		state = STATE_CMD_ALARM_RESET;
	}

	if (y_online && checkAlarm(nodey)) {
		Serial.print("Y ");
		printAlarm(nodey);
		state = STATE_CMD_ALARM_RESET;
	}

	if (z_online && checkAlarm(nodez)) {
		Serial.print("Z ");
		printAlarm(nodez);
		state = STATE_CMD_ALARM_RESET;
	}

	status_led(1, 1, 1);
	runState();
	status_led(0, 0, 0);
}

enum states readCommand() {
	uint16_t scratch_size = 32;
	byte scratch[32];

    scratch_size = acc.read(scratch, scratch_size, 1);

	for (int i = 0; (i < scratch_size) && (cmd_size < sizeof(cmd)); i++)
		cmd[cmd_size++] = scratch[i];
	if (cmd_size == 0)
		return STATE_READCMD;
	switch (cmd[0]) {
	case 'e': // Simple echo to work around ADK bug
		cmd_size = 0;
		return STATE_CMD_ECHO;
	case 'r':
		cmd_size = 0;
		return STATE_CMD_ALARM_RESET;
	case 'i':
		cmd_size = 0;
		return STATE_CMD_INIT;
	case 'h':
		cmd_size = 0;
		return STATE_CMD_HOME;
	case 'd':
		if (cmd_size < 3)
			break;
		upload_count = ((cmd[1] << 8) | cmd[2]);
		cmd_size = 0;
		return STATE_INIT_UPLOAD;
	case 'c':
		if (cmd_size < 2)
			break;
		cmd_size = 0;
		return STATE_PROCESS_COMMAND;
	default:
		cmd_size = 0;
		return STATE_READCMD;
	}
	return STATE_READCMD;
}

// Set the tristate LED
void status_led(int r, int g, int b) {
	digitalWrite(led_red, (r == 1 ? HIGH : LOW));
	digitalWrite(led_green, (g == 1 ? HIGH : LOW));
	digitalWrite(led_blue, (b == 1 ? HIGH : LOW));
}

