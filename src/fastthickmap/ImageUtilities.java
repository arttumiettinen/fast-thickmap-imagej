package fastthickmap;

public class ImageUtilities {

	/**
	 * Calculates maximum value in the given image.
	 * @param img
	 * @return
	 */
	public static float max(Image img) {
		float M = Float.NEGATIVE_INFINITY;
		
		for (int z = 0; z < img.depth(); z++) {
			for (int y = 0; y < img.height(); y++) {
				for (int x = 0; x < img.width(); x++) {
					float p = img.get(x, y, z);
					if(p > M)
						M = p;
				}
			}
		}
		
		return M;
	}
	
	/**
	 * Sets all pixels in the image to given value.
	 * @param img
	 * @param val
	 */
	public static void setValue(Image img, float val) {
		for (int z = 0; z < img.depth(); z++) {
			for (int y = 0; y < img.height(); y++) {
				for (int x = 0; x < img.width(); x++) {
					img.set(x, y, z, val);
				}
			}
		}
	}
	
	/**
	 * Returns largest integer value whose square is less than given value.
	 */
	public static int largestIntWhoseSquareIsLessThan(int square)
	{
		// Initial guess using floating point math
		int result = (int)Math.floor(Math.sqrt(square));

		// Refine the result in the case there are floating point inaccuracies
		while (result * result < square)
			result++;
		result--;
		return result;
	}
	
}
