package fastthickmap;

import java.util.concurrent.atomic.AtomicInteger;

import ij.*;
import ij.plugin.filter.*;
import ij.process.ImageProcessor;

/**
 * ImageJ plugin that calculates squared Euclidean distance map.
 * The distance transform code is adapted from pi2, and that is in turn adapted from ITK from file
 * ITK/Modules/Filtering/DistanceMap/include/itkSignedMaurerDistanceMapImageFilter.hxx
 * The original file has been licensed under the Apache License, Version 2.0.
 * 
 * The algorithm used here is published in
 * Calvin R. Maurer, Vijay Raghavan, A Linear Time Algorithm for Computing Exact Euclidean Distance Transforms of Binary Images in Arbitrary Dimensions, IEEE TRANSACTIONS ON PATTERN ANALYSIS AND MACHINE INTELLIGENCE, VOL. 25, NO. 2, FEBRUARY 2003
 * 
 * @author miettinen_a
 *
 */
public class Squared_Distance_Map_ implements PlugInFilter {
	
	/**
	 * Prepares image for distance map calculation.
	 * Replaces pixels colored with backgroundValue by 0 and all other pixels by maximum possible value.
	 * @param slices
	 * @param backgroundValue
	 */
	public static void prepare(Image slices, float backgroundValue) {
		
		
		for (int z = 0; z < slices.depth(); z++)
		{
			float[] slice = slices.getSlice(z);
			
			for(int n = 0; n < slice.length; n++)
			{
				if (slice[n] == backgroundValue)
					slice[n] = 0;
				else
					slice[n] = Float.MAX_VALUE;
			}
		}
		
	}
	
	
	
	private static boolean remove(float d1, float d2, float df, float x1, float x2, float xf)
	{
		float a = x2 - x1;
		float b = xf - x2;
		float c = xf - x1;

		float value = (c * d2 - b * d1 - a * df - a * b * c);

		return value > 0;
	}

	private static float calcD(float gl, float hl, float iw)
	{
		float d1 = gl + (hl - iw) * (hl - iw);
		return d1;
	}
	
	/**
	 * Helper for distance map calculation.
	 * Processes one row in dimension d whose start point is idx.
	 * Optimized version that does not store nearest object point for each dmap point.
	 * g and h are temporary images whose size must be output.dimension(d) x 1 x 1
	 * @param d
	 * @param idx
	 * @param output
	 * @param g
	 * @param h
	 */
	private static void voronoi(int d, Vec3c idx, Image output, float[] g, float[] h)
	{
		long nd = output.getDimensions().get(d);

		int l = -1;

		for (long i = 0; i < nd; i++)
		{
			idx.set(d, i);

			float di = output.get(idx);

			float iw = (float)i;

			if (di < Float.MAX_VALUE)
			{
				if (l < 1)
				{
					l++;
					g[l] = di;
					h[l] = iw;
				}
				else
				{
					while ((l >= 1) && remove(g[l - 1], g[l], di, h[l - 1], h[l], iw))
					{
						l--;
					}
					l++;
					g[l] = di;
					h[l] = iw;
				}
			}
		}

		if (l == -1)
		{
			return;
		}

		int ns = l;

		l = 0;

		for (long i = 0; i < nd; i++)
		{
			float iw = (float)i;

			float d1 = calcD(g[l], h[l], iw);

			while (l < ns)
			{
				float d2 = calcD(g[l + 1], h[l + 1], iw);

				// then compare d1 and d2
				if (d1 <= d2)
				{
					break;
				}
				l++;
				d1 = d2;
			}
			idx.set(d, i);

			output.set(idx, d1);
		}

	}
	
	
	/**
	 * Processing of one dimension of the distance map.
	 * @param slices
	 * @param dimensions
	 * @param dimension
	 */
	private static void processDimension(Image slices, int dimension) throws InterruptedException {
		
		Vec3c reducedDimensions = slices.getDimensions();
		reducedDimensions.set(dimension, 1);
		int rowCount = (int)(reducedDimensions.x * reducedDimensions.y * reducedDimensions.z);
		
		int nd = (int)slices.getDimensions().get(dimension);
		
		
		// Parallel version
		
		ThreadLocal<float[]> g = new ThreadLocal<float[]>() {
			@Override protected float[] initialValue() {
				return new float[nd];
			}
	    };
				
	    ThreadLocal<float[]> h = new ThreadLocal<float[]>() {
			@Override protected float[] initialValue() {
				return new float[nd];
			}
	    };
		
	    AtomicInteger progress = new AtomicInteger(0);
	    
		Loop.withIndex(0, rowCount, new Loop.Each() {
			
			@Override
			public void run(long n) {
				
				Vec3c start = Image.indexToCoords(n, reducedDimensions);

			    // Process the current row
				voronoi(dimension, start, slices, g.get(), h.get());
				
				IJ.showProgress(progress.incrementAndGet(), rowCount);
			}
		});
		
		// Single-threaded version
		/*
		// Temporary buffer
		float[] g = new float[nd];
		float[] h = new float[nd];
		
		for (int n = 0; n < rowCount; n++)
		{
		    Vec3c start = Image.indexToCoords(n, reducedDimensions);

		    // Process the current row
			voronoi(dimension, start, slices, g, h);
		    
			IJ.showProgress(n, (int)rowCount);
		}
		*/
	}
	
	/**
	 * Calculates squared distance map of img.
	 * @param img
	 * @param backgroundValue Pixels having this value are assumed to belong to the background.
	 * @throws InterruptedException
	 */
	public static void squaredDistanceMap(Image img, float backgroundValue) throws InterruptedException {
		prepare(img, backgroundValue);
		
		for(int n = 0; n < img.getDimensionality(); n++)
			processDimension(img, n);
	}
	
	private ImageStack stack;
	
	@Override
	public void run(ImageProcessor ip) {
		
		Image img = new Image(stack);
		
		try {
			squaredDistanceMap(img, 0);
		}
		catch (InterruptedException e) {
			
		}
		
	}

	@Override
	public int setup(String arg0, ImagePlus img) {
		
		stack = img.getStack();
		return DOES_32;
		
	}

}
