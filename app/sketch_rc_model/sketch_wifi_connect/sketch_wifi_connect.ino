#include "WiFiEsp.h"
#include <WiFiEspUdp.h>
#include <Servo.h>

#ifndef HAVE_HWSERIAL1
#include "SoftwareSerial.h"
SoftwareSerial Serial1(6, 7); // RX, TX
#endif

int ch1_pos = 95;//55-95-135
int ch2_pos = 2000;
//#include "AFmotor/AFMotor.h"
Servo ch1_servo;
Servo ch2_servo;

char ssid[] = "C5H5OH";            // your network SSID (name)
char pass[] = "rootms123";        // your network password
int status = WL_IDLE_STATUS;     // the Wifi radio's status
boolean haveSSID = false;
unsigned int localPort = 8888;  // local port to listen on
unsigned int remotePort = 8889;  // local port to listen on

char packetBuffer[255];          // buffer to hold incoming packet
char ReplyBuffer[] = "ACK";      // a string to send back
WiFiEspUDP Udp;

void setup() {
  // initialize serial for debugging
  Serial.begin(115200);
  Serial.setTimeout(10);
  // initialize serial for ESP module
  Serial1.begin(115200);
  Serial1.setTimeout(10);
  // initialize ESP module
  WiFi.init(&Serial1);

  // check for the presence of the shield
  if (WiFi.status() == WL_NO_SHIELD) {
    Serial.println("WiFi shield not present");
    // don't continue
    while (true);
  }

  // Print WiFi MAC address
  printMacAddress();
  listNetworks();
  if(haveSSID==true)connectToNet();
  UdpUp();
  
  ch1_servo.attach(9);
  ch1_servo.write(ch1_pos);
  pinMode(10,OUTPUT);
  //Serial.print("Setup ESC");
  ch2_servo.attach(7);
}

void loop()
{
  // scan for existing networks
  //yield();
  int packetSize = Udp.parsePacket();
  if (packetSize) {
    Serial.print("Received packet of size ");
    Serial.println(packetSize);
    Serial.print("From ");
    IPAddress remoteIp = Udp.remoteIP();
    Serial.print(remoteIp);
    Serial.print(", port ");
    Serial.println(Udp.remotePort());

    // read the packet into packetBufffer
    int len = Udp.read(packetBuffer, 255);
    if (len > 0) {
      packetBuffer[len] = 0;
    }
    Serial.println("Contents:");
    Serial.println(packetBuffer);
    for(int i=0;i<len;++i){
      if (packetBuffer[i] == 35) {
        int incomingByte = packetBuffer[++i]-48;
        Serial.print("i ");
        Serial.println(i);
        Serial.print("incomingByte ");
        Serial.println(incomingByte);
        if (incomingByte == 1) {
          if (packetBuffer[++i] == 35) {
            ch1_pos = 0;
            String value;
            i++;
            //int k=100;
            while(packetBuffer[i]!=35 && packetBuffer[i]!=0){
              value+=packetBuffer[i++];
              //ch1_pos += (packetBuffer[i]-48)*k;
              //k/=10;
            }
            ch1_pos=value.toInt();
            //ch1_pos/=(k*10);
            i--;
            if(ch1_pos<55)ch1_pos=55;
            if(ch1_pos>135)ch1_pos=135;
            Serial.print("chanel1 get ");
            Serial.println(ch1_pos);
            ch1_servo.write(ch1_pos);
          }
        } else if (incomingByte == 2) {
          if (packetBuffer[++i] == 35) {
            ch2_pos = 0;
            String value;
            i++;
            while(packetBuffer[i]!=35 && packetBuffer[i]!=0){
              value+=packetBuffer[i++];
            }
            ch2_pos=value.toInt();
            Serial.print("i: ");
            Serial.println(i);
            i--;
            if(ch2_pos<0)ch2_pos=0;
            if(ch2_pos>200)ch2_pos=200;
            ch2_pos -= 100;
            //ch2_pos = Serial.parseInt();
            Serial.print("chanel2 get ");
            Serial.print(ch2_pos);
            Serial.print(" set speed:");
            Serial.println(map(abs(ch2_pos), 0, 100, 0, 255));
            ch2_servo.write(map(ch2_pos, -100, 100, 40, 150));
            analogWrite(10,map(abs(ch2_pos), 0, 100, 0, 255));
          }
        }
      }
    }
    // send a reply, to the IP address and port that sent us the packet we received
    Udp.beginPacket(Udp.remoteIP(), remotePort);
    Udp.write(ReplyBuffer);
    Udp.endPacket();
  }else{
    if (WiFi.status() != WL_CONNECTED) {
      haveSSID = false;
      Udp.stop();
      listNetworks();
      if(haveSSID==true){
        connectToNet();
        UdpUp();
      }else
        delay(1000);
    }else{
//      long rssi = WiFi.RSSI();
//      Serial.print("signal strength (RSSI):");
//      Serial.print(rssi);
//      Serial.println(" dBm");
      //yield();

      delay(100);
    }
  }
  
  //listNetworks();

  //delay(10000);
}

