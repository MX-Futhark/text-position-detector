package textlocator;

import java.awt.Rectangle;
import java.util.LinkedList;
import java.util.List;

/**
 * Locates text area at a given image resolution. See section IV.B
 * NOTE: 1. only a rough outline of the algorithm is provided and certain
 *          parts had to be outright guessed.
 *       2. this class assumes that the text is horizontal
 * @author MX-Futhark
 */
class UniresolutionTextPositionDetector {

	// TODO: not ideal to rely on a factor to discriminate peaks from valleys
	// find several strategies and compare them
	private static final float PEAK_VALLEY_THRESHOLD_FACTOR = 0.1f;
	private static final int HORIZONTAL_PEAK_ABS_MIN_THRESHOLD = 2048;
	private static final int VERTICAL_PEAK_ABS_MIN_THRESHOLD = 256;

	private static final int HORIZONTAL_MERGEABLE_HOLE_SIZE = 1;
	private static final int VERTICAL_MERGEABLE_HOLE_SIZE = 4;

	private static final int MIN_FONT_SIZE = 8;
	private static final int MAX_FONT_SIZE = 24;
	private static final float MIN_CHAR_ASPECT_RATIO = 1.0f;

	/**
	 * Detects text region in an edge map. See Fig6.
	 * @param edgeMap The already computed edge map.
	 * @return All text regions found in the edge map.
	 */
	public static List<Rectangle> getRegions(GrayImage edgeMap) {

		List<Rectangle>
			regionsQueue = new LinkedList<>(),
			validRegions = new LinkedList<>(),
			tentativeHorizontalRegions = new LinkedList<>(),
			tentativeVerticalRegions = new LinkedList<>();

		regionsQueue.add(
			new Rectangle(edgeMap.getWidth(), edgeMap.getHeight())
		);

		while (!regionsQueue.isEmpty()) {

			Rectangle region = regionsQueue.remove(0);
			tentativeHorizontalRegions =
				getHorizontalSubRegions(edgeMap, region);

			for (Rectangle hSubRegion : tentativeHorizontalRegions) {

				boolean hIndivisible = hSubRegion.equals(region);
				tentativeVerticalRegions =
					getVerticalSubRegions(edgeMap, hSubRegion, hIndivisible);

				for (Rectangle vSubRegion : tentativeVerticalRegions) {

					// indivisible region
					if (vSubRegion.equals(hSubRegion)) {
						validRegions.add(vSubRegion);
					} else {
						regionsQueue.add(vSubRegion);
					}
				}
			}
		}

		return validRegions;
	}

	private static int[] getProjection(GrayImage edgeMap,
		Rectangle region, boolean horizontal) {

		int aMin = (int) (horizontal ? region.getY() : region.getX()),
			bMin = (int) (horizontal ? region.getX() : region.getY()),
			aLen = (int) (horizontal ? region.getHeight() : region.getWidth()),
			bLen = (int) (horizontal ? region.getWidth() : region.getHeight()),
			ind = 0;

		int[] res = new int[aLen];

		for (int a = aMin; a < aMin + aLen; ++a) {
			int total = 0;
			for (int b = bMin; b < bMin + bLen; ++b) {
				// TODO other possible strategy: counting edge pixels instead of
				//      taking their value into account
				total +=
					edgeMap.getValue(horizontal ? b : a, horizontal ? a : b);
			}
			res[ind] = total;
			++ind;
		}

		return res;
	}

