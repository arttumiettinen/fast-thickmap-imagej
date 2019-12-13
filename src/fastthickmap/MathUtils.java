package fastthickmap;

/**
 * Simple mathematical utlities.
 * 
 * @author miettinen_a
 *
 */
public class MathUtils {

	/**
	 * Clamps the given value to range [lower, upper].
	 */
	public static int clamp(int value, int lower, int upper) {
		if (value < lower)
			return lower;
		else if (value > upper)
			return upper;
		return value;
	}

	/**
	 * Clamps each component i of the vector to range [lower.i, upper.i].
	 */
	public static void clamp(Vec3i value, Vec3i lower, Vec3i upper) {
		value.x = clamp(value.x, lower.x, upper.x);
		value.y = clamp(value.y, lower.y, upper.y);
		value.z = clamp(value.z, lower.z, upper.z);
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
