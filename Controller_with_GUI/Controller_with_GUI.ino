//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//
//                                                                              //
// Joystick Computer Control with Layout Bank                                   //
// Created by: Aaron Weinstein                                                  //
// Company: BrightLogic                                                         //
// Date: 7/9/2012                                                               //
// Description: This program enables control of a computer mouse and keyboard   //
// from two 4-directional joysticks, with the ability to adjust control Layouts //
// and cycle between these Layouts easily                                       //
// Hardware: Two 4-direction joysticks, Arduino Leonardo, USB cable             //
//                                                                              // 
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~//

// All arrays stored as:
//{lUp, lDown, lLeft, lRight, rUp, rDown, rLeft, rRight}

const int joyStickPins[8] = {
  5, 4, 2, 3, 9, 8, 6, 7 };
const int ledPins[4] = {
  13, 12, 11, 10};


// Bank of arrays to store 8 joyStrokes 
// Four types of joyStrokes: MouseX(x), MouseY(y), Keyboard(k), Setup(s)
// Layout is a set of 8 directions on the chinstick
// A mode is the set of Layouts you can switch between quickly
// Every Layout corresponds to a type array and val array

// Vals are either increment values or ASCII key numbers
// Types are: s for Layout changes, k for keyboard, y for mouse movement in y-axis, 
// P for page scrolls, x for mouse movement in x-axis, and c for clicks
// CAPITAL TYPES ARE FOR REPEATED ACTIONS i.e. HOLDING THE KEY OR BUTTON
// lower case types are for single actions, if held they will repeat but with
// pauses in between, like clicking really fast
// recommend capital for clicks and lower for keyboard, but both will work

// Mouse directions: Y: + is down, - is up. X: - is left, + is right
// Click Types: l = left, r = right, m = middle click

// Any joystick motion can correspond to any action

int numLayouts = 4;
int numLayoutsOld;
const int modeHoldTime = 1000;

char allTypes[] = {
  's', 'k', 'K', 'y', 'x', 'p', 'P', 'c', 'C'};
int allTypesSize = 9;

char joyBankTypes[4][9];
int joyBankVals [4][9];

char normalTypes[][9] = {  
  //lu,  ld,  ll,  lr,  ru,  rd,  rl,  rr
  {
    'y', 'y', 'x', 'x', 's', 'c', 'C', 'C'      }
  ,
  {
    'y', 'y', 'x', 'x', 's', 'c', 'C', 'C'      }
  ,
  {
    'y', 'y', 'x', 'x', 's', 'c', 'C', 'C'      }
  ,
  {
    'y', 'y', 'x', 'x', 's', 'c', 'C', 'C'      }
  , 
};

int normalVals[][9] =  {
  //  lu,  ld,  ll,  lr,  ru,  rd,  rl,  rr,  spd
  {  
    -1,   1,  -1,   1,   1, 'm', 'r', 'l',    1                                    }
  ,
  {  
    -1,   1,  -1,   1,   1, 'm', 'r', 'l',    2                                    }
  ,
  {  
    -1,   1,  -1,   1,   1, 'm', 'r', 'l',    4                                    }
  ,
  {  
    -1,   1,  -1,   1,   1, 'm', 'r', 'l',    8                                    }
  ,                            
};

int currentLayout = 0; // Used to store which Layout is in use, in reference to bank
char modeKey = 'n'; //Stores current mode or set of Layouts

// Use to see button states and change of state
int oldReading[8];
int reading[8];

const char KEY_INPUT_CODE = '!',
STROKE_INPUT_CODE = '#',
END_INPUT_CODE = '$';

void setup()
{
  for(int a = 0; a <= 8; a++)
  {
    pinMode(joyStickPins[a], INPUT_PULLUP); //setup buttons as inputs
  }
  Mouse.begin(); //start mouse control
  Keyboard.begin(); //start keyboard control
  Serial.begin(9600); 

  for(int q = 0; q < 3; q++) //blink LEDs three times to indicate ready
  {
    for(int i = 0; i < 4; i++){
      pinMode(ledPins[i], OUTPUT);
      digitalWrite(ledPins[i], 1); //turn on all
    }
    delay(500);
    for(int i = 0; i < 4; i++)
    {
      digitalWrite(ledPins[i], 0); //turn off all
    }
    delay(250);
  }

  changeBank(normalTypes, normalVals); //set bank to initial mode and layout
  ledGo(); //display current layout
}

