/************************************************************************************************

Asteroids.java

  Usage:

  <applet code="Asteroids.class" width=w height=h></applet>

  Keyboard Controls:

  S            - Start Game    P           - Pause Game
  Cursor Left  - Rotate Left   Cursor Up   - Fire Thrusters
  Cursor Right - Rotate Right  Cursor Down - Fire Retro Thrusters
  Spacebar     - Fire Cannon   H           - Hyperspace
  M            - Toggle Sound  D           - Toggle Graphics Detail

************************************************************************************************/

import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.*;
import java.applet.Applet;
import java.applet.AudioClip;



/************************************************************************************************
  Main applet code.
************************************************************************************************/

public class Asteroids extends Applet implements Runnable {

  // Thread control variables.

  Thread loadThread;
  Thread loopThread;

  // Constants

  static final int DELAY = 50;             // Milliseconds between screen updates.

  static final int MAX_SHIPS = 4;           // Starting number of ships per game.

  static final int MAX_SHOTS =  6;          // Maximum number of sprites for photons,
  static final int MAX_ROCKS =  8;          // asteroids and explosions.
  static final int MAX_SCRAP = 20;

  static final int SCRAP_COUNT = 30;        // Counter starting values.
  static final int HYPER_COUNT = 60;
  static final int STORM_PAUSE = 30;
  static final int UFO_PASSES  =  3;

  static final int MIN_ROCK_SIDES =  8;     // Asteroid shape and size ranges.
  static final int MAX_ROCK_SIDES = 12;
  static final int MIN_ROCK_SIZE  = 20;
  static final int MAX_ROCK_SIZE  = 40;
  static final int MIN_ROCK_SPEED =  2;
  static final int MAX_ROCK_SPEED = 12;

  static final int BIG_POINTS    =  25;     // Points for shooting different objects.
  static final int SMALL_POINTS  =  50;
  static final int UFO_POINTS    = 250;
  static final int MISSLE_POINTS = 500;
  static final int SHIP_POINTS = 1000;

  static final int NEW_SHIP_POINTS = 5000;  // Number of points needed to earn a new ship.
  static final int NEW_UFO_POINTS  = 1000;  // Number of points between flying saucers.

  // Background stars.

  int     numStars;
  Point[] stars;

  // Game data.

  int scoreB;
  int scoreG;
  int highScore;
  int newShipScore;
  int newUfoScore;

  boolean loaded = false;
  boolean paused;
  boolean playing;
  boolean sound;
  boolean detail;

  // Key flags.

  boolean left  = false;
  boolean right = false;
  boolean up    = false;
  boolean down  = false;
  
  boolean left2  = false;
  boolean right2 = false;
  boolean up2    = false;
  boolean down2  = false;
  boolean spacebar2  = false;
  boolean startKey  = false;
  boolean shipWraping  = false;

  // Sprite objects.

  AsteroidsSprite   ship;
  AsteroidsSprite   ship2;
  AsteroidsSprite   ufo;
  AsteroidsSprite   missle;
  AsteroidsSprite[] photons    = new AsteroidsSprite[MAX_SHOTS];
  AsteroidsSprite[] photons2    = new AsteroidsSprite[MAX_SHOTS];
  AsteroidsSprite[] asteroids  = new AsteroidsSprite[MAX_ROCKS];
  AsteroidsSprite[] explosions = new AsteroidsSprite[MAX_SCRAP];

  // Ship data.

  int shipsLeft;       // Number of ships left to play, including current one.
  int shipsLeft2;
  int shipCounter;     // Time counter for ship explosion.
  int shipCounter2;
  int hyperCounter;    // Time counter for hyperspace.
  int hyperCounter2; 
  // Photon data.

  int[] photonCounter = new int[MAX_SHOTS];    // Time counter for life of a photon.
  int   photonIndex;                           // Next available photon sprite.
  int[] photonCounter2 = new int[MAX_SHOTS];    // Time counter for life of a photon.
  int   photonIndex2;
  // Flying saucer data.

  int ufoPassesLeft;    // Number of flying saucer passes.
  int ufoCounter;       // Time counter for each pass.

  // Missle data.

  int missleCounter;    // Counter for life of missle.

  // Asteroid data.

  boolean[] asteroidIsSmall = new boolean[MAX_ROCKS];    // Asteroid size flag.
  int       asteroidsCounter;                            // Break-time counter.
  int       asteroidsSpeed;                              // Asteroid speed.
  int       asteroidsLeft;                               // Number of active asteroids.

  // Explosion data.

  int[] explosionCounter = new int[MAX_SCRAP];  // Time counters for explosions.
  int   explosionIndex;                         // Next available explosion sprite.

  // Sound clips.

  AudioClip crashSound;
  AudioClip explosionSound;
  AudioClip fireSound;
  AudioClip missleSound;
  AudioClip saucerSound;
  AudioClip thrustersSound;
  AudioClip warpSound;

  // Flags for looping sound clips.

  boolean thrustersPlaying;
  boolean saucerPlaying;
  boolean misslePlaying;

  // Values for the offscreen image.

  Dimension offDimension;
  Image offImage;
  Graphics offGraphics;

  // Font data.

  Font font = new Font("Helvetica", Font.BOLD, 12);
  FontMetrics fm;
  int fontWidth;
  int fontHeight;

  // Applet information.
  
  ServerSocket ss;
  Socket socket;
  ObjectOutputStream objectOutputStream;
  ObjectInputStream objectInputStream;

  public String getAppletInfo() {

    return("Asteroids, Copyright 1998 by Mike Hall.");
  }