	// TODO: do without this, try not to split regions in the first place
	private static List<Rectangle> mergeSubRegions(List<Rectangle> subRegions,
		boolean horizontal, boolean wasHorizontalIndivisible) {

		List<Rectangle> res = new LinkedList<>();

		boolean mergeProgresses;

		// merge regions with negligible holes
		Rectangle previousSubRegion = null, currentSubRegion = null;
		for (Rectangle subRegion : subRegions) {

			mergeProgresses = false;
			currentSubRegion = subRegion;

			if (previousSubRegion != null) {
				if (horizontal) {
					if (subRegion.getY() - (previousSubRegion.getY()
							+ previousSubRegion.getHeight())
						<= HORIZONTAL_MERGEABLE_HOLE_SIZE) {

						mergeProgresses = true;
					}
				} else {

					double valleyWidth =
						subRegion.getX() - (previousSubRegion.getX()
						+ previousSubRegion.getWidth());
					double subRegionHeight = subRegion.getHeight();

					if (valleyWidth <= VERTICAL_MERGEABLE_HOLE_SIZE
						|| (wasHorizontalIndivisible
							&& subRegionHeight >= MIN_FONT_SIZE
							&& subRegionHeight <= MAX_FONT_SIZE
							&& valleyWidth < 1.5 * MIN_CHAR_ASPECT_RATIO
								* subRegionHeight)) {

						mergeProgresses = true;
					}
				}
			}
			if (mergeProgresses) {
				previousSubRegion =
					previousSubRegion.union(subRegion);
				currentSubRegion = previousSubRegion;
			} else if (previousSubRegion != null) {
				res.add(previousSubRegion);
			}
			previousSubRegion = currentSubRegion;
		}
		if (previousSubRegion != null) {
			res.add(previousSubRegion);
		}

		return res;

	}

	private static List<Rectangle> getSubRegions(GrayImage edgeMap,
		Rectangle region, boolean horizontal,
		boolean wasHorizontalIndivisible) {

		int[] projection = getProjection(edgeMap, region, horizontal);
		int min = Integer.MAX_VALUE, max = 0;

		// TODO: local thresholding with a window of size 2*MAX_FONT_SIZE,
		//       moving 2*MIN_FONT_SIZE per iteration?
		for (int i = 0; i < projection.length; ++i) {
			if (projection[i] < min) {
				min = projection[i];
			}
			if (projection[i] > max) {
				max = projection[i];
			}
		}

		int threshold = Math.max(
			(int)(min + (max - min) * PEAK_VALLEY_THRESHOLD_FACTOR),
			horizontal
				? HORIZONTAL_PEAK_ABS_MIN_THRESHOLD
				: VERTICAL_PEAK_ABS_MIN_THRESHOLD
		);


		int i = 0, regionStart = 0;
		List<Rectangle> subRegions = new LinkedList<>();

		while(i < projection.length) {

			while (i < projection.length && projection[i] < threshold) ++i;

			regionStart = i;

			while (i < projection.length && projection[i] >= threshold) ++i;

			if (regionStart < projection.length) {
				subRegions.add(new Rectangle(
					(int) region.getX() + (horizontal ? 0 : regionStart),
					(int) region.getY() + (horizontal ? regionStart : 0),
					horizontal ? (int) region.getWidth() : i - regionStart,
					horizontal ? i - regionStart : (int) region.getHeight()
				));
			}
		}

		return
			mergeSubRegions(subRegions, horizontal, wasHorizontalIndivisible);
	}

	private static List<Rectangle> getHorizontalSubRegions(GrayImage edgeMap,
		Rectangle region) {

		List<Rectangle>
			subRegions = getSubRegions(edgeMap, region, true, false),
			res = new LinkedList<>();

		for (Rectangle subRegion : subRegions) {
			if (subRegion.getHeight() < MIN_FONT_SIZE) continue;
			res.add(subRegion);
		}

		return res;
	}

	private static List<Rectangle> getVerticalSubRegions(GrayImage edgeMap,
		Rectangle region, boolean wasHorizontalIndivisible) {

		List<Rectangle>
			subRegions =
				getSubRegions(edgeMap, region, false, wasHorizontalIndivisible),
			res = new LinkedList<>();

		for (Rectangle subRegion : subRegions) {

			double height = subRegion.getHeight(),
				width = subRegion.getWidth();

			if (height <= MAX_FONT_SIZE
				&& width >= height * MIN_CHAR_ASPECT_RATIO) {

				res.add(subRegion);
			}
		}

		return res;
	}

}
