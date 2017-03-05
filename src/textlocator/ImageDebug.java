package textlocator;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Utility class to output intermediate results.
 * @author MX-Futhark
 */
class ImageDebug {

	private static final boolean DEBUG = false;

	/**
	 * Print the given image.
	 * @param img The image to print.
	 * @throws IOException
	 */
	public static void print(BufferedImage img, String imgName)
		throws IOException {

		if (!DEBUG) return;

		ImageIO.write(img, "png", new File(imgName + ".png"));
	}

	/**
	 * Prints the given image in levels of gray.
	 * @param img The image to print.
	 * @throws IOException
	 */
	public static void print(GrayImage img, String imgName) throws IOException {
		print(img.toBufferedImage(), imgName);
	}
}
