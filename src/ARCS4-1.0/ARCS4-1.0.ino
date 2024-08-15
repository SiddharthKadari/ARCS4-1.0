#include <string.h>
#include "AccelStepper.h"

#define DEBUG

//======================== ROBOT STATE AND CONTROL CONSTANTS ========================
#define ELEVATOR_STEP_PIN   0
#define ELEVATOR_DIR_PIN    1
#define ELEVATOR_ENA_PIN    2
#define ROTATOR_STEP_PIN    22
#define ROTATOR_DIR_PIN     23
#define ROTATOR_ENA_PIN     24
#define FLIPPER_STEP_PIN    0
#define FLIPPER_DIR_PIN     1
#define FLIPPER_ENA_PIN     2

AccelStepper elevator =  AccelStepper(AccelStepper::DRIVER, ELEVATOR_STEP_PIN, ELEVATOR_DIR_PIN);
AccelStepper rotator =  AccelStepper(AccelStepper::DRIVER, ROTATOR_STEP_PIN, ROTATOR_DIR_PIN);
AccelStepper flipper =  AccelStepper(AccelStepper::DRIVER, FLIPPER_STEP_PIN, FLIPPER_DIR_PIN);

const long elevator_steps_per_rev = 200;
const long rotator_steps_per_rev = 200;
const long flipper_steps_per_rev = 200;

const long elevator_positions[] = { 0.0   * elevator_steps_per_rev, 
                                    0.2   * elevator_steps_per_rev, 
                                    0.4   * elevator_steps_per_rev, 
                                    0.6   * elevator_steps_per_rev, 
                                    0.8   * elevator_steps_per_rev};
const long rotator_quarter_turn = rotator_steps_per_rev / 4;
const long rotator_half_turn =    rotator_steps_per_rev / 2;
const long flipper_opposing_position = flipper_steps_per_rev / 4;

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
const uint8_t ROTATE_PRIM   = 0b000000;
const uint8_t ROTATE_SECD   = 0b000100;
const uint8_t ROTATE_Q_CW   = ROTATE_QTURN + ROTATE_CW;
const uint8_t ROTATE_Q_CCW  = ROTATE_QTURN + ROTATE_CCW;
const uint8_t ROTATE_H_CW   = ROTATE_HTURN + ROTATE_CW;
const uint8_t ROTATE_H_CCW  = ROTATE_HTURN + ROTATE_CCW;

//======================== BIT ABSTRACTION STRUCTURES AND CONSTANTS ========================

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
const uint8_t FACE_TO_PYSY_BITMASK = 1<<AXIS_DESCRIPTOR_BITLENGTH;
const uint8_t FACE_TO_AXIS_BITMASK = FACE_TO_PYSY_BITMASK - 1;
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
bool flipper_position = 0; //0 is the starting config, 1 is the opposite config

// the setup function runs once when you press reset or power the board
const bool SERIAL_USB_COMMS = false; // make this true to use the external computer system, false to use the arduino serial monitor
const bool SOLVE_MODE = false; // make this true to run the primary solve sequence
void setup() {
  // initialize digital pin LED_BUILTIN as an output.
  pinMode(LED_BUILTIN, OUTPUT);
  Serial.begin(115200);
  delay(10);

  elevator.setEnablePin(ELEVATOR_ENA_PIN);
  elevator.setPinsInverted(true, false, true);
  elevator.enableOutputs();
  rotator.setEnablePin(ROTATOR_ENA_PIN);
  rotator.setPinsInverted(false, false, true);
  rotator.enableOutputs();
  flipper.setEnablePin(FLIPPER_ENA_PIN);
  flipper.setPinsInverted(true, false, true);
  flipper.enableOutputs();

  elevator.setMaxSpeed(elevator_steps_per_rev);
  elevator.setAcceleration(elevator_steps_per_rev * 100);
  rotator.setMaxSpeed(rotator_steps_per_rev);
  rotator.setAcceleration(rotator_steps_per_rev * 100);
  flipper.setMaxSpeed(flipper_steps_per_rev);
  flipper.setAcceleration(flipper_steps_per_rev * 100);


  delay(800);
  const String startMessage = "Arduino Start Message";
  if(SERIAL_USB_COMMS){
    serialSend(startMessage);
    delay(10);
  }
}

void loop(){}