void loop()
{
  readStick(); //reads and updates reading memory
  findType(); //determines action to be performed
  moveMouse(); //moves mouse
}

void readStick() //reads stick values and stores them in reading arrays
{
  for(int b = 0; b <= 7; b++)
  {
    oldReading[b] = reading[b]; //send past values back
    reading[b] = !(digitalRead(joyStickPins[b])); //update with new ones
  }
}

int offCount = 0; //counter for time without pressing stick

void findType() //figures out what to do and sends to that function
{
  if ((reading[0] == 0) && (reading[1] == 0) && //all off, no press
  (reading[2] == 0) && (reading[3] == 0) &&
    (reading[4] == 0) && (reading[5] == 0) &&
    (reading[6] == 0) && (reading[7] == 0) &&
    (oldReading[0] == 0) && (oldReading[1] == 0) &&
    (oldReading[2] == 0) && (oldReading[3] == 0) &&
    (oldReading[4] == 0) && (oldReading[5] == 0) &&
    (oldReading[6] == 0) && (oldReading[7] == 0)){

    offCount++;
    if (offCount > 1000){ //runs after some time if nothing is pressed
      if(joyBankVals[0][0] == 0){ //fail safe if read blank layout
        changeBank(normalTypes, normalVals);
      }
      letGoAll();  //fail safe, if nothing is pressed, the controller stops
      offCount = 0;
    }  
  }

  else {  //something was pressed
    for(int c = 0; c <= 7; c++) {
      if((joyBankTypes[currentLayout][c] > 90) && (reading[c] == 1)) { //lowercase letter, and ON
        switch (joyBankTypes[currentLayout][c]) {
        case 's':  //setup
          //if(oldReading[c] == 0) //only goes once when the button is pressed, won't 
          //{                      //cycle if held
            doSetup(c);          //changes the Layout
          //}
          break;
        case 'y':  //mouse y
          addMouseY(c);  //adds to the y portion of mouseToMove
          break;
        case 'x':  //mouse x
          addMouseX(c);  //adds to the x portion of mouseToMove
          break;
        case 'p':  //mouse scroll
          addMouseP(c);
          break;
        case 'c':  // click
          doClick(c);  //clicks once
          break;
        case 'k':  // keyboard
          doKeyboard(c);  //presses the right key once
          break;
        }
      }
      if((joyBankTypes[currentLayout][c] <= 90) && (reading[c] == 1) && (oldReading[c] == 0))
      {  //capital letter and switched on
        switch (joyBankTypes[currentLayout][c])
        {
        case 'K': //keyboard
          startKey(c); //presses key
          break;
        case 'P':
          doP(c);
          break;
        case 'C':  //click
          startClick(c); //presses mouse
          break;
        }
      }
      if((joyBankTypes[currentLayout][c] <= 90) && (reading[c] == 0) && (oldReading[c] == 1))
      { //capital letter and switched off
        switch (joyBankTypes[currentLayout][c])
        {
        case 'K':  //keyboard
          stopKey(c);  //releases key
          break;
        case 'C':
          stopClick(c);
          break;
        }
      }
    }
  }
}

void letGoAll() //fail safe shutdown
{
  Keyboard.releaseAll();
  Mouse.release(MOUSE_LEFT);
  Mouse.release(MOUSE_RIGHT);
  Mouse.release(MOUSE_MIDDLE);
}

