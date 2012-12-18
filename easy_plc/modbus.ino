// Move operations: Set INx bits/Set DRIVE bit, wait for BUSY on/unset DRVIE, wait for INP on, wait for BUSY off 
boolean sendMoveCommand(ModbusMaster axis, long stepno) {
  long r;
  
  axis.setTransmitBuffer(0x00, 0x3f & cmd[1]);
  r = axis.writeMultipleCoils(0x0010, 6);
  if (r == axis.ku8MBResponseTimedOut) {
    Serial.println("Missing axis - ignoring.");
    return false;
  }
  if (r != axis.ku8MBSuccess) {
    Serial.print("Error: ");
    Serial.println(r, HEX);
    return false;
  }
  r = axis.writeSingleCoil(0x001a, 0xff);
  if (r == axis.ku8MBResponseTimedOut) {
    Serial.println("Missing axis - ignoring.");
    return false;
  }
  if (r != axis.ku8MBSuccess) {
    Serial.print("Error: ");
    Serial.println(r, HEX);
    return false;
  }
  return true;
}

void enableSerial() {
  nodex.writeSingleCoil(0x0030,0xff);
  if (y_online)
    nodey.writeSingleCoil(0x0030,0xff);
  if (z_online)
    nodez.writeSingleCoil(0x0030,0xff);
}

void sendReset() {
  nodex.writeSingleCoil(0x001b, 0xff);
  if (y_online)
    nodey.writeSingleCoil(0x001b, 0xff);
  if (z_online)
    nodez.writeSingleCoil(0x001b, 0xff);
  delay(200);
  nodex.writeSingleCoil(0x001b, 0x00);
  if (y_online)
    nodey.writeSingleCoil(0x001b, 0x00);
  if (z_online)
    nodez.writeSingleCoil(0x001b, 0x00);
}

void sendSetup() {
  nodex.writeSingleCoil(0x001c, 0xff);
  if (y_online)
    nodey.writeSingleCoil(0x001c, 0xff);
  if (z_online)
    nodez.writeSingleCoil(0x001c, 0xff);
  delay(200);
  nodex.writeSingleCoil(0x001c, 0x00);
  if (y_online)
    nodey.writeSingleCoil(0x001c, 0x00);
  if (z_online)
    nodez.writeSingleCoil(0x001c, 0x00);
}

void sendSvon() {
  nodex.writeSingleCoil(0x0019, 0xff);
  if (y_online)
    nodey.writeSingleCoil(0x0019, 0xff);
  if (z_online)
    nodez.writeSingleCoil(0x0019, 0xff);
}

void clearDrive() {
  nodex.writeSingleCoil(0x001a, 0x00);
  if (y_online)
    nodey.writeSingleCoil(0x001a, 0x00);
  if (z_online)
    nodez.writeSingleCoil(0x001a, 0x00);
}

boolean checkAlarm(ModbusMaster axis) {
  axis.readDiscreteInputs(0x0040, 0x0010);
  long r = axis.getResponseBuffer(0);
  return (r & 0x8000) != 0;
}  

boolean checkInp(ModbusMaster axis) {
  axis.readDiscreteInputs(0x0040, 0x0010);
  long r = axis.getResponseBuffer(0);
  return (r & 0x0800) != 0;
}  
  
boolean checkBusy(ModbusMaster axis) {
  axis.readDiscreteInputs(0x0040, 0x0010);
  long r = axis.getResponseBuffer(0);
  return (r & 0x0100) != 0;
}

void printDeviceName(ModbusMaster axis) {
  char a;
  
  Serial.print("Device name: ");
  axis.readHoldingRegisters(0x000e,0x0010);
  for (int b=0; b < 8; b++) {
    result = axis.getResponseBuffer(b);
    char a = highByte(result);
    Serial.print(a);
    a = lowByte(result);
    Serial.print(a);
  }
  Serial.println();
}

long getPosition(ModbusMaster axis) {
  long r;
  
  r = axis.readHoldingRegisters(0x9000, 0x000d);
  if (r == axis.ku8MBResponseTimedOut) {
    Serial.println("Missing axis - ignoring.");
    return 0;
  }
  if (r != axis.ku8MBSuccess) {
    Serial.print("Error: ");
    Serial.println(r, HEX);
    return 0;
  }
  return (axis.getResponseBuffer(0) << 16) | axis.getResponseBuffer(1);
}

void printState(ModbusMaster axis) {
  Serial.print("Position: ");
  axis.readHoldingRegisters(0x9000, 0x000d);
  long p = (axis.getResponseBuffer(0) << 16) | axis.getResponseBuffer(1);
  Serial.println(p);
  Serial.print("Speed: ");
  Serial.println(axis.getResponseBuffer(2), DEC);
  Serial.print("Thrust: ");
  Serial.println(axis.getResponseBuffer(3), DEC);
  Serial.print("Target Position: ");
  p = (axis.getResponseBuffer(4) << 16) | axis.getResponseBuffer(5);
  Serial.println(p);
  Serial.print("Step #: ");
  Serial.println(axis.getResponseBuffer(6), DEC);  
}

void printInputs(ModbusMaster axis) {
  char bits[17];
  Serial.println("- JOG+ JOG- SETUP RESET DRIVE SVON HOLD - - IN5 IN4 IN3 IN2 IN1 IN0");
  axis.readCoils(0x0010, 0x0010);
  long r = axis.getResponseBuffer(0);
  for (int i=0; i < 16; i++)
    if (r & (1 << (15-i)))
      bits[i] = '1';
    else 
      bits[i] = '0';
  bits[16] = '\0';
  Serial.println(bits);
  
}

void printOutputs(ModbusMaster axis) {
  char bits[17];
  Serial.println("ALARM ESTOP WAREA AREA INP SETON SVRE BUSY 0 0 OUT5 OUT4 OUT3 OUT2 OUT1 OUT0");
  axis.readDiscreteInputs(0x0040, 0x0010);
  long r = axis.getResponseBuffer(0);
  for (int i=0; i < 16; i++)
    if (r & (1 << (15-i)))
      bits[i] = '1';
    else 
      bits[i] = '0';
  bits[16] = '\0';
  Serial.println(bits);
}

void printDrivingMode(ModbusMaster axis) {
  axis.readCoils(0x0030, 0x0001);
  long r = axis.getResponseBuffer(0);
  if (r & 1)
    Serial.println("Serial drive mode");
  else 
    Serial.println("Parallel drive mode");
}

void printAlarm(ModbusMaster axis) {
    axis.readHoldingRegisters(0x0380, 0x0001);
    long r = axis.getResponseBuffer(0);
    Serial.print("Alarm: ");
    Serial.println((r&0xff00)>>8, DEC);
}

void reportStatus(ModbusMaster axis) {
  printState(axis);
  printOutputs(axis);
  printInputs(axis);
}

