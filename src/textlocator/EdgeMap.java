package textlocator;

import java.awt.Point;
import java.util.Set;
import java.util.TreeSet;

/**
 * Edge map related methods, see section IV.B
 * @author MX-Futhark
 */
class EdgeMap {

	// see section IV.B, global thresholding part
	public static final float EDGE_CORRECTION_FACTOR = 0.5f;
	public static final float EDGE_THRESHOLD_FACTOR = 0.3f;

	// see section IV.B, local thresholding part
	public static final int KERNEL_SIZE = 8;
	public static final int WINDOW_SIZE_FACTOR = 3; // assumed to be odd
	public static final float MIN_EDGE_PROFILE_BL_ROW_FACTOR = 0.3f;
	public static final int CONT_BL_ROWS_THRESHOLD = 4;
	public static final float LOCAL_EDGE_SUPPRESSION_THRESHOLD_FACTOR = 0.05f;
	public static final int HISTOGRAM_SIZE = 64;

	// see section IV.B, text recovery part
	public static final int TEXT_LABELING_RECT_WIDTH = 10;
	public static final int TEXT_LABELING_RECT_HEIGHT = 4;
	public static final int TEXT_LABELING_RECT_STEP_X = 5;
	public static final int TEXT_LABELING_RECT_STEP_Y = 2;
	public static final float EDGE_DENSITY_THRESHOLD_FACTOR = 0.20f;
	public static final float[][] HYSTERESIS_MASK =
		{{0.5f, 0.5f, 0.5f, 0.5f, 0.5f},
		 {0.5f, 0.8f, 0.8f, 0.8f, 0.5f},
		 {0.5f, 0.8f, 1.0f, 0.8f, 0.5f},
		 {0.5f, 0.8f, 0.8f, 0.8f, 0.5f},
		 {0.5f, 0.5f, 0.5f, 0.5f, 0.5f}};


	/**
	 * Applies global thresholding to the gray image. See section IV.B
	 * @param img The gray image.
	 * @return The gray image after global thresholding.
	 */
	public static GrayImage applyGlobalThresholding(GrayImage img) {

		GrayImage map = new GrayImage(img.getWidth(), img.getHeight());

		GrayImage[] sobels = {
			Sobel.convolve(img, Sobel.HORIZONTAL, Sobel.ABS | Sobel.NORMALIZED),
			Sobel.convolve(img, Sobel.VERTICAL, Sobel.ABS | Sobel.NORMALIZED),
			Sobel.convolve(img, Sobel.LDIAGONAL, Sobel.ABS | Sobel.NORMALIZED),
			Sobel.convolve(img, Sobel.RDIAGONAL, Sobel.ABS | Sobel.NORMALIZED)
		};

		for (int j = 0; j < map.getHeight(); ++j) {
			for (int i = 0; i < map.getWidth(); ++i) {

				int maxValue = -1, maxDirection = -1;
				for (int s = 0; s < sobels.length; ++s) {
					int value = Math.abs(sobels[s].getValue(i, j));
					if (value > maxValue) {
						maxValue = value;
						maxDirection = s;
					}
				}

				int normalDirection = -1;
				switch (maxDirection) {
				case Sobel.HORIZONTAL:
					normalDirection = Sobel.VERTICAL;
					break;
				case Sobel.VERTICAL:
					normalDirection = Sobel.HORIZONTAL;
					break;
				case Sobel.LDIAGONAL:
					normalDirection = Sobel.RDIAGONAL;
					break;
				case Sobel.RDIAGONAL:
					normalDirection = Sobel.LDIAGONAL;
					break;
				}

				int value = maxValue
					+ (int) (Math.abs(sobels[normalDirection].getValue(i, j))
					* EDGE_CORRECTION_FACTOR);
				value = Math.min(value, 255);
				map.setValue(
					i, j, value < 255 * EDGE_THRESHOLD_FACTOR ? 0 : value
				);
			}
		}

		return map;
	}


