/*
 * relays.c
 *
 *  Created on: Jan 12, 2013
 *      Author: stock
 */

#include "Arduino.h"
#include "easy_plc_firmware.h"

void relayCommand() {
      switch (0x0f & cmd[1]) {
        case 0:
          digitalWrite(relay_air, HIGH);
          state = STATE_READY;
          break;
        case 1:
          digitalWrite(relay_air, LOW);
          state = STATE_READY;
          break;
        case 2:
          digitalWrite(relay_extend_mould, HIGH);
          digitalWrite(relay_retract_mould, LOW);
          state = STATE_RELAY_EXTEND_WAIT;
          break;
        case 3:
          digitalWrite(relay_extend_mould, LOW);
          digitalWrite(relay_retract_mould, HIGH);
          state = STATE_RELAY_RETRACT_WAIT;
          break;
       default:
          state = STATE_READY;
          break;
      }
}

void relayExtendWait() {
      if (millis()-timein_state > 5000) {
        state = STATE_RELAY_STOP;
        return;
      }
      if (digitalRead(relay_extend_stop) == LOW) {
        digitalWrite(relay_extend_mould, LOW);
        state = STATE_READY;
      }
}

void relayRetractWait() {
      if (millis()-timein_state > 5000) {
        state = STATE_RELAY_STOP;
        return;
      }
      if (digitalRead(relay_retract_stop) == LOW) {
        digitalWrite(relay_retract_mould, LOW);
        state = STATE_READY;
      }
}

void relayStop() {
      digitalWrite(relay_extend_mould, LOW);
      digitalWrite(relay_retract_mould, LOW);
      status_led(1,0,0);
      state = STATE_RELAY_ERROR;
}

void relayError() {
      if (digitalRead(execute_button) == LOW) {
        status_led(0,0,0);
        state = STATE_READY;
      }
}


