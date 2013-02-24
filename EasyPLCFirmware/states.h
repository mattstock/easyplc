/*
 * states.h
 *
 *  Created on: Jan 12, 2013
 *      Author: stock
 */

#ifndef STATES_H_
#define STATES_H_

extern boolean sdready, isexecute, x_online, y_online, z_online;

enum states {
  STATE_READY = 0,
  STATE_READCMD, // 1
  STATE_EXECUTE, // 2
  STATE_CMD_ALARM_RESET, // 3
  STATE_CMD_HOME, // 4
  STATE_CMD_INIT, // 5
  STATE_CMD_ECHO, // 6
  STATE_INIT_UPLOAD, // 7
  STATE_PROCESS_COMMAND, // 8
  STATE_SD_ERROR, // 9
  STATE_UPLOAD, // 10
  STATE_INIT_EXECUTE, // 11
  STATE_MOVE_WAIT_BUSY_ON, // 12
  STATE_MOVE_WAIT_BUSY_OFF, // 13
  STATE_MOVE_WAIT_INP_ON, // 14
  STATE_TIMEOUT, // 15
  STATE_RELAY_COMMAND, // 16
  STATE_RELAY_EXTEND_WAIT, // 17
  STATE_RELAY_RETRACT_WAIT, // 18
  STATE_RELAY_STOP, // 19
  STATE_RELAY_ERROR, // 20
};

enum states readCommand();
void sendAck();
void runState();
void checkPosition();
int sendPosition(int oldpos, int newpos);

#endif /* STATES_H_ */
