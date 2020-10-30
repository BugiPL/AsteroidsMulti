import java.awt.Polygon;
import java.io.Serializable;

public class MultiList implements Serializable {

	/**
	 * 
	 */
	AsteroidsSprite[] asteroids;
	boolean[] asteroidsIsSmall;
	AsteroidsSprite[] photons;
	AsteroidsSprite[] photons2;
	AsteroidsSprite ship;
	AsteroidsSprite ship2;
	AsteroidsSprite[] explosions;
	AsteroidsSprite ufo;
	AsteroidsSprite missle;
	int missleCounter;
	
	int hyperCounter;    // Time counter for hyperspace.
	int hyperCounter2;
	int shipsLeft;
	int shipsLeft2;
	int scoreB;
	int scoreG;
	
	boolean playing;

	
	private static final long serialVersionUID = 2974474241799756570L;
	public MultiList(AsteroidsSprite[] aster, boolean[] small, AsteroidsSprite ship1, AsteroidsSprite ship2, AsteroidsSprite[] pho, AsteroidsSprite[] pho2, AsteroidsSprite[] exp, int hyper1, int hyper2,int shipsLeft1,int shipsLeft2, int score1, int score2, AsteroidsSprite u,AsteroidsSprite m,int missleCounter, boolean play) {
		asteroids = aster;
		asteroidsIsSmall = small;
		ship = ship1;
		this.ship2 = ship2;
		photons = pho;
		photons2 = pho2;
		explosions =  exp;
		hyperCounter = hyper1;
		hyperCounter2 = hyper2;
		this.shipsLeft = shipsLeft1;
		this.shipsLeft2 = shipsLeft2;
		this.scoreB = score1;
		this.scoreG = score2;
		this.ufo = u;
		this.missle = m;
		this.missleCounter = missleCounter;
		this.playing = play;
	}
}