	/**
	 * Applies local thresholding to a gray image. See section IV.B
	 * @param map The gray image after global thresholding.
	 * @return The gray image after local thresholding.
	 */
	public static GrayImage applyLocalThresholding(GrayImage map) {

		GrayImage res = new GrayImage(map.getWidth(), map.getHeight());

		for (int j = 0; j < map.getHeight(); j += KERNEL_SIZE) {
			for (int i = 0; i < map.getWidth(); i += KERNEL_SIZE) {

				int kernelThreshold = getKernelThreshold(map, i, j);

				int maxX = Math.min(i + KERNEL_SIZE, map.getWidth()),
					maxY = Math.min(j + KERNEL_SIZE, map.getHeight());

				for (int l = j; l < maxY; ++l) {
					for (int k = i; k < maxX; ++k) {
						int value = map.getValue(k, l);
						res.setValue(k, l, value < kernelThreshold ? 0 : value);
					}
				}
			}
		}

		return res;
	}

	private static Histogram getEdgeStrengthHistogram(GrayImage map,
		int startX, int startY, int endX, int endY) {

		Histogram res = new Histogram(HISTOGRAM_SIZE);

		for (int j = startY; j < endY; ++j) {
			for (int i = startX; i < endX; ++i) {
				res.inc(map.getValue(i, j) / HISTOGRAM_SIZE);
			}
		}

		return res;
	}

	// TODO: break this down into several methods
	private static int getKernelThreshold(GrayImage map,
		int xStart, int yStart) {

		final int WINDOW_SIZE = 3 * KERNEL_SIZE;

		int winOffset = KERNEL_SIZE * (WINDOW_SIZE_FACTOR / 2);

		int startYInd = Math.max(yStart - winOffset, 0),
			startXInd = Math.max(xStart - winOffset, 0),
			endYInd = Math.min(
				yStart + KERNEL_SIZE + winOffset,
				map.getHeight()
			),
			endXInd = Math.min(
				xStart + KERNEL_SIZE + winOffset,
				map.getWidth()
			);

		int totalEdgeProfiles = 0, maxEdgeStrength = 0, currentNbContBlRows = 0,
			maxNbContBlRows = 0, maxEdgeProfile = 0;

		for (int j = startYInd; j < endYInd; ++j) {
			int rowEdgeProfile = 0;
			for (int i = startXInd; i < endXInd; ++i) {
				int value = map.getValue(i, j);
				if (value > maxEdgeStrength) {
					maxEdgeStrength = value;
				}
				rowEdgeProfile += value > 0 ? 1 : 0;
			}
			totalEdgeProfiles += rowEdgeProfile;
			if (rowEdgeProfile > maxEdgeProfile) {
				maxEdgeProfile = rowEdgeProfile;
			}
			if (rowEdgeProfile
				< (endXInd - startXInd) * MIN_EDGE_PROFILE_BL_ROW_FACTOR) {

				++currentNbContBlRows;
				if (currentNbContBlRows > maxNbContBlRows) {
					maxNbContBlRows = currentNbContBlRows;
				}
			} else {
				currentNbContBlRows = 0;
			}
		}

		if (totalEdgeProfiles < WINDOW_SIZE * WINDOW_SIZE
			* LOCAL_EDGE_SUPPRESSION_THRESHOLD_FACTOR) {

			return maxEdgeStrength + 1;
		}

		Histogram h = getEdgeStrengthHistogram(
			map, startXInd, startYInd, endXInd, endYInd
		);
		int integerMean = (int) h.getMean();
		if (maxNbContBlRows >= CONT_BL_ROWS_THRESHOLD) {
			return h.getOtsuThreshold(0, integerMean) * (256 / HISTOGRAM_SIZE);
		} else {
			return h.getOtsuThreshold(integerMean, h.getBins())
				* (256 / HISTOGRAM_SIZE);
		}
	}


