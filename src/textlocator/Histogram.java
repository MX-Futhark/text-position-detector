package textlocator;

/**
 * Represents an histogram.
 * @author MX-Futhark
 */
class Histogram {

	private int[] data;

	/**
	 * Histogram constructor.
	 * @param bins Number of bins in the histogram.
	 */
	public Histogram(int bins) {
		data = new int[bins];
	}

	/**
	 * Increments data at a given index.
	 * @param ind The bin index.
	 */
	public void inc(int ind) {
		++data[ind];
	}

	/**
	 * Getter of the number of bins in the histogram.
	 * @return the numner of bins in the histogram.
	 */
	public int getBins() {
		return data.length;
	}

	/**
	 * Provides the sum of data between two bins.
	 * @param minInd Bin to start counting from (inclusive).
	 * @param maxInd Bin to stop counting at (exclusive).
	 * @return The sum of data between the two bins.
	 */
	public int getTotal(int minInd, int maxInd) {

		int total = 0;

		for (int i = minInd; i < maxInd; ++i) {
			total += data[i];
		}

		return total;
	}

	/**
	 * Provides the global mean of the histogram.
	 * @return the global mean of the histogram.
	 */
	public float getMean() {

		int total = 0, weightedTotal = 0;

		for (int i = 0; i < data.length; ++i) {
			total += data[i];
			weightedTotal += data[i] * i;
		}

		return (weightedTotal * 1.0f) / total;
	}

	/**
	 * Provides the Otsu threshold between two bins.
	 * @param minInd Inclusive lower bin index.
	 * @param maxInd Exclusive upper bin index.
	 * @return The Otsu threshold between the two bins.
	 */
	public int getOtsuThreshold(int minInd, int maxInd) {

		int sum = 0, sumB = 0, wB = 0, wF = 0, mB, mF, max = 0, between,
			threshold = minInd, pixelsNumber = getTotal(minInd, maxInd);

		for (int i = minInd; i < maxInd; ++i) {

			wB += data[i];

			if (wB == 0) continue;

			wF = pixelsNumber - wB;

			if (wF == 0) break;

			sumB += i * data[i];
			mB = sumB / wB;
			mF = (sum - sumB) / wF;
			between = wB * wF * (mB - mF) * (mB - mF);

			if (between > max) {
				max = between;
				threshold = i;
			}
		}
		return threshold;
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		int maxDigits = (data.length+"").length();

		for (int i = 0; i < data.length; ++i) {

			sb.append(String.format("%0" + maxDigits + "d", i) + ": ");


			for (int n = 0; n < data[i]; ++n) {
				sb.append("|");
			}

			sb.append("\n");
		}

		return sb.toString();
	}

}
