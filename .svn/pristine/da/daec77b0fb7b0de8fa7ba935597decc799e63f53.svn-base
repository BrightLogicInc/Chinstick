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

  void display() {
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

  void mouseCursor() {
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

  void init(int num, String inCover, int inValue) {
    buttons[num] = new Button(inCover, inValue);
  }

  void init(int num, Button inButton) {
    buttons[num] = inButton;
  }

  void clearCursor() { 
    cursor = null;
  } 

  void centerX(){
    x = width/2;
  }

  int findStrokeLocation(char inChar){
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
  
  int moveCursor() {
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
  
  int moveCursorWithUp() {
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

  int pressInt() {
    return cursorLoc;
  }

  int getSize() {
    return buttons.length;
  }

  int getX() {
    return x;
  }

  int getY() {
    return y;
  }
}

