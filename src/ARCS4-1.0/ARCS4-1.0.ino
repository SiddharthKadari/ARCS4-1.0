#include <string.h>

#define DEBUG

//======================== BIT ABSTRACTION STRUCTURES AND CONSTANTS ========================

// Robot State Descriptors
const uint8_t HEIGHT_FLIP = 0; // Elevator fully lowered, cube can flip
const uint8_t HEIGHT_DEPTH1 = 1; // Depth of 1 sides can be rotated
const uint8_t HEIGHT_DEPTH2 = 2; // Depth of 2 sides can be rotated
const uint8_t HEIGHT_DEPTH3 = 3; // Depth of 3 sides can be rotated
const uint8_t HEIGHT_DEPTH4 = 4; // Depth of 3 sides can be rotated
const uint8_t ROTATE_QTURN  = 0b000000;
const uint8_t ROTATE_HTURN  = 0b100000;
const uint8_t ROTATE_CW     = 0b000000;
const uint8_t ROTATE_CCW    = 0b010000;

// Axes Descriptors:
const uint8_t AXIS_DESCRIPTOR_BITLENGTH = 2;
const uint8_t AXIS_UD = 0;
const uint8_t AXIS_RL = 1;
const uint8_t AXIS_FB = 2;
// Face Descriptors:
// face = m2m1m0
//    m1m0  = axis descriptor
//    m2    = primary(0)/secondary(1) face
const uint8_t FACE_DESCRIPTOR_BITLENGTH = 1 + AXIS_DESCRIPTOR_BITLENGTH;
const uint8_t FACE_TO_AXIS_BITMASK = ~(-1<<AXIS_DESCRIPTOR_BITLENGTH);
const uint8_t FACE_U = 
              (0 <<AXIS_DESCRIPTOR_BITLENGTH) + 
              AXIS_UD; // 0
const uint8_t FACE_R = 
              (0 <<AXIS_DESCRIPTOR_BITLENGTH) + 
              AXIS_RL; // 1
const uint8_t FACE_F = 
              (0 <<AXIS_DESCRIPTOR_BITLENGTH) + 
              AXIS_FB; // 2
const uint8_t FACE_D = 
              (1 <<AXIS_DESCRIPTOR_BITLENGTH) + 
              AXIS_UD; // 4
const uint8_t FACE_L = 
              (1 <<AXIS_DESCRIPTOR_BITLENGTH) + 
              AXIS_RL; // 5
const uint8_t FACE_B = 
              (1 <<AXIS_DESCRIPTOR_BITLENGTH) + 
              AXIS_FB; // 6

// Orientations of the Cube:
// The orientation of the cube can be described by the face that is currently facing up (can be turned by the turning mechanism) and the face that can be reoriented to face up by the flipping mechanism
// All orientations can be described by 6 bits, first 3 corresponding to the upward face, next 3 corresponding to the front facing face, which can be flipped to face up
// orient = m5m4m3m2m1m0
//    m5m4m3 = upward face
//    m2m1m0 = front face
const uint8_t ORIENT_DESCRIPTOR_BITLENGTH = 2* FACE_DESCRIPTOR_BITLENGTH;
const uint8_t ORIENT_TO_FRONT_FACE_BITMASK = ~(-1<<FACE_DESCRIPTOR_BITLENGTH);
const uint8_t ORIENT_TO_UP_FACE_BITMASK = (~(-1<<FACE_DESCRIPTOR_BITLENGTH))<<FACE_DESCRIPTOR_BITLENGTH;
const uint8_t ORIENT_UR = (FACE_U <<FACE_DESCRIPTOR_BITLENGTH) +
                           FACE_R;