void doSetup(int valLocation)  //changes Layout
{
  int startTime = millis();
  int timePassed;
  while (!digitalRead(joyStickPins[valLocation]) == 1)
  {
    timePassed = millis() - startTime;
    delay(100);
    if (timePassed >= modeHoldTime) //if held will change whole mode
    {
      Keyboard.end();
      Mouse.end();
      modeSwitch(valLocation);
      startTime = millis();
      currentLayout = -1;
      Keyboard.begin();
      Mouse.begin();
      delay(250);
    }
  }

  currentLayout++;  // change Layout reference number, IMPORTANT LINE

  if((currentLayout >= numLayouts) || (currentLayout < 0) || 
    (joyBankVals[currentLayout][valLocation] == 0)) {
    currentLayout = 0;
  } //loop around
  ledGo();
  delay(100);
  Serial.write(currentLayout);
}

void modeSwitch(int valLocation)
{
  for(int i = 0; i < 4; i++)
  {
    digitalWrite(ledPins[i], LOW);
  }
  Serial.write(255); //alert GUI
  while(!digitalRead(joyStickPins[valLocation])) //while setup stroke held
  {
    delay(250);
  }

  long place = millis();
  int b = 0;
  digitalWrite(ledPins[0], HIGH);
  char directions[8] = {
    'u', 'd', 'l', 'r', 'U', 'D', 'L', 'R'    }; //for keyboard motion sent to gui
  boolean done = false;

  while (done == false) //waiting to recieve new sets
  {
    if((millis() - place) > 125) //cycle LEDs
    {
      digitalWrite(ledPins[b], LOW);
      if (b == 3) b = 0;
      else b++;
      digitalWrite(ledPins[b], HIGH);
      place = millis();
    }

    for(int i = 0; i < 8; i++) //check for stroke
    {
      if (!digitalRead(joyStickPins[i]) == HIGH)
      {
        Serial.write(directions[i]);
        delay(250);
        if(normalTypes[0][i] == 's'){
          delay(500);
          if(!Serial.available()){
            done = true;
          }
        }
      }
    }

    char inChar = Serial.read();

    if(inChar == STROKE_INPUT_CODE){ //Start recieving a layout Code
      readBank();
    }

    else if(inChar == KEY_INPUT_CODE){  //if sent a keystroke, controller acts as keyboard input
      Keyboard.write(Serial.read());
    }
    
    else if(inChar == END_INPUT_CODE){
      done = true;
    }
  }
  currentLayout = 0;
}

void readBank(){
  int set = 0;
  while(set < 4){
    for(int i = 0; i < 8; i++){
      while(!Serial.available()){
      }
      joyBankTypes[set][i] = Serial.read();
    }
    for(int i = 0; i < 9; i++){
      while(!Serial.available()){
      }
      joyBankVals[set][i] = Serial.read();
    }
    if(Serial.available() > 10){ 
      set++;
    }
    else if(Serial.available() < 10) {
      for(int i = set + 1; i < 4; i++) {
        for(int j = 0; j < 8; j++){
          joyBankTypes[i][j] = 0;
          joyBankVals[i][j] = 0;
        }
        joyBankVals[i][8] = 0;
      }
      set = 4;
    }
  }
}

void changeBank(char newTypes[4][9], int newVals[4][9])
{
  for (int i = 0; i < 4; i++){
    for (int q = 0; q < 9; q++){
      joyBankTypes[i][q] = newTypes[i][q];
      joyBankVals[i][q] = newVals[i][q];
    }
  }
}

int mouseToMove[3]; // Temporary storage before mouse movements, used for diagonals

void addMouseY(int valLocation) // add to mouseToMove array in Y axis
{
  mouseToMove[1] += joyBankVals[currentLayout][valLocation];  //checks val from Layout
}

void addMouseX(int valLocation)// add to mouseToMove array in X axis
{
  mouseToMove[0] += joyBankVals[currentLayout][valLocation]; //checks val from Layout
}

void addMouseP(int valLocation)
{
  mouseToMove[2] += joyBankVals[currentLayout][valLocation];
}

