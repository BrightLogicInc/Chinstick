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
    cursor.display(color3);
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
    int num = 128;
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
  
  int moveCursor() {
    int index, val;
    char type;
    if (port.available() > 0) { 
      index = findStrokeLocation(port.readChar());
      
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
        //mode = 0;
        return 255;
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
