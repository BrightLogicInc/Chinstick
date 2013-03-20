//-----------INIT SECTION-------------------------------------//
import processing.serial.*;
import java.awt.Frame;

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

void stop(){
  println("Stopping");
  sendProfile(0);
}

void setup() {
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

void draw() {
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

void mouseMoved(){
  if(mode == START_MODE){
    keyboard.mouseCursor();
  }
  else if(mode == FIND_MODE_MODE){
    modesButtons.mouseCursor(); 
  }
}

void sendProfile(int inProfile) {
  println("Sending profile: " + inProfile);
  sets = profiles[inProfile].getChildren("set");
  for (int i = 0; i < profiles[inProfile].getInt("numSets"); i++) {
    sendSet(i);
  }
  currentProfile = inProfile;
  
  while(port.available() == 0){}
  delay(1000);
  while(port.available() > 0){
    print(char(port.read()));
  }
}

void sendSet(int inSet) {
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



void wipe() {
  fill(255, 166, 0);
  rect(0, 0, displayWidth, displayHeight);
  fill(255);
//  line(width/2, 0, width/2, height);
//  line(0, height/2, width, height/2);
}



