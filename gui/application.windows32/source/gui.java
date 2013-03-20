import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.serial.*; 
import java.awt.Frame; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class gui extends PApplet {

//-----------INIT SECTION-------------------------------------//



//Default types and values arrays, used to store current layout//
char[] types = {
  'y', 'y', 'x', 'x', 's', 'c', 'C', 'C'
};
int[] vals = {
  -1, 1, -1, 1, 1, 'm', 'r', 'l', 1
};


//Frame frame;
Layout layout;             //Layout display on the top
Keyboard keyboard;         //Keyboard


ButtonGroup profilesButtons, //Button group for profile options taken from XML
modesButtons, //for mode options
strokeOptions, //for stroke types
moveOptions, //pain
clickOptions, //pain
keyboardOptions, //pain
setupOptions;  //pain


Serial port; //for communication with controller


XML xml;
XML[] profiles; //topmost level, holds all profile types
XML[] sets;    //level within profile, holds sets within one profile
XML[] strokes;  //level within set, holds strokes within one set
int currentProfile = 0; //tracks profile within the profiles[] array


PFont f;  //for displaying text


//Mode trackers, draw uses them to determine what to display
int START_MODE = 0, //standard mode
FIND_MODE_MODE = 1, //when controller is held and user starts to use gui
SELECT_PROFILE_MODE = 2, //user picks a new profile to load
KEYBOARD_MODE = 3, //user uses the keyboard to enter text
CUSTOM_SET_MODE = 4; //user defines a custom set
int mode = START_MODE;  //mode stores the current mode of the gui



Boolean done; //unused right now
int type = 0; //within custom set?
int strokeLoc = 0;
int stdTextSize = 16;

public void stop(){
  println("Stopping");
  sendProfile(0);
}

public void setup() {
  xml = loadXML("test.xml");
  profiles = xml.getChildren("profile"); //load profiles
  sets = profiles[0].getChildren("set"); //start sets within standard profile

  String portStringTemp = ""; //string to store serial port, helps resolve timing issues
  int count = 0;  //tells whether port has changed at all since startup
  boolean gotPort = false; //tells whether port has been found


  /*Arduino leonardo when starting up establishes a serial port, 
   then after some time that port disappears and the leonardo creates
   a new one. The second one is the one that works. This while loop
   runs if the gui is running while the arduino is still starting up.
   It must wait for the first port to appear and dissappear then wait
   the second port to appear. There are no problems as long as the
   controller isnt unplugged and replugged in while the gui is open*/

  while ( ( (Serial.list ()).length == 0)) { //gui started first, no ports yet
    while (gotPort == false) {
      try {
        if (count == 0) { //is this is the first port seen
          portStringTemp = Serial.list()[0];  //set port to first on the list, expect it to switch
          count++;  //recieved first port, now looking for second
        }
        else if (!(Serial.list ()[0].equals(portStringTemp))) { //the port switched from the first
          gotPort = true;  //done
        }
      }
      catch (Exception e) {
      } //catches array out of bounds exception if there are no ports
    }
  }

  println(Serial.list()[0]); //troubleshooting
  port = new Serial(this, Serial.list()[0], 9600); //instantiate serial with proper port and speed 

  size(480, 272); //window size
  frame.setResizable(true);
  
  f = createFont("Arial", stdTextSize, true); //create font
  textFont(f);  //set displaying font to the one we just created
  textAlign(CENTER, CENTER); //easiest to align text from the centers
  smooth();  //makes gui look nice

  layout = new Layout(types, vals, width/2, height/2); //instantiate layout with startup values and types
  keyboard = new Keyboard(width/2, height/2);  //instantiate keyboard with a location

  profilesButtons = new ButtonGroup(profiles.length, width/2, 50);  //use profiles[] to instantiate buttons

  modesButtons = new ButtonGroup(3, width/2, 10); //instantiate mode buttons
  modesButtons.init(0, "Profile Change", 2); //last number correlates to mode selected when pressed
  modesButtons.init(1, "Keyboard", 3);
  modesButtons.init(2, "Custom Set", 4);

  //wipe();
  //background(255, 166, 0);
}

ButtonGroup[] subOptions = new ButtonGroup[4];

public void draw() {
  wipe();  //clears screen with backround color

  if (mode == START_MODE) {
    layout.display();  //show the current layout
    //keyboard.display();  //show the keyboard
    if (port.available() > 0) {  //if the controller switches layouts or moves into gui
      mode = layout.read();  //when layout is changed, mode stays, 
      //when controller looks to gui, mode is find mode mode
    }
  }

  else if (mode == FIND_MODE_MODE) { //first selection mode
    mode = modesButtons.moveCursor();  //move the cursor between options, selection changes the mode
    if (mode == 255) mode = 0;
    //layout.displayNav();
    
    if (mode == -1) mode = 1;  //nothing new selected
    if (mode == 4) { //custom set mode
      setupOptions = new ButtonGroup(width/2, 10);

      strokeOptions = new ButtonGroup(4, width/2, 10); //instantiate a button group for stroke options
      strokeOptions.init(0, "Setup", 0);
      strokeOptions.init(1, "Move Mouse", 1);
      strokeOptions.init(2, "Click Mouse", 2);
      strokeOptions.init(3, "Keyboard", 3);

      moveOptions = new ButtonGroup(4, width/2, 50); //instantiate a button group for mouse movements
      moveOptions.init(0, "Mouse Up", 'u');
      moveOptions.init(1, "Mouse Down", 'd');
      moveOptions.init(2, "Mouse Left", 'l');
      moveOptions.init(3, "Mouse Right", 'r');

      clickOptions = new ButtonGroup(3, width/2, 50); //instantiate a button group for clicks
      clickOptions.init(0, "Left Click", 'l');
      clickOptions.init(1, "Right Click", 'r');
      clickOptions.init(2, "Middle Click", 'm');

      subOptions[0] = setupOptions;
      subOptions[1] = moveOptions;
      subOptions[2] = clickOptions;
      subOptions[3] = keyboard;

      layout.clear();
      done = true;
    }
    else if (mode == 2) {
      for (int i = 0; i < profilesButtons.getSize(); i++) {
        profilesButtons.init(  i, 
        profiles[i].getString("name"), 
        profiles[i].getInt("numSets"));
      }
    }
    modesButtons.display();
  }

  else if (mode == SELECT_PROFILE_MODE) {
    //layout.displayNav();
    if (profilesButtons.moveCursor() > 0) {
      sendProfile(profilesButtons.pressInt());
    }
    modesButtons.display();
    profilesButtons.display();
  }

  else if (mode == KEYBOARD_MODE) {
    //layout.displayCustom( "Move\nUp", 
//    "Move\nDown", 
//    "Move\nLeft", 
//    "Move\nRight", 
//    "Quit to\nNormal", 
//    "Enter\nKey", 
//    "Backspace \nKey", 
//    "Select");
    keyboard.enterText();
    //modesButtons.display();
    keyboard.display();
  }

  else if (mode == CUSTOM_SET_MODE) {
    if (strokeLoc > 7) {
      layout.createXML();
      profiles = xml.getChildren("profile");
      sendProfile(profiles.length - 1);
      strokeLoc = 0;
    }
    if (done) {
      layout.highlight(strokeLoc);
      type = strokeOptions.moveCursor();
      layout.display();
      strokeOptions.display();
      println(type);      
      if (type != -1 && type != 255) {
        if (type != 0) subOptions[type].display();
        strokeOptions.clearCursor();
        done = false;
      }
    }
    else if (!done) {
      println("moving cursor");
      int press = subOptions[type].moveCursor();
      if (type == 0) {
        println("I know it's setup");
        layout.setStroke(strokeLoc, 's', 1);
        strokeLoc++;
        done = true;
        println("I'm leaving type == 0 stuff");
      }
      else {
        layout.highlight(strokeLoc);
        strokeOptions.display();
        subOptions[type].display();
        layout.display();
        if (press > 0) {
          while (subOptions[type].moveCursor () == press) {
          }
          done = true;
          char sendingType = 0;
          if (type == 0) sendingType = 's';
          else if (type == 1) {
            if (press == 'u') {
              sendingType = 'y';
              press = -1;
            }
            if (press == 'd') {
              sendingType = 'y';
              press = 1;
            }
            if (press == 'l') {
              sendingType = 'x';
              press = -1;
            }
            if (press == 'r') {
              sendingType = 'x';
              press = 1;
            }
          }
          else if (type == 2 && press == 'r') sendingType = 'c';
          else if (type == 2 && press == 'l') sendingType = 'c';
          else if (type == 2 && press == 'm') sendingType = 'C';
          else if (type == 3) sendingType = 'K';
          println(sendingType + ", " + press);
          layout.setStroke(strokeLoc, sendingType, press);
          strokeLoc++;
        }
      }
    }
  }
}

public void mouseMoved(){
  if(mode == START_MODE){
    keyboard.mouseCursor();
  }
  else if(mode == FIND_MODE_MODE){
    modesButtons.mouseCursor(); 
  }
}

public void sendProfile(int inProfile) {
  println("Sending profile: " + inProfile);
  sets = profiles[inProfile].getChildren("set");
  for (int i = 0; i < profiles[inProfile].getInt("numSets"); i++) {
    sendSet(i);
  }
  currentProfile = inProfile;
  
  while(port.available() == 0){}
  delay(1000);
  while(port.available() > 0){
    print(PApplet.parseChar(port.read()));
  }
}

public void sendSet(int inSet) {
  XML[] 
    strokes = sets[inSet].getChildren("stroke");
  for (int i = 0; i < strokes.length; i++) {
    String typeString = strokes[i].getString("type");
    types[i] = typeString.charAt(0);
    String valString = strokes[i].getString("value");
    int valInt = PApplet.parseInt(valString);
    if (valInt == 0) {
      valInt = valString.charAt(0);
    }
    vals[i] = valInt;
  }
  vals[8] = sets[inSet].getInt("speed");
  for (int i = 0; i < 8; i++) {
    port.write(types[i]);
    //println(types[i]);
  }
  for (int i = 0; i < 9; i++) {
    port.write(vals[i]);
    //println(vals[i]);
  }
  mode = 0;
}



public void wipe() {
  fill(255, 166, 0);
  rect(0, 0, displayWidth, displayHeight);
  fill(255);
//  line(width/2, 0, width/2, height);
//  line(0, height/2, width, height/2);
}



 class Button {

  String cover;
  int value;
  int x;
  int y;
  int keyHeight = 28;
  int keySpace = 3;
  int keyWidth;
  int arrayLocation;

  Button(String inCover, int inValue){
    cover = inCover;
    value = inValue;
    if(cover.length() == 1) keyWidth = keyHeight;
    else if(cover.equals("  ")) keyWidth = 6*keyHeight + 5*keySpace;
    else if(cover.equals("Tab")) keyWidth = 2*keyHeight + keySpace;
    else if(cover.equals("Caps Lock")) keyWidth = 2*keyHeight + keySpace;
    else if(cover.equals("Enter")) keyWidth = 2*keyHeight + keySpace;
    else if(cover.equals("Shift")) keyWidth = 2*keyHeight + keySpace;
    else if(cover.equals("Ctrl")) keyWidth = 2*keyHeight + keySpace;
    else if(cover.equals("Done")) keyWidth = 2*keyHeight + keySpace;
    else if(cover.equals("Bksp")) keyWidth = 2*keyHeight + keySpace;
    else if(cover.equals("Left")) keyWidth = keyHeight;
    else if(cover.equals("Down")) keyWidth = keyHeight;
    else if(cover.equals("Right")) keyWidth = keyHeight;
    else if(cover.equals("Up")) keyWidth = 3*keyHeight + 2*keySpace;
    else if(cover.length() < 1) keyWidth = 0;
    else keyWidth = PApplet.parseInt(textWidth(cover) * 1.05f);
  }

  public int getWidth() {
    return keyWidth;
  }

  public boolean isIn(int inX, int inY){
    if(x < inX && inX < x + keyWidth && y < inY && inY < y + keyHeight) return true;
    else return false;
  }

  public void display(int inX, int inY, int inColor){
    x = inX;
    y = inY;
    int tSize = stdTextSize;
    fill(inColor);
    rect(inX,inY,keyWidth,keyHeight, 5);
    fill(255);
    textSize(stdTextSize);
    while(textWidth(cover) > (keyWidth * .975f)){
      tSize--;
      textSize(tSize);
    }
    text(cover, inX+(keyWidth/2), inY+(keyHeight/2));
    textSize(stdTextSize);
  }
  
  public void display(int inColor) { //for cursor purposes
    int tSize = stdTextSize;
    fill(inColor);
    rect( x, y , keyWidth, keyHeight, 5);
    fill(255);
    textSize(stdTextSize);
    while(textWidth(cover) > (keyWidth* .975f)){
      tSize--;
      textSize(tSize);
    }
    text(cover, x + (keyWidth/2), y + (keyHeight/2));
    textSize(stdTextSize);
  }

  public void press(){
    if(cover.equals("Done")){
      sendProfile(profilesButtons.pressInt());
      mode = 0;
    }
    else port.write(value);
  }
  
  public int pressInt(){
    return value;
  }
  
  public boolean isBlank(){
    if(cover.equals("")){
      return true;
    }
    else {
      return false;
    }
  }
  
  public void move(int in){
    arrayLocation += in;
    println("Moved " + in);
  }
  
  public int getArrayLocation(){
    return arrayLocation;
  }
}
class ButtonGroup {
  Button[] buttons;
  int x;
  int y;
  int cursorLoc = 0;
  Button cursor;

  ButtonGroup(int inX, int inY) {
    x = inX;
    y = inY;
  }

  ButtonGroup(int inSize, int inX, int inY) {
    buttons = new Button[inSize];
    x = inX;
    y = inY;
    cursor = buttons[cursorLoc];
  }

  public void display() {
    centerX();
    //wipe();
    cursor = buttons[cursorLoc];
    noStroke();
    smooth();
    fill(color(255, 166, 0));
    //    rect(x-10, y-10, 1000, 100);
    int trackX = 0;
    for(int i = 0; i < buttons.length; i++){
      trackX += buttons[i].getWidth() + 5;
    }
    trackX = x - (trackX / 2);
    int trackY = y;
    for (int i = 0; i < buttons.length; i++) {
      buttons[i].display(trackX, trackY, color(84, 14, 176));
      trackX += buttons[i].getWidth() + 5;
    }
    cursor.display(color(0, 191, 57));
  }

  public void mouseCursor() {
    for (int i = 0; i < buttons.length; i++) {
      //println(mouseX + ", " + mouseY + " vs. " + buttons[i].x + ", " + buttons[i].y);
      if (buttons[i].isIn(mouseX, mouseY)) {
        println(i);
        cursorLoc = i;
        i = buttons.length;
      }
    }
    this.display();
  }

  public void init(int num, String inCover, int inValue) {
    buttons[num] = new Button(inCover, inValue);
  }

  public void init(int num, Button inButton) {
    buttons[num] = inButton;
  }

  public void clearCursor() { 
    cursor = null;
  } 

  public void centerX(){
    x = width/2;
  }

  public int moveCursor() {
    char in = ' ';
    if (port.available() > 0) { 
      in = port.readChar();
      if (in == 'r') {
        cursorLoc++;
      }
      else if (in == 'l') {  
        cursorLoc--;
      }
      else if (in == 'R') {
        println(cursor.pressInt());
        return cursor.pressInt();
      }
      //      else if (in == 'd') {
      //        return 'd';
      //      }
      //      else if (in == 'u') {
      //        return 'u';
      //      }
      else if (in == 'U') {
        sendProfile(0);
        //mode = 0;
        return 255;
      }
      cursorLoc = (cursorLoc + buttons.length) % buttons.length;
      cursor = buttons[cursorLoc];
    }
    return -1;
  }

  public int pressInt() {
    return cursorLoc;
  }

  public int getSize() {
    return buttons.length;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }
}

class SetMenu{
  ButtonGroup firstButtons;
  ButtonGroup[] arrayOfButtonGroups;
  int x;
  int y;
  
  SetMenu(ButtonGroup inButtons){
    firstButtons = inButtons;
    x = firstButtons.x;
    y = firstButtons.y;
  }
}
    
class Keyboard extends ButtonGroup{

  int x;
  int y;
  int rows = 5;
  int cols = 15;
  int curR = 2;
  int curC = 7;
  int keySize = 28;
  int keySpace = 3;
  String[][] coverList = {
    {
      "`", "", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "Bksp"
    }
    , 
    {
      "Tab", "", "q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "[", "]", "\\",
    }
    , 
    {
      "Caps Lock", "", "a", "s", "d", "f", "g", "h", "j", "k", "l", ";", "'", "", "Enter"
    }
    , 
    {
      "Shift", "", "z", "x", "c", "v", "b", "n", "m", ",", ".", "/", "", "Up", ""
    }
    , 
    { 
      "Ctrl", "", "Done", "", "", "", "", "  ", "", "", "Done", "", "Left", "Down", "Right"
    }
  };

  int[][] valueList = {
    {
      '`', -1, '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '-', '=', 178
    }
    , 
    {
      179, -1, 'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', '[', ']', 92
    }
    , 
    {
      193, -1, 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ';', '"', 1, 176
    }
    , 
    {
      132, -1, 'z', 'x', 'c', 'v', 'b', 'n', 'm', ',', '.', '/', 1, 218, -1
    }
    , 
    {
      128, -1, 0, 1, 1, 1, 1, ' ', -1, 1, 0, 1, 216, 217, 215
    }
  };

  Button[][] keys = new Button[rows][cols]; 
  Button cursor;

  Keyboard(int inX, int inY) {
    super(inX, inY);
    x = inX;
    y = inY;
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        keys[i][j] = new Button(coverList[i][j], valueList[i][j]);
      }
    }
    cursor = keys[curR][curC];
  }

  public void clearCursor(){
    cursor = null;
  }

  public void getCursor(){
    cursor = keys[curR][curC];
  }

  public void press() {
    cursor.press();
  }

  public void center(){
    x = width/2;
    y = height/2;
  }

  public void mouseCursor(){
    for(int i = 0; i < rows; i++){
      for(int j = 0; j < cols; j++){
        if(keys[i][j].isIn(mouseX, mouseY)){
          cursor = keys[i][j];
          i = rows;
          j = cols;
        }
      }
    }
  }

  public void display() {
    center();
    wipe();
    noStroke();
    smooth();
    fill(color(255, 166, 0));
    //    rect(x-10, y-10, 1000, 1000);
    int trackX = PApplet.parseInt(x - ((PApplet.parseFloat(cols/2) + .5f) * (keySize + keySpace)));
    int trackY = PApplet.parseInt(y - ((PApplet.parseFloat(rows/2) + .5f) * (keySize + keySpace)));
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        keys[i][j].display(trackX, trackY, color(84, 14, 176));
        if (keys[i][j].getWidth() != 0) trackX += keys[i][j].getWidth() + keySpace;
      }
      trackY += (keySize + keySpace);
      trackX = PApplet.parseInt(x - ((PApplet.parseFloat(cols/2) + .5f) * (keySize + keySpace)));
    }
    cursor.display(color(0, 191, 57));
  }

  public int moveCursor() {
    char in = ' ';
    if (port.available() > 0) { 
      in = port.readChar();
      if (in == 'r') {
        this.moveCursor(1, 0);
      }
      else if (in == 'l') {  
        this.moveCursor(-1, 0);
      }
      else if (in == 'u') {  
        this.moveCursor(0, 1);
      }
      else if (in == 'd') {  
        this.moveCursor(0, -1);
      }
      else if (in == 'R') {
        println(cursor.pressInt());
        return cursor.pressInt();
      }
      else if (in == 'U') {
        sendProfile(0);
        //mode = 0;
        return 0;
      }
    }
    return -1;
  }

  public void moveCursor(int moveX, int moveY) {
    curC += moveX;
    curR -= moveY;
    curC = (curC + cols) % cols;
    curR = (curR + rows) % rows;
    cursor = keys[curR][curC];
    if (cursor.isBlank()) {
      if (moveX == 0) {
        moveCursor(valueList[curR][curC], 0);
      }
      else moveCursor(moveX, 0);
    }
  }

  public void enterText() {
    char in = ' ';
    if (port.available() > 0) { 
      in = port.readChar();
      switch(in) {
      case 'u':
        keyboard.moveCursor(0, 1);
        break;
      case 'd':
        keyboard.moveCursor(0, -1);
        break;
      case 'l':
        keyboard.moveCursor(-1, 0);
        break;
      case 'r':
        keyboard.moveCursor(1, 0);
        break;
      case 'U':
        mode = 0;
        sendProfile(0);
        break;
      case 'D':
        port.write(176);
        break;
      case 'L':
        port.write(178);
        break;
      case 'R':
        keyboard.press();
        break;
      }
    }
  }
}

