# easyplc
An older project to integrate AVR/Arduino with PLC systems

The work is broken down into three major parts - the Android UI, using the Android Accessory Mode to talk to the Arduino (with an accessory mode USB port), and then the hardware components to interface to the PLC bus.

So there are two chunks of code (one for Android, one for Arduino).  I'm using a library that I modified to talk using the fairly standard PLC protocols and using RS485 if I'm remembering correctly at the hardware level.

There's also some kicad stuff that was an aborted attempt to make a dedicated board to hold the special components for the project.

If I was doing it over again, I would likely replace the Arduino and Android interface with standard wireless networking.  That may not work in all use cases.  Getting rid of the Accessory mode stuff though makes this project a lot simpler.
