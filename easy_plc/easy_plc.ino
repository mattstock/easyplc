#include <Wire.h>
#include <ModbusMaster.h>
#include <SD.h>
#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>
#include <MemoryFree.h>
#include "states.h"


AndroidAccessory acc("Bexkat Systems LLC",
                     "PLC Operator",
                     "PLC Operator Control Module",
                     "1.0",
                     "http://www.bexkat.com/",
                     "00337366235678");

File dataFile;

const int sd_select = 48; // prod is 23
const int led_red = 8;
const int led_green = 9;
const int led_blue = 10;
const int relay_extend_mould = 38;
const int relay_retract_mould = 36;
const int relay_air = 34;
const int relay_extend_stop = 24;
const int relay_retract_stop = 26;
const int execute_button = 40;

uint16_t result;
char cmd[20];
int cmd_size = 0;
enum states state_prev,state;
int upload_count;
boolean sdready, isexecute, y_online, z_online;
long timein_state, timein_status;
int x,y,z;
uint8_t buttons;
ModbusMaster active_node;
ModbusMaster nodex(1,1); // Serial 1, slave 1
ModbusMaster nodey(1,2); // Serial 1, slave 2
ModbusMaster nodez(1,3); // Serial 1, slave 3

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
  status_led(0,0,0);
  
  // Pay attention to USB events.
  acc.powerOn();

  enableSerial();
  clearDrive();
  state = STATE_CMD_INIT;
  state_prev = STATE_READY;
  sdready = false;
  isexecute = false;
  y_online = false;
  z_online = false;
}

void loop() {
  // Print out the current state
  if (state != state_prev) {
    Serial.print(timein_state, DEC);
    Serial.print(" : ");
    Serial.println(state);
    state_prev = state;
    Serial.print("Free memory: ");
    Serial.println(freeMemory());
    timein_state = millis();
  }


  if (checkAlarm(active_node)) {
    printAlarm(active_node);
    state = STATE_CMD_ALARM_RESET;
  }    
  
  status_led(1,1,1);  
  runState();
  status_led(0,0,0);
}

enum states readCommand() {
  int scratch_size;
  byte scratch[32];
  
  scratch_size = acc.read(scratch, sizeof(scratch), 1);
  for (int i=0; (i < scratch_size) && (cmd_size < sizeof(cmd)); i++)
    cmd[cmd_size++] = scratch[i];
  if (cmd_size == 0)
    return STATE_READCMD;
  Serial.print("command size = ");
  Serial.print(cmd[0]);
  Serial.print(": ");
  Serial.println(cmd_size);
  switch (cmd[0]) {
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
  digitalWrite(led_red,(r == 1 ? HIGH : LOW));
  digitalWrite(led_green, (g == 1 ? HIGH : LOW));
  digitalWrite(led_blue, (b == 1 ? HIGH : LOW));
}

/*
 * Axis - specific helper functions.
 */
int sendPosition(int oldpos, int newpos) {
  char scratch[5];
  
  if (oldpos != newpos) {
      scratch[4] = (byte) (newpos & 0xff);
      newpos = newpos >> 8;
      scratch[3] = (byte) (newpos & 0xff);
      newpos = newpos >> 8;
      scratch[2] = (byte) (newpos & 0xff);
      newpos = newpos >> 8;
      scratch[1] = (byte) (newpos & 0xff);
      scratch[0] = 0;
      acc.write(scratch, 5);
      return newpos;
  }
  return oldpos;
}
 
void checkPosition() {
  if (millis()-timein_status > 500) {
    x = sendPosition(x, getPosition(nodex));
    if (y_online)
      y = sendPosition(y, getPosition(nodey));
    if (z_online)
      z = sendPosition(z, getPosition(nodez));
    timein_status = millis();
  }
}
 