	// TODO: this is largely unhelpful, find out why. The algorithm described
	//       in the paper is actually very ambiguous.
	/**
	 * Applies text recovery to the gray image. See section IV.B
	 * @param map The gray image after global thresholding.
	 * @param postLocalThresholdMap The gray image after local thresholding.
	 * @return The gray image after text recovery.
	 */
	public static GrayImage applyTextRecovery(GrayImage map,
		GrayImage postLocalThresholdMap) {

		GrayImage res = textLabeling(map, postLocalThresholdMap);
		TreeSet<Pixel> newTextPixels = new TreeSet<>();

		for (int j = 0; j < map.getHeight(); ++j) {
			for (int i = 0; i < map.getWidth(); ++i) {
				newTextPixels.clear();
				if (res.getValue(i, j) > 0) {
					int x = i, y = j;
					do {
						if (!newTextPixels.isEmpty()) {
							Point p = newTextPixels.pollFirst();
							x = (int) p.getX();
							y = (int) p.getY();
						}
						applyHysteresisMask(map, res, x, y, newTextPixels);
					} while (!newTextPixels.isEmpty());
				}
			}
		}

		return res;
	}

	private static void applyHysteresisMask(GrayImage map,
		GrayImage textRecoveryMap, int x, int y, Set<Pixel> newTextPixels) {

		int maskOffset = HYSTERESIS_MASK.length / 2;

		int postLocalThresholdValue = textRecoveryMap.getValue(x, y);

		for (int j = -maskOffset; j <= maskOffset; ++j) {

			if (y + j < 0 || y + j >= map.getHeight()) continue;

			for (int i = -maskOffset; i <= maskOffset; ++i) {

				if (x + i < 0 || x + i >= map.getHeight()) continue;
				int value = map.getValue(x + i, y + j),
					previousTextMapValue =
						textRecoveryMap.getValue(x + i, y + j);

				if (value >= postLocalThresholdValue
						* HYSTERESIS_MASK[j + maskOffset][i + maskOffset]) {

					textRecoveryMap.setValue(x + i, y + j, value);
				} else {
					value = previousTextMapValue;
					textRecoveryMap.setValue(x + i, y + j, value);
				}

				int newTextMapValue = textRecoveryMap.getValue(x + i, y + j);
				if (previousTextMapValue != newTextMapValue
					&& newTextMapValue > 0) {

					newTextPixels.add(new Pixel(x + i, y + j));
				}
			}
		}
	}

	private static GrayImage textLabeling(GrayImage map,
		GrayImage postLocalThresholdMap) {

		GrayImage res = new GrayImage(map.getWidth(), map.getHeight());

		for (int j = 0; j < map.getHeight(); j += TEXT_LABELING_RECT_STEP_Y) {
			for (int i = 0; i < map.getWidth();
				i += TEXT_LABELING_RECT_STEP_X) {

				int maxX =
					Math.min(i + TEXT_LABELING_RECT_WIDTH, map.getWidth()),
					maxY =
					Math.min(j + TEXT_LABELING_RECT_HEIGHT, map.getHeight());

				int totalEdgeStrength = 0,
					pixelsNumber = (maxX - i) * (maxY - j);

				for (int l = j; l < maxY; ++l) {
					for (int k = i; k < maxX; ++k) {
						totalEdgeStrength +=
							postLocalThresholdMap.getValue(k, l);
					}
				}

				int edgeDensity = totalEdgeStrength / 255;

				if (edgeDensity
					> EDGE_DENSITY_THRESHOLD_FACTOR * pixelsNumber) {

					for (int l = j; l < maxY; ++l) {
						for (int k = i; k < maxX; ++k) {
							// non zero = text
							int value = postLocalThresholdMap.getValue(k, l);
							res.setValue(k, l, value);
						}
					}
				}

			}
		}

		return res;
	}

}