void moveMouse()  // moves mouse based on increments in mouseToMove
{
  if ((mouseToMove[0] != 0) || (mouseToMove[1] != 0) || (mouseToMove[2] != 0)) //check to see that the mouse should move
  {
    Mouse.move(mouseToMove[0], mouseToMove[1], mouseToMove[2]); //moves mouse
  }
  mouseToMove[0] = 0; //reset all values in mouseToMove, or else mouse will run away
  mouseToMove[1] = 0;
  mouseToMove[2] = 0;
  delay(joyBankVals[currentLayout][8]);
}

void doP(int valLocation)
{
  Mouse.move( 0, 0, joyBankVals[currentLayout][valLocation]);
  delay(250);
}

boolean leftLockOn = false;

void doClick(int valLocation) //clicks and releases mouse, for lowercase type
{
  int type = joyBankVals[currentLayout][valLocation]; //checks val from Layout
  switch (type)
  {
  case 'l':
    Mouse.click(MOUSE_LEFT);
    break;
  case 'r':
    Mouse.click(MOUSE_RIGHT);
    break;
  case 'm':
    if(Mouse.isPressed(MOUSE_LEFT) == false){ //Left Lock on
      Mouse.press(MOUSE_LEFT);
      delay(10);
      digitalWrite(ledPins[3], HIGH);
    }
    else {          //Left lock off
      Mouse.release(MOUSE_LEFT);
      digitalWrite(ledPins[3], LOW);
    }
    break;
  }
  delay(250);
}

void startClick(int valLocation) //presses mouse, for CAPITAL type
{
  int type = joyBankVals[currentLayout][valLocation]; //checks val from Layout
  switch (type) //clicks appropriate button
  {
  case 'l':
    Mouse.press(MOUSE_LEFT);
    break;
  case 'r':
    Mouse.press(MOUSE_RIGHT);
    break;
  case 'm':
    Mouse.press(MOUSE_LEFT);
    break;
  }
}

void stopClick(int valLocation) //releases mouse, for CAPITAL type
{
  int type = joyBankVals[currentLayout][valLocation]; //checks val from Layout
  switch (type)
  {
  case 'l':
    Mouse.release(MOUSE_LEFT);
    break;
  case 'r':
    Mouse.release(MOUSE_RIGHT);
    break;
  case 'm':
    Mouse.release(MOUSE_LEFT);
    break;
  }
}

void doKeyboard(int valLocation)  //clicks and releases key, for lowercase type
{
  Keyboard.write(joyBankVals[currentLayout][valLocation]); //checks val from Layout
  delay(1);
}

void startKey(int valLocation) //presses key, for CAPITAL type
{
  Keyboard.press(joyBankVals[currentLayout][valLocation]); //checks val from Layout
  delay(1);
}

void stopKey(int valLocation)  //releases key, for CAPITAL type
{
  Keyboard.release(joyBankVals[currentLayout][valLocation]); //checks val from Layout
}

void ledGo() //shows current layout on LEDs
{
  for(int i = 0; i < 4; i++)
  {
    digitalWrite(ledPins[i], LOW);
  }
  digitalWrite(ledPins[currentLayout], HIGH);
}

void ledFlash(int a)
{
  for(int i = 0; i < 4; i++)
  {
    digitalWrite(ledPins[i], LOW);
  }
  for(int i = 0; i < 3; i++)
  {
    digitalWrite(ledPins[a], HIGH);
    delay(150);
    digitalWrite(ledPins[a], LOW);
    delay(100);
  }
}

//--------------------------------------------------------------------------------------------//
//---------------------------OLD FUNCTIONS----------------------------------------------------//
//--------------------------------------------------------------------------------------------//

//String textToEnter;

