// Only modify this file to include
// - function definitions (prototypes)
// - include files
// - extern variable definitions
// In the appropriate section

#ifndef easy_plc_firmware_H_
#define easy_plc_firmware_H_
#include "Arduino.h"
//add your includes for the project easy_plc_firmware here


//end of add your includes here
#ifdef __cplusplus
extern "C" {
#endif

void loop();
void setup();
void status_led(int r, int g, int b);

#ifdef __cplusplus
} // extern "C"
#endif

#include "ModbusMaster.h"
#include "SD.h"
#include "AndroidAccessory.h"
#include "states.h"
#include "modbus.h"

#define sd_select 48 // prod is 23
#define led_red 11
#define led_green 12
#define led_blue 13
#define relay_extend_mould 38
#define relay_retract_mould 36
#define relay_air 34
#define relay_extend_stop 24
#define relay_retract_stop 26
#define execute_button 40

extern uint16_t result;
extern char cmd[];
extern enum states state_prev,state;
extern long timein_state, timein_status;
extern AndroidAccessory acc;
extern int x,y,z;
extern int upload_count;
extern File dataFile;

#endif /* easy_plc_firmware_H_ */
