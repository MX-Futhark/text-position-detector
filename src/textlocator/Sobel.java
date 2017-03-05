package textlocator;

/**
 * Utility methods to apply the Sobel filter.
 * @author MX-Futhark
 */
class Sobel {

	public static final int HORIZONTAL = 0;
	public static final int VERTICAL = 1;
	public static final int LDIAGONAL = 2;
	public static final int RDIAGONAL = 3;

	public static final int RAW = 0;
	public static final int ABS = 1;
	public static final int NORMALIZED = 2;

	private static final float[][] HORIZONTAL_A =
		{{-1f,  0f,  1f},
		 {-2f,  0f,  2f},
		 {-1f,  0f,  1f}};

	private static final float[][] VERTICAL_A =
		{{-1f, -2f, -1f},
		 { 0f,  0f,  0f},
		 { 1f,  2f,  1f}};

	private static final float[][] LDIAGONAL_A =
		{{-2f, -1f,  0f},
		 {-1f,  0f,  1f},
		 { 0f,  1f,  2f}};

	private static final float[][] RDIAGONAL_A =
		{{0f, -1f, -2f},
		 {1f,  0f, -1f},
		 {2f,  1f,  0f}};

	private static int convolveLocal(GrayImage img, int x, int y,
		float[][] filter) {

		int halfLen = filter.length / 2,
		    res = 0;

		for (int j = -halfLen; j <= halfLen; ++j) {
			for (int i = -halfLen; i <= +halfLen; ++i) {
				res += filter[halfLen - j][halfLen - i]
					* img.getExtendedValue(x+i, y+j, true);
			}
		}

		return res;
	}

	public static GrayImage convolve(GrayImage img, int direction, int mode) {

		GrayImage res = new GrayImage(img.getWidth(), img.getHeight());

		float[][] filter = null;
		switch (direction) {
		case HORIZONTAL:
			filter = HORIZONTAL_A;
			break;
		case VERTICAL:
			filter = VERTICAL_A;
			break;
		case LDIAGONAL:
			filter = LDIAGONAL_A;
			break;
		case RDIAGONAL:
			filter = RDIAGONAL_A;
			break;
		}

		for (int j = 0; j < img.getHeight(); ++j) {
			for (int i = 0; i < img.getWidth(); ++i) {
				int value = convolveLocal(img, i, j, filter);
				switch (mode) {
				case ABS:
					value = Math.abs(value);
					break;
				case NORMALIZED:
					value = (value + 255 * 4) / 8;
					break;
				case ABS | NORMALIZED:
					value = Math.abs(value) / 4;
					break;
				}
				res.setValue(i, j, value);
			}
		}

		return res;
	}

}