//void modeSwitch(int valLocation)
//{
//  for(int i = 0; i < 4; i++)
//  {
//    digitalWrite(ledPins[i], LOW);
//  }
//  while(!digitalRead(joyStickPins[valLocation]))
//  {
//    delay(100);
//  }
//  Serial.write(255);
////  for(int i = 0; i < 16; i++){ //send all zeros
////    Serial.write(zero);
////  }
//  long place = millis();
//  int b = 0;
//  digitalWrite(ledPins[0], HIGH);
//  char directions[8] = {'u', 'd', 'l', 'r', 'U', 'D', 'L', 'R'}; //for keyboard motion
//  boolean done = false;
//  while (done == false) //waiting to recieve new sets
//  {
//    if((millis() - place) > 125) //cycle LEDs
//    {
//      digitalWrite(ledPins[b], LOW);
//      if (b == 3) b = 0;
//      else b++;
//      digitalWrite(ledPins[b], HIGH);
//      place = millis();
//    }
//    for(int i = 0; i < 8; i++) //check for stroke
//    {
//      if (!digitalRead(joyStickPins[i]) == HIGH)
//      {
//        Serial.write(directions[i]);
//        delay(250);
//      }
//    }
//    if(Serial.available() == 1){
//      Keyboard.write(Serial.read());
//    }
//    
//    else if(Serial.available() > 1){
//      readBank();
//      done = true;
//    }
//  }
//    currentLayout = 0;
////  Serial.println(modeKey);
////  Serial.println(" ");
////  switch (modeKey)
////  {
////  case 'T':
////    changeBank(tetrisTypes, tetrisVals);
////    break;
////  case 'c':
////    modeSwitch(valLocation);
////    break;
////  case 'q':
////    changeBank(qwopTypes, qwopVals);
////    break;
////  case 'r':
////    changeBank(raidenTypes, raidenVals);
////    break;
////  case 'n':
////    changeBank(normalTypes, normalVals);
////    Serial.println("Normal mode");
////    ledFlash(0);
////    break;
////  case 'm':
////    changeBank(minecraftTypes, minecraftVals);
////    Serial.println("Minecraft mode");
////    ledFlash(1);
////    /*delay(500);
////     Keyboard.press('t');
////     delay(100);
////     Keyboard.releaseAll();
////     delay(500);
////     Keyboard.println("/longerlegs");
////     delay(500); */
////    break;
////  case 'a':
////    changeBank(arrowTypes, arrowVals);
////    Serial.println("Arrows mode");
////    ledFlash(2);
////    break;
////  case 'w':
////    changeBank(wasdTypes, wasdVals);
////    Serial.println("WASD mode");
////    ledFlash(3);
////    break;
////  case 't':
////    //Serial.println("Text Enter");
////    changeBank(normalTypes, normalVals);
////    currentLayout = 0;
////    delay(500);
////    enterText();
////    modeKey = 'n';
////    break;
////  case'p':
////    Serial.print("Text Paste: ");
////    Serial.println(textToEnter);
////    Keyboard.print(textToEnter);
////    changeBank(normalTypes, normalVals);
////    modeKey = 'n';
////    break;
////  }
//}