class Layout {

  int x;
  int y;
  char[] types = new char[8];
  int[] vals = new int[9];
  int d = 50;
  int currentSet;
  int newSets = 0;
  XML customProf;

  Layout(char[] inTypes, int[] inVals, int inX, int inY) {
    for (int i = 0; i < 8; i++) {
      types[i] = inTypes[i];
      vals[i] = inVals[i];
    }
    println("inX: " + inX);
    println("x: " + x);
    x = inX - (3 * d);
    y = inY;
  }

  public int read() {
    int numSets = profiles[currentProfile].getInt("numSets");
    int inRead = port.read();
    println(inRead);
    if (inRead == 255) return 1;
    currentSet = (inRead + numSets) % numSets;
    strokes = sets[currentSet].getChildren("stroke");
    for (int i = 0; i < 8; i++) {
      String typeString = strokes[i].getString("type");
      types[i] = typeString.charAt(0);

      String valString = strokes[i].getString("value");
      int valInt = PApplet.parseInt(valString);
      if (valInt == 0) {
        valInt = valString.charAt(0);
      }
      vals[i] = valInt;
    }
    return 0;
    //    for (int i = 0; i < 8; i++) {
    //      types[i] = port.readChar();
    //    }
    //    for (int i = 0; i < 8; i++) {
    //      vals[i] = port.read();
    //      if ((vals[i] > 128) && (types[i] != 'k') && (types[i] != 'K'))  vals[i] -= 256;
    //    }
    //    if ((types[0] == 0) && (types[1] == 0) && (types[2] == 0) && (types[3] == 0) && 
    //      (types[4] == 0) && (types[5] == 0) && (types[6] == 0) && (types[7] == 0)) {
    //      for (int i = 0; i < profilesButtons.getSize(); i++) {
    //        profilesButtons.init(i, profiles[i].getString("name"), profiles[i].getInt("numSets"));
    //      }
    //      return 1;
    //    }
  }

