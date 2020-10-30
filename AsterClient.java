/************************************************************************************************

Asteroids.java

  Usage:

  <applet code="AsteroidsClient.class" width=w height=h></applet>

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
  The AsteroidsSprite class defines a game object, including it's shape, position, movement and
  rotation. It also can detemine if two objects collide.
************************************************************************************************/



/************************************************************************************************
  Main applet code.
************************************************************************************************/

public class AsterClient extends Applet implements Runnable {

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
  boolean spacebar2  = false;
  boolean startKey  = false;
  boolean shipWraping = false;

  // Sprite objects.

  AsteroidsSprite   ship;
  AsteroidsSprite   ship2;
  AsteroidsSprite   ufo;
  AsteroidsSprite   missle;
  AsteroidsSprite[] photons    = new AsteroidsSprite[MAX_SHOTS];
  AsteroidsSprite[] photons2    = new AsteroidsSprite[MAX_SHOTS];
  AsteroidsSprite[] asteroids;
  AsteroidsSprite[] explosions = new AsteroidsSprite[MAX_SCRAP];;

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

  boolean[] asteroidIsSmall;   // Asteroid size flag.
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

  // Sockets and data stream objects
  
  Socket socket;
  ObjectInputStream objectInputStream;
  ObjectOutputStream objectOutputStream;


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



    // Create explosion sprites.

    for (i = 0; i < MAX_SCRAP; i++)
      explosions[i] = new AsteroidsSprite();

    // Set font data.

    g.setFont(font);
    fm = g.getFontMetrics();
    fontWidth = fm.getMaxAdvance();
    fontHeight = fm.getHeight();

    // Initialize game data and put us in 'game over' mode.

