package fastthickmap;

/**
 * 3-element vector
 * @author arilmiet
 *
 */
public class Vec3 {

	public double x, y, z;
	
	/**
	 * Constructor
	 * @param x
	 * @param y
	 * @param z
	 */
	public Vec3(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vec3(Vec3 right)
	{
		this.x = right.x;
		this.y = right.y;
		this.z = right.z;
	}
	
	public void set(Vec3 right)
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
	public void set(int n, double value) {
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
	public double get(int n) {
		if(n == 0)
			return x;
		else if(n == 1)
			return y;
		else if(n == 2)
			return z;
		else
			throw new ArrayIndexOutOfBoundsException("Invalid Vec3 index.");
	}
	
	/**
	Calculates dot product between this vector and the given vector.
	*/
	public double dot(Vec3 right)
	{
		return (x * right.x) + (y * right.y) + (z * right.z);
	}
	
	/**
	Calculates the squared Euclidean norm of this vector.
	*/
	public double normSquared()
	{
		return this.dot(this);
	}

	/**
	Calculates the Euclidean norm of this vector.
	*/
	public double norm()
	{
		return Math.sqrt(normSquared());
	}
	
	/**
	 * Normalizes this vector.
	 */
	public void normalize()
	{
		double length = norm();
		if (Math.abs(length) > 0.0000001)
		{
			x /= length;
			y /= length;
			z /= length;
		}
	}
	
	public Vec3 normalized()
	{
		Vec3 v = new Vec3(this);
		v.normalize();
		return v;
	}
	
	/**
	Calculates cross product between this vector and the given vector.
	*/
	public Vec3 cross(Vec3 right)
	{
		return new Vec3(y * right.z - z * right.y,
						z * right.x - x * right.z,
						x * right.y - y * right.x);
	}
	
	public Vec3 add(Vec3 right)
	{
		return new Vec3(x + right.x, y + right.y, z + right.z);
	}
	
	public Vec3 sub(Vec3 right)
	{
		return new Vec3(x - right.x, y - right.y, z - right.z);
	}
	
	public Vec3 mul(double c)
	{
		return new Vec3(x * c, y * c, z * c);
	}
	
	public Vec3 div(double c)
	{
		return new Vec3(x / c, y / c, z / c);
	}
	
	
	public void addInPlace(Vec3 right)
	{
		x += right.x;
		y += right.y;
		z += right.z;
	}
	
	public void subInPlace(Vec3 right)
	{
		x -= right.x;
		y -= right.y;
		z -= right.z;
	}
	
	@Override
	public boolean equals(Object o) {
		
        if (o == this) 
            return true;
  
        if (o == null || getClass() != o.getClass())
        	return false;
            
        Vec3c v = (Vec3c)o; 
            
        final double EPS = 1e-15;
        return Math.abs(x - v.x) < EPS &&
        		Math.abs(y - v.y) < EPS &&
        		Math.abs(z - v.z) < EPS;
	}
}