//char qwerty[4][13] = {
//  {
//    '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '-', '=', -50                            }
//  ,
//  {
//    9, 'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', '[', ']'                            }
//  ,
//  {
//    -5, 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ';', '"',  10                             }
//  ,
//  { 
//    -6, 'z', 'x', 'c', 'v', ' ', 'b', 'n', 'm', ',', '.', '/', -6                           }
//};
//char QWERTY[4][13] = {
//  {
//    '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_', '+', -50                            }
//  ,
//  {
//    9, 'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', '{', '}'                            }
//  ,
//  {
//    -5, 'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', ';', '"', 10                            }
//  ,
//  { 
//    -6, 'Z', 'X', 'C', 'V', ' ', 'B', 'N', 'M', '<', '>', '?', -6                            }
//};
//char q[4][13];  
//
//void enterText()
//{
//  int zero = 0;
//  for(int i = 0; i < 16; i++){
//    Serial.write(zero);
//  }
//  boolean done = false;
//  
//  /*char buffer[50];
//  //char c;
//  int x = 6;
//  int y = 2;
//  boolean caps = false;
//  boolean shift = false;
//  //textToEnter = "";
//  changeQ(caps);
//  moveKeys(x, y);*/
//  while(!done)
//  {
//    readStick();
//    if(reading[0] == 1) Serial.print('u');
//    if(reading[1] == 1) Serial.print('d');
//    if(reading[2] == 1) Serial.print('l');
//    if(reading[3] == 1) Serial.print('r');
//    if(reading[7] == 1) Serial.print('e');
//    delay(375);
//    /*x += 13;
//    y += 4;
//    x %= 13;
//    y %= 4;
//    if(reading[0] == 1) 
//    {
//      Serial.println("Done");
//      done = true;
//    }
//    if(reading[2] == 1) 
//    {
//      Serial.print("You entered: backspace");
//      Keyboard.write(178); //backspace
//      delay(250);
//    }
//    if(reading[1] == 1) 
//    {
//      Serial.print("You entered: enter");
//
//      Keyboard.write(10); //enter
//      delay(250);
//    }
//    if(reading[3] == 1) //press
//    {
//      Serial.print("You entered: ");
//      if(q[y][x] == -50) Serial.print("BackSpace");
//      else if(q[y][x] == -5) Serial.print("CAP");
//      else if(q[y][x] == -6) Serial.print("Shf");
//      else if(q[y][x] == 9) Serial.print("Tab");
//      else if(q[y][x] == 10) Serial.print("Enter");
//      else if(q[y][x] == ' ') Serial.print("Space");
//      else Serial.println(q[y][x]);
//      if(q[y][x] > 0) 
//      {
//        Keyboard.press(q[y][x]);
//        delay(100);
//        Keyboard.release(q[y][x]);
//        if(shift == true)
//        {
//          caps = !caps;
//          changeQ(caps);
//          shift = false;
//          moveKeys(x, y);
//        }
//      }
//      else if(q[y][x] == -5)
//      {
//        caps = !caps;
//        changeQ(caps);
//        moveKeys(x, y);
//        shift = false;
//      }
//      else if(q[y][x] == -6) 
//      {
//        caps = !caps;
//        changeQ(caps);
//        moveKeys(x, y);
//        shift = true;
//      }
//      else Keyboard.write(128 - q[y][x]);
//      delay(150);
//    }
//    if((reading[4] == 1) || (reading[5] == 1) || (reading[6] == 1) || (reading[7] == 1))
//    {
//      moveKeys(x, y);
//      delay(150);
//    }*/
//  }
//}

//void moveKeys(int x, int y)
//{
//  Serial.print("\n\n\n\n\n  ");
//  for(int a = 0; a < 4; a++)
//  {
//    for(int b = 0; b < 13; b++)
//    {
//      if((a == y) && (b == x)) Serial.print("(");
//      else Serial.print(" ");
//      if(q[a][b] < 32)
//      {
//        if(q[a][b] == -50) Serial.print("BackSpace");
//        else if(q[a][b] == -5) Serial.print("CAP");
//        else if(q[a][b] == -6) Serial.print("Shf");
//        else if(q[a][b] == 9) Serial.print("Tab");
//        else if(q[a][b] == 10) Serial.print("Enter");
//      }
//      else Serial.print(q[a][b]);
//      if((a == y) && (b == x)) Serial.print(")");
//      else Serial.print(" ");
//    }
//    Serial.println(" ");
//  }
//}
//
//void changeQ(boolean caps)
//{
//  for(int a = 0; a < 4; a++)
//  {
//    for(int b = 0; b < 13; b++)
//    {
//      if(caps == false) q[a][b] = qwerty[a][b];
//      else q[a][b] = QWERTY[a][b];
//    }
//  }
//}