    try {
		setServer();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    
    highScore = 0;
    sound = true;
    detail = true;
    initGame();
    endGame();
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

    initExplosions();
    playing = true;
    paused = false;
    
    // sending and receiving data from server
    try {
		MultiList list = sendData();
		asteroids = list.asteroids;
		asteroidIsSmall = list.asteroidsIsSmall;
		ship = list.ship;
		ship2 = list.ship2;
		photons = list.photons;
		photons2 = list.photons2;
		hyperCounter = list.hyperCounter;
		hyperCounter2 = list.hyperCounter2;
		scoreB = list.scoreB;
		scoreG = list.scoreG;
		ufo = list.ufo;
		missle = list.missle;
		missleCounter = list.missleCounter;
	} catch (ClassNotFoundException | IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    


    //rendering all the recived data
    for(int l=0;l<asteroids.length;l++) {
    	asteroids[l].render();
    	
    }
    for(int l=0;l<photons.length;l++) {
    	photons[l].render();
    	
    }
    
    for(int l=0;l<photons2.length;l++) {
    	photons2[l].render();
    	
    }
    for(int l=0;l<explosions.length;l++) {
    	explosions[l].render();
    	
    }
    
  }

  public void endGame() {

    // Stop ship, flying saucer, guided missle and associated sounds.

    playing = false;


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
    	  //conecting to server once again to send and grab data from it
    	  try {
    			MultiList list = sendData();
    			asteroids = list.asteroids;
    			asteroidIsSmall = list.asteroidsIsSmall;
    			ship = list.ship;
    			ship2 = list.ship2;
    			photons = list.photons;
    			photons2 = list.photons2;
    			shipsLeft = list.shipsLeft;
    			shipsLeft2 = list.shipsLeft2;
    			scoreB = list.scoreB;
    			scoreG = list.scoreG;
    			ufo = list.ufo;
    			missle = list.missle;
    			missleCounter = list.missleCounter;
    			playing = list.playing;
    			//explosions = list.explosions;
    		} catch (ClassNotFoundException | IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    	    
    	    //checking for asteroids colisions
    	    for(int l=0;l<asteroids.length;l++) {
    	    	asteroids[l].render();
    	    	
    	    	for (j = 0; j < photons.length; j++)
    	            if (photons[j].active && asteroids[l].active && asteroids[l].isColliding(photons[j])) {
    	              asteroidsLeft--;
    	              asteroids[l].active = false;
    	              photons[j].active = false;
    	              if (sound)
    	                explosionSound.play();
    	              explode(asteroids[l]);
    	              

    	            }
    	    	for (j = 0; j < photons2.length; j++)
    	            if (photons2[j].active && asteroids[l].active && asteroids[l].isColliding(photons2[j])) {
    	              asteroidsLeft--;
    	              asteroids[l].active = false;
    	              photons2[j].active = false;
    	              if (sound)
    	                explosionSound.play();
    	              explode(asteroids[l]);
    	              

    	            }
    	    	if (ship.active && hyperCounter <= 0 && asteroids[l].active && asteroids[l].isColliding(ship)) {
    	              if (sound)
    	                crashSound.play();

    	              explode(ship);
    	              hyperCounter = HYPER_COUNT;
    	              ship.active = false;
    	              shipCounter = SCRAP_COUNT;
    	              
    	              //stopShip();
    	              //stopUfo();
    	              //stopMissle();
    	            }
    	    	if (ship2.active && hyperCounter2 <= 0 && asteroids[l].active && asteroids[l].isColliding(ship2)) {
  	              if (sound)
  	                crashSound.play();

  	              explode(ship2);
  	              hyperCounter2 = HYPER_COUNT;
  	              ship2.active = false;
  	              shipCounter2 = SCRAP_COUNT;
  	            
  	              //stopShip();
  	              //stopUfo();
  	              //stopMissle();
  	            }
    	    	
    	    	if (ship.active && hyperCounter <= 0 && ship2.active && hyperCounter2 <= 0 && ship.isColliding(ship2)) {
    	        	if (sound)
    	                crashSound.play();
    	             explode(ship);
    	             explode(ship2);
    	             hyperCounter = HYPER_COUNT;
     	             ship.active = false;
     	             shipCounter = SCRAP_COUNT;
    	             hyperCounter2 = HYPER_COUNT;
     	             ship2.active = false;
     	             shipCounter2 = SCRAP_COUNT;
     	            
     	           
    	        }
    	    	//checking for ship collision with photons (players fighting each other)
    	    	for (j = 0; j < MAX_SHOTS; j++)
    	            if (photons2[j].active && ship.active && hyperCounter <= 0 && ship.isColliding(photons2[j])) {
    	            	photons2[j].active = false;
    	              if (sound)
    	                explosionSound.play();
    	              explode(ship);
    	              hyperCounter = HYPER_COUNT;
      	              ship.active = false;
      	              shipCounter = SCRAP_COUNT;
    	              
    	              //stopShip();
    	            }
    	        for (j = 0; j < MAX_SHOTS; j++)
    	            if (photons[j].active && ship2.active && hyperCounter2 <= 0 && ship2.isColliding(photons[j])) {
    	            	photons[j].active = false;
    	              if (sound)
    	                explosionSound.play();
    	              explode(ship2);
    	              hyperCounter2 = HYPER_COUNT;
      	              ship2.active = false;
      	              shipCounter2 = SCRAP_COUNT;
    	              
    	            }
    	        //checking for colision with ufo
    	        for (i = 0; i < MAX_SHOTS; i++)
    	            if (photons[i].active && ufo.isColliding(photons[i])) {
    	              if (sound)
    	                crashSound.play();
    	              explode(ufo);
    	              ufo.active = false;
    	              ufoCounter = 0;
    	              ufoPassesLeft = 0;
    	              if (loaded)
    	                saucerSound.stop();
    	              saucerPlaying = false;
    	            }
    	          for (i = 0; i < MAX_SHOTS; i++)
    	              if (photons2[i].active && ufo.isColliding(photons2[i])) {
    	                if (sound)
    	                  crashSound.play();
    	                explode(ufo);
    	                ufo.active = false;
    	                ufoCounter = 0;
    	                ufoPassesLeft = 0;
    	                if (loaded)
    	                  saucerSound.stop();
    	                saucerPlaying = false;
    	                
    	              }
    	          //checking for collision with missle
    	          for (i = 0; i < MAX_SHOTS; i++) {
    	              if (photons[i].active && missle.isColliding(photons[i])) {
    	                if (sound)
    	                  crashSound.play();
    	                explode(missle);
    	                missle.active = false;
    	                missleCounter = 0;
    	                if (loaded)
    	                  missleSound.stop();
    	                misslePlaying = false;
    	                scoreB += MISSLE_POINTS;
    	              }
    	            if (photons2[i].active && missle.isColliding(photons2[i])) {
    	                if (sound)
    	                  crashSound.play();
    	                explode(missle);
    	                missle.active = false;
    	                missleCounter = 0;
    	                if (loaded)
    	                  missleSound.stop();
    	                misslePlaying = false;
    	                scoreG += MISSLE_POINTS;
    	              }
    	            }
    	          //checking for missile colliding ship
    	          if (missle.active && ship.active && hyperCounter <= 0 && ship.isColliding(missle)) {
    	              if (sound)
    	                crashSound.play();
    	              explode(ship);
    	              hyperCounter = HYPER_COUNT;
      	              ship.active = false;
      	              shipCounter = SCRAP_COUNT;
      	              explode(ufo);
      	              ufo.active = false;
      	              ufoCounter = 0;
      	              ufoPassesLeft = 0;
      	              if (loaded)
      	            	  saucerSound.stop();
      	              saucerPlaying = false;
      	              explode(missle);
      	              missle.active = false;
      	              missleCounter = 0;
      	              if (loaded)
	                  missleSound.stop();
      	              misslePlaying = false;
    	            }
    	          if (missle.active && ship2.active && hyperCounter2 <= 0 && ship2.isColliding(missle)) {
    	                if (sound)
    	                  crashSound.play();
    	                explode(ship2);
    	                hyperCounter = HYPER_COUNT;
        	            ship.active = false;
        	            shipCounter = SCRAP_COUNT;
        	            explode(ufo);
        	            ufo.active = false;
        	            ufoCounter = 0;
        	            ufoPassesLeft = 0;
        	            if (loaded)
        	            	saucerSound.stop();
        	            saucerPlaying = false;
        	            explode(missle);
        	            missle.active = false;
        	            missleCounter = 0;
        	            if (loaded)
  	                    missleSound.stop();
        	            misslePlaying = false;
    	              }
    	    }
    	 // If the ship is not in hyperspace, see if it is hit.

            
    	    if (ship.active && hyperCounter > 0)
    	        hyperCounter--;
    	    if (ship2.active && hyperCounter2 > 0)
    	        hyperCounter2--;
    	    for(int l=0;l<photons.length;l++) {
    	    	photons[l].render();
    	    	
    	    }
    	    for(int l=0;l<photons2.length;l++) {
    	    	photons2[l].render();
    	    	
    	    }
    	    for(int l=0;l<explosions.length;l++) {
    	    	explosions[l].render();
    	    	
    	    }

    	    


        updateExplosions();

    	


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
      photonIndex2++;
      spacebar2 = true;
    }
    
    

    // 'H' key: warp ship into hyperspace by moving to a random location and starting counter.

    if (key == 104 && ship2.active && hyperCounter2 <= 0) {
      shipWraping = true;
      
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

    if (key == 115 && loaded && !playing) {
     startKey = true;
     playing = true;
     paused = false;
    }

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

    for (i = 0; i < asteroids.length; i++)
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

    offGraphics.drawString("Score Black: " + scoreB, fontWidth, fontHeight);
    offGraphics.drawString("Ships: " + shipsLeft2, fontWidth, d.height - fontHeight);
    s = "Score Green: " + scoreG;
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
  
  //seting up the server setings
  public void setServer() throws UnknownHostException, IOException {
	  socket = new Socket("localhost", 4444);
	  objectInputStream = new ObjectInputStream(socket.getInputStream());
	  objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
  }
  //grabing data
  public MultiList sendData() throws IOException, ClassNotFoundException {

	//This is an artificial delay in communication between server and client(set to 0 for no delay)
	  try {
			Thread.sleep(0);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	InputList inputs = new InputList(left,right,up,down,spacebar2,startKey, shipWraping);
	objectOutputStream.writeObject(inputs);
	spacebar2 = false;
	startKey = false;
	shipWraping = false;
	objectOutputStream.reset();
	
	MultiList list = (MultiList)objectInputStream.readObject();
	
	return list;
	
  }
  public void sendGraphics(Graphics g) {
	  
  }
}