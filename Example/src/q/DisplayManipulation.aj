package q;

import p.Display;
//import p.Point;
import p.Figure;

public aspect DisplayManipulation {
	// Refresh the display after Figure modifications.
	after():
		execution(* Figure+.set*(..)) {
			Display.update();
		}

	// Zoom in.
	double around():
		execution(double Figure+.get*(..)) {
			return proceed() * 0.5;
	}
}
