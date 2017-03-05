package textlocator;

import java.awt.Point;

/**
 * Comparable pixel.
 * @author MX-Futhark
 */
@SuppressWarnings("serial")
class Pixel extends Point implements Comparable<Point> {

	public Pixel(int x, int y) {
		super(x, y);
	}

	@Override
	public int compareTo(Point o) {
		int res = Integer.compare((int) this.getX(), (int) o.getX());
		if (res == 0) {
			res = Integer.compare((int) this.getY(), (int) o.getY());
		}
		return res;
	}

}