void serialEvent(){

  uint8_t numBytes = Serial.read();

  uint8_t data[numBytes];

  Serial.readBytes(data, numBytes);

  //DO STUFF WITH data
  // serialSend(data, numBytes);
  if(SOLVE_MODE){
    for(uint8_t move : data){
      uint8_t upwardFaceXorMove = orientation >> FACE_DESCRIPTOR_BITLENGTH ^ move;
      if(upwardFaceXorMove & FACE_TO_AXIS_BITMASK){// if the primary axis is not the target axis
        if((orientation ^ move) & FACE_TO_AXIS_BITMASK){// if the secondary axis is not the target axis
          executeMoveTertiaryAxisIsTarget(move);
        }else{// if the secondary axis is the target axis
          executeMoveSecondaryAxisIsTarget(move);
        }
      }else{ //if the primary axis is already the target axis
        executeMovePrimaryAxisIsTarget(move, upwardFaceXorMove & FACE_TO_PYSY_BITMASK);
      }
    }
  }
}
void executeMovePrimaryAxisIsTarget(uint8_t move, uint8_t isPrimaryAxisInverted){
  setHeight(move & MOVE_DEPTH_BITMASK ? 2 : isPrimaryAxisInverted / 2 + 1);
  rotate(move, isPrimaryAxisInverted);
}
void executeMoveSecondaryAxisIsTarget(uint8_t move){
  setHeight(HEIGHT_FLIP);
  flip();

  executeMovePrimaryAxisIsTarget(move, (orientation >> FACE_DESCRIPTOR_BITLENGTH ^ move) & FACE_TO_PYSY_BITMASK);
}
void executeMoveTertiaryAxisIsTarget(uint8_t move){
  setHeight(HEIGHT_DEPTH4);

  rotate((((((orientation >> 2) ^ (orientation >> 1)) & ~(orientation << 1)) ^ (orientation << 2) ^ orientation ^ (orientation >> 1) ^ (orientation >> 3) ^ move) & FACE_TO_PYSY_BITMASK) << 2, 0);

  executeMoveSecondaryAxisIsTarget(move);
}




/* ======================== ROTATION PARAMETER ========================
  rotation = m7m6m5m4m3m2m1m0

  m5 of the parameter corresponds to turn magnitude, quarter turn if 0, half turn if 1
  m4 determines turn direction, is fed directly into DIR pin, 0 for CW, 1 for CCW
  m2 determines if the intended turn face is a primary or secondary face, 0 for primary, 1 for secondary

  If the currently facing upward face does not match the intended turn face according to m2, then the turn direction will be inverted (CW <-> CCW)
*/
// isPrimaryAxisInverted is non-zero if the intended turn face is opposite from the currently upward facing face
// isPrimaryAxisInverted = (((orientation >> FACE_DESCRIPTOR_BITLENGTH) ^ rotation) & FACE_TO_PYSY_BITMASK)
void rotate(uint8_t rotation, uint8_t isPrimaryAxisInverted){
  #ifdef DEBUG
  uint8_t startOrientation = orientation;
  #endif

  if(height == HEIGHT_DEPTH4 || isPrimaryAxisInverted){ // if the whole cube is being rotated, or if the intended turn face is opposite from the currently upward facing face, then the orientation is changing
    if(rotation & MOVE_MAG_BITMASK){ // 180 degree turn, means the secondary axis is being flipped
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
                    (((((orientation >> 2) ^ (orientation >> 1)) & ~(orientation << 1)) ^ (orientation << 2) ^ orientation ^ (orientation >> 1) ^ (orientation >> 3) ^ (rotation >> 2)) & FACE_TO_PYSY_BITMASK); // compute whether new face is primary or secondary
    }
  }

  rotator.move((((rotation >> 4) & 1) * 2 - 1) * ((rotation >> 5) + 1) * rotator_quarter_turn);
  rotator.runToPosition();

  #ifdef DEBUG
  serialSendDebug("ROTATE (depth = " + String(height) + ((rotation & MOVE_MAG_BITMASK) == MOVE_MAG_BITMASK ? ", HALF " : ", QUARTER ") + ((rotation & MOVE_DIR_BITMASK) == MOVE_DIR_BITMASK ? "CCW " : "CW ") + (rotation & FACE_TO_PYSY_BITMASK ? "SECONDARY)" : "PRIMARY)"), startOrientation);
  #endif
}
void flip(){
  #ifdef DEBUG
  uint8_t startOrientation = orientation;
  #endif

  if(height == 0){ //front and up facing sides swap
    orientation = ((orientation & ORIENT_TO_FRONT_FACE_BITMASK) << FACE_DESCRIPTOR_BITLENGTH) + (orientation >> 3);
  }else{ //front facing side switches to the opposite side, up remains same
    orientation ^= REORIENT_180_ROTATION_BITMASK;
  }

  flipper_position = !flipper_position;
  flipper.runToNewPosition(flipper_opposing_position * flipper_position);

  #ifdef DEBUG
  serialSendDebug("FLIP", startOrientation);
  #endif
}
void setHeight(uint8_t newHeight){
  #ifdef DEBUG
  uint8_t startOrientation = orientation;
  #endif

  height = newHeight;
  elevator.runToNewPosition(elevator_positions[height]);

  #ifdef DEBUG
  serialSendDebug("HEIGHT: " + String(height), startOrientation);
  #endif
}

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