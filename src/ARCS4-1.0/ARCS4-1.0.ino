#include <string.h>

// the setup function runs once when you press reset or power the board
void setup() {
  // initialize digital pin LED_BUILTIN as an output.
  pinMode(LED_BUILTIN, OUTPUT);
  Serial.begin(115200);
  delay(500);
  serialSend("Arduino Start Message");
  delay(500);
}

void serialEvent(){

  uint8_t numBytes = Serial.read();

  uint8_t data[numBytes];

  Serial.readBytes(data, numBytes);

  //DO STUFF WITH data
}

void loop(){

}

void serialSend(String str){
  Serial.write(str.length());
  Serial.print(str);
}
void serialSend(uint8_t *data, int length){
  Serial.write((uint8_t)length);
  Serial.write(data, length);
}