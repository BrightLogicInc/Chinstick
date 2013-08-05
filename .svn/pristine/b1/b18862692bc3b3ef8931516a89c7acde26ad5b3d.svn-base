void setup(){
  for(int i = 2; i < 13; i++){
    pinMode(i, INPUT_PULLUP);
  }
  Serial.begin(9600);
  
}

void loop(){
  for(int i = 2; i < 13; i++){
    if(digitalRead(i) == 0) Serial.print(i);
  }
  delay(100);
  Serial.println("");
}
