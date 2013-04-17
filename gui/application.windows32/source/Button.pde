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
    else keyWidth = int(textWidth(cover) + stdTextSize/2);
  }

  int getWidth() {
    return keyWidth;
  }
  
  int getHeight(){
    return keyHeight;
  }

  boolean isIn(int inX, int inY){
    if(x < inX && inX < x + keyWidth && y < inY && inY < y + keyHeight) return true;
    else return false;
  }

  void display(int inX, int inY, color inColor){
    x = inX;
    y = inY;
    int tSize = stdTextSize;
    fill(inColor);
    rect(inX,inY,keyWidth,keyHeight, 5);
    fill(colorT);
    textSize(stdTextSize);
    while(textWidth(cover) > (keyWidth * .975)){
      tSize--;
      textSize(tSize);
    }
    text(cover, inX+(keyWidth/2), inY+(keyHeight/2));
    textSize(stdTextSize);
  }
  
  void display(color inColor) { //for cursor purposes
    int tSize = stdTextSize;
    fill(inColor);
    rect( x, y , keyWidth, keyHeight, 5);
    fill(colorT);
    textSize(stdTextSize);
    while(textWidth(cover) > (keyWidth* .975)){
      tSize--;
      textSize(tSize);
    }
    text(cover, x + (keyWidth/2), y + (keyHeight/2));
    textSize(stdTextSize);
  }
  
  void displayC(color inColor) { //for cursor purposes
    int tSize = stdTextSize;
    fill(inColor);
    rect( x, y , keyWidth, keyHeight, 5);
    fill(colorTC);
    textSize(stdTextSize);
    while(textWidth(cover) > (keyWidth* .975)){
      tSize--;
      textSize(tSize);
    }
    text(cover, x + (keyWidth/2), y + (keyHeight/2));
    textSize(stdTextSize);
  }

  void press(){
    if(cover.equals("Done")){
      sendProfile(currentProfile);
      mode = 0;
    }
    else port.write(value);
  }
  
  int pressInt(){
    return value;
  }
  
  boolean isBlank(){
    if(cover.equals("")){
      return true;
    }
    else {
      return false;
    }
  }
  
  void move(int in){
    arrayLocation += in;
    println("Moved " + in);
  }
  
  int getArrayLocation(){
    return arrayLocation;
  }
}