//void printKeys(int currentLayout) //displays stroke map on serial monitor
//{
//  Serial.println(" ");
//  int i = 0;
//  for(int printGridLoc = 1; printGridLoc<= 24; printGridLoc++)
//  {
//    if (printGridLoc == 2) i = 0;
//    else if (printGridLoc == 18) i = 1;
//    else if (printGridLoc == 9) i = 2;
//    else if (printGridLoc == 11) i = 3;
//    else if (printGridLoc == 6) i = 4;
//    else if (printGridLoc == 22) i = 5;
//    else if (printGridLoc == 13) i = 6;
//    else if (printGridLoc == 15) i = 7;
//    else i = 8;
//    if(i != 8)
//    {
//      switch (joyBankTypes[currentLayout][i])
//      {
//      case 's':
//        Serial.print(" Setup ");
//        break;
//      case 'C':
//        Serial.print(char(joyBankVals[currentLayout][i]));
//        Serial.print(" click");
//        break;
//      case 'c':
//        Serial.print(char(joyBankVals[currentLayout][i]));
//        Serial.print(" click");
//        break;
//      case 'y':
//        if(joyBankVals[currentLayout][i] < 0)
//        {
//          Serial.print("mouse u");
//        }
//        if(joyBankVals[currentLayout][i] > 0)
//        {
//          Serial.print("mouse d");
//        }
//        break;
//      case 'x':
//        if(joyBankVals[currentLayout][i] < 0)
//        {
//          Serial.print("mouse l");
//        }
//        if(joyBankVals[currentLayout][i] > 0)
//        {
//          Serial.print("mouse r");
//        }
//        break;
//      case 'K':
//        if(joyBankVals[currentLayout][i] == 32)
//        {
//          Serial.print(" space ");
//        }
//        else if(joyBankVals[currentLayout][i] < 128)
//        {
//          Serial.print("   ");
//          Serial.print(char(joyBankVals[currentLayout][i]));
//          Serial.print("   ");
//        }
//        else
//        {
//          switch (joyBankVals[currentLayout][i])
//          {
//          case 128:
//            Serial.print("  ctrl ");
//            break;
//          case 129:
//            Serial.print(" shift ");
//            break;
//          case 130:
//            Serial.print("  alt  ");
//            break;
//          case 132:
//            Serial.print("  ctrl ");
//            break;
//          case 133:
//            Serial.print(" shift ");
//            break;
//          case 134:
//            Serial.print("  alt  ");
//            break;
//          case 218:
//            Serial.print("u arrow");
//            break;
//          case 217:
//            Serial.print("d arrow");
//            break;
//          case 216:
//            Serial.print("l arrow");
//            break;
//          case 215:
//            Serial.print("r arrow");
//            break;
//          case 178:
//            Serial.print("backspc");
//            break;
//          case 179:
//            Serial.print("  tab  ");
//            break;
//          case 176:
//            Serial.print(" enter ");
//            break;         
//          case 177:
//            Serial.print("  esc  ");
//            break;
//          case 212:
//            Serial.print(" delete");
//            break;
//          case 193:
//            Serial.print("cap loc");
//            break;
//          }
//        }
//      }
//    }
//    else if((printGridLoc == 8) || (printGridLoc == 16) || (printGridLoc == 24))
//    {
//      Serial.println(" ");
//      Serial.println(" ");
//    }
//    else
//    {
//      Serial.print("       ");
//    }
//  }
//  Serial.println("--------------------------------------------------------");
//}

