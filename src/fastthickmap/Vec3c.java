//package fastthickmap;
//
///**
// * 3-element vector
// * @author arilmiet
// *
// */
//public class Vec3c {
//
//	public long x, y, z;
//	
//	/**
//	 * Constructor
//	 * @param x
//	 * @param y
//	 * @param z
//	 */
//	public Vec3c(long x, long y, long z)
//	{
//		this.x = x;
//		this.y = y;
//		this.z = z;
//	}
//	
//	public Vec3c(Vec3c right)
//	{
//		this.x = right.x;
//		this.y = right.y;
//		this.z = right.z;
//	}
//	
//	public void set(Vec3c right)
//	{
//		this.x = right.x;
//		this.y = right.y;
//		this.z = right.z;
//	}
//	
//	/**
//	 * Sets n:th component of this vector to given value.
//	 * @param n
//	 * @param value
//	 */
//	public void set(int n, long value) {
//		if(n == 0)
//			x = value;
//		else if(n == 1)
//			y = value;
//		else if(n == 2)
//			z = value;
//		else
//			throw new ArrayIndexOutOfBoundsException("Invalid Vec3 index.");
//	}
//	
//	/**
//	 * Gets n:th component of this vector.
//	 * @param n
//	 * @return
//	 */
//	public long get(int n) {
//		if(n == 0)
//			return x;
//		else if(n == 1)
//			return y;
//		else if(n == 2)
//			return z;
//		else
//			throw new ArrayIndexOutOfBoundsException("Invalid Vec3 index.");
//	}
//	
//	public Vec3c add(Vec3c right)
//	{
//		return new Vec3c(x + right.x, y + right.y, z + right.z);
//	}
//	
//	public Vec3c sub(Vec3c right)
//	{
//		return new Vec3c(x - right.x, y - right.y, z - right.z);
//	}
//	
//	public Vec3c mul(long c)
//	{
//		return new Vec3c(x * c, y * c, z * c);
//	}
//	
//	public Vec3c div(long c)
//	{
//		return new Vec3c(x / c, y / c, z / c);
//	}
//	
//	/**
//	 * Component-wise quotient of two vectors.
//	 * @param v
//	 * @return
//	 */
//	public Vec3c divc(Vec3c v)
//	{
//		return new Vec3c(x / v.x, y / v.y, z / v.z);
//	}
//	
//	
//	public void addInPlace(Vec3c right)
//	{
//		x += right.x;
//		y += right.y;
//		z += right.z;
//	}
//	
//	public void subInPlace(Vec3c right)
//	{
//		x -= right.x;
//		y -= right.y;
//		z -= right.z;
//	}
//	
//	/**
//	 * Gets the maximum element in this vector.
//	 * @return
//	 */
//	public long max() 
//	{
//		long M = x;
//		if(y > M)
//			M = y;
//		if(z > M)
//			M = z;
//		return M;
//	}
//	
//	/**
//	 * Increases element of given dimension by one.
//	 * @param dim
//	 */
//	public void inc(int dim) {
//		set(dim, get(dim) + 1);
//	}
//	
//	/**
//	 * Increases element of given dimension by given value.
//	 * @param dim
//	 */
//	public void inc(int dim, long step) {
//		set(dim, get(dim) + step);
//	}
//	
//	@Override
//	public boolean equals(Object o) {
//		
//        if (o == this) 
//            return true;
//  
//        if (o == null || getClass() != o.getClass())
//        	return false;
//            
//        Vec3c v = (Vec3c)o; 
//            
//        return x == v.x && y == v.y && z == v.z; 
//	}
//}
