package fastthickmap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * Plugin that implements distance ridge calculation using Danielsson's
 * algorithm.
 * 
 * @author miettinen_a
 *
 */
public class Squared_Distance_Map_To_Squared_Distance_Ridge_ implements PlugInFilter {

	private static ArrayList<Integer> squareTable = new ArrayList<Integer>(); 

	/**
	 * Init table of largest integers whose square is less than table index.
	 */
	private static void initSquareTable(int maxSquare) {
		if (squareTable.size() < maxSquare) {
			squareTable.ensureCapacity(maxSquare);
			while (squareTable.size() <= maxSquare)
				squareTable.add(MathUtils.largestIntWhoseSquareIsLessThan(squareTable.size()));
		}
	}

	/**
	 * Get largest integer whose square is less than given value. To use this
	 * function, the cache must be first initialized with initSquareTable.
	 */
	private static int largestIntWhoseSquareIsLessThanCached(int square) {
		return squareTable.get(square);
	}
	
	/**
	Helper for getMaxSphereRadius.
	Tests if a sphere of squared radius Rdot2 centered at (cx, cy, cz) fits inside a sphere of radius R2 centered at origin.
	*/
	private static boolean testFit(int cx, int cy, int cz, int R2, int size, int Rdot2)
	{
		for (int z = 0; z < size; z++)
		{
			int dz = z - cz;
			for (int y = 0; y < size; y++)
			{
				// Squared x-coordinate of the surface of a sphere centered at (0, cy, cz) and having squared radiut Rdot2.
				int dy = y - cy;
				int test = Rdot2 - dy * dy - dz * dz;

				if (test >= 0)
				{
					test = largestIntWhoseSquareIsLessThanCached(test) + cx;


					// Squared x-coordinate of the surface of a sphere centered at origin and having squared radius R2.
					int Rx = R2 - y * y - z * z;

					if (Rx >= 0)
						Rx = largestIntWhoseSquareIsLessThanCached(Rx);
					else
						Rx = -1;

					if (test > Rx)
					{
						// The sphere does not fit
						return false;
					}
				}
			}
		}

		// Here sphere with radius of Rdot fits inside the central sphere.
		return true;
	}



	private static int search(int R2Index, ArrayList<Integer> radii2, int R2, int cx, int cy, int cz, int size)
	{
		// Binary search for last index for which testFit(... R2, size, radii2[index]) gives true.
		// This handles also -1 elements in the radii2 array.

		int  first = R2Index;
		while (first > 0 && (radii2.get(first) < 0 || largestIntWhoseSquareIsLessThanCached(radii2.get(first)) >= largestIntWhoseSquareIsLessThanCached(R2) - 2))
			first--;

		int last = R2Index;

		while (last - first > 1)
		{
			int mid = (last + first) / 2;

			// Handle -1 elements in the array
			while (mid < last && radii2.get(mid) < 0)
				mid++;

			if (mid >= last)
			{
				// last is the first non -1 element after (and including) mid.
				last = (last + first) / 2;
			}
			else
			{

				int Rdot2 = radii2.get(mid);

				if (testFit(cx, cy, cz, R2, size, Rdot2))
				{
					// Fits. Set start = mid
					first = mid;
				}
				else
				{
					// Does not fit. Set end = mid
					last = mid;
				}
			}
		}

		return first;
	}



