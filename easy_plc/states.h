enum states {
  STATE_READY = 0,
  STATE_READCMD,
  STATE_EXECUTE,
  STATE_CMD_ALARM_RESET,
  STATE_CMD_HOME,
  STATE_CMD_INIT,
  STATE_INIT_UPLOAD,
  STATE_PROCESS_COMMAND,
  STATE_SD_ERROR,
  STATE_UPLOAD,
  STATE_INIT_EXECUTE,
  STATE_MOVE_WAIT_BUSY_ON,
  STATE_MOVE_WAIT_BUSY_OFF,
  STATE_MOVE_WAIT_INP_ON,
  STATE_TIMEOUT,
  STATE_RELAY_COMMAND,
  STATE_RELAY_EXTEND_WAIT,
  STATE_RELAY_RETRACT_WAIT,
  STATE_RELAY_STOP,
  STATE_RELAY_ERROR
};

enum states readCommand();

