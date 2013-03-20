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

  int moveCursor() {
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

