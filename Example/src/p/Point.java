package p;

public class Point implements Figure {
	private double x;
	private double y;

	public void setX(double x) {
		this.x = x;
	}

	public void setTwiceX(double x) {
		this.x = 2 * x;
	}
	
	public double getY() {
		return this.y;
	}
	
	// Adding this method does not break any advice.
//	 public double getTwiceY() {
//	     return this.y * 2;
//	 }

	// Adding this method breaks the after() advice
	// but not the around() advice.
//	 public void move(double x, double y) {
//	     this.x = x;
//	     this.y = y;
//	 }

	// Adding this method breaks the around() advice
	// but not the after() advice.
//	public double inverseOfY() {
//		return -this.y;
//	}
}