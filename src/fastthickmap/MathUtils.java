package fastthickmap;

public class MathUtils {

	/**
	 * Clamps the given value to range [lower, upper].
	 */
	public static long clamp(long value, long lower, long upper) {
		if (value < lower)
			return lower;
		else if (value > upper)
			return upper;
		return value;
	}

	/**
	 * Clamps each component i of the vector to range [lower.i, upper.i].
	 */
	public static void clamp(Vec3c value, Vec3c lower, Vec3c upper) {
		value.x = clamp(value.x, lower.x, upper.x);
		value.y = clamp(value.y, lower.y, upper.y);
		value.z = clamp(value.z, lower.z, upper.z);
	}

}
