import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.serial.*; 
import java.awt.Frame; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
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
XML[] naviStrokes; //level to hold the navigational strokes
XML[] keyboardStrokes;
XML serialInfo;
int currentProfile = 0; //tracks profile within the profiles[] array

Button textBigger, textSmaller;


PFont f;  //for displaying text#


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
int stdTextSize = 18;

int color1 = 0xff80c8ff;  ////backgroundfill
int color2 = 0xff0090ff;  ////top and buttons
int color3 = 0xffe6f4ff;  //cursor
int colorT = 0xff000000;
int colorTC = 0xff000000;

char KEY_INPUT_CODE = '!',
STROKE_INPUT_CODE = '#',
END_INPUT_CODE = '$',
REGULAR_LAYOUT_CODE = '@',
GUI_ATTENTION_CODE = '*';

public void stop(){
  println("Stopping");
  sendProfile(0);
}

public void setup() {
  xml = loadXML("Catalog.xml");
  profiles = xml.getChildren("profile"); //load profiles
  sets = profiles[currentProfile].getChildren("set"); //start sets within standard profile
  naviStrokes = xml.getChildren("naviStroke");
  keyboardStrokes = xml.getChildren("keyboardStroke");
  serialInfo = xml.getChild("serialInfo");
  

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

//  while ( ( (Serial.list ()).length == 0)) { //gui started first, no ports yet
//    while (gotPort == false) {
//      try {
//        if (count == 0) { //is this is the first port seen
//          portStringTemp = Serial.list()[0];  //set port to first on the list, expect it to switch
//          count++;  //recieved first port, now looking for second
//        }
//        else if (!(Serial.list ()[0].equals(portStringTemp))) { //the port switched from the first
//          gotPort = true;  //done
//        }
//      }
//      catch (Exception e) {
//      } //catches array out of bounds exception if there are no ports
//    }
//  }
  
//  while(gotPort == false){
//    try {
//      for(int i = 0; i <=s Serial.list().length; i++){
//        port = new Serial(this, Serial.list()[i], 9600);
//        delay(500);
//        if(port.read() == GUI_ATTENTION_CODE) gotPort = true;
//      }
//    }
//    catch (Exception e) {}
//  }
//  
//  port.write(GUI_ATTENTION_CODE);
  
//  println(Serial.list()[0]); //troubleshooting
//
//  
  port = new Serial(this, serialInfo.getString("name"), serialInfo.getInt("baud")); //instantiate serial with proper port and speed 

  size(480, 272); //window size
  frame.setResizable(true);
  
  //println(PFont.list());
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
  
  textLeading(stdTextSize/4);
  
  textBigger = new Button("TEXT+", 1);
  textSmaller = new Button("text-", 1);
}

ButtonGroup[] subOptions = new ButtonGroup[4];

