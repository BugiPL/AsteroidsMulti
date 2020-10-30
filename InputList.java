import java.io.Serializable;

public class InputList implements Serializable {

	/**
	 * 
	 */

	boolean left2;
	boolean right2;
	boolean up2;
	boolean down2;
	boolean spacebar2;
	boolean startKey;
	boolean shipWraping;
	//boolean explodeShip1;
	//boolean explodeShip2;
	
	
	
	private static final long serialVersionUID = 2974474241899756570L;
	public InputList(boolean left, boolean right, boolean up, boolean down, boolean space, boolean start, boolean wrap) {
		left2 = left;
		right2 = right;
		up2 = up;
		down2 = down;
		spacebar2 = space;
		startKey = start;
		//explodeShip1 = explShip1;
		//explodeShip2 = explShip2;
		shipWraping = wrap;
				
	}
}