  public void displayCustom(String s0, String s1, String s2, String s3, String s4, String s5, String s6, String s7) {
    center();
    fill(255, 166, 0);
    noStroke();
    //    rect(x-d, y-d, 1000, 1000);
    fill(255);
    stroke(0);
    ellipseMode(CENTER);
    ellipse(x(1), y(2), 15, 15);
    ellipse(x(5), y(2), 15, 15);
    fill(0);
    textSize(stdTextSize);
    text(s0, x(1), y(1));
    text(s1, x(1), y(3));
    text(s2, x(0), y(2));
    text(s3, x(2), y(2));
    text(s4, x(5), y(1));
    text(s5, x(5), y(3));
    text(s6, x(4), y(2));
    text(s7, x(6), y(2));
  }    

  public void center(){
    x = width / 2;
    y = height / 2;
  }

  public void clear(){
    for(int i = 0; i < 8; i++){
      types[i] = 0;
      vals[i] = 0;
    }
    vals[8] = 0;
  }
  
  public void displayNav() {
    center();
    this.displayCustom(
    "Move\nUp", 
    "Move\nDown", 
    "Move\nLeft", 
    "Move\nRight", 
    "Quit to\nNormal", 
    "TBD", 
    "TBD", 
    "Select");
  }
  
  public void setStroke(int loc, char inType, int inVal){
    types[loc] = inType;
    vals[loc] = inVal;
    println("layout.setStroke(): " + types[loc] + ", " + vals[loc]);
  }
    