	/*
	Calculates squared radius of largest sphere that is centered at (cx, cy, cz) and fits inside sphere of radius sqrt(r2) centered at (0, 0, 0).
	Works for positive (cx, cy, cz) only.
	indices lookup table gives index for squared radius.
	radii lookup table gives radius for index.
	NOTE: This is optimized version that does not use separate mask array, and does only 1/8 of processing of the unoptimized version.
	*/
	private static int getMaxSphereRadius(int cx, int cy, int cz, ArrayList<Integer> radii2, int R2Index)
	{
		int R2 = radii2.get(R2Index);
		if (R2 < 0)
			throw new IllegalArgumentException("Determining Danielsson tables for squared radius that is not a sum of three squares.");

		
		int Rint = (int)Math.ceil(Math.sqrt(R2));
		int size = Rint + 1;

		int trial = search(R2Index, radii2, R2, cx, cy, cz, size);

		return trial;
	}
	
	
	/**
	 * Calculates squared radius of largest sphere that is centered at (cx, cy, cz)
	 * and fits inside sphere of radius sqrt(r2) centered at (0, 0, 0). Works for
	 * positive (cx, cy, cz) only. indices lookup table gives index for squared
	 * radius. radii lookup table gives radius for index. NOTE: This is optimized
	 * version that does not use separate mask array, and does only 1/8 of
	 * processing of the unoptimized version.
	 */
//	private static int getMaxSphereRadius(int cx, int cy, int cz, ArrayList<Integer> radii2, int R2Index) {
//		int R2 = radii2.get(R2Index);
//		if (R2 < 0)
//			throw new IllegalArgumentException(
//					"Determining Danielsson tables for squared radius that is not a sum of three squares.");
//
//		// Create mask of sphere centered at origin, having radius R
//		int Rint = (int) Math.ceil(Math.sqrt(R2));
//
//		int size = Rint + 1;
//
//		// Find the largest r that still fits inside the central sphere
//		// by testing mask of sphere centered at (cx, cy, cz).
//		int startInd = R2Index;
//		for (int RdotInd = startInd - 1; RdotInd >= 0; RdotInd--) {
//			int Rdot2 = radii2.get(RdotInd);
//
//			if (Rdot2 >= 0) {
//				boolean fits = true;
//
//				outerLoop: for (int z = 0; z < size; z++) {
//					int dz = z - cz;
//					for (int y = 0; y < size; y++) {
//						// Squared x-coordinate of the surface of a sphere centered at (0, cy, cz) and
//						// having squared radiut Rdot2.
//						int dy = y - cy;
//						int test = Rdot2 - dy * dy - dz * dz;
//
//						if (test >= 0) {
//							test = largestIntWhoseSquareIsLessThanCached(test) + cx;
//							
//							// Squared x coordinate of the surface of a sphere centered at origin and having
//							// squared radius R2.
//							int Rx = R2 - y * y - z * z;
//
//							if (Rx >= 0)
//								Rx = largestIntWhoseSquareIsLessThanCached(Rx);
//							else
//								Rx = -1;
//
//							if (test > Rx) {
//								// The sphere does not fit, we can skip processing of both inner loops.
//								fits = false;
//								break outerLoop;
//							}
//						}
//					}
//				}
//
//				// Here sphere with radius of Rdot fits inside the central sphere.
//				if (fits)
//					return Rdot2;
//			}
//
//		}
//
//		return 0;
//	}

	/**
	 * Expands Danielsson lookup tables so that they cover at least squared radius
	 * R2max.
	 */
	private static void expandDanielssonTables(ArrayList<Integer> table1, ArrayList<Integer> table2,
			ArrayList<Integer> table3, int R2max) throws InterruptedException {
		// Test if the tables are already complete.
		if (table1.size() >= R2max)
			return;

		// Determine which elements of the tables need to be determined
		int Rmax = MathUtils.largestIntWhoseSquareIsLessThan(R2max) + 1;
		final int invalidValue = -1;
		final int toBeDeterminedValue = Integer.MAX_VALUE;
		for (int z = 0; z < Rmax; z++) {
			for (int y = 0; y < Rmax; y++) {
				for (int x = 0; x < Rmax; x++) {
					int R2 = x * x + y * y + z * z;
					if (R2 <= R2max) {
						// radius is less than maximum, add it to the tables.
						while (table1.size() <= R2) {
							table1.add(invalidValue);
							table2.add(invalidValue);
							table3.add(invalidValue);
						}

						if (table1.get(R2) == invalidValue) {
							table1.set(R2, toBeDeterminedValue);
							table2.set(R2, toBeDeterminedValue);
							table3.set(R2, toBeDeterminedValue);
						}
					}
				}
			}

			IJ.showProgress(z, Rmax);
		}

		// Create a list of possible radius^2 values.
		int maxr2 = 0;
		ArrayList<Integer> radii2 = new ArrayList<Integer>();
		for (int r2 = 0; r2 < table1.size(); r2++) {
			if (table1.get(r2) != invalidValue) // Value is possible if it is not marked as invalid in the tables.
			{
				radii2.add(r2);
				if(r2 > maxr2)
					maxr2 = r2;
			}
			else {
				radii2.add(-1);
			}
		}
		
		
		initSquareTable(maxr2);

		AtomicInteger progress = new AtomicInteger(0);
		Loop.withIndex(0, table1.size(), new Loop.Each() {

			@Override
			public void run(long r2l) {
				int r2 = (int) r2l;
				if (table1.get(r2) == toBeDeterminedValue) {
					table1.set(r2, getMaxSphereRadius(1, 0, 0, radii2, r2));
					table2.set(r2, getMaxSphereRadius(1, 1, 0, radii2, r2));
					table3.set(r2, getMaxSphereRadius(1, 1, 1, radii2, r2));
				}

				IJ.showProgress(progress.incrementAndGet(), table1.size());
			}
		});

		// Single-threaded version
		//for (int r2 = 0; r2 < table1.size(); r2++) {
		//	if (table1.get(r2) == toBeDeterminedValue) {
		//		table1.set(r2, getMaxSphereRadius(1, 0, 0, radii2, r2));
		//		table2.set(r2, getMaxSphereRadius(1, 1, 0, radii2, r2));
		//		table3.set(r2, getMaxSphereRadius(1, 1, 1, radii2, r2));
		//  }
		//}
	}