public void draw() {
  wipe();  //clears screen with backround color

  textSmaller.display(3, (height - textSmaller.keyHeight - 3), color2);
  textBigger.display((width - textBigger.keyWidth - 3), (height - textBigger.keyHeight  - 3), color2);

  if (mode == START_MODE) {
    layout.display();  //show the current layout
    if (port.available() > 0) {  //if the controller switches layouts or moves into gui
      mode = layout.read();  //when layout is changed, mode stays, 
      //when controller looks to gui, mode is find mode mode
    }
  }

  else if (mode == FIND_MODE_MODE) { //first selection mode
    modesButtons.display();
    mode = modesButtons.moveCursor();  //move the cursor between options, selection changes the mode
    if (mode == GUI_ATTENTION_CODE) mode = 0;
    //layout.displayNav();
    
    if (mode == -1) mode = 1;  //nothing new selected
    if (mode == 4) { //custom set mode
      setupOptions = new ButtonGroup(width/2, 10);

      strokeOptions = new ButtonGroup(4, width/2, 10); //instantiate a button group for stroke options
      strokeOptions.init(0, "Setup", 0);
      strokeOptions.init(1, "Move Mouse", 1);
      strokeOptions.init(2, "Click Mouse", 2);
      strokeOptions.init(3, "Keyboard", 3);

      moveOptions = new ButtonGroup(4, width/2, (10 + (stdTextSize * 2))); //instantiate a button group for mouse movements
      moveOptions.init(0, "Mouse Up", 'u');
      moveOptions.init(1, "Mouse Down", 'd');
      moveOptions.init(2, "Mouse Left", 'l');
      moveOptions.init(3, "Mouse Right", 'r');

      clickOptions = new ButtonGroup(3, width/2, (10 + (stdTextSize * 2))); //instantiate a button group for clicks
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
      profiles = xml.getChildren("profile");
      profilesButtons = new ButtonGroup(profiles.length, width/2, (10 + (stdTextSize * 2)));
      for (int i = 0; i < profilesButtons.getSize(); i++) {
        profilesButtons.init(  i, 
        profiles[i].getString("name"), 
        profiles[i].getInt("numSets"));
      }
    }
    
    else if(mode == CUSTOM_SET_MODE){
      strokeLoc = 0;
    }
  }

  else if (mode == SELECT_PROFILE_MODE) {
    int moveNum = profilesButtons.moveCursorWithUp();
    if (moveNum > 0) {
      sendProfile(profilesButtons.pressInt());
    }
    else if(moveNum == -2) {
      mode = FIND_MODE_MODE;
    }
    modesButtons.display();
    profilesButtons.display();
  }

  else if (mode == KEYBOARD_MODE) {
    keyboard.enterText();
    keyboard.display();
  }

  else if (mode == CUSTOM_SET_MODE) {
    if (done) {
      layout.highlight(strokeLoc);
      type = strokeOptions.moveCursor();
      layout.displayWithoutTop();
      strokeOptions.display();
      println(type);      
      if (type != -1 && type != GUI_ATTENTION_CODE) {
        if (type != 0) subOptions[type].display();
        strokeOptions.clearCursor();
        done = false;
      }
      else if(type == GUI_ATTENTION_CODE){
        layout.clearStrokes();
      }
    }
    else if (!done) {
      wipe();
//      if (type != 3) {
//        int press = subOptions[type].moveCursorWithUp();
//      }
      if (type == 0) {
        layout.setStroke(strokeLoc, 's', 1);
        strokeLoc++;
        done = true;
      }
      else {
        int press = -1;
        if(type == 3) {
          press = subOptions[type].moveCursor();
        }
        else {
          press = subOptions[type].moveCursorWithUp();
        }
        wipe();
        if(type != 3){
          layout.highlight(strokeLoc);
          strokeOptions.display();
          layout.displayWithoutTop();
        }
        subOptions[type].display();
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
        else if (press == -2) {
          done = true;
        }
      }
    }
    if (strokeLoc > 7) {
      layout.createXML();
      profiles = xml.getChildren("profile");
      sendProfile(profiles.length - 1);
      strokeLoc = 0;
    }
  }
}

//void mouseMoved(){
//  if(mode == START_MODE){
//    keyboard.mouseCursor();
//  }
//  else if(mode == FIND_MODE_MODE){
//    modesButtons.mouseCursor(); 
//  }
//  else if(mode == SELECT_PROFILE_MODE){
//    profilesButtons.mouseCursor();
//  }
//  else if(mode == KEYBOARD_MODE){
//    keyboard.mouseCursor();
//  }
//  else if(mode == CUSTOM_SET_MODE){
//    strokeOptions.mouseCursor();
//    if(!done){
//      subOptions[type].moveCursor();
//    }
//  }
//}
//

public void mouseClicked(){
  if(textBigger.isIn(mouseX, mouseY)) stdTextSize++;
  else if(textSmaller.isIn(mouseX, mouseY)) stdTextSize--;
  textSize(stdTextSize);
}