  public void init() {

    Graphics g;
    Dimension d;
    int i;

    // Take credit.

    System.out.println("Asteroids, Copyright 1998 by Mike Hall.");

    // Find the size of the screen and set the values for sprites.

    g = getGraphics();
    d = size();
    AsteroidsSprite.width = d.width;
    AsteroidsSprite.height = d.height;

    // Generate starry background.

    numStars = AsteroidsSprite.width * AsteroidsSprite.height / 5000;
    stars = new Point[numStars];
    for (i = 0; i < numStars; i++)
      stars[i] = new Point((int) (Math.random() * AsteroidsSprite.width), (int) (Math.random() * AsteroidsSprite.height));

 // Create shape for the ship sprite.

    ship = new AsteroidsSprite();
    ship.shape.addPoint(0, -10);
    ship.shape.addPoint(7, 10);
    ship.shape.addPoint(-7, 10);
    
    ship2 = new AsteroidsSprite();
    ship2.shape.addPoint(0, -10);
    ship2.shape.addPoint(7, 10);
    ship2.shape.addPoint(-7, 10);

    // Create shape for the photon sprites.

    for (i = 0; i < MAX_SHOTS; i++) {
      photons[i] = new AsteroidsSprite();
      photons[i].shape.addPoint(1, 1);
      photons[i].shape.addPoint(1, -1);
      photons[i].shape.addPoint(-1, 1);
      photons[i].shape.addPoint(-1, -1);
    }
    
    for (i = 0; i < MAX_SHOTS; i++) {
        photons2[i] = new AsteroidsSprite();
        photons2[i].shape.addPoint(1, 1);
        photons2[i].shape.addPoint(1, -1);
        photons2[i].shape.addPoint(-1, 1);
        photons2[i].shape.addPoint(-1, -1);
      }

    // Create shape for the flying saucer.

    ufo = new AsteroidsSprite();
    ufo.shape.addPoint(-15, 0);
    ufo.shape.addPoint(-10, -5);
    ufo.shape.addPoint(-5, -5);
    ufo.shape.addPoint(-5, -9);
    ufo.shape.addPoint(5, -9);
    ufo.shape.addPoint(5, -5);
    ufo.shape.addPoint(10, -5);
    ufo.shape.addPoint(15, 0);
    ufo.shape.addPoint(10, 5);
    ufo.shape.addPoint(-10, 5);

    // Create shape for the guided missle.

    missle = new AsteroidsSprite();
    missle.shape.addPoint(0, -4);
    missle.shape.addPoint(1, -3);
    missle.shape.addPoint(1, 3);
    missle.shape.addPoint(2, 4);
    missle.shape.addPoint(-2, 4);
    missle.shape.addPoint(-1, 3);
    missle.shape.addPoint(-1, -3);

    // Create asteroid sprites.

    for (i = 0; i < MAX_ROCKS; i++)
      asteroids[i] = new AsteroidsSprite();

    // Create explosion sprites.

    for (i = 0; i < MAX_SCRAP; i++)
      explosions[i] = new AsteroidsSprite();

    // Set font data.

    g.setFont(font);
    fm = g.getFontMetrics();
    fontWidth = fm.getMaxAdvance();
    fontHeight = fm.getHeight();

    // Initialize game data and put us in 'game over' mode.
    
    
    
    highScore = 0;
    sound = true;
    detail = true;
    initGame();
    endGame();
    //seting up the server
    try {
		setServer();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  }

  public void initGame() {

    // Initialize game data and sprites.

    scoreB = 0;
    scoreG = 0;
    shipsLeft = MAX_SHIPS;
    shipsLeft2 = MAX_SHIPS;
    asteroidsSpeed = MIN_ROCK_SPEED;
    newShipScore = NEW_SHIP_POINTS;
    newUfoScore = NEW_UFO_POINTS;
    initShip();
    initShip2();
    initPhotons();
    initPhotons2();
    stopUfo();
    stopMissle();
    initAsteroids();
    initExplosions();
    playing = true;
    paused = false;
    
    
  }

  public void endGame() {

    // Stop ship, flying saucer, guided missle and associated sounds.

    playing = false;
    stopShip();
    stopShip2();
    stopUfo();
    stopMissle();
  }

  public void start() {

    if (loopThread == null) {
      loopThread = new Thread(this);
      loopThread.start();
    }
    if (!loaded && loadThread == null) {
      loadThread = new Thread(this);
      loadThread.start();
    }
  }

  public void stop() {

    if (loopThread != null) {
      loopThread.stop();
      loopThread = null;
    }
    if (loadThread != null) {
      loadThread.stop();
      loadThread = null;
    }
  }

  public void run() {

    int i, j;
    long startTime;

    // Lower this thread's priority and get the current time.

    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
    startTime = System.currentTimeMillis();

    // Run thread for loading sounds.

    if (!loaded && Thread.currentThread() == loadThread) {
      loadSounds();
      loaded = true;
      loadThread.stop();
    }

    // This is the main loop.

    while (Thread.currentThread() == loopThread) {

      if (!paused) {

        // Move and process all sprites.

        updateShip();
        updateShip2();
        updatePhotons();
        updatePhotons2();
        
        
        
        
        try {
        	if (playing && scoreB > newUfoScore && !ufo.active) {
                newUfoScore += NEW_UFO_POINTS;
                ufoPassesLeft = UFO_PASSES;
                initUfo();
              }
              if (playing && scoreG > newUfoScore && !ufo.active) {
                  newUfoScore += NEW_UFO_POINTS;
                  ufoPassesLeft = UFO_PASSES;
                  initUfo();
                }
            //contacting the server  
    		InputList input = sendData(asteroids,asteroidIsSmall,ship,ship2,photons,photons2,explosions,hyperCounter,hyperCounter2,shipsLeft,shipsLeft2,scoreB,scoreG,ufo,missle,missleCounter,playing);
    		updateUfo();
    		updateMissle();
    		right2 = input.right2;
    		left2 = input.left2;
    		up2 = input.up2;
    		down2 = input.down2;
    		spacebar2 = input.spacebar2;
    		startKey = input.startKey;
    		shipWraping = input.shipWraping;
    		//Checking if "S" was presed in the client
    		if(startKey==true) {
    			initGame();
    			startKey = false;
    		}
    		//Checking if the client is shoting
    		if (spacebar2 == true && ship2.active) {
    	        if (sound & !paused)
    	          fireSound.play();
    	        photonIndex2++;
    	        if (photonIndex2 >= MAX_SHOTS)
    	          photonIndex2 = 0;
    	        photons2[photonIndex2].active = true;
    	        photons2[photonIndex2].currentX = ship2.currentX;
    	        photons2[photonIndex2].currentY = ship2.currentY;
    	        photons2[photonIndex2].deltaX = MIN_ROCK_SIZE * -Math.sin(ship2.angle);
    	        photons2[photonIndex2].deltaY = MIN_ROCK_SIZE *  Math.cos(ship2.angle);
    	        photonCounter2[photonIndex2] = Math.min(AsteroidsSprite.width, AsteroidsSprite.height) / MIN_ROCK_SIZE;
    	        spacebar2 = false;
    	        
    	        
    	      }
    	} catch (ClassNotFoundException | IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
        //checking for "H" key in client
        if (shipWraping == true && ship2.active && hyperCounter2 <= 0) {
            ship2.currentX = Math.random() * AsteroidsSprite.width;
            ship2.currentX = Math.random() * AsteroidsSprite.height;
            hyperCounter2 = HYPER_COUNT;
            if (sound & !paused)
              warpSound.play();
          }
        //Checking for collisions
        if (ship.active && hyperCounter <= 0 && ship2.active && hyperCounter2 <= 0 && ship.isColliding(ship2)) {
        	if (sound)
                crashSound.play();
             explode(ship);
             explode(ship2);
             stopShip();
             stopShip2();
        }
        
        for (j = 0; j < MAX_SHOTS; j++)
            if (photons2[j].active && ship.active && hyperCounter <= 0 && ship.isColliding(photons2[j])) {
              photons2[j].active = false;
              if (sound)
                explosionSound.play();
              explode(ship);
              stopShip();
                scoreG += SHIP_POINTS;
            }
        for (j = 0; j < MAX_SHOTS; j++)
            if (photons[j].active && ship2.active && hyperCounter2 <= 0 && ship2.isColliding(photons[j])) {
              photons[j].active = false;
              if (sound)
                explosionSound.play();
              explode(ship2);
              stopShip2();
                scoreB += SHIP_POINTS;
            }

        updateAsteroids();
        
        // Check the score and advance high score, add a new ship or start the flying
        // saucer as necessary.

        
        if (scoreB > newShipScore) {
          newShipScore += NEW_SHIP_POINTS;
          shipsLeft++;
        }
        if (scoreG > newShipScore) {
            newShipScore += NEW_SHIP_POINTS;
            shipsLeft2++;
          }
        
        updateExplosions();
        

        // If all asteroids have been destroyed create a new batch.

        if (asteroidsLeft <= 0)
            if (--asteroidsCounter <= 0)
              initAsteroids();
      }

      // Update the screen and set the timer for the next loop.

      repaint();
      try {
        startTime += DELAY;
        Thread.sleep(Math.max(0, startTime - System.currentTimeMillis()));
      }
      catch (InterruptedException e) {
        break;
      }
    }
  }

  public void loadSounds() {

    // Load all sound clips by playing and immediately stopping them.

    try {
      crashSound     = getAudioClip(new URL(getDocumentBase(), "crash.au"));
      explosionSound = getAudioClip(new URL(getDocumentBase(), "explosion.au"));
      fireSound      = getAudioClip(new URL(getDocumentBase(), "fire.au"));
      missleSound    = getAudioClip(new URL(getDocumentBase(), "missle.au"));
      saucerSound    = getAudioClip(new URL(getDocumentBase(), "saucer.au"));
      thrustersSound = getAudioClip(new URL(getDocumentBase(), "thrusters.au"));
      warpSound      = getAudioClip(new URL(getDocumentBase(), "warp.au"));
    }
    catch (MalformedURLException e) {}

    crashSound.play();     crashSound.stop();
    explosionSound.play(); explosionSound.stop();
    fireSound.play();      fireSound.stop();
    missleSound.play();    missleSound.stop();
    saucerSound.play();    saucerSound.stop();
    thrustersSound.play(); thrustersSound.stop();
    warpSound.play();      warpSound.stop();
  }

  public void initShip() {

    ship.active = true;
    ship.angle = 0.0;
    ship.deltaAngle = 0.0;
    ship.currentX = -20.0;
    ship.currentY = 0.0;
    ship.deltaX = 0.0;
    ship.deltaY = 0.0;
    ship.render();
    if (loaded)
      thrustersSound.stop();
    thrustersPlaying = false;

    hyperCounter = 0;
  }
  
  public void initShip2() {

	    ship2.active = true;
	    ship2.angle = 0.0;
	    ship2.deltaAngle = 0.0;
	    ship2.currentX = 20.0;
	    ship2.currentY = 0.0;
	    ship2.deltaX = 0.0;
	    ship2.deltaY = 0.0;
	    ship2.render();
	    if (loaded)
	      thrustersSound.stop();
	    thrustersPlaying = false;

	    hyperCounter2 = 0;
	  }

  public void updateShip() {

    double dx, dy, limit;

    if (!playing)
      return;

    // Rotate the ship if left or right cursor key is down.

    if (left) {
      ship.angle += Math.PI / 16.0;
      if (ship.angle > 2 * Math.PI)
        ship.angle -= 2 * Math.PI;
    }
    if (right) {
      ship.angle -= Math.PI / 16.0;
      if (ship.angle < 0)
        ship.angle += 2 * Math.PI;
    }

    // Fire thrusters if up or down cursor key is down. Don't let ship go past
    // the speed limit.

    dx = -Math.sin(ship.angle);
    dy =  Math.cos(ship.angle);
    limit = 0.8 * MIN_ROCK_SIZE;
    if (up) {
      if (ship.deltaX + dx > -limit && ship.deltaX + dx < limit)
        ship.deltaX += dx;
      if (ship.deltaY + dy > -limit && ship.deltaY + dy < limit)
        ship.deltaY += dy;
    }
    if (down) {
      if (ship.deltaX - dx > -limit && ship.deltaX - dx < limit)
        ship.deltaX -= dx;
      if (ship.deltaY - dy > -limit && ship.deltaY - dy < limit)
        ship.deltaY -= dy;
    }

    // Move the ship. If it is currently in hyperspace, advance the countdown.

    if (ship.active) {
      ship.advance();
      ship.render();
      if (hyperCounter > 0)
        hyperCounter--;
    }

    // Ship is exploding, advance the countdown or create a new ship if it is
    // done exploding. The new ship is added as though it were in hyperspace.
    // (This gives the player time to move the ship if it is in imminent danger.)
    // If that was the last ship, end the game.

    else
      if (--shipCounter <= 0)
        if (shipsLeft > 0) {
          initShip();
          hyperCounter = HYPER_COUNT;
        }
        else
          endGame();

  }
  
  public void updateShip2() {

	    double dx, dy, limit;

	    if (!playing)
	      return;

	    // Rotate the ship if left or right cursor key is down.

	    if (left2) {
	      ship2.angle += Math.PI / 16.0;
	      if (ship2.angle > 2 * Math.PI)
	        ship2.angle -= 2 * Math.PI;
	    }
	    if (right2) {
	      ship2.angle -= Math.PI / 16.0;
	      if (ship2.angle < 0)
	        ship2.angle += 2 * Math.PI;
	    }

	    // Fire thrusters if up or down cursor key is down. Don't let ship go past
	    // the speed limit.

	    dx = -Math.sin(ship2.angle);
	    dy =  Math.cos(ship2.angle);
	    limit = 0.8 * MIN_ROCK_SIZE;
	    if (up2) {
	      if (ship2.deltaX + dx > -limit && ship2.deltaX + dx < limit)
	        ship2.deltaX += dx;
	      if (ship2.deltaY + dy > -limit && ship2.deltaY + dy < limit)
	        ship2.deltaY += dy;
	    }
	    if (down2) {
	      if (ship2.deltaX - dx > -limit && ship2.deltaX - dx < limit)
	        ship2.deltaX -= dx;
	      if (ship2.deltaY - dy > -limit && ship2.deltaY - dy < limit)
	        ship2.deltaY -= dy;
	    }

	    // Move the ship. If it is currently in hyperspace, advance the countdown.

	    if (ship2.active) {
	      ship2.advance();
	      ship2.render();
	      if (hyperCounter2 > 0)
	        hyperCounter2--;
	    }

	    // Ship is exploding, advance the countdown or create a new ship if it is
	    // done exploding. The new ship is added as though it were in hyperspace.
	    // (This gives the player time to move the ship if it is in imminent danger.)
	    // If that was the last ship, end the game.

	    else
	      if (--shipCounter2 <= 0)
	        if (shipsLeft2 > 0) {
	          initShip2();
	          hyperCounter2 = HYPER_COUNT;
	        }
	        else
	          endGame();
	          
	  }

  public void stopShip() {

    ship.active = false;
    shipCounter = SCRAP_COUNT;
    if (shipsLeft > 0)
      shipsLeft--;
    if (loaded)
      thrustersSound.stop();
    thrustersPlaying = false;
  }
  
  public void stopShip2() {

	    ship2.active = false;
	    shipCounter2 = SCRAP_COUNT;
	    if (shipsLeft2 > 0)
	      shipsLeft2--;
	    if (loaded)
	      thrustersSound.stop();
	    thrustersPlaying = false;
	  }

  public void initPhotons() {

    int i;

    for (i = 0; i < MAX_SHOTS; i++) {
      photons[i].active = false;
      photonCounter[i] = 0;
    }
    photonIndex = 0;
  }
  
  public void initPhotons2() {

	    int i;

	    for (i = 0; i < MAX_SHOTS; i++) {
	      photons2[i].active = false;
	      photonCounter2[i] = 0;
	    }
	    photonIndex2 = 0;
	  }

  public void updatePhotons() {

    int i;

    // Move any active photons. Stop it when its counter has expired.

    for (i = 0; i < MAX_SHOTS; i++)
      if (photons[i].active) {
        photons[i].advance();
        photons[i].render();
        if (--photonCounter[i] < 0)
          photons[i].active = false;
      }
  }
  
  public void updatePhotons2() {

	    int i;

	    // Move any active photons. Stop it when its counter has expired.

	    for (i = 0; i < MAX_SHOTS; i++)
	      if (photons2[i].active) {
	        photons2[i].advance();
	        photons2[i].render();
	        if (--photonCounter2[i] < 0)
	          photons2[i].active = false;
	      }
	  }

  public void initUfo() {

    double temp;

    // Randomly set flying saucer at left or right edge of the screen.

    ufo.active = true;
    ufo.currentX = -AsteroidsSprite.width / 2;
    ufo.currentY = Math.random() * AsteroidsSprite.height;
    ufo.deltaX = MIN_ROCK_SPEED + Math.random() * (MAX_ROCK_SPEED - MIN_ROCK_SPEED);
    if (Math.random() < 0.5) {
      ufo.deltaX = -ufo.deltaX;
      ufo.currentX = AsteroidsSprite.width / 2;
    }
    ufo.deltaY = MIN_ROCK_SPEED + Math.random() * (MAX_ROCK_SPEED - MIN_ROCK_SPEED);
    if (Math.random() < 0.5)
      ufo.deltaY = -ufo.deltaY;
    ufo.render();
    saucerPlaying = true;
    if (sound)
      saucerSound.loop();

    // Set counter for this pass.

    ufoCounter = (int) Math.floor(AsteroidsSprite.width / Math.abs(ufo.deltaX));
  }

  public void updateUfo() {

    int i, d;

    // Move the flying saucer and check for collision with a photon. Stop it when its
    // counter has expired.

    if (ufo.active) {
      ufo.advance();
      ufo.render();
      if (--ufoCounter <= 0)
        if (--ufoPassesLeft > 0)
          initUfo();
        else
          stopUfo();
      else {
        for (i = 0; i < MAX_SHOTS; i++)
          if (photons[i].active && ufo.isColliding(photons[i])) {
            if (sound)
              crashSound.play();
            explode(ufo);
            stopUfo();
            scoreB += UFO_POINTS;
          }
        for (i = 0; i < MAX_SHOTS; i++)
            if (photons2[i].active && ufo.isColliding(photons2[i])) {
              if (sound)
                crashSound.play();
              explode(ufo);
              stopUfo();
              scoreG += UFO_POINTS;
            }

          // On occassion, fire a missle at the ship if the saucer is not
          // too close to it.
          if(scoreB>=scoreG) {
          d = (int) Math.max(Math.abs(ufo.currentX - ship.currentX), Math.abs(ufo.currentY - ship.currentY));
          if (ship.active && hyperCounter <= 0 && ufo.active && !missle.active &&
              d > 4 * MAX_ROCK_SIZE && Math.random() < .03)
            initMissle();
          }
          else {
              d = (int) Math.max(Math.abs(ufo.currentX - ship2.currentX), Math.abs(ufo.currentY - ship2.currentY));
              if (ship2.active && hyperCounter2 <= 0 && ufo.active && !missle.active &&
                  d > 4 * MAX_ROCK_SIZE && Math.random() < .03)
                initMissle();
              }
       }
    }
  }

  public void stopUfo() {

    ufo.active = false;
    ufoCounter = 0;
    ufoPassesLeft = 0;
    if (loaded)
      saucerSound.stop();
    saucerPlaying = false;
  }

  public void initMissle() {

    missle.active = true;
    missle.angle = 0.0;
    missle.deltaAngle = 0.0;
    missle.currentX = ufo.currentX;
    missle.currentY = ufo.currentY;
    missle.deltaX = 0.0;
    missle.deltaY = 0.0;
    missle.render();
    missleCounter = 3 * Math.max(AsteroidsSprite.width, AsteroidsSprite.height) / MIN_ROCK_SIZE;
    if (sound)
      missleSound.loop();
    misslePlaying = true;
  }

  public void updateMissle() {

    int i;

    // Move the guided missle and check for collision with ship or photon. Stop it when its
    // counter has expired.

    if (missle.active) {
      if (--missleCounter <= 0)
        stopMissle();
      else {
        guideMissle();
        missle.advance();
        missle.render();
        for (i = 0; i < MAX_SHOTS; i++) {
          if (photons[i].active && missle.isColliding(photons[i])) {
            if (sound)
              crashSound.play();
            explode(missle);
            stopMissle();
            scoreB += MISSLE_POINTS;
          }
        if (photons2[i].active && missle.isColliding(photons2[i])) {
            if (sound)
              crashSound.play();
            explode(missle);
            stopMissle();
            scoreG += MISSLE_POINTS;
          }
        }
        if (missle.active && ship.active && hyperCounter <= 0 && ship.isColliding(missle)) {
          if (sound)
            crashSound.play();
          explode(ship);
          stopShip();
          stopUfo();
          stopMissle();
        }
        if (missle.active && ship2.active && hyperCounter2 <= 0 && ship2.isColliding(missle)) {
            if (sound)
              crashSound.play();
            explode(ship2);
            stopShip2();
            stopUfo();
            stopMissle();
          }
      }
    }
  }

  public void guideMissle() {

    double dx, dy, angle;

    if (!ship.active || hyperCounter > 0)
      return;

    // Find the angle needed to hit the ship.
    if(scoreB>=scoreG) {
    	dx = ship.currentX - missle.currentX;
    	dy = ship.currentY - missle.currentY;
    }
    else {
    	dx = ship2.currentX - missle.currentX;
        dy = ship2.currentY - missle.currentY;
    }
    if (dx == 0 && dy == 0)
      angle = 0;
    if (dx == 0) {
      if (dy < 0)
        angle = -Math.PI / 2;
      else
        angle = Math.PI / 2;
    }
    else {
      angle = Math.atan(Math.abs(dy / dx));
      if (dy > 0)
        angle = -angle;
      if (dx < 0)
        angle = Math.PI - angle;
    }

    // Adjust angle for screen coordinates.

    missle.angle = angle - Math.PI / 2;

    // Change the missle's angle so that it points toward the ship.

    missle.deltaX = MIN_ROCK_SIZE / 3 * -Math.sin(missle.angle);
    missle.deltaY = MIN_ROCK_SIZE / 3 *  Math.cos(missle.angle);
  }

  public void stopMissle() {

    missle.active = false;
    missleCounter = 0;
    if (loaded)
      missleSound.stop();
    misslePlaying = false;
  }

  public void initAsteroids() {

    int i, j;
    int s;
    double theta, r;
    int x, y;

    // Create random shapes, positions and movements for each asteroid.

    for (i = 0; i < MAX_ROCKS; i++) {

      // Create a jagged shape for the asteroid and give it a random rotation.

      asteroids[i].shape = new Polygon();
      s = MIN_ROCK_SIDES + (int) (Math.random() * (MAX_ROCK_SIDES - MIN_ROCK_SIDES));
      for (j = 0; j < s; j ++) {
        theta = 2 * Math.PI / s * j;
        r = MIN_ROCK_SIZE + (int) (Math.random() * (MAX_ROCK_SIZE - MIN_ROCK_SIZE));
        x = (int) -Math.round(r * Math.sin(theta));
        y = (int)  Math.round(r * Math.cos(theta));
        asteroids[i].shape.addPoint(x, y);
      }
      asteroids[i].active = true;
      asteroids[i].angle = 0.0;
      asteroids[i].deltaAngle = (Math.random() - 0.5) / 10;

      // Place the asteroid at one edge of the screen.

      if (Math.random() < 0.5) {
        asteroids[i].currentX = -AsteroidsSprite.width / 2;
        if (Math.random() < 0.5)
          asteroids[i].currentX = AsteroidsSprite.width / 2;
        asteroids[i].currentY = Math.random() * AsteroidsSprite.height;
      }
      else {
        asteroids[i].currentX = Math.random() * AsteroidsSprite.width;
        asteroids[i].currentY = -AsteroidsSprite.height / 2;
        if (Math.random() < 0.5)
          asteroids[i].currentY = AsteroidsSprite.height / 2;
      }

      // Set a random motion for the asteroid.

      asteroids[i].deltaX = Math.random() * asteroidsSpeed;
      if (Math.random() < 0.5)
        asteroids[i].deltaX = -asteroids[i].deltaX;
      asteroids[i].deltaY = Math.random() * asteroidsSpeed;
      if (Math.random() < 0.5)
        asteroids[i].deltaY = -asteroids[i].deltaY;

      asteroids[i].render();
      asteroidIsSmall[i] = false;
    }

    asteroidsCounter = STORM_PAUSE;
    asteroidsLeft = MAX_ROCKS;
    if (asteroidsSpeed < MAX_ROCK_SPEED)
      asteroidsSpeed++;
  }

  public void initSmallAsteroids(int n) {

    int count;
    int i, j;
    int s;
    double tempX, tempY;
    double theta, r;
    int x, y;

    // Create one or two smaller asteroids from a larger one using inactive asteroids. The new
    // asteroids will be placed in the same position as the old one but will have a new, smaller
    // shape and new, randomly generated movements.

    count = 0;
    i = 0;
    tempX = asteroids[n].currentX;
    tempY = asteroids[n].currentY;
    do {
      if (!asteroids[i].active) {
        asteroids[i].shape = new Polygon();
        s = MIN_ROCK_SIDES + (int) (Math.random() * (MAX_ROCK_SIDES - MIN_ROCK_SIDES));
        for (j = 0; j < s; j ++) {
          theta = 2 * Math.PI / s * j;
          r = (MIN_ROCK_SIZE + (int) (Math.random() * (MAX_ROCK_SIZE - MIN_ROCK_SIZE))) / 2;
          x = (int) -Math.round(r * Math.sin(theta));
          y = (int)  Math.round(r * Math.cos(theta));
          asteroids[i].shape.addPoint(x, y);
        }
        asteroids[i].active = true;
        asteroids[i].angle = 0.0;
        asteroids[i].deltaAngle = (Math.random() - 0.5) / 10;
        asteroids[i].currentX = tempX;
        asteroids[i].currentY = tempY;
        asteroids[i].deltaX = Math.random() * 2 * asteroidsSpeed - asteroidsSpeed;
        asteroids[i].deltaY = Math.random() * 2 * asteroidsSpeed - asteroidsSpeed;
        asteroids[i].render();
        asteroidIsSmall[i] = true;
        count++;
        asteroidsLeft++;
      }
      i++;
    } while (i < MAX_ROCKS && count < 2);
  }

  public void updateAsteroids() {

    int i, j;

    // Move any active asteroids and check for collisions.

    for (i = 0; i < MAX_ROCKS; i++)
      if (asteroids[i].active) {
        asteroids[i].advance();
        asteroids[i].render();

        // If hit by photon, kill asteroid and advance score. If asteroid is large,
        // make some smaller ones to replace it.

        for (j = 0; j < MAX_SHOTS; j++)
          if (photons[j].active && asteroids[i].active && asteroids[i].isColliding(photons[j])) {
            asteroidsLeft--;
            asteroids[i].active = false;
            photons[j].active = false;
            if (sound)
              explosionSound.play();
            explode(asteroids[i]);
            if (!asteroidIsSmall[i]) {
              scoreB += BIG_POINTS;
              initSmallAsteroids(i);
            }
            else
              scoreB += SMALL_POINTS;
          }
        for (j = 0; j < MAX_SHOTS; j++)
            if (photons2[j].active && asteroids[i].active && asteroids[i].isColliding(photons2[j])) {
              asteroidsLeft--;
              asteroids[i].active = false;
              photons2[j].active = false;
              if (sound)
                explosionSound.play();
              explode(asteroids[i]);
              if (!asteroidIsSmall[i]) {
                scoreG += BIG_POINTS;
                initSmallAsteroids(i);
              }
              else
                scoreG += SMALL_POINTS;
            }

        // If the ship is not in hyperspace, see if it is hit.

        if (ship.active && hyperCounter <= 0 && asteroids[i].active && asteroids[i].isColliding(ship)) {
          if (sound)
            crashSound.play();
          explode(ship);
          stopShip();
          stopUfo();
          stopMissle();
        }
        
        if (ship2.active && hyperCounter2 <= 0 && asteroids[i].active && asteroids[i].isColliding(ship2)) {
            if (sound)
              crashSound.play();
            explode(ship2);
            stopShip2();
            stopUfo();
            stopMissle();
          }
    }
  }

  public void initExplosions() {

    int i;

    for (i = 0; i < MAX_SCRAP; i++) {
      explosions[i].shape = new Polygon();
      explosions[i].active = false;
      explosionCounter[i] = 0;
    }
    explosionIndex = 0;
  }

  public void explode(AsteroidsSprite s) {

    int c, i, j;

    // Create sprites for explosion animation. The each individual line segment of the given sprite
    // is used to create a new sprite that will move outward  from the sprite's original position
    // with a random rotation.

    s.render();
    c = 2;
    if (detail || s.sprite.npoints < 6)
      c = 1;
    for (i = 0; i < s.sprite.npoints; i += c) {
      explosionIndex++;
      if (explosionIndex >= MAX_SCRAP)
        explosionIndex = 0;
      explosions[explosionIndex].active = true;
      explosions[explosionIndex].shape = new Polygon();
      explosions[explosionIndex].shape.addPoint(s.shape.xpoints[i], s.shape.ypoints[i]);
      j = i + 1;
      if (j >= s.sprite.npoints)
        j -= s.sprite.npoints;
      explosions[explosionIndex].shape.addPoint(s.shape.xpoints[j], s.shape.ypoints[j]);
      explosions[explosionIndex].angle = s.angle;
      explosions[explosionIndex].deltaAngle = (Math.random() * 2 * Math.PI - Math.PI) / 15;
      explosions[explosionIndex].currentX = s.currentX;
      explosions[explosionIndex].currentY = s.currentY;
      explosions[explosionIndex].deltaX = -s.shape.xpoints[i] / 5;
      explosions[explosionIndex].deltaY = -s.shape.ypoints[i] / 5;
      explosionCounter[explosionIndex] = SCRAP_COUNT;
    }
  }

  public void updateExplosions() {

    int i;

    // Move any active explosion debris. Stop explosion when its counter has expired.

    for (i = 0; i < MAX_SCRAP; i++)
      if (explosions[i].active) {
        explosions[i].advance();
        explosions[i].render();
        if (--explosionCounter[i] < 0)
          explosions[i].active = false;
      }
  }

  public boolean keyDown(Event e, int key) {

    // Check if any cursor keys have been pressed and set flags.

    if (key == Event.LEFT)
      left = true;
    if (key == Event.RIGHT)
      right = true;
    if (key == Event.UP)
      up = true;
    if (key == Event.DOWN)
      down = true;

    if ((up || down) && ship.active && !thrustersPlaying) {
      if (sound && !paused)
        thrustersSound.loop();
      thrustersPlaying = true;
    }

    // Spacebar: fire a photon and start its counter.

    if (key == 32 && ship.active) {
      if (sound & !paused)
        fireSound.play();
      photonIndex++;
      if (photonIndex >= MAX_SHOTS)
        photonIndex = 0;
      photons[photonIndex].active = true;
      photons[photonIndex].currentX = ship.currentX;
      photons[photonIndex].currentY = ship.currentY;
      photons[photonIndex].deltaX = MIN_ROCK_SIZE * -Math.sin(ship.angle);
      photons[photonIndex].deltaY = MIN_ROCK_SIZE *  Math.cos(ship.angle);
      photonCounter[photonIndex] = Math.min(AsteroidsSprite.width, AsteroidsSprite.height) / MIN_ROCK_SIZE;
    }
    
    

    // 'H' key: warp ship into hyperspace by moving to a random location and starting counter.

    if (key == 104 && ship.active && hyperCounter <= 0) {
      ship.currentX = Math.random() * AsteroidsSprite.width;
      ship.currentX = Math.random() * AsteroidsSprite.height;
      hyperCounter = HYPER_COUNT;
      if (sound & !paused)
        warpSound.play();
    }

    // 'P' key: toggle pause mode and start or stop any active looping sound clips.

    if (key == 112) {
      if (paused) {
        if (sound && misslePlaying)
          missleSound.loop();
        if (sound && saucerPlaying)
          saucerSound.loop();
        if (sound && thrustersPlaying)
          thrustersSound.loop();
      }
      else {
        if (misslePlaying)
          missleSound.stop();
        if (saucerPlaying)
          saucerSound.stop();
        if (thrustersPlaying)
          thrustersSound.stop();
      }
      paused = !paused;
    }

    // 'M' key: toggle sound on or off and stop any looping sound clips.

    if (key == 109 && loaded) {
      if (sound) {
        crashSound.stop();
        explosionSound.stop();
        fireSound.stop();
        missleSound.stop();
        saucerSound.stop();
        thrustersSound.stop();
        warpSound.stop();
      }
      else {
        if (misslePlaying && !paused)
          missleSound.loop();
        if (saucerPlaying && !paused)
          saucerSound.loop();
        if (thrustersPlaying && !paused)
          thrustersSound.loop();
      }
      sound = !sound;
    }

    // 'D' key: toggle graphics detail on or off.

    if (key == 100)
      detail = !detail;

    // 'S' key: start the game, if not already in progress.

    if (key == 115 && loaded && !playing)
      initGame();

    return true;
  }

  public boolean keyUp(Event e, int key) {

    // Check if any cursor keys where released and set flags.

    if (key == Event.LEFT)
      left = false;
    if (key == Event.RIGHT)
      right = false;
    if (key == Event.UP)
      up = false;
    if (key == Event.DOWN)
      down = false;

    if (!up && !down && thrustersPlaying) {
      thrustersSound.stop();
      thrustersPlaying = false;
    }


    return true;
  }

  public void paint(Graphics g) {

    update(g);
  }

  public void update(Graphics g) {

    Dimension d = size();
    int i;
    int c;
    String s;

    // Create the offscreen graphics context, if no good one exists.

    if (offGraphics == null || d.width != offDimension.width || d.height != offDimension.height) {
      offDimension = d;
      offImage = createImage(d.width, d.height);
      offGraphics = offImage.getGraphics();
    }

    // Fill in background and stars.

    offGraphics.setColor(Color.black);
    offGraphics.fillRect(0, 0, d.width, d.height);
    if (detail) {
      offGraphics.setColor(Color.white);
      for (i = 0; i < numStars; i++)
        offGraphics.drawLine(stars[i].x, stars[i].y, stars[i].x, stars[i].y);
    }

    // Draw photon bullets.

    offGraphics.setColor(Color.white);
    for (i = 0; i < MAX_SHOTS; i++)
      if (photons[i].active)
        offGraphics.drawPolygon(photons[i].sprite);
    
    offGraphics.setColor(Color.white);
    for (i = 0; i < MAX_SHOTS; i++)
      if (photons2[i].active)
        offGraphics.drawPolygon(photons2[i].sprite);
    // Draw the guided missle, counter is used to quickly fade color to black when near expiration.

    c = Math.min(missleCounter * 24, 255);
    offGraphics.setColor(new Color(c, c, c));
    if (missle.active) {
      offGraphics.drawPolygon(missle.sprite);
      offGraphics.drawLine(missle.sprite.xpoints[missle.sprite.npoints - 1], missle.sprite.ypoints[missle.sprite.npoints - 1],
                           missle.sprite.xpoints[0], missle.sprite.ypoints[0]);
    }

    // Draw the asteroids.

    for (i = 0; i < MAX_ROCKS; i++)
      if (asteroids[i].active) {
        if (detail) {
          offGraphics.setColor(Color.black);
          offGraphics.fillPolygon(asteroids[i].sprite);
        }
        offGraphics.setColor(Color.white);
        offGraphics.drawPolygon(asteroids[i].sprite);
        offGraphics.drawLine(asteroids[i].sprite.xpoints[asteroids[i].sprite.npoints - 1], asteroids[i].sprite.ypoints[asteroids[i].sprite.npoints - 1],
                             asteroids[i].sprite.xpoints[0], asteroids[i].sprite.ypoints[0]);
      }

    // Draw the flying saucer.

    if (ufo.active) {
      if (detail) {
        offGraphics.setColor(Color.black);
        offGraphics.fillPolygon(ufo.sprite);
      }
      offGraphics.setColor(Color.white);
      offGraphics.drawPolygon(ufo.sprite);
      offGraphics.drawLine(ufo.sprite.xpoints[ufo.sprite.npoints - 1], ufo.sprite.ypoints[ufo.sprite.npoints - 1],
                           ufo.sprite.xpoints[0], ufo.sprite.ypoints[0]);
    }

    // Draw the ship, counter is used to fade color to white on hyperspace.

    c = 255 - (255 / HYPER_COUNT) * hyperCounter;
    if (ship.active) {
      if (detail && hyperCounter == 0) {
        offGraphics.setColor(Color.black);
        offGraphics.fillPolygon(ship.sprite);
      }
      offGraphics.setColor(new Color(c, c, c));
      offGraphics.drawPolygon(ship.sprite);
      offGraphics.drawLine(ship.sprite.xpoints[ship.sprite.npoints - 1], ship.sprite.ypoints[ship.sprite.npoints - 1],
                           ship.sprite.xpoints[0], ship.sprite.ypoints[0]);
    }
    
    c = 255 - (255 / HYPER_COUNT) * hyperCounter2;
    if (ship2.active) {
      if (detail && hyperCounter2 == 0) {
        offGraphics.setColor(Color.green);
        offGraphics.fillPolygon(ship2.sprite);
      }
      offGraphics.setColor(new Color(c, c, c));
      offGraphics.drawPolygon(ship2.sprite);
      offGraphics.drawLine(ship2.sprite.xpoints[ship2.sprite.npoints - 1], ship2.sprite.ypoints[ship2.sprite.npoints - 1],
                           ship2.sprite.xpoints[0], ship2.sprite.ypoints[0]);
    }

    // Draw any explosion debris, counters are used to fade color to black.

    for (i = 0; i < MAX_SCRAP; i++)
      if (explosions[i].active) {
        c = (255 / SCRAP_COUNT) * explosionCounter [i];
        offGraphics.setColor(new Color(c, c, c));
        offGraphics.drawPolygon(explosions[i].sprite);
      }

    // Display status and messages.

    offGraphics.setFont(font);
    offGraphics.setColor(Color.white);

    offGraphics.drawString("Black Score: " + scoreB, fontWidth, fontHeight);
    offGraphics.drawString("Ships: " + shipsLeft, fontWidth, d.height - fontHeight);
    s = "Green Score: " + scoreG;
    offGraphics.drawString(s, d.width - (fontWidth + fm.stringWidth(s)), fontHeight);
    if (!sound) {
      s = "Mute";
      offGraphics.drawString(s, d.width - (fontWidth + fm.stringWidth(s)), d.height - fontHeight);
    }
    
    if (!playing) {
      s = "A S T E R O I D S";
      offGraphics.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 2);
      s = "Copyright 1998 by Mike Hall";
      offGraphics.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 2 + fontHeight);
      if (!loaded) {
        s = "Loading sounds...";
        offGraphics.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 4);
      }
      else {
          s = "Game Over";
          offGraphics.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 4);
          s = "Last Black Score: " + scoreB + "             Last Green Score: " + scoreG;
          offGraphics.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 4 + (fontHeight*2));
          int combined = scoreB + scoreG;
          s = "Combined Score : " + combined;
          offGraphics.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 4 + (fontHeight*3));
          s = "'S' to Start";
          offGraphics.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 4 + (fontHeight*5));
        }
    }
    else if (paused) {
      s = "Game Paused";
      offGraphics.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 4);
    }

    // Copy the off screen buffer to the screen.
    g.drawImage(offImage, 0, 0, this);
    
    
  }
  
  //seting up the server
  public void setServer() throws IOException {
	  	ss = new ServerSocket(4444);

		socket = ss.accept();
		objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
		objectInputStream = new ObjectInputStream(socket.getInputStream());

  }
  
  //sending and reciving data
  public InputList sendData(AsteroidsSprite[] asteroids, boolean[] small, AsteroidsSprite ship1, AsteroidsSprite ship2, AsteroidsSprite[] pho, AsteroidsSprite[] pho2, AsteroidsSprite[] expl, int hyper1, int hyper2, int shipsLeft1, int shipsLeft2,int score1, int score2, AsteroidsSprite ufo, AsteroidsSprite missle, int missleCounter, boolean playing) throws IOException, ClassNotFoundException {
	
	//ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
	
	
	
	InputList inputs = (InputList)objectInputStream.readObject();
	
	MultiList list = new MultiList(asteroids,small,ship1, ship2, pho, pho2, expl, hyper1, hyper2,shipsLeft1,shipsLeft2,score1,score2,ufo,missle, missleCounter, playing);
	objectOutputStream.writeObject(list);
	objectOutputStream.reset();
	return inputs;

  }
  public void sendGraphics(Graphics g) {
	  
  }
}
