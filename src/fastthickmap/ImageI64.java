package fastthickmap;

/**
 * Image that stores 64-bit integer values.
 * @author miettinen_a
 *
 */
public class ImageI64 extends ImageBase {

	public ImageI64(Vec3c dimensions) {
		super(dimensions);
		
		slices = new long[depth()][width() * height()];
	}
	
	/**
	 * Stores image data.
	 */
	private long[][] slices;

	/**
	 * Gets value of pixel.
	 * @param slices
	 * @param dimensions
	 * @param pos
	 * @return
	 */
	public long get(Vec3c pos)
	{
		return slices[(int)pos.z][(int)(pos.y * width() + pos.x)];
	}

	/**
	 * Sets value of pixel.
	 * @param slices
	 * @param dimensions
	 * @param pos
	 * @param value
	 */
	public void set(Vec3c pos, long value)
	{
		slices[(int)pos.z][(int)(pos.y * width() + pos.x)] = value;
	}
}
