package fastthickmap;

public class Vec3i {
	public int x, y, z;
	
	/**
	 * Constructor
	 * @param x
	 * @param y
	 * @param z
	 */
	public Vec3i(int x, int y, int z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vec3i(Vec3i right)
	{
		this.x = right.x;
		this.y = right.y;
		this.z = right.z;
	}
	
	public void set(Vec3i right)
	{
		this.x = right.x;
		this.y = right.y;
		this.z = right.z;
	}
	
	/**
	 * Sets n:th component of this vector to given value.
	 * @param n
	 * @param value
	 */
	public void set(int n, int value) {
		if(n == 0)
			x = value;
		else if(n == 1)
			y = value;
		else if(n == 2)
			z = value;
		else
			throw new ArrayIndexOutOfBoundsException("Invalid Vec3 index.");
	}
	
	/**
	 * Gets n:th component of this vector.
	 * @param n
	 * @return
	 */
	public int get(int n) {
		if(n == 0)
			return x;
		else if(n == 1)
			return y;
		else if(n == 2)
			return z;
		else
			throw new ArrayIndexOutOfBoundsException("Invalid Vec3 index.");
	}
	
	public Vec3i add(Vec3i right)
	{
		return new Vec3i(x + right.x, y + right.y, z + right.z);
	}
	
	public Vec3i sub(Vec3i right)
	{
		return new Vec3i(x - right.x, y - right.y, z - right.z);
	}
	
	public Vec3i mul(int c)
	{
		return new Vec3i(x * c, y * c, z * c);
	}
	
	public Vec3i div(int c)
	{
		return new Vec3i(x / c, y / c, z / c);
	}
	
	/**
	 * Component-wise quotient of two vectors.
	 * @param v
	 * @return
	 */
	public Vec3i divc(Vec3i v)
	{
		return new Vec3i(x / v.x, y / v.y, z / v.z);
	}
	
	
	public void addInPlace(Vec3i right)
	{
		x += right.x;
		y += right.y;
		z += right.z;
	}
	
	public void subInPlace(Vec3i right)
	{
		x -= right.x;
		y -= right.y;
		z -= right.z;
	}
	
	/**
	 * Gets the maximum element in this vector.
	 * @return
	 */
	public int max() 
	{
		int M = x;
		if(y > M)
			M = y;
		if(z > M)
			M = z;
		return M;
	}
	
	/**
	 * Increases element of given dimension by one.
	 * @param dim
	 */
	public void inc(int dim) {
		set(dim, get(dim) + 1);
	}
	
	/**
	 * Increases element of given dimension by given value.
	 * @param dim
	 */
	public void inc(int dim, int step) {
		set(dim, get(dim) + step);
	}
	
	@Override
	public boolean equals(Object o) {
		
        if (o == this) 
            return true;
  
        if (o == null || getClass() != o.getClass())
        	return false;
            
        Vec3i v = (Vec3i)o; 
            
        return x == v.x && y == v.y && z == v.z; 
	}
}