	/**
	 * Reads lookup table from disk.
	 */
	private static void readTable(List<Integer> table, String filename) {
		table.clear();

		try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)))) {
			while (true)
				table.add(in.readInt());
		} catch (EOFException ignore) {
			// Finished reading, that's fine.
		} catch (IOException ex) {
			// We will re-calculate the table.
			table.clear();
		}
	}

	/**
	 * Writes lookup table to disk.
	 */
	private static void writeTable(List<Integer> table, String filename) {
		try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)))) {
			for (int n = 0; n < table.size(); n++)
				out.writeInt(table.get(n));
		} catch (IOException ignore) {
			// We ignore the exception. As a result, the data is not cached.
		}
	}

	/**
	 * Creates Danielsson lookup tables that cover at least squared radius R2max.
	 * Uses old tables if they are found.
	 */
	private static void getDanielssonTables(ArrayList<Integer> table1, ArrayList<Integer> table2,
			ArrayList<Integer> table3, int R2max) throws InterruptedException {
		readTable(table1, "danielsson_table_1.jdat");
		readTable(table2, "danielsson_table_2.jdat");
		readTable(table3, "danielsson_table_3.jdat");

		if (table1.size() < R2max) {
			expandDanielssonTables(table1, table2, table3, R2max);
			writeTable(table1, "danielsson_table_1.jdat");
			writeTable(table2, "danielsson_table_2.jdat");
			writeTable(table3, "danielsson_table_3.jdat");
		}
	}

	/**
	 * Extracts pixels in neighbourhood from a big image. This function is tuned for
	 * conversion from float to int pixels. Sets those neighbourhood pixels to zero
	 * that are outside the image.
	 * 
	 * @param nb Neighbourhood pixels are assigned to this image. The size of this
	 *           image is not checked but it must be 2 * nbRadius + 1.
	 */
	private static void getNeighbourhoodZero(Image img, int cx, int cy, int cz, Vec3i nbRadius, int[][][] nb) {
		//Vec3i start = nbCenter.sub(nbRadius);
		int startx = cx - nbRadius.x;
		int starty = cy - nbRadius.y;
		int startz = cz - nbRadius.z;
		
		//Vec3i start0 = start;
		int start0x = startx;
		int start0y = starty;
		int start0z = startz;
		
		//Vec3i end = nbCenter.add(nbRadius);
		int endx = cx + nbRadius.x;
		int endy = cy + nbRadius.y;
		int endz = cz + nbRadius.z;

		// Make sure start point is in the image.
		// If start is > image dimensions, checks made for end will prevent the loops
		// from running,
		// so there's no need to check that start < image dimensions.
		if (startx < 0)
			startx = 0;
		if (starty < 0)
			starty = 0;
		if (startz < 0)
			startz = 0;

		int w = img.width();
		int h = img.height();
		int d = img.depth();

		// Make sure end point is in the image.
		if (endx > w - 1)
			endx = w - 1;
		if (endy > h - 1)
			endy = h - 1;
		if (endz > d - 1)
			endz = d - 1;

		// Zero neighbourhood if there's not enough data to fill it.
		if (startx == 0 || starty == 0 || startz == 0 || endx == w - 1 || endy == h - 1 || endz == d - 1) {
			for (int z = 0; z < 2 * nbRadius.z + 1; z++) {
				for (int y = 0; y < 2 * nbRadius.y + 1; y++) {
					for (int x = 0; x < 2 * nbRadius.x + 1; x++) {
						nb[x][y][z] = 0;
					}
				}
			}
		}

		// Copy data to neighbourhood image.
		for (int z = startz; z <= endz; z++) {
			for (int y = starty; y <= endy; y++) {
				for (int x = startx; x <= endx; x++) {
					float val = img.get(x, y, z);
					nb[x - start0x][y - start0y][z - start0z] = (int) Math.round(val);
				}
			}
		}
	}

	/**
	 * Calculates centers of locally maximal disks using Danielsson algorithm. The
	 * output image can be interpreted as medial axis or distance ridge. Drawing a
	 * sphere on each nonzero pixel, with radius and color equal to pixel value,
	 * results in thickness map of the structure (assuming that larger colors
	 * replace smaller ones). See e.g. Yaorong Ge and J. Michael Fitzpatrick - On
	 * the Generation of Skeletons from Discrete Euclidean Distance Maps
	 * 
	 * @param dmap2 Squared Euclidean distance map of the input geometry.
	 * @param out   Distance ridge. Squared distance values of pixels that
	 *              correspond to centers of locally maximal disks are set to this
	 *              image. Other values are not changed. Usually this image should
	 *              be empty before calling this function.
	 */
	public static void danielsson(Image dmap2, Image out) throws InterruptedException {

		int maxr2 = (int) Math.round(ImageUtils.max(dmap2));

		ArrayList<Integer> table1 = new ArrayList<Integer>();
		ArrayList<Integer> table2 = new ArrayList<Integer>();
		ArrayList<Integer> table3 = new ArrayList<Integer>();

		getDanielssonTables(table1, table2, table3, maxr2);

		ThreadLocal<int[][][]> nbStore = new ThreadLocal<int[][][]>() {
			@Override
			protected int[][][] initialValue() {
				return new int[3][3][3];
			}
		};

		AtomicInteger progress = new AtomicInteger(0);
		Loop.withIndex(0, dmap2.depth(), new Loop.Each() {

			@Override
			public void run(long zl) {
				
				int z = (int)zl;
				Vec3i ones = new Vec3i(1, 1, 1);
				
				for (int y = 0; y < dmap2.height(); y++) {
					for (int x = 0; x < dmap2.width(); x++) {

						//Vec3c pos = new Vec3c(x, y, z);

						float cf = dmap2.get(x, y, z);
						int c = (int) Math.round(cf);
						if (c != 0) {
							int[][][] nb = nbStore.get();

							getNeighbourhoodZero(dmap2, x, y, z, ones, nb);

							// Check all neighbours
							if (!(
								// 6-neighbours, one coordinate changes by one pixel.
								table1.get(nb[0][1][1]) >= c || table1.get(nb[2][1][1]) >= c || table1.get(nb[1][0][1]) >= c
										|| table1.get(nb[1][2][1]) >= c || table1.get(nb[1][1][0]) >= c
										|| table1.get(nb[1][1][2]) >= c ||
								// 18-neighbours but not 6-neighbours, two coordinates change by one pixel.
								table2.get(nb[0][0][1]) >= c || table2.get(nb[0][2][1]) >= c || table2.get(nb[2][0][1]) >= c
										|| table2.get(nb[2][2][1]) >= c || table2.get(nb[1][0][0]) >= c
										|| table2.get(nb[1][0][2]) >= c || table2.get(nb[1][2][0]) >= c
										|| table2.get(nb[1][2][2]) >= c || table2.get(nb[0][1][0]) >= c
										|| table2.get(nb[0][1][2]) >= c || table2.get(nb[2][1][0]) >= c
										|| table2.get(nb[2][1][2]) >= c ||
								// Corners, three coordinates change by one pixel.
								table3.get(nb[0][0][0]) >= c || table3.get(nb[0][2][0]) >= c || table3.get(nb[2][0][0]) >= c
										|| table3.get(nb[2][2][0]) >= c || table3.get(nb[0][0][2]) >= c
										|| table3.get(nb[0][2][2]) >= c || table3.get(nb[2][0][2]) >= c
										|| table3.get(nb[2][2][2]) >= c)) {
								// This is center of locally maximal sphere
								out.set(x, y, z, c);
							} else {
								// Not a center of locally maximal sphere
								out.set(x, y, z, 0);
							}
						}

					}
				}

				IJ.showProgress(progress.incrementAndGet(), dmap2.depth());
			}
		});

	}

	private ImagePlus iplus;

	@Override
	public void run(ImageProcessor ip) {

		Image img = new Image(iplus.getStack());

		ImageStack out = iplus.createEmptyStack();
		for (int z = 0; z < img.depth(); z++)
			out.addSlice(new FloatProcessor(img.width(), img.height(), new float[img.width() * img.height()]));
		Image outImg = new Image(out);

		try {
			danielsson(img, outImg);
			iplus.setStack(out);
		} catch (InterruptedException e) {

		}

	}

	@Override
	public int setup(String arg0, ImagePlus img) {

		iplus = img;
		return DOES_32;

	}

}
