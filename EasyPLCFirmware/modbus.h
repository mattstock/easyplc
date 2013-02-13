/*
 * modbus.h
 *
 *  Created on: Jan 12, 2013
 *      Author: stock
 */

#ifndef MODBUS_H_
#define MODBUS_H_

#include "Arduino.h"
#include "ModbusMaster.h"

#ifdef __cplusplus
extern "C" {
#endif

boolean sendMoveCommand(ModbusMaster axis, long stepno);
void enableSerial();
void sendReset();
void sendSetup();
void sendSvon();
void clearDrive();
boolean checkAlarm(ModbusMaster axis);
boolean checkInp(ModbusMaster axis);
boolean checkBusy(ModbusMaster axis);
void printDeviceName(ModbusMaster axis);
long getPosition(ModbusMaster axis);
void printState(ModbusMaster axis);
void printInputs(ModbusMaster axis);
void printOutputs(ModbusMaster axis);
void printDrivingMode(ModbusMaster axis);
void printAlarm(ModbusMaster axis);
void reportStatus(ModbusMaster axis);

#ifdef __cplusplus
} // extern "C"
#endif

extern ModbusMaster active_node, nodex, nodey, nodez;

#endif /* MODBUS_H_ */
