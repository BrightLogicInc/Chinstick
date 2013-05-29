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

  void clearCursor(){
    cursor = null;
  }

  void getCursor(){
    cursor = keys[curR][curC];
  }

  void press() {
    port.write(KEY_INPUT_CODE); //keyboard start code
    cursor.press();
  }

  void center(){
    x = width/2;
    y = height/2;
  }

  void mouseCursor(){
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

  void display() {
    center();
    wipe();
    noStroke();
    smooth();
    fill(color1);
    keySize = stdTextSize * 3 / 2;
    //    rect(x-10, y-10, 1000, 1000);
    int trackX = int(x - ((float(cols/2) + .5) * (keySize + keySpace)));
    int trackY = int(y - ((float(rows/2) + .5) * (keySize + keySpace)));
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        keys[i][j].display(trackX, trackY, color2);
        if (keys[i][j].getWidth() != 0) trackX += keys[i][j].getWidth() + keySpace;
      }
      trackY += (keySize + keySpace);
      trackX = int(x - ((float(cols/2) + .5) * (keySize + keySpace)));
    }
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

  void moveCursor(int moveX, int moveY) {
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
    }
    return num;
  }

  void enterText() {
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
        mode = 0;
        sendProfile(currentProfile);
        break;
      case 'c':
        keyboard.press();
        break;
      }
    }
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
      
      switch(type) {
      case 'y':
        keyboard.moveCursor(0, val);
        break;
      case 'x':
        keyboard.moveCursor(val, 0);
        break;
      case 's':
        sendProfile(0);
        return 255;
      case 'c':
        println(cursor.pressInt());
        return cursor.pressInt();
      }
    }
    return -1;
  }
  
}