//char arrowTypes[][9] = {  
//  //lu,  ld,  ll,  lr,  ru,  rd,  rl,  rr
//  {
//    's', 'K', 'K', 'K', 'K', 'K', 'K', 'K'                                }
//  , //arrows
//  {
//    's', 'C', 'C', 'C', 'y', 'y', 'x', 'x'                                }
//  , //mouse
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0                                }
//  ,
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0                                } 
//};
//
//int arrowVals[][9] =  {
//  //  lu,  ld,  ll,  lr,  ru,  rd,  rl,  rr,  spd
//  {  
//    1, 176, 'p',  32, 218, 217, 216, 215,    2                                }
//  ,
//  {  
//    1, 'm', 'l', 'r',  -1,   1,  -1,   1,    2                                }
//  ,
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0,    0                                }
//  ,
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0,    0                                }
//};
//
//char minecraftTypes[][9] = {  
//  //lu,  ld,  ll,  lr,  ru,  rd,  rl,  rr
//  {
//    's', 'K', 'C', 'C', 'y', 'y', 'x', 'x'                                }
//  ,
//  {
//    's', 'K', 'C', 'C', 'K', 'K', 'x', 'x'                                }
//  ,
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0                                }
//  ,
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0                                } 
//};
//
//int minecraftVals[][9] =  {
//  //  lu,  ld,  ll,  lr,  ru,  rd,  rl,  rr,  spd
//  {  
//    1, 'e', 'l', 'r',  -1,   1,  -1,   1,    2                                }
//  , // look and place
//  {  
//    1, 177, 'l', 'r', 'w', 's',  -1,   1,    2                                }
//  , // move
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0,    0                                }
//  ,
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0,    0                                }
//};                         
//
//
//
//char raidenTypes[][9] = {  
//  //lu,  ld,  ll,  lr,  ru,  rd,  rl,  rr
//  {
//    's', 'K', 'K', 'K', 'K', 'K', 'K', 'K'                                }
//  , //arrows
//  {
//    's', 'C', 'C', 'C', 'y', 'y', 'x', 'x'                                }
//  , //mouse
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0                                }
//  ,
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0                                } 
//};
//
//int raidenVals[][9] =  {
//  //  lu,  ld,  ll,  lr,  ru,  rd,  rl,  rr,  spd
//  {  
//    1, 176, 'z', 'x', 218, 217, 216, 215,    2                                }
//  ,
//  {  
//    1, 'm', 'l', 'r',  -1,   1,  -1,   1,    2                                }
//  ,
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0,    0                                }
//  ,
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0,    0                                }
//};                         
//
//char wasdTypes[][9] = {  
//  //lu,  ld,  ll,  lr,  ru,  rd,  rl,  rr
//  {
//    's', 'K', 'K', 'K', 'K', 'K', 'K', 'K'                                }
//  , //arrows
//  {
//    's', 'C', 'C', 'C', 'y', 'y', 'x', 'x'                                }
//  , //mouse
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0                                }
//  ,
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0                                } 
//};
//
//int wasdVals[][9] =  {
//  //  lu,  ld,  ll,  lr,  ru,  rd,  rl,  rr,  spd
//  {  
//    1, 176, 'p',  32, 'w', 's', 'a', 'd',    2                                }
//  ,
//  {  
//    1, 'm', 'l', 'r',  -1,   1,  -1,   1,    2                                }
//  ,
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0,    0                                }
//  ,
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0,    0                                }
//};                         
//
//char qwopTypes[][9] = {  
//  //lu,  ld,  ll,  lr,  ru,  rd,  rl,  rr
//  {
//    's', 'K', 'K', 'K', 'K', 'K', 'K', 'K'                                }
//  , //arrows
//  {
//    's', 'C', 'C', 'C', 'y', 'y', 'x', 'x'                                }
//  , //mouse
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0                                }
//  ,
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0                                } 
//};
//
//int qwopVals[][9] =  {
//  //  lu,  ld,  ll,  lr,  ru,  rd,  rl,  rr,  spd
//  {  
//    1, 176, 'p',  32, 'q', 'o', 'w', 'p',    2                                }
//  ,
//  {  
//    1, 'm', 'l', 'r',  -1,   1,  -1,   1,    2                                }
//  ,
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0,    0                                }
//  ,
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0,    0                                }
//};                         
//
//char tetrisTypes[][9] = {  
//  //lu,  ld,  ll,  lr,  ru,  rd,  rl,  rr
//  {
//    's', 'K', 'K', 'K', 'K', 'K', 'K', 'K'                                }
//  , //arrows
//  {
//    's', 'C', 'C', 'C', 'y', 'y', 'x', 'x'                                }
//  , //mouse
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0                                }
//  ,
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0                                } 
//};
//
//int tetrisVals[][9] =  {
//  //  lu,  ld,  ll,  lr,  ru,  rd,  rl,  rr,  spd
//  {  
//    1, 217, 133, 177, 218, ' ', 216, 215, 2                                }
//  ,
//  {  
//    1, 'm', 'l', 'r',  -1,   1,  -1,   1,    2                                }
//  ,
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0,    0                                }
//  ,
//  {  
//    0,   0,   0,   0,   0,   0,   0,   0,    0                                }
//};