  public void createXML(){
    println("XML CREATION");
    if(newSets == 0){
      customProf = xml.addChild("profile");
      customProf.setString("name", "Custom");
    }
    newSets++;
    customProf.setInt("numSets", newSets);
    XML customSet = customProf.addChild("set");
    customSet.setString("name", "Custom");
    customSet.setString("speed", "2");
    String sendType = "";
    for(int i = 0; i < 8; i++){
      sendType += types[i];
      XML customStroke = customSet.addChild("stroke");
      customStroke.setString("type", sendType);
      customStroke.setInt("value", vals[i]);
      sendType = "";
    }
    println(customProf);
    
  }

  public void display() {
    center();
    fill(255, 166, 0);
    noStroke();
    //    rect(x-d, y-d, 1000, 1000);
    fill(255);
    stroke(0);
    ellipseMode(CENTER);
    ellipse(x(1), y(2), 15, 15);
    ellipse(x(5), y(2), 15, 15);
    fill(0);
    text("Profile: " + profiles[currentProfile].getString("name"), x(0), y(0));
    text("Set: " + sets[currentSet].getString("name"), x(3), y(0));
    text("Speed: " + sets[currentSet].getInt("speed"), x(6), y(0));
    text(findString(0), x(1), y(1));
    text(findString(1), x(1), y(3));
    text(findString(2), x(0), y(2));
    text(findString(3), x(2), y(2));
    text(findString(4), x(5), y(1));
    text(findString(5), x(5), y(3));
    text(findString(6), x(4), y(2));
    text(findString(7), x(6), y(2));
  }

