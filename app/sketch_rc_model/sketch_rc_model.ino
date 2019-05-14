int ch1_pos = 95;//55-95-135
int ch2_pos = 2000;
#include <Servo.h>
//#include "AFmotor/AFMotor.h"
Servo ch1_servo;
Servo ch2_servo;
//AF_DCMotor motor1(1);
void setup() {
  Serial.begin(9600);
  Serial.setTimeout(10);
  // put your setup code here, to run once:
  ch1_servo.attach(9);
  ch1_servo.write(ch1_pos);
  pinMode(10,OUTPUT);
  //Serial.print("Setup ESC");
  ch2_servo.attach(7);
//  motor1.setSpeed(0);
//  motor1.run(RELEASE);
  //delay(100);
  //Serial.print("Write max");
  //ch2_servo.writeMicroseconds(2000);
  //delay(1000);
  //while (!Serial.available());
  //Serial.read();
  //Serial.print("Write min");
  //ch2_servo.writeMicroseconds(700);
  //delay(1500);
}
int incomingByte = 0;
void loop() {
  // put your main code here, to run repeatedly:
  if (Serial.available() > 0) {
    incomingByte = Serial.read();
    if (incomingByte == 35) {
      //Serial.println(incomingByte);
      incomingByte = Serial.parseInt();
      if (incomingByte == 1) {
        ch1_pos = Serial.parseInt();
        if(ch1_pos<55)ch1_pos=55;
        if(ch1_pos>135)ch1_pos=135;
        Serial.print("chanel1 get ");
        Serial.println(ch1_pos);
        ch1_servo.write(ch1_pos);
      } else if (incomingByte == 2) {
        ch2_pos = Serial.parseInt();
        Serial.print("chanel2 get ");
        Serial.print(ch2_pos);
        Serial.print(" set speed:");
        Serial.println(map(abs(ch2_pos), 0, 100, 0, 255));
        //map(ch2_pos, 0, 100, 700, 2300)
        //#2 700
        ch2_servo.write(map(ch2_pos, -100, 100, 40, 150));
        analogWrite(10,map(abs(ch2_pos), 0, 100, 0, 255));
        //motor1.setSpeed(map(abs(ch2_pos), 0, 100, 0, 255));
        //delay(3000);
        //ch2_servo.writeMicroseconds(400);
        //delay(1000);
        //ch2_servo.write(ch2_pos);
      }
    }
  }
//  if(ch2_pos<0){
//    motor1.run(BACKWARD);
//  }else if(ch2_pos>0){
//    motor1.run(FORWARD);
//  }else{
//    motor1.run(RELEASE);  
//    delay(250);
//  }
  //ch2_servo.writeMicroseconds(ch2_pos);
  //ch1_servo.write(ch1_pos);
  //ch2_servo.write(ch2_pos);
  //Servo refresh();
}
