package fastthickmap;

/**
 * Image that stores 64-bit integer values.
 * @author miettinen_a
 *
 */
public class ImageI64 extends ImageBase {

	public ImageI64(Vec3i dimensions) {
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
	public long get(Vec3i pos)
	{
		return slices[pos.z][pos.y * width() + pos.x];
	}
	
	public long get(int x, int y, int z) {
		return slices[z][y * width() + x];
	}

	/**
	 * Sets value of pixel.
	 * @param slices
	 * @param dimensions
	 * @param pos
	 * @param value
	 */
	public void set(Vec3i pos, long value)
	{
		slices[pos.z][pos.y * width() + pos.x] = value;
	}
	
	public void set(int x, int y, int z, long value) {
		slices[z][y * width() + x] = value;
	}
}
