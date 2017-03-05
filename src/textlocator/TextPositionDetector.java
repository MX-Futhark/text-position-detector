package textlocator;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Locates text on an image.
 * Based on Michael R. Lyu, Jiqiang Song and Min Cai's "A comprehensive method
 *   for multilingual video text detection, localization and extraction".
 * The extraction part is left out.
 * @author MX-Futhark
 */
public class TextPositionDetector {

	/**
	 * Number of image resolutions to work with.
	 */
	public static final int ITERATIONS_NUMBER = 2;

	/**
	 * Applies the full multiresolution pipeline. See section Fig2.
	 * @param rgbImg The input image in which to locate text.
	 * @return All found text areas.
	 * @throws IOException
	 */
	public static List<Rectangle> apply(BufferedImage rgbImg)
		throws IOException {

		ImageDebug.print(rgbImg, "init");

		// global thresholding
		GrayImage grayImg =
			new GrayImage(rgbImg, GrayImage.DesaturationMethod.LUMINANCE);
		ImageDebug.print(grayImg, "gray");
		GrayImage edges = EdgeMap.applyGlobalThresholding(grayImg);
		List<Rectangle> allTextAreas = new LinkedList<>();

		for (int i = 1; i <= ITERATIONS_NUMBER; ++i) {

			// resize edge map
			BufferedImage initialEdgeMap = edges.toBufferedImage();
			hideFoundTextAreas(initialEdgeMap, allTextAreas);
			BufferedImage resizedEdgeMap = resizeEdgeMap(initialEdgeMap, i);
			ImageDebug.print(resizedEdgeMap, String.format("edges_A_%02d", i));

			// local thresholding
			GrayImage newEdges = new GrayImage(resizedEdgeMap);
			GrayImage postLocalThresholdMap =
				EdgeMap.applyLocalThresholding(newEdges);
			ImageDebug.print(postLocalThresholdMap,
				String.format("edges_B_%02d", i));

			// text recovery
			newEdges = postLocalThresholdMap;
				EdgeMap.applyTextRecovery(newEdges, postLocalThresholdMap);
			BufferedImage newEdgesBI = newEdges.toBufferedImage();
			ImageDebug.print(newEdgesBI, String.format("edges_C_%02d", i));

			// region detection
			List<Rectangle> textAreas =
				UniresolutionTextPositionDetector.getRegions(newEdges);
			allTextAreas.addAll(resizeTextAreas(textAreas, i));

			drawRectangles(newEdgesBI, textAreas,
				Color.YELLOW, String.format("areas_%02d", i));
		}

		drawRectangles(rgbImg, allTextAreas, Color.MAGENTA, "result");

		return allTextAreas;
	}

	// draws the bounds of text areas for debugging purpose
	private static void drawRectangles(BufferedImage img,
		List<Rectangle> rectangles, Color c, String imgName)
		throws IOException {

		Graphics2D graph = img.createGraphics();
		graph.setColor(c);

		for (Rectangle rectangle : rectangles) {
			graph.draw(rectangle);
		}

		graph.dispose();
		ImageDebug.print(img, imgName);
	}

	// resize the edge map following the function f(l) = l
	// see section IV.A
	// TODO: this function should be configurable
	private static BufferedImage resizeEdgeMap(BufferedImage edgeMap,
		int scaleDownFactor) {

		int scaleX = edgeMap.getWidth() / scaleDownFactor,
			scaleY = edgeMap.getHeight() / scaleDownFactor;

		Image img = edgeMap.getScaledInstance(
			scaleX, scaleY, Image.SCALE_AREA_AVERAGING
		);

		BufferedImage res =
			new BufferedImage(scaleX, scaleY, BufferedImage.TYPE_INT_RGB);

		res.getGraphics().drawImage(img, 0, 0 , null);

		return res;
	}

	// removes (supposed) text from the image to avoid detecting it again at
	// lower resolutions
	private static void hideFoundTextAreas(BufferedImage edgeMap,
		List<Rectangle> textAreas) {

		Graphics2D graph = edgeMap.createGraphics();
		graph.setColor(Color.BLACK);

		for (Rectangle textArea : textAreas) {
			graph.fill(textArea);
		}

		graph.dispose();
	}

	// resizes the text areas to match the resolution of the original image
	private static List<Rectangle> resizeTextAreas(List<Rectangle> textAreas,
		int scaleUpFactor) {

		List<Rectangle> res = new LinkedList<>();

		for (Rectangle textArea : textAreas) {

			res.add(new Rectangle(
				(int) textArea.getX() * scaleUpFactor,
				(int) textArea.getY() * scaleUpFactor,
				(int) textArea.getWidth() * scaleUpFactor,
				(int) textArea.getHeight() * scaleUpFactor
			));

		}

		return res;
	}

}