public void sendProfile(int inProfile) {
  println("Sending profile: " + inProfile);
  sets = profiles[inProfile].getChildren("set");
  port.write(STROKE_INPUT_CODE); //start code
  for (int i = 0; i < profiles[inProfile].getInt("numSets"); i++) {
    sendSet(i);
  }
  currentProfile = inProfile;
  port.write(END_INPUT_CODE); //stop code
  layout.reset();
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
  fill(color1);
  rect(0, 0, displayWidth, displayHeight);
  fill(colorT);
//  line(width/2, 0, width/2, height);
//  line(0, height/2, width, height/2);
}



 class Button {

  String cover;
  int value;
  int x;
  int y;
  int keyHeight = stdTextSize * 3 / 2;
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
    else keyWidth = PApplet.parseInt(textWidth(cover) + stdTextSize/2);
  }

  public int getWidth() {
    newWidth();
    return keyWidth;
  }
  
  public int getHeight(){
    return keyHeight;
  }

  public boolean isIn(int inX, int inY){
    if(x < inX && inX < x + keyWidth && y < inY && inY < y + keyHeight) return true;
    else return false;
  }
  
  public void newWidth(){
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
    else keyWidth = PApplet.parseInt(textWidth(cover) + stdTextSize/2);
  }

  public void display(int inX, int inY, int inColor){
    keyHeight = stdTextSize * 3 / 2;
    newWidth();
    x = inX;
    y = inY;
    int tSize = stdTextSize;
    fill(inColor);
    rect(inX,inY,keyWidth,keyHeight, 5);
    fill(colorT);
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
    keyHeight = stdTextSize * 3 / 2;
    newWidth();
    fill(inColor);
    rect( x, y , keyWidth, keyHeight, 5);
    fill(colorT);
    textSize(stdTextSize);
    while(textWidth(cover) > (keyWidth* .975f)){
      tSize--;
      textSize(tSize);
    }
    text(cover, x + (keyWidth/2), y + (keyHeight/2));
    textSize(stdTextSize);
  }
  
  public void displayC(int inColor) { //for cursor purposes
    int tSize = stdTextSize;
    keyHeight = stdTextSize * 3 / 2;
    newWidth();
    fill(inColor);
    rect( x, y , keyWidth, keyHeight, 5);
    fill(colorTC);
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
      sendProfile(currentProfile);
      mode = 0;
    }
    else {
      port.write(value);
    }
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
    fill(color1);
    //    rect(x-10, y-10, 1000, 100);
    int trackX = 0;
    for(int i = 0; i < buttons.length; i++){
      trackX += buttons[i].getWidth() + 5;
    }
    trackX = x - (trackX / 2);
    int trackY = y;
    for (int i = 0; i < buttons.length; i++) {
      buttons[i].display(trackX, trackY, color2);
      trackX += buttons[i].getWidth() + 5;
    }
    cursor.displayC(color3);
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

  public int findStrokeLocation(char inChar){
    int num = 0;
    switch(inChar) {
      case ('u'): num = 0; break;
      case ('d'): num = 1; break;
      case ('l'): num = 2; break;
      case ('r'): num = 3; break;
      case ('U'): num = 4; break;
      case ('D'): num = 5; break;
      case ('L'): num = 6; break;
      case ('R'): num = 7; break;
      case (200): mode = 1; break;
    }
    return num;
  }
  
  public int moveCursor() {
    int index, val;
    char type, inChar;
    inChar = port.readChar();
    if(inChar == GUI_ATTENTION_CODE){
      port.write(GUI_ATTENTION_CODE);
    }
    else if(inChar == REGULAR_LAYOUT_CODE){
      mode = 0;
    }
    else { 
      index = findStrokeLocation(inChar);
      
      String typeString = naviStrokes[index].getString("type");
      type = typeString.charAt(0);
      
      String valString = naviStrokes[index].getString("value");
      val = PApplet.parseInt(valString);
      
      if (type == 'x') {
        cursorLoc += val;
      }
      else if (type == 'c'){
        println(cursor.pressInt());
        return cursor.pressInt();
      }
      else if (type == 's'){
        sendProfile(0);
        mode = 0;
        return GUI_ATTENTION_CODE;
      }
      cursorLoc = (cursorLoc + buttons.length) % buttons.length;
      cursor = buttons[cursorLoc];
    }
    return -1;
  }
  
  public int moveCursorWithUp() {
    int index, val;
    char type, inChar;
    inChar = 0;
    if(port.available() > 0){
      inChar = port.readChar();
    }
    print("  " + inChar);
    if(inChar == GUI_ATTENTION_CODE){
      port.write(GUI_ATTENTION_CODE);
    }
    else if (inChar != 0){ 
      index = findStrokeLocation(inChar);
      
      String typeString = naviStrokes[index].getString("type");
      type = typeString.charAt(0);
      
      String valString = naviStrokes[index].getString("value");
      val = PApplet.parseInt(valString);
      
      if (type == 'x') {
        cursorLoc += val;
      }
      else if(type == 'y' && val == 1) {
        return -2;
      }
      else if (type == 'c'){
        println(cursor.pressInt());
        return cursor.pressInt();
      }
      else if (type == 's'){
        sendProfile(0);
        mode = 0;
        return GUI_ATTENTION_CODE;
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
  int keySize = stdTextSize * 3 / 2;
  int keySpace = 3;
  String[][] coverList = {
    {
      "`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "Bksp", ""
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
  
  String[][] capsList = {
    {
      "~", "!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "_", "+", "Bksp", ""
    }
    , 
    {
      "Tab", "", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "{", "}", "|",
    }
    , 
    {
      "Caps Lock", "", "A", "S", "D", "F", "G", "H", "J", "K", "L", ":", "\"", "", "Enter"
    }
    , 
    {
      "Shift", "", "Z", "X", "C", "V", "B", "N", "M", "<", ">", "?", "", "Up", ""
    }
    , 
    { 
      "Ctrl", "", "Done", "", "", "", "", "  ", "", "", "Done", "", "Left", "Down", "Right"
    }
  };

  int[][] valueList = {
    {
      '`', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '-', '=', 178, -1
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
      133, -1, 'z', 'x', 'c', 'v', 'b', 'n', 'm', ',', '.', '/', 1, 218, -1
    }
    , 
    {
      128, -1, 0, 1, 1, 1, 1, ' ', -1, 1, 0, 1, 216, 217, 215
    }
  };

  Button[][] keys = new Button[rows][cols]; 
  Button[][] capKeys = new Button[rows][cols];
  Button cursor;
  
  boolean shift = false;
  boolean ctrl = false;
  boolean capsLock = false;

  Keyboard(int inX, int inY) {
    super(inX, inY);
    x = inX;
    y = inY;
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        keys[i][j] = new Button(coverList[i][j], valueList[i][j]);
        capKeys[i][j] = new Button(capsList[i][j], valueList[i][j]);
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
    if(!cursor.cover.equals("Done")){
      port.write(KEY_INPUT_CODE); //keyboard start code
    }
    if(cursor.cover.equals("Shift")) {
      shift = !shift;
    }
    else if(cursor.cover.equals("Ctrl")){
      ctrl = !ctrl;
    }
    else if(cursor.cover.equals("Caps Lock")){
      capsLock = !capsLock;
    }
    else {
      shift = false;
      ctrl = false;
    }
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
    fill(color1);
    keySize = stdTextSize * 3 / 2;
    //    rect(x-10, y-10, 1000, 1000);
    int trackX = PApplet.parseInt(x - ((PApplet.parseFloat(cols/2) + .5f) * (keySize + keySpace)));
    int trackY = PApplet.parseInt(y - ((PApplet.parseFloat(rows/2) + .5f) * (keySize + keySpace)));
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        if(shift == false && capsLock == false){
          keys[i][j].display(trackX, trackY, color2);
          if (keys[i][j].getWidth() != 0) trackX += keys[i][j].getWidth() + keySpace;
        }
        else {
          capKeys[i][j].display(trackX, trackY, color2);
          if (capKeys[i][j].getWidth() != 0) trackX += capKeys[i][j].getWidth() + keySpace;
        }
      }
      trackY += (keySize + keySpace);
      trackX = PApplet.parseInt(x - ((PApplet.parseFloat(cols/2) + .5f) * (keySize + keySpace)));
    }
    if(shift == true) keys[3][0].displayC(color3);
    if(ctrl == true) keys[4][0].displayC(color3);
    if(capsLock == true) keys[2][0].displayC(color3);
    cursor.displayC(color3);
  }

//  int moveCursor() {
//    char in = ' ';
//    if (port.available() > 0) { 
//      in = port.readChar();
//      if (in == 'r') {
//        this.moveCursor(1, 0);
//      }
//      else if (in == 'l') {  
//        this.moveCursor(-1, 0);
//      }
//      else if (in == 'u') {  
//        this.moveCursor(0, 1);
//      }
//      else if (in == 'd') {  
//        this.moveCursor(0, -1);
//      }
//      else if (in == 'R') {
//        println(cursor.pressInt());
//        return cursor.pressInt();
//      }
//      else if (in == 'U') {
//        sendProfile(0);
//        //mode = 0;
//        return 0;
//      }
//    }
//    return -1;
//  }

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

  public int findStrokeLocation(char inChar){
    int num = 0;
    switch(inChar) {
      case ('u'): num = 0; break;
      case ('d'): num = 1; break;
      case ('l'): num = 2; break;
      case ('r'): num = 3; break;
      case ('U'): num = 4; break;
      case ('D'): num = 5; break;
      case ('L'): num = 6; break;
      case ('R'): num = 7; break;
    }
    return num;
  }

  public void enterText() {
    int index, val;
    char type;
    if (port.available() > 0) { 
      index = findStrokeLocation(port.readChar());
      
      String typeString = keyboardStrokes[index].getString("type");
      type = typeString.charAt(0);
      
      String valString = keyboardStrokes[index].getString("value");
      val = PApplet.parseInt(valString);
      
      switch(type) {
      case 'y':
        keyboard.moveCursor(0, val);
        break;
      case 'x':
        keyboard.moveCursor(val, 0);
        break;
      case 's':
        mode = 0;
        sendProfile(currentProfile);
        break;
      case 'c':
        if(val == 1){ 
          keyboard.press();
        }
        else {
          port.write(KEY_INPUT_CODE);
          port.write(val);
        }
        break;
      }
    }
  }
  
  public int moveCursor() {
    int index, val;
    char type;
    if (port.available() > 0) { 
      index = findStrokeLocation(port.readChar());
      
      String typeString = naviStrokes[index].getString("type");
      type = typeString.charAt(0);
      
      String valString = naviStrokes[index].getString("value");
      val = PApplet.parseInt(valString);
      
      switch(type) {
      case 'y':
        keyboard.moveCursor(0, val);
        break;
      case 'x':
        keyboard.moveCursor(val, 0);
        break;
      case 's':
        sendProfile(0);
        return GUI_ATTENTION_CODE;
      case 'c':
        println(cursor.pressInt());
        return cursor.pressInt();
      }
    }
    return -1;
  }
  
}

class Layout {

  int x;
  int y;
  char[] types = new char[8];
  int[] vals = new int[9];
  int dx = width/8;
  int dy = height/6;
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
    x = inX - (3 * dx);
    y = inY;
  }

  public int read() {
    int numSets = profiles[currentProfile].getInt("numSets");
    print((int)port.read() + " ");
    int inRead = port.read();
    println(inRead);
    if (inRead == GUI_ATTENTION_CODE) {
      port.write(GUI_ATTENTION_CODE);
      //while(port.read() != GUI_ATTENTION_CODE);
      return 1;
    }

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
    fill(colorT);
    //stroke(0);
    ellipseMode(CENTER);
    ellipse(x(1), y(2), 15, 15);
    ellipse(x(5), y(2), 15, 15);
    fill(colorT);
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
    dx = width / 8;
    dy = height / 5;
    //stroke(255);
//    for(int i = 0; i < 8; i++){
//      line(x(i), 0, x(i), height);
//    }
//    for(int i = 0; i < 5; i++){
//      line(0, y(i), width, y(i));
//    }
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
  
  public void clearStrokes(){
    strokeLoc = 0;
    for(int i = 0; i < types.length; i++){
      types[i] = 0;
    }
    for(int i = 0; i < vals.length; i++){
      vals[i] = 0;
    }
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

  public void reset(){
    currentSet = 0;
  }

  public void display() {
    center();
    textLeading(stdTextSize);
    fill(color1);
    noStroke();
    //    rect(x-d, y-d, 1000, 1000);
    fill(color2);
    //stroke(0);
    ellipseMode(CENTER);
    ellipse(x(1), y(2), 15, 15);
    ellipse(x(5), y(2), 15, 15);
    fill(color2);
    noStroke();
    rect(3, 3, width - 6, y(.375f), 3);
    fill(colorT);
    textAlign(LEFT, BOTTOM);
    text("Profile: " + profiles[currentProfile].getString("name"), 20, y(.375f)/2);
    textAlign(RIGHT, BOTTOM);
    text("Speed: " + sets[currentSet].getInt("speed"), width - 20, y(.375f)/2);
    textAlign(CENTER, BOTTOM);
    text("Set: " + sets[currentSet].getString("name"), x(3), y(.375f) - 5);
    textAlign(CENTER, CENTER);
    fill(colorT);
    text(findString(0), x(1), y(1));
    text(findString(1), x(1), y(3));
    text(findString(2), x(0), y(2));
    text(findString(3), x(2), y(2));
    text(findString(4), x(5), y(1));
    text(findString(5), x(5), y(3));
    text(findString(6), x(4), y(2));
    text(findString(7), x(6), y(2));
    center();
  }
  
  public void displayWithoutTop() {
    center();
    fill(color1);
    noStroke();
    //    rect(x-d, y-d, 1000, 1000);
    fill(colorT);
    //stroke(0);
    ellipseMode(CENTER);
    ellipse(x(1), y(2), 15, 15);
    ellipse(x(5), y(2), 15, 15);
    fill(colorT);
    text(findString(0), x(1), y(1));
    text(findString(1), x(1), y(3));
    text(findString(2), x(0), y(2));
    text(findString(3), x(2), y(2));
    text(findString(4), x(5), y(1));
    text(findString(5), x(5), y(3));
    text(findString(6), x(4), y(2));
    text(findString(7), x(6), y(2));
    center();
  }

  public void highlight(int inLoc) {
    rectMode(CENTER);
    noStroke();
    fill(color3);
    switch (inLoc) {
    case 0:
      rect(x(1), y(1), dx, dy, 5);
      break;
    case 1:
      rect(x(1), y(3), dx, dy, 5);
      break;
    case 2:
      rect(x(0), y(2), dx, dy, 5);
      break;
    case 3:
      rect(x(2), y(2), dx, dy, 5);
      break;
    case 4:
      rect(x(5), y(1), dx, dy, 5);
      break;
    case 5:
      rect(x(5), y(3), dx, dy, 5);
      break;
    case 6:
      rect(x(4), y(2), dx, dy, 5);
      break;
    case 7:
      rect(x(6), y(2), dx, dy, 5);
      break;
    }
    rectMode(CORNER);
  }

    public float x(float num) {
      num -= 3;
      return x + (num * dx);
    }

    public float y(float num) {
      num -= 1.5f;
      return y + (num * dy);
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