void connectToNet(){
  while ( WiFi.status() != WL_CONNECTED) {
    Serial.print("Attempting to connect to WPA SSID: ");
    Serial.println(ssid);
    // Connect to WPA/WPA2 network
    status = WiFi.begin(ssid, pass);
  }
  Serial.println("Connected to wifi");
  printWifiStatus();
}
void UdpUp(){
  if (WiFi.status() == WL_CONNECTED){
    Serial.print("UDP upping on port:");
    Serial.println(localPort);
    //Udp.begin(localPort);
    while (! Udp.begin(localPort) ) {                             // UDP protocol connected to localPort which is a variable
      Serial.print("+");
      yield();                                                    // Play nicely with RTOS (alias = delay(0))
    }
    Serial.println("*UP*");
    //Udp.listen(localPort)
  }
}
void printMacAddress()
{
  // get your MAC address
  byte mac[6];
  WiFi.macAddress(mac);
  
  // print MAC address
  char buf[20];
  sprintf(buf, "%02X:%02X:%02X:%02X:%02X:%02X", mac[5], mac[4], mac[3], mac[2], mac[1], mac[0]);
  Serial.print("MAC address: ");
  Serial.println(buf);
}

void listNetworks()
{
  // scan for nearby networks
  int numSsid = WiFi.scanNetworks();
  if (numSsid == -1) {
    Serial.println("Couldn't get a wifi connection");
    while (true);
  }

  // print the list of networks seen
  Serial.print("Number of available networks:");
  Serial.println(numSsid);

  // print the network number and name for each network found
  for (int thisNet = 0; thisNet < numSsid; thisNet++) {
    Serial.print(thisNet);
    Serial.print(") ");
    Serial.print(WiFi.SSID(thisNet));
    if(strcmp(WiFi.SSID(thisNet),ssid) == 0){
        haveSSID = true;
    }
    Serial.print("\tSignal: ");
    Serial.print(WiFi.RSSI(thisNet));
    Serial.print(" dBm");
    Serial.print("\tEncryption: ");
    printEncryptionType(WiFi.encryptionType(thisNet));
  }
}

void printEncryptionType(int thisType) {
  // read the encryption type and print out the name
  switch (thisType) {
    case ENC_TYPE_WEP:
      Serial.print("WEP");
      break;
    case ENC_TYPE_WPA_PSK:
      Serial.print("WPA_PSK");
      break;
    case ENC_TYPE_WPA2_PSK:
      Serial.print("WPA2_PSK");
      break;
    case ENC_TYPE_WPA_WPA2_PSK:
      Serial.print("WPA_WPA2_PSK");
      break;
    case ENC_TYPE_NONE:
      Serial.print("None");
      break;
  }
  Serial.println();
}

void printWifiStatus() {
  // print the SSID of the network you're attached to:
  Serial.print("SSID: ");
  Serial.println(WiFi.SSID());

  // print your WiFi shield's IP address:
  IPAddress ip = WiFi.localIP();
  Serial.print("IP Address: ");
  Serial.println(ip);

  // print the received signal strength:
  long rssi = WiFi.RSSI();
  Serial.print("signal strength (RSSI):");
  Serial.print(rssi);
  Serial.println(" dBm");
}
