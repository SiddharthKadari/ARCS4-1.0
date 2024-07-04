#include <string.h>

// the setup function runs once when you press reset or power the board
void setup() {
  // initialize digital pin LED_BUILTIN as an output.
  pinMode(LED_BUILTIN, OUTPUT);
  Serial.begin(115200);
  delay(800);
  serialSend("Arduino Start Message");
  delay(200);
}

void loop(){
  if(Serial.available()){
    int d = Serial.read();
    serialSend("0");
    blink(d-48);
  }
}

void blink(int b) {
  for(int i = 0; i < b; i++){
    digitalWrite(LED_BUILTIN, HIGH);  // turn the LED on (HIGH is the voltage level)
    delay(1000);                      // wait for a second
    digitalWrite(LED_BUILTIN, LOW);   // turn the LED off by making the voltage LOW
    delay(1000);                      // wait for a second
  }
  serialSend("Blink Done");
}

void serialSend(String str){
  Serial.write(str.length());
  Serial.print(str);
}