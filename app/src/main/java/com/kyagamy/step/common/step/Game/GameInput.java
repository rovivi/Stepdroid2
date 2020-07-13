package com.kyagamy.step.common.step.Game;


public class GameInput {
    public static float timeToAutoDown  =0.5f;
 public static  enum States {
     UNPRESSED,
     PRESSED,
     PRESSED_AND_HOLD

 }

    public GameInput(){

    }
    private States status= States.UNPRESSED;
    private long pressedTime ;

    public States getStatus() {
        return status;
    }

    public void down() {
        if (status== States.UNPRESSED){
            this.status= States.PRESSED;
            pressedTime=System.currentTimeMillis();
        }
    }

    public void up() {
            this.status=States.UNPRESSED;
    }

    public void pressed() {
        this.status=States.PRESSED_AND_HOLD;
    }

    public long getPressedTime() {
        return pressedTime;
    }

    public void update (){
        if (status==States.PRESSED){
            if (System.currentTimeMillis()-pressedTime/1000>timeToAutoDown )
                status=States.PRESSED_AND_HOLD;
        }
    }

}