const uint8_t ORIENT_UF = (FACE_U <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_F;
const uint8_t ORIENT_UL = (FACE_U <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_L;
const uint8_t ORIENT_UB = (FACE_U <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_B;
const uint8_t ORIENT_RU = (FACE_R <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_U;
const uint8_t ORIENT_RF = (FACE_R <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_F;
const uint8_t ORIENT_RD = (FACE_R <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_D;
const uint8_t ORIENT_RB = (FACE_R <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_B;
const uint8_t ORIENT_FU = (FACE_F <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_U;
const uint8_t ORIENT_FR = (FACE_F <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_R;
const uint8_t ORIENT_FD = (FACE_F <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_D;
const uint8_t ORIENT_FL = (FACE_F <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_L;
const uint8_t ORIENT_DR = (FACE_D <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_R;
const uint8_t ORIENT_DF = (FACE_D <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_F;
const uint8_t ORIENT_DL = (FACE_D <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_L;
const uint8_t ORIENT_DB = (FACE_D <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_B;
const uint8_t ORIENT_LU = (FACE_L <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_U;
const uint8_t ORIENT_LF = (FACE_L <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_F;
const uint8_t ORIENT_LD = (FACE_L <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_D;
const uint8_t ORIENT_LB = (FACE_L <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_B;
const uint8_t ORIENT_BU = (FACE_B <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_U;
const uint8_t ORIENT_BR = (FACE_B <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_R;
const uint8_t ORIENT_BD = (FACE_B <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_D;
const uint8_t ORIENT_BL = (FACE_B <<FACE_DESCRIPTOR_BITLENGTH) + 
                           FACE_L;
;
const uint8_t REORIENT_180_ROTATION_BITMASK = 1 << AXIS_DESCRIPTOR_BITLENGTH;

const uint8_t MOVE_MAG_BITMASK    = 0b00100000;
const uint8_t MOVE_DIR_BITMASK    = 0b00010000;
const uint8_t MOVE_DEPTH_BITMASK  = 0b00001000;
const uint8_t MOVE_FACE_BITMASK   = 0b00000111;

/* ======================== BIT ABSTRACTION STRUCTURES ALGEBRA ========================

  orientation = cube orientation
  face = one of the 6 faces of the cube (0, 1, 2, 4, 5, 6)
  axis = one of the 3 axes of faces of the cube (0, 1, and 2)

    upward facing face = orientation >> FACE_DESCRIPTOR_BITLENGTH
    front facing face = orientation & ORIENT_TO_FRONT_FACE_BITMASK
    axis = face & FACE_TO_AXIS_BITMASK
    primary/secondary = face >> AXIS_DESCRIPTOR_BITLENGTH

  Cube rotation 180 degrees of depth 4:
    orientation ^= REORIENT_180_ROTATION_BITMASK

  Given a certain orientation, here is an efficient way of identifying which is the third axis, that is not the upward axis or the forward axis
    uint8_t thirdAxis = 3 - (orientation + (orientation >> FACE_DESCRIPTOR_BITLENGTH)) & FACE_TO_AXIS_BITMASK;

*/

// ======================== System State Variables ========================
uint8_t height = 0;
uint8_t orientation = ORIENT_UF;

// the setup function runs once when you press reset or power the board
const bool SERIAL_MONITOR_TEST = false; // make this true to test the arduino using the serial monitor, not the external computer system
const bool SOLVE_MODE = true; // make this true to run the primary solve sequence
void setup() {
  // initialize digital pin LED_BUILTIN as an output.
  pinMode(LED_BUILTIN, OUTPUT);
  Serial.begin(115200);
  delay(1000);
  const String startMessage = "Arduino Start Message";
  if(!SERIAL_MONITOR_TEST){
    serialSend(startMessage);
    delay(10);
  }
}

void serialEvent(){

  uint8_t numBytes = Serial.read();

  uint8_t data[numBytes];

  Serial.readBytes(data, numBytes);

  //DO STUFF WITH data
  // serialSend(data, numBytes);
  for(uint8_t move : data){
  }
  setHeight(0);
  flip();
  rotate(ROTATE_CW + ROTATE_QTURN);
  setHeight(1);
  rotate(ROTATE_CW + ROTATE_QTURN);
  setHeight(2);
  rotate(ROTATE_CW + ROTATE_QTURN);
  setHeight(3);
  rotate(ROTATE_CW + ROTATE_QTURN);
  setHeight(4);
  rotate(ROTATE_CW + ROTATE_QTURN);
  flip();
  rotate(ROTATE_CW + ROTATE_HTURN);
  rotate(ROTATE_CCW + ROTATE_HTURN);
  rotate(ROTATE_CW + ROTATE_QTURN);
  rotate(ROTATE_CCW + ROTATE_QTURN);
}

/* ======================== ROTATION PARAMETER ========================
  rotation = m7m6m5m4m3m2m1m0

  m5 of the parameter corresponds to turn magnitude, quarter turn if 0, half turn if 1
  m4 determines turn direction, is fed directly into DIR pin, 0 for CW, 1 for CCW
*/
void rotate(uint8_t rotation){
  #ifdef DEBUG
  uint8_t startOrientation = orientation;
  #endif

  if(height == HEIGHT_DEPTH4){ // if the whole cube is being rotated, then the orientation is changing
    if((rotation & MOVE_MAG_BITMASK) == ROTATE_HTURN){ // 180 degree turn, means the secondary axis is being flipped
      orientation ^= REORIENT_180_ROTATION_BITMASK;
    }else{ // 90 degree turn, the secondary axis changes to the remaining axis
      /*
        When a 90 degree turn occurrs, the upward facing face remains the same, but the front facing face changes
        To calculate the new front facing face, we can apply the following:
        assume orientation is formatted as follows:
        orientation = m5m4m3m2m1m0
        dir = the direction defined by the rotation parameter

        To calculate the new axis:
          newAxis = thirdAxis = 3 - (orientation + (orientation >> FACE_DESCRIPTOR_BITLENGTH)) & FACE_TO_AXIS_BITMASK;
        To calculate if the new face is the primary or secondary face:
          newPriSec = ((m4 ^ m3) & ~m1) ^ m0 ^ m2 ^ m3 ^ m5 ^ dir;

        When calculating the new front face, the following code will perform the above calculations, but they may appear differently to optimize computation time
      */
      orientation = (orientation & ORIENT_TO_UP_FACE_BITMASK) + //preserve upward face
                    (3 - ((orientation + (orientation >> FACE_DESCRIPTOR_BITLENGTH)) & FACE_TO_AXIS_BITMASK)) + // insert new axis of front face;
                    (((((orientation >> 2) ^ (orientation >> 1)) & ~(orientation << 1)) ^ (orientation << 2) ^ orientation ^ (orientation >> 1) ^ (orientation >> 3) ^ (rotation >> 2)) & 0b100); // compute whether new face is primary or secondary
    }
  }

  #ifdef DEBUG
  serialSendDebug("ROTATE (depth = " + String(height) + ((rotation & MOVE_MAG_BITMASK) == MOVE_MAG_BITMASK ? ", HALF " : ", QUARTER ") + ((rotation & MOVE_DIR_BITMASK) == MOVE_DIR_BITMASK ? "CCW)" : "CW)"), startOrientation);
  #endif
}
void flip(){
  #ifdef DEBUG
  uint8_t startOrientation = orientation;
  #endif

  if(height == 0){ //front and up facing sides swap
    orientation = (orientation & ORIENT_TO_FRONT_FACE_BITMASK) << FACE_DESCRIPTOR_BITLENGTH + (orientation >> 3);
  }else{ //front facing side switches to the opposite side, up remains same
    orientation ^= REORIENT_180_ROTATION_BITMASK;
  }

  #ifdef DEBUG
  serialSendDebug("FLIP", startOrientation);
  #endif
}
void setHeight(uint8_t newHeight){
  #ifdef DEBUG
  uint8_t startOrientation = orientation;
  #endif

  height = newHeight;

  #ifdef DEBUG
  serialSendDebug("HEIGHT: " + String(height), startOrientation);
  #endif
}

void loop(){}

void serialSend(const String &str){
  Serial.write(str.length());
  Serial.print(str);
}
void serialSend(uint8_t *data, uint8_t &length){
  Serial.write(length);
  Serial.write(data, length);
}
void serialSend(uint8_t &data){
  Serial.write(1);
  Serial.write(data);
}
#ifdef DEBUG
void serialSendDebug(const String &str, uint8_t &startOrientation){
  serialSend("DEBUG :\t" + String(startOrientation) + "\t->\t" + String(orientation) + "\t: " + str);
}
void serialSendDebug(const String &str){
  serialSend("DEBUG : " + str);
}
#endif