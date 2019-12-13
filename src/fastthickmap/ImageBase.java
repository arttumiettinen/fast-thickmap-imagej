package fastthickmap;

/**
 * Base class for images, provides basic functionality like dimension management.
 * @author miettinen_a
 *
 */
public abstract class ImageBase {

	/**
	 * Stores dimensions of the image.
	 */
	private Vec3i dimensions;
	
	/**
	 * Constructor
	 * @param dimensions
	 */
	public ImageBase(Vec3i dimensions) {
		this.dimensions = dimensions;
	}
	
	/**
	 * Determines the dimensionality of the image.
	 * @param stack
	 * @return
	 */
	public int getDimensionality() {
		
		int dimensionality;
		
		if(dimensions.z > 1)
			dimensionality = 3;
		else if(dimensions.y > 1)
			dimensionality = 2;
		else if(dimensions.x > 1)
			dimensionality = 1;
		else
			dimensionality = 0;
		
		return dimensionality;
	}
	
	/**
	 * Gets dimensions of this image.
	 * @return
	 */
	public Vec3i getDimensions() {
		return new Vec3i(dimensions);
	}
	
	/**
	 * Gets depth of this image.
	 * @return
	 */
	public int depth() {
		return dimensions.z;
	}
	
	/**
	 * Gets height of this image.
	 * @return
	 */
	public int height() {
		return dimensions.y;
	}
	
	/**
	 * Gets width of this image.
	 * @return
	 */
	public int width() {
		return dimensions.x;
	}
	
	/**
	 * Gets dimension of image from dimension index.
	 * @param dim
	 * @return
	 */
	public int getDimension(int dim) {
		return dimensions.get(dim);
	}
	
	/**
	 * Gets count of pixels in this image.
	 * @return
	 */
	public long pixelCount() {
		return (long)width() * (long)height() * (long)depth();
	}
	
	/**
	 * Test whether the given coordinates are inside this image.
	 */
	public boolean isInImage(Vec3i pos) {
		return pos.x >= 0 && pos.y >= 0 && pos.z >= 0 && pos.x < width() && pos.y < height() && pos.z < depth();
	}
}
