package fastthickmap;

import ij.ImageStack;

/**
 * Provides simple access to ImageJ float stacks.
 * Does not copy data but refers to the ImageJ data directly.
 * @author miettinen_a
 *
 */
public class Image extends ImageBase {
	
	
	/**
	 * Converts linear index to coordinates.
	 * @param ind
	 * @param size
	 * @return
	 */
	public static Vec3i indexToCoords(long ind, Vec3i size)
	{
		long xy = (long)size.x * (long)size.y;
		int z = (int)(ind / xy);
		int y = (int)((ind - z * xy) / size.x);
		int x = (int)(ind - z * xy - y * size.x);

		return new Vec3i(x, y, z);
	}
	
	/**
	 * Stores reference to the data (really stored in ImageJ system).
	 */
	private float[][] slices;
	
	
	/**
	 * Creates a view of the image stack as an array of float arrays.
	 * The array is accessed like array[z][y*width + x].
	 * @param stack
	 * @return
	 */
	private static float[][] toFloatArray(ImageStack stack) {
		
		float[][] slices = new float[stack.getSize()][];
		for (int z = 0; z < stack.getSize(); z++) {
			slices[z] = (float[])stack.getImageArray()[z];
		}
		
		return slices;
	}
	
	
	/**
	 * Constructor, creates Image from IJ stack.
	 * @param stack
	 */
	public Image(ImageStack stack) {
		
		super(new Vec3i(stack.getWidth(), stack.getHeight(), stack.getSize()));
		
		slices = toFloatArray(stack);
	}
	
	/**
	 * Gets one slice of the stack as array.
	 * @param z
	 * @return
	 */
	public float[] getSlice(int z) {
		return slices[z];
	}
	
	/**
	 * Gets value of pixel.
	 * @param slices
	 * @param dimensions
	 * @param pos
	 * @return
	 */
	public float get(Vec3i pos)
	{
		return slices[pos.z][pos.y * width() + pos.x];
	}
	
	public float get(int x, int y, int z) {
		return slices[z][y * width() + x];
	}

	/**
	 * Sets value of pixel.
	 * @param slices
	 * @param dimensions
	 * @param pos
	 * @param value
	 */
	public void set(Vec3i pos, float value)
	{
		slices[pos.z][pos.y * width() + pos.x] = value;
	}
	
	public void set(int x, int y, int z, float value) {
		slices[z][y * width() + x] = value;
	}

}