  public void highlight(int inLoc) {
    rectMode(CENTER);
    noStroke();
    fill(color(0, 191, 57));
    switch (inLoc) {
    case 0:
      rect(x(1), y(1), d, d, 5);
      break;
    case 1:
      rect(x(1), y(3), d, d, 5);
      break;
    case 2:
      rect(x(0), y(2), d, d, 5);
      break;
    case 3:
      rect(x(2), y(2), d, d, 5);
      break;
    case 4:
      rect(x(5), y(1), d, d, 5);
      break;
    case 5:
      rect(x(5), y(3), d, d, 5);
      break;
    case 6:
      rect(x(4), y(2), d, d, 5);
      break;
    case 7:
      rect(x(6), y(2), d, d, 5);
      break;
    }
    rectMode(CORNER);
  }

    public float x(float num) {
      num -= 3;
      return x + (num * d);
    }

    public float y(float num) {
      num -= 1.5f;
      return y + (num * d);
    }

    public String findString(int num) {
      String str = "";
      if (types[num] == 'y') {
        str += "Mouse\n";
        if (vals[num] < 0) { 
          str += "up";
        }
        else if (vals[num] > 0) { 
          str += "down";
        }
      }
      else if (types[num] == 'x') {
        str += "Mouse\n";
        if (vals[num] > 0) {  
          str += "Right";
        }
        else if (vals[num] < 0) {  
          str += "Left";
        }
      }
      else if (types[num] == 'c' || types[num] == 'C') {
        switch(vals[num]) {
        case 'r':   
          str += "Right";  
          break;
        case 'l':   
          str += "Left";   
          break;
        case 'm':   
          str += "Middle"; 
          break;
        }
        str += "\nClick";
      }
      else if (types[num] == 's') {
        str += "Setup \nKey";
      }
      else if (types[num] == 'k' || types[num] == 'K') {
        switch(vals[num]) {
        case 8: 
          str += "Backspace \nKey";   
          break;
        case 9: 
          str += "Tab \nKey";   
          break;
        case 10: 
          str += "Enter \nKey";   
          break;
        case 27: 
          str += "Esc \nKey";   
          break;
        case 32: 
          str += "Space \nKey";   
          break;
        case 128: 
          str += "Ctrl \nKey";   
          break;
        case 129: 
          str += "Shift \nKey";   
          break;
        case 130: 
          str += "Alt \nKey";   
          break;
        case 132: 
          str += "Ctrl \nKey";   
          break;
        case 133: 
          str += "Shift \nKey";   
          break;
        case 134: 
          str += "Alt \nKey";   
          break;
        case 218: 
          str += "Up \nArrow";   
          break;
        case 217: 
          str += "Down \nArrow";   
          break;
        case 216: 
          str += "Left \nArrow";   
          break;
        case 215: 
          str += "Right \nArrow";   
          break;
        case 178: 
          str += "BackSpace \nKey";   
          break;
        case 179: 
          str += "Tab \nKey";   
          break;
        case 176: 
          str += "Return \nKey";   
          break;
        case 177: 
          str += "Esc \nKey";   
          break;
        case 209: 
          str += "Insert \nKey";   
          break;
        case 212: 
          str += "Delete \nKey";   
          break;
        default: 
          str += PApplet.parseChar(vals[num]) + "\nKey"; 
          break;
        }
      }
      return str;
    }
  }

  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "gui" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
