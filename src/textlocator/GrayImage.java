package textlocator;

import java.awt.image.BufferedImage;

/**
 * Represents an image in levels of gray.
 * @author MX-Futhark
 */
class GrayImage {

	/**
	 * Determines the desaturation method to use on a color image.
	 * @author MX-Futhark
	 */
	public enum DesaturationMethod {
		AVERAGE, LUMINANCE, DESATURATION, GREEN_ONLY
	}

	private int[][] values;

	/**
	 * Constructor for a black image.
	 * @param width The width of the image.
	 * @param height The height of the image.
	 */
	public GrayImage(int width, int height) {
		values = new int[height][width];
	}

	/**
	 * Constructor for a gray image from a color image.
	 * @param rgbImg The RGB image.
	 * @param method The desaturation method.
	 */
	public GrayImage(BufferedImage rgbImg, DesaturationMethod method) {
		this(rgbImg.getWidth(), rgbImg.getHeight());
		for (int j = 0; j < getHeight(); ++j) {
			for (int i = 0; i < getWidth(); ++i) {
				switch (method) {
				case AVERAGE:
					setValue(i, j, average(rgbImg.getRGB(i, j)));
					break;
				case LUMINANCE:
					setValue(i, j, luminance(rgbImg.getRGB(i, j)));
					break;
				case DESATURATION:
					setValue(i, j, desaturation(rgbImg.getRGB(i, j)));
					break;
				case GREEN_ONLY:
					setValue(i, j, (rgbImg.getRGB(i, j) >> 8) & 0xFF);
					break;
				}
			}
		}
	}

	/**
	 * Constructor of a GrayImage from a BufferedImage already in gray.
	 * @param grayImg The gray image to convert.
	 */
	public GrayImage(BufferedImage grayImg) {
		this(grayImg, DesaturationMethod.GREEN_ONLY);
	}

	/**
	 * Getter on the height of the image.
	 * @return The height of the image.
	 */
	public int getHeight() {
		return values.length;
	}

	/**
	 * Getter on the width of the image.
	 * @return The width of the image.
	 */
	public int getWidth() {
		return getHeight() > 0 ? values[0].length : 0;
	}

	// average value of a color pixel
	private static int average(int rgb) {
		return ((rgb & 0xFF)
			+ ((rgb >> 8) & 0xFF)
			+ ((rgb >> 16) & 0xFF)) / 3;
	}

	// luminance value of a color pixel
	private static int luminance(int rgb) {
		return (int) (0.0722 * (rgb & 0xFF) // b
			+ 0.7152 * ((rgb >> 8) & 0xFF) // g
			+ 0.2126 * ((rgb >> 16) & 0xFF)); // r
	}

	// desaturated value of a color pixel
	private static int desaturation(int rgb) {
		int b = (rgb & 0xFF),
			g = ((rgb >> 8) & 0xFF),
			r = ((rgb >> 16) & 0xFF);
		return (Math.max(Math.max(r, g), b)
			+ Math.min(Math.min(r, g), b)) / 2;
	}

	/**
	 * Returns the level of gray of a pixel.
	 * @param x The X coordinate of the pixel.
	 * @param y The Y coordinate of the pixel.
	 * @return The level of gray of the chosen pixel.
	 */
	public int getValue(int x, int y) {
		return values[y][x];
	}

	/**
	 * Set the level of gray of a pixel.
	 * @param x The X coordinate of the pixel.
	 * @param y The Y coordinate of the pixel.
	 * @param value The new value of the pixel.
	 */
	public void setValue(int x, int y, int value) {
		values[y][x] = value;
	}

	/**
	 * Converts the GrayImage into a BufferedImage.
	 * @return The corresponding BufferedImage.
	 */
	public BufferedImage toBufferedImage() {
		BufferedImage res = new BufferedImage(
			getWidth(),
			getHeight(),
			BufferedImage.TYPE_INT_RGB
		);

		for (int j = 0; j < getHeight(); ++j) {
			for (int i = 0; i < getWidth(); ++i) {
				int value = Math.min(Math.max(getValue(i, j), 0), 255);
				res.setRGB(i, j, (value << 16) | (value << 8) | value);
			}
		}

		return res;
	}

	/**
	 * Utility method to get the value of a pixel even when going over the edge.
	 * @param x The X-coordinate of the pixel.
	 * @param y The Y-coordinate of the pixel.
	 * @param extendEdge True to use the value of the closest pixel if going
	 *                   over the edge, false to use black by default.
	 * @return The value of the chosen pixel.
	 */
	public int getExtendedValue(int x, int y, boolean extendEdge) {
		return extendEdge
			// use value of the closest edge
			? getValue(
				Math.min(Math.max(x, 0), getWidth() - 1),
				Math.min(Math.max(y, 0), getHeight() - 1)
			)
			// use zeroes
			: (x < 0 || x >= getWidth() ||	y < 0 || y >= getHeight())
				? 0
				: getValue(x, y);
	}

}
