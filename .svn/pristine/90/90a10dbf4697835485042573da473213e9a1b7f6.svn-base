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

  int read() {
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

  void displayCustom(String s0, String s1, String s2, String s3, String s4, String s5, String s6, String s7) {
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

  void center(){
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

  void clear(){
    for(int i = 0; i < 8; i++){
      types[i] = 0;
      vals[i] = 0;
    }
    vals[8] = 0;
  }
  
  void displayNav() {
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
  
  void setStroke(int loc, char inType, int inVal){
    types[loc] = inType;
    vals[loc] = inVal;
    println("layout.setStroke(): " + types[loc] + ", " + vals[loc]);
  }
  
  void clearStrokes(){
    strokeLoc = 0;
    for(int i = 0; i < types.length; i++){
      types[i] = 0;
    }
    for(int i = 0; i < vals.length; i++){
      vals[i] = 0;
    }
  }
    
  void createXML(){
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

  void reset(){
    currentSet = 0;
  }

  void display() {
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
    rect(3, 3, width - 6, y(.375), 3);
    fill(colorT);
    textAlign(LEFT, BOTTOM);
    text("Profile: " + profiles[currentProfile].getString("name"), 20, y(.375)/2);
    textAlign(RIGHT, BOTTOM);
    text("Speed: " + sets[currentSet].getInt("speed"), width - 20, y(.375)/2);
    textAlign(CENTER, BOTTOM);
    text("Set: " + sets[currentSet].getString("name"), x(3), y(.375) - 5);
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
  
  void displayWithoutTop() {
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

  void highlight(int inLoc) {
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

    float x(float num) {
      num -= 3;
      return x + (num * dx);
    }

    float y(float num) {
      num -= 1.5;
      return y + (num * dy);
    }

    String findString(int num) {
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
          str += char(vals[num]) + "\nKey"; 
          break;
        }
      }
      return str;
    }
  }

