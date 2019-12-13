package fastthickmap;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * ImageJ plugin that converts squared distance ridge or squared distance map to squared radius map.
 * 
 * @author miettinen_a
 *
 */
public class Squared_Distance_Ridge_To_Squared_Radius_Map_ implements PlugInFilter {

	/**
	 * Tests if discretized circle whose squared radius is r1Square fit completely
	 * into discretized circle whose squared radius is r2Square.
	 */
	private static boolean doesDiscretizedCircle1FitInto2(int r1Square, int r2Square) {
		if (r1Square == r2Square)
			return true;

		int r1 = MathUtils.largestIntWhoseSquareIsLessThan(r1Square);
		int r2 = MathUtils.largestIntWhoseSquareIsLessThan(r2Square);

		// If the r1 > r2, the projection of the circle 1 to x-axis completely overlaps
		// projection of circle 2.
		// Circle 1 cannot fit into circle 2.
		if (r1 > r2)
			return false;

		// If r1 < r2, the circle 1 surely fits into circle 2.
		if (r1 < r2)
			return true;

		// Projection of both circles to x-axis is the same. Now test if the circles are
		// the same in each
		// y-directional pixel column.
		for (int x = 0; x <= r1; x++) {
			int y1Square = r1Square - x * x;
			int y2Square = r2Square - x * x;
			if (MathUtils.largestIntWhoseSquareIsLessThan(y1Square) > MathUtils
					.largestIntWhoseSquareIsLessThan(y2Square))
				return false;
		}

		return true;
	}

	/**
	 * Lookup table for doesDiscretizedCircle1FitInto2Cached. Element
	 * circleLookup[r2] stores the maximal squared radius of a circle that fits into
	 * a circle of squared radius r2.
	 */
	private static ArrayList<Integer> circleLookup;

	/**
	 * Builds lookup table that doesDiscretizedCircle1FitInto2Cached uses.
	 * 
	 * @param maxrSquare Maximum squared radius found in the image.
	 */
	private static void buildCircleLookup(int maxrSquare) {

		if (circleLookup == null)
			circleLookup = new ArrayList<Integer>();

		int firstr2 = circleLookup.size();
		while (circleLookup.size() < maxrSquare + 1)
			circleLookup.add(0);

		for (int r2 = firstr2; r2 < maxrSquare + 1; r2++) {
			// Find the maximal squared radius of a circle that fits into a circle of
			// squared radius r2.
			circleLookup.set(r2, r2);
			int rdot2 = r2;
			while (true) {
				rdot2++;
				if (!doesDiscretizedCircle1FitInto2(rdot2, r2))
					break;
				circleLookup.set(r2, rdot2);
			}
		}
	}

	/**
	 * Tests if discretized circle whose squared radius is r1Square fit completely
	 * into discretized circle whose squared radius is r2Square. Assumes
	 * buildCircleLookup has been called with argument whose value is greater than
	 * or equal to r2Square.
	 */
	private static boolean doesDiscretizedCircle1FitInto2Cached(int r1Square, int r2Square) {
		return r1Square <= circleLookup.get(r2Square);
	}

	/**
	 * Data entry that must be saved for each pixel for each sphere that must be
	 * considered in that pixel. This data structure should be as small as possible.
	 */
	// In C++ version we use struct similar to this, but here it causes a lot of
	// overhead.
	// Thus we declare that RiStorageItem = int, where the two shorts are packed
	// into.
//	private static class RiStorageItem {
//		/**
//		 * x and y coordinate of the center of the sphere that this item corresponds to.
//		 */
//		short srcX, srcY;
//
//		public RiStorageItem(short x, short y) {
//			srcX = x;
//			srcY = y;
//		}
//	};

	/**
	 * Packs two shorts into one int that is RiStorageItem.
	 * 
	 * @param srcX
	 * @param srcY
	 * @return
	 */
	private static int makeRiStorageItem(short srcX, short srcY) {
		return ((int) srcX << 16) | (int) srcY;
	}

	/**
	 * Gets srcX component from int packed with makeRiStorageItem.
	 * 
	 * @param riStorageItem
	 * @return
	 */
	private static short getSrcX(int riStorageItem) {
		return (short) (riStorageItem >> 16);
	}

	/**
	 * Gets srcY component from int packed with makeRiStorageItem.
	 * 
	 * @param riStorageItem
	 * @return
	 */
	private static short getSrcY(int riStorageItem) {
		return (short) (riStorageItem & 0xffff);
	}

	/**
	 * Image that contains list of RiStorageItems in each pixel.
	 * 
	 * @author miettinen_a
	 *
	 */
	private static class RiImage extends ImageBase {
		/**
		 * The first array stores z-slices of the image (length = depth). The second
		 * array stores each slice (length = width * height). Each pixel stores array of
		 * ints (length = varying).
		 */
		private int[][][] data;

		public RiImage(Vec3i dimensions) {
			super(dimensions);
			// NOTE: Raw array is a bit faster to initialize than ArrayList,
			// and we don't need to resize the array anywhere.
			data = new int[depth()][width() * height()][];
		}

		public int[] get(Vec3i pos) {
			return data[pos.z][pos.y * width() + pos.x];
		}

		public int[] get(int x, int y, int z) {
			return data[z][y * width() + x];
		}

		public void set(Vec3i pos, int[] items) {
			data[pos.z][pos.y * width() + pos.x] = items;
		}

		public void set(int x, int y, int z, int[] items) {
			data[z][y * width() + x] = items;
		}
	}

	private static class Box {
		private Vec3i pos;
		// private Vec3c size;

		public Box(Vec3i pos, Vec3i size) {
			this.pos = pos;
			// this.size = size;
		}
	}

	private static class ActiveSpheresSuperItem implements Comparable<ActiveSpheresSuperItem> {
		/*
		 * Squared radius of the sphere that initiated creation of this extent.
		 */
		public int R2;

		/*
		 * Squared extent of the sphere in the current dimension.
		 */
		public int ri2;

		/*
		 * Center point of the sphere in the current dimension. This must be signed in
		 * order to support block-wise processing.
		 */
		public int xi;

		/**
		 * x and y coordinate of the center of the sphere that this item corresponds to.
		 */
		public short srcX;
		public short srcY;

		public ActiveSpheresSuperItem(int R2, int ri2, int xi, short srcX, short srcY) {
			this.R2 = R2;
			this.ri2 = ri2;
			this.xi = xi;
			this.srcX = srcX;
			this.srcY = srcY;
		}

		@Override
		public int compareTo(ActiveSpheresSuperItem b) {
			if (this.R2 != b.R2)
				// return this.R2 > b.R2;
				return -(this.R2 - b.R2);
			// return this.ri2 > b.ri2;
			return -(this.ri2 - b.ri2);
		}
	};

	private static class RiSuperItem implements Comparable<RiSuperItem> {
		/**
		 * Squared radius of the sphere that initiated creation of this extent.
		 */
		public int R2;

		/**
		 * Squared extent of the sphere in the next dimension.
		 */
		public int ri2;

		/**
		 * x and y coordinate of the center of the sphere that this item corresponds to.
		 */
		public short srcX;
		public short srcY;

		public RiSuperItem(int R2, int ri2, short srcX, short srcY) {
			this.R2 = R2;
			this.ri2 = ri2;
			this.srcX = srcX;
			this.srcY = srcY;
		}

		@Override
		public int compareTo(RiSuperItem b) {
			if (this.R2 != b.R2)
				return -(this.R2 - b.R2);
			return -(this.ri2 - b.ri2);
		}
	};

	private static class TempArrays {
		public ArrayList<ActiveSpheresSuperItem> activeSpheres = new ArrayList<ActiveSpheresSuperItem>(40);
		public ArrayList<ActiveSpheresSuperItem> activeSpheresTmp = new ArrayList<ActiveSpheresSuperItem>(40);
		public ArrayList<ActiveSpheresSuperItem> Ctmp = new ArrayList<ActiveSpheresSuperItem>(40);
		public ArrayList<RiSuperItem> resultTmp = new ArrayList<RiSuperItem>(40);
		public ArrayList<RiSuperItem> resultTmp2 = new ArrayList<RiSuperItem>(40);

		public PriorityQueue<ActiveSpheresSuperItem> activeSpheresPrio = new PriorityQueue<ActiveSpheresSuperItem>();
	}

	/**
	 * @param centers       Image containing only the row to be processed before
	 *                      first call to this method.
	 * @param ri            Full ri image that will be updated.
	 * @param rowStart      Start point of the pixel row to be processed.
	 * @param dim           Dimension that we are processing.
	 * @param step          +1 or -1 to indicate the direction of the pass.
	 * @param activeSpheres List containing initial active spheres for the row. At
	 *                      exit, contains list of active spheres after processing
	 *                      the row.
	 */
	private static void singlePassSuper(ArrayList<ArrayList<RiSuperItem>> centers, ArrayList<ArrayList<RiSuperItem>> ri,
			int dimensionality, Vec3i dimensions, Vec3i rowStart, int dim, int step, TempArrays tempArrays) {

		// Stores the spheres that have been encountered and that have not been passed
		// yet.
		// Stores the center point, original radius, and ri.
		ArrayList<ActiveSpheresSuperItem> activeSpheres = tempArrays.activeSpheres;
		ArrayList<ActiveSpheresSuperItem> activeSpheresTmp = tempArrays.activeSpheresTmp;
		ArrayList<ActiveSpheresSuperItem> Ctmp = tempArrays.Ctmp;

		ArrayList<RiSuperItem> resultTmp = tempArrays.resultTmp;
		ArrayList<RiSuperItem> resultTmp2 = tempArrays.resultTmp2;

		activeSpheres.clear();
		activeSpheresTmp.clear();
		Ctmp.clear();
		resultTmp.clear();
		resultTmp2.clear();

		// Set start point to the start or to the end of the current row.
		Vec3i p = new Vec3i(rowStart);
		if (step < 0)
			p.inc(dim, dimensions.get(dim) - 1);

		for (int i = 0; i < dimensions.get(dim); i++, p.inc(dim, step)) {
			int x = (int) p.get(dim);

			// If there is one or more sphere centers at the current location, add them to
			// the set of active spheres.
			ArrayList<RiSuperItem> C = centers.get((int) p.get(dim));

			// The C list is sorted by R and so is activeSpheres list.
			// Instead of finding place for each item, convert each item in C list to
			// activeSpheres format (add x coordinate)
			// and then merge the two sorted lists to construct the new activeSpheres list.
			Ctmp.clear();
			for (int n = 0; n < C.size(); n++) {
				RiSuperItem item = C.get(n);
				Ctmp.add(new ActiveSpheresSuperItem(item.R2, item.ri2, x, item.srcX, item.srcY));
			}

			ListUtils.merge(Ctmp, activeSpheres, activeSpheresTmp);
			ArrayList<ActiveSpheresSuperItem> temp = activeSpheres;
			activeSpheres = activeSpheresTmp;
			activeSpheresTmp = temp;

			// Iterate through all active spheres and calculate radius for next dimension.
			resultTmp.clear();
			ListIterator<ActiveSpheresSuperItem> it = activeSpheres.listIterator();
			while (it.hasNext()) {
				ActiveSpheresSuperItem item = it.next();

				int Rorig2 = item.R2;
				int R2 = item.ri2;
				int cx = item.xi;

				short origcx = item.srcX;
				short origcy = item.srcY;

				int dx = Math.abs(x - cx);

				// Calculate new ri^2
				int rn2 = R2 - dx * dx;
				if (rn2 > 0) {
					// Insert ry2 into the list, but don't insert duplicates.
					if (resultTmp.size() > 0 && Rorig2 == resultTmp.get(resultTmp.size() - 1).R2) {
						// This is a duplicate R2 entry. Use the entry with the larger ri.
						if (resultTmp.get(resultTmp.size() - 1).ri2 < rn2)
							resultTmp.set(resultTmp.size() - 1, new RiSuperItem(Rorig2, rn2, origcx, origcy));
					} else {
						resultTmp.add(new RiSuperItem(Rorig2, rn2, origcx, origcy));
					}
				} else {
					// ry is non-positive, i.e. dx >= R
					// This sphere is not active anymore, so remove it from the list of active
					// spheres.
					it.remove();
				}
			}

			// Rebuild ri list from resultTmp. Don't include those items that are hidden by
			// other items.
			if (resultTmp.size() > 0) {
				// Add ri from the previous pass to the ri list and save the result to
				// resultTmp2.
				// Note that both resultTmp2 and rilist are sorted so we can just merge them.
				ArrayList<RiSuperItem> rilist = ri.get((int) p.get(dim));
				ListUtils.merge(rilist, resultTmp, resultTmp2);

				// Linear time algorithm for finding relevant ri (those not hidden by other
				// items).
				// Requires that ri list is sorted according to R in descending order and then
				// by ri in descending order.
				// Non-hidden items satisfy R > R_prev || ri > ri_prev. The first condition is
				// always true as we sort the list by R.
				rilist.clear();
				rilist.add(resultTmp2.get(0));
				for (int n = 1; n < resultTmp2.size(); n++) {
					int currri2 = rilist.get(rilist.size() - 1).ri2;
					RiSuperItem resultTmp2n = resultTmp2.get(n);
					int newri2 = resultTmp2n.ri2;

					if (newri2 > currri2) // This is the basic condition that works always (but may include unnecessary
											// items in the rilist)
					{
						if (dim == dimensionality - 2) // 3 - 2 == 1 == 2nd dimension
						{
							// In the second last dimension only really visible spans are needed as there's
							// no next dimension whose ri we would calculate based on the spans.
							if (MathUtils.largestIntWhoseSquareIsLessThan(newri2) > MathUtils.largestIntWhoseSquareIsLessThan(currri2))
								rilist.add(resultTmp2n);
						} else if (dim == dimensionality - 3) // 3 - 3 == 0 == 1st dimension
						{
							// In the third last dimension we know that only those spans are required that
							// produce visible circles in the output.
							if (!doesDiscretizedCircle1FitInto2Cached(newri2, currri2))
								rilist.add(resultTmp2n);
						} else {
							// Here we could insert test if discretized spheres fit into each other etc.
							// etc.
							rilist.add(resultTmp2n);
						}
					}
				}

				// Make sure we don't use extraneous memory by storing empty items in each pixel
				// of the ri image.
				rilist.trimToSize();
			}
		}
	}

	/**
	 * Same than singlePass but generates result image instead of ri image, and is
	 * thus a little bit faster.
	 * 
	 * @param centers              Image containing only the row to be processed
	 *                             before first call to this method.
	 * @param Result               image. Pixels must be set to zero before first
	 *                             call to this method.
	 * @param rowStart             Start point of the pixel row to be processed.
	 * @param dim                  Dimension that we are processing.
	 * @param step                 +1 or -1 to indicate the direction of the pass.
	 * @param initialActiveSpheres Initial active spheres list. Contains final
	 *                             active spheres list at output. Set to nullptr to
	 *                             assume empty list.
	 */
	private static void singlePassFinalSuper(ArrayList<ArrayList<RiSuperItem>> centers, Image result, Vec3i rowStart,
			int dim, int step, Vec3i blockOrigin, TempArrays tempArrays) {

		// Stores the spheres that have been encountered and that have not been passed
		// yet.
		// Stores the sphere with the largest R at the top of the priority queue.
		PriorityQueue<ActiveSpheresSuperItem> activeSpheres = tempArrays.activeSpheresPrio;
		activeSpheres.clear();

		// Set start point to the start or end of the current row.
		Vec3i p = new Vec3i(rowStart);
		if (step < 0)
			p.inc(dim, result.getDimension(dim) - 1);

		for (int i = 0; i < result.getDimension(dim); i++, p.inc(dim, step)) {
			int x = p.get(dim);

			// If there is one or more sphere centers at the current location, add them to
			// the set of active spheres.
			ArrayList<RiSuperItem> C = centers.get((int) p.get(dim));
			for (RiSuperItem item : C) {
				activeSpheres.add(new ActiveSpheresSuperItem(item.R2, item.ri2, x, (short) 0, (short) 0));
			}

			while (!activeSpheres.isEmpty()) {
				ActiveSpheresSuperItem item = activeSpheres.peek();

				int Rorig2 = item.R2;
				int R2 = item.ri2;
				int cx = item.xi;

				int dx = Math.abs(x - cx);

				// Calculate new ri^2
				int rn2 = R2 - dx * dx;
				if (rn2 > 0) {
					// Note that previous pass may have assigned larger value to the output.
					// Vec3c pp = p.add(blockOrigin);
					int ppx = p.x + blockOrigin.x;
					int ppy = p.y + blockOrigin.y;
					int ppz = p.z + blockOrigin.z;
					if (Rorig2 > result.get(ppx, ppy, ppz))
						result.set(ppx, ppy, ppz, Rorig2);
					break;
				} else {
					// ry is non-positive, i.e. dx >= R
					// This sphere is not active anymore, so remove it from the list of active
					// spheres.
					activeSpheres.remove();
				}

			}

		}
	}

	/**
	 * Copies squared sphere radius data from original centers image to ri image.
	 * 
	 * @param centers2 Original distance ridge.
	 * @param ri       Temporary image that is to be initialized.
	 * @param bounds   If the ri covers only a block of centers2, a box defining the
	 *                 block. Pass Box from [0, 0, 0] to centers2.dimensions() to
	 *                 prepare whole image.
	 */
	private static void prepareSuper(Image centers2, RiImage ri, Box bounds) {
		if (centers2.getDimensions().max() >= Short.MAX_VALUE)
			throw new IllegalArgumentException("Linear image size exceeds " + Short.MAX_VALUE
					+ " pixels. This implementation is not configured for that big images.");

		int bx = (int) bounds.pos.x;
		int by = (int) bounds.pos.y;
		int bz = (int) bounds.pos.z;

		for (int z = 0; z < ri.depth(); z++) {
			for (int y = 0; y < ri.height(); y++) {
				for (int x = 0; x < ri.width(); x++) {
					float R2 = centers2.get(bx + x, by + y, bz + z);

					if (R2 > 0) {
						ri.set(x, y, z, new int[] {
								makeRiStorageItem((short) (x + bounds.pos.x), (short) (y + bounds.pos.y)) });
					}
				}
			}
		}
	}

	/**
	 * Sets size[dim] = 0 and returns the result.
	 */
	private static Vec3i getReducedDimensions(Vec3i size, int dim) {
		size.set(dim, 1);
		return size;
	}

	/**
	 * Converts RiStorageItem to RiSuperItem.
	 * 
	 * @param p Position (in the block) where the item is taken from.
	 */
	private static RiSuperItem toRiItem(int riStorageItem, Vec3i p, Vec3i blockPos, Image dmap2Full) {

		short srcX = getSrcX(riStorageItem);
		short srcY = getSrcY(riStorageItem);

		// This version reads always from the full image (difference compared to c++
		// version)
		float R2f = dmap2Full.get(srcX, srcY, p.z + blockPos.z);
		int R2 = (int) Math.round(R2f);
		int dx = p.x - (srcX - blockPos.x);
		int dy = p.y - (srcY - blockPos.y);

		int ri2 = R2 - dx * dx - dy * dy;

		return new RiSuperItem(R2, ri2, srcX, srcY);
	}

	/**
	 * Converts RiSuperItem to RiStorageItem
	 */
	private static int toRiStorageItem(RiSuperItem item) {
		return makeRiStorageItem(item.srcX, item.srcY);
	}

	/**
	 * Converts RiStorageSet to RiSuperSet.
	 * 
	 * @param in Source set
	 * @param p  Position (in the block) where the in set is taken from.
	 */
	private static void toRiSet(int[] riStorageSet, ArrayList<RiSuperItem> out, Vec3i p, Vec3i blockPos,
			Image dmap2Full) {
		out.clear();
		if (riStorageSet != null) {
			for (int n = 0; n < riStorageSet.length; n++)
				out.add(toRiItem(riStorageSet[n], p, blockPos, dmap2Full));
		}
	}

	/**
	 * Converts RiSuperSet to RiStorageSet.
	 */
	private static int[] toStorageSet(ArrayList<RiSuperItem> in) {
		if (in.size() <= 0)
			return null;

		int[] out = new int[in.size()];
		for (int n = 0; n < in.size(); n++)
			out[n] = toRiStorageItem(in.get(n));
		return out;
	}

	/**
	 * Makes one pass over image in specific dimension and direction.
	 * 
	 * @param ri               Image containing ri values from processing of
	 *                         previous dimension.
	 * @param dim              Dimension to process.
	 * @param result           Result image.
	 * @param showProgressInfo Set to true to show progress indicator.
	 */
	private static void processDimensionSuper(RiImage ri, int dim, Image dmap2, Image result, Box currBlock,
			int dimensionality) throws InterruptedException {

		// ri: for current block
		// dmap2: for whole image
		// result: for whole image

		// Determine count of pixels to process
		Vec3i reducedDimensions = getReducedDimensions(ri.getDimensions(), dim);
		long rowCount = (long) reducedDimensions.x * (long) reducedDimensions.y * (long) reducedDimensions.z;

		Vec3i blockPos = new Vec3i(currBlock.pos);

		boolean isFinalPass = !(dim < dimensionality - 1);

		AtomicInteger progress = new AtomicInteger(0);

		// Temporary buffers
		ThreadLocal<ArrayList<ArrayList<RiSuperItem>>> inRowStore = new ThreadLocal<ArrayList<ArrayList<RiSuperItem>>>() {
			@Override
			protected ArrayList<ArrayList<RiSuperItem>> initialValue() {
				ArrayList<ArrayList<RiSuperItem>> result = new ArrayList<ArrayList<RiSuperItem>>();
				for (int n = 0; n < ri.getDimension(dim); n++)
					result.add(new ArrayList<RiSuperItem>());
				return result;
			}
		};

		ThreadLocal<ArrayList<ArrayList<RiSuperItem>>> outRowStore = new ThreadLocal<ArrayList<ArrayList<RiSuperItem>>>() {
			@Override
			protected ArrayList<ArrayList<RiSuperItem>> initialValue() {
				ArrayList<ArrayList<RiSuperItem>> result = new ArrayList<ArrayList<RiSuperItem>>();
				for (int n = 0; n < ri.getDimension(dim); n++)
					result.add(new ArrayList<RiSuperItem>());
				return result;
			}
		};

		ThreadLocal<TempArrays> tempArraysStore = new ThreadLocal<TempArrays>() {
			@Override
			protected TempArrays initialValue() {
				return new TempArrays();
			}
		};

		// Determine start points of pixel rows and process each row
		Loop.withIndex(0, rowCount, new Loop.Each() {

			@Override
			public void run(long n) {

				ArrayList<ArrayList<RiSuperItem>> inRow = inRowStore.get();
				ArrayList<ArrayList<RiSuperItem>> outRow = outRowStore.get();
				TempArrays tempArrays = tempArraysStore.get();

				Vec3i start = Image.indexToCoords(n, reducedDimensions);

				// Make a copy of the current row as we update the row in the forward pass but
				// need the original data in the backward pass.
				Vec3i pos = new Vec3i(start);
				for (int x = 0; x < ri.getDimension(dim); x++, pos.inc(dim)) {
					toRiSet(ri.get(pos), inRow.get(x), pos, blockPos, dmap2);
					outRow.get(x).clear();
				}

				if (!isFinalPass) {
					singlePassSuper(inRow, outRow, dmap2.getDimensionality(), ri.getDimensions(), start, dim, 1,
							tempArrays);
					singlePassSuper(inRow, outRow, dmap2.getDimensionality(), ri.getDimensions(), start, dim, -1,
							tempArrays);

					// Copy data back to storage
					pos = start;
					for (int x = 0; x < ri.getDimension(dim); x++, pos.inc(dim)) {
						ri.set(pos, toStorageSet(outRow.get(x)));
					}
				} else {
					singlePassFinalSuper(inRow, result, start, dim, 1, blockPos, tempArrays);
					singlePassFinalSuper(inRow, result, start, dim, -1, blockPos, tempArrays);
				}

				IJ.showProgress(progress.incrementAndGet(), (int) rowCount);
			}
		});
	}

	/**
	 * Updates circleLookup array in order to be able to process the given image.
	 * 
	 * @param dmap2
	 */
	private static void buildCircleLookup(Image dmap2) throws InterruptedException {

		float M = ImageUtils.max(dmap2);
		if (M >= Integer.MAX_VALUE)
			throw new IllegalArgumentException(
					"The squared distance map contains too large values. (This error is easily avoidable by changing buildCircleLookup functionality.)");
		buildCircleLookup((int) Math.round(M));
	}

	/**
	 * Calculate squared local radius from squared distance map.
	 * 
	 * @param dmap2 Squared distance map.
	 * @param tmap2 At output, squared radius map.
	 * @throws InterruptedException
	 */
	public static void thickmap2SingleBlock(Image dmap2, Image tmap2) throws InterruptedException {

		buildCircleLookup(dmap2);

		RiImage ri = new RiImage(dmap2.getDimensions());
		Box fullBox = new Box(new Vec3i(0, 0, 0), dmap2.getDimensions());
		prepareSuper(dmap2, ri, fullBox);
		ImageUtils.setValue(tmap2, 0);

		for (int n = 0; n < dmap2.getDimensionality(); n++)
			processDimensionSuper(ri, n, dmap2, tmap2, fullBox, dmap2.getDimensionality());
	}

	/**
	 * Calculates mean radius of non-zero pixels in squared distance map.
	 * 
	 * @param dmap2
	 * @return
	 */
	public static double calcNonZeroMeanR(Image dmap2) throws InterruptedException {

		// Single-threaded version
//		double sum = 0;
//		double count = 0;
//		for (int z = 0; z < dmap2.depth(); z++) {
//			for (int y = 0; y < dmap2.height(); y++) {
//				for (int x = 0; x < dmap2.width(); x++) {
//					float pix = dmap2.get(new Vec3c(x, y, z));
//					if (pix != 0) {
//						sum += Math.sqrt(pix);
//						count++;
//					}
//				}
//			}
//		}

		DoubleAdder sum = new DoubleAdder();
		DoubleAdder count = new DoubleAdder();

		Loop.withIndex(0, dmap2.depth(), new Loop.Each() {

			@Override
			public void run(long z) {

				int iz = (int) z;
				double localSum = 0;
				double localCount = 0;

				for (int y = 0; y < dmap2.height(); y++) {
					for (int x = 0; x < dmap2.width(); x++) {
						float pix = dmap2.get(x, y, iz);
						if (pix != 0) {
							localSum += Math.sqrt(pix);
							localCount++;
						}
					}
				}

				sum.add(localSum);
				count.add(localCount);
			}

		});

		double s = sum.sum();
		double c = count.sum();

		if (c > 0)
			s /= c;

		return s;
	}

	/**
	 * Creates name (prefix) for a temporary file.
	 * 
	 * @param dir
	 * @return
	 */
	private static String createTempFilename(String dir) {

		if (dir == null || dir.isEmpty())
			dir = System.getProperty("java.io.tmpdir");

		// Add something random so that multiple instances can run at the same time
		// without interfering with one another.
		return dir + "/thickmap_temp_images/ri_" + java.util.UUID.randomUUID();
	}

	/**
	 * Calculatesamount of memory required for processing image with given
	 * dimensions and mean radius.
	 * 
	 * @param dimensions
	 * @param meanr
	 * @return
	 */
	private static double getMemoryRequirement(Vec3i dimensions, double meanr) {
		// Memory requirement for C++ code is
		// (4.5 + 0.2 * meanr) * dmap2.pixelCount() * sizeof(float)
		// Here we don't have small vector optimization and additionally Java has
		// big overhead for all objects, so make it pretty much larger!

		final double FLOAT_SIZE = 4;
		return (7 + 0.4 * meanr) * (double) dimensions.x * (double) dimensions.y * (double) dimensions.z * FLOAT_SIZE;
	}

	/**
	 * Gets suitable distribution direction, given direction that we are currently
	 * processing.
	 * 
	 * @param dim
	 * @return
	 */
	private static int getDistributionDirection(int dim) {
		switch (dim) {
		case 0:
			return 2;
		case 1:
			return 2;
		case 2:
			return 1;
		default:
			throw new IllegalArgumentException("Unsupported dimension.");
		}
	}

	/**
	 * Creates name of ri data file.
	 * 
	 * @param indexFile
	 * @param blockIndex
	 * @return
	 */
	private static String createDatFileName(String indexFile, int blockIndex) {
		return indexFile + "_block" + blockIndex + ".dat";
	}

	/**
	 * Packs block index and start index into one 64-bit value.
	 * 
	 * @param blockIndex
	 * @param startIndex
	 * @return
	 */
	private static long setBlockAndStart(int blockIndex, long startIndex) {
		return (long) blockIndex + ((startIndex & 0xffffffffffffl) << 16);
	}

	/**
	 * Concatenates block and start index into 64-bit int and stores it in the index
	 * image.
	 * 
	 * @param blockIndex
	 * @param startIndex
	 * @param index
	 * @param pos
	 */
//	private static void setBlockAndStart(int blockIndex, long startIndex, ImageI64 index, Vec3i pos) {
//		index.set(pos, setBlockAndStart(blockIndex, startIndex));
//	}

	/**
	 * Extracts block index from data word created by setBlockAndStart.
	 * 
	 * @param data
	 * @return
	 */
	private static short getBlockIndex(long data) {
		return (short) (data & 0x000000000000ffff);
	}

	/**
	 * Extracts start index from data word created by setBlockAndStart.
	 * 
	 * @param data
	 * @return
	 */
	private static long getStartIndex(long data) {
		return (data & 0xffffffffffff0000l) >> 16;
	}

	/**
	 * Writes ri image block to file.
	 */
	private static void writeRiBlock(RiImage ri, String indexFilePrefix, int blockIndex, Vec3i filePosition,
			Vec3i fileDimensions) throws FileNotFoundException, IOException {
		if (blockIndex > Short.MAX_VALUE)
			throw new IllegalArgumentException("Too many blocks.");

		ImageI64 index = new ImageI64(ri.getDimensions());

		String indexFile = Raw.concatDimensions(indexFilePrefix, fileDimensions);

		FileUtils.createFoldersFor(indexFile);

		String blockFileName = createDatFileName(indexFilePrefix, blockIndex);

		try (DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(new FileOutputStream(blockFileName)))) {

			long startIndex = 0;
			for (int z = 0; z < ri.depth(); z++) {
				for (int y = 0; y < ri.height(); y++) {
					for (int x = 0; x < ri.width(); x++) {
						// Vec3c pos = new Vec3c(x, y, z);
						// setBlockAndStart(blockIndex, startIndex, index, pos);
						index.set(x, y, z, setBlockAndStart(blockIndex, startIndex));

						int[] s = ri.get(x, y, z);

						// Write size
						short count = s != null ? (short) s.length : 0;
						out.writeShort(count);

						if (count > 0) {
							// Write items
							for (int m = 0; m < s.length; m++) {
								short val = getSrcX(s[m]);
								out.writeShort(val);

								val = getSrcY(s[m]);
								out.writeShort(val);
							}
						}

						startIndex += 2 * count + 1;
					}
				}
			}
		}

		Raw.writeBlock(index, indexFile, filePosition, fileDimensions);
	}

	/**
	 * Reads ri image block from file.
	 */
	private static void readRiBlock(RiImage ri, String indexFilePrefix, Vec3i start, Vec3i fileDimensions)
			throws IOException {
		// index(x, y, z) gives the start index to read from the data file dat.
		// dat[index(x, y, z)] gives the count of elements.
		ImageI64 index = new ImageI64(ri.getDimensions());

		String indexFile = Raw.concatDimensions(indexFilePrefix, fileDimensions);
		Raw.readBlockNoParse(index, indexFile, start, fileDimensions);

		Map<Short, DiskMappedReadBuffer> datFiles = new HashMap<Short, DiskMappedReadBuffer>();
		try {
			for (int z = 0; z < ri.depth(); z++) {
				for (int y = 0; y < ri.height(); y++) {
					for (int x = 0; x < ri.width(); x++) {
						// Vec3c pos = new Vec3c(x, y, z);
						long startItem = index.get(x, y, z);
						short blockIndex = getBlockIndex(startItem);
						long startIndex = getStartIndex(startItem);

						// Map dat file if it is not open.
						DiskMappedReadBuffer dat = datFiles.getOrDefault(blockIndex, null);
						if (dat == null) {
							String datFileName = createDatFileName(indexFilePrefix, blockIndex);
							dat = new DiskMappedReadBuffer(datFileName);
							datFiles.put(blockIndex, dat);
						}

						short count = dat.readShort(startIndex);

						int[] vals = new int[count];
						for (int i = 0; i < count; i++) {
							short srcX = dat.readShort((startIndex + 1) + 2 * i);
							short srcY = dat.readShort((startIndex + 1) + 2 * i + 1);
							vals[i] = makeRiStorageItem(srcX, srcY);
						}
						ri.set(x, y, z, vals);
					}
				}

			}
		} finally {
			for (DiskMappedReadBuffer b : datFiles.values()) {
				try {
					b.close();
				} catch (IOException e) {
					// Do nothing
				}
			}
		}

	}

	private static void processDimensionBlock(Image dmap2, Image tmap2, int dim, String riPrefix, Vec3i blockOrigin,
			Vec3i blockSize, int blockIndex) throws InterruptedException, IOException {

		// Make sure block does not go out of the original image
		if (blockOrigin.x + blockSize.x > dmap2.width())
			blockSize.x = dmap2.width() - blockOrigin.x;
		if (blockOrigin.y + blockSize.y > dmap2.height())
			blockSize.y = dmap2.height() - blockOrigin.y;
		if (blockOrigin.z + blockSize.z > dmap2.depth())
			blockSize.z = dmap2.depth() - blockOrigin.z;

		// This can be used for rudimentary I/O timing
		// StopWatch t = new StopWatch();

		// Initialize ri for the block
		RiImage ri = new RiImage(blockSize);
		if (dim > 0) {
			// Read ri from previous dimension output
			// t.start();
			readRiBlock(ri, riPrefix + "_dim" + (dim - 1), blockOrigin, dmap2.getDimensions());
			// IJ.log("Reading took " + t.stop() + " ms");
		} else {
			// Initialize ri from dmap2
			for (int z = 0; z < ri.depth(); z++) {
				for (int y = 0; y < ri.height(); y++) {
					for (int x = 0; x < ri.width(); x++) {
						// Vec3c p = new Vec3c(x, y, z);
						// float R2 = dmap2.get(p.add(blockOrigin));
						float R2 = dmap2.get(x + blockOrigin.x, y + blockOrigin.y, z + blockOrigin.z);
						if (R2 > 0) {
							// ri.get(p).add(new RiStorageItem((short) (x + blockOrigin.x), (short) (y +
							// blockOrigin.y)));
							// ri.set(p, new int[] { makeRiStorageItem((short) (x + blockOrigin.x), (short)
							// (y + blockOrigin.y)) });
							ri.set(x, y, z, new int[] {
									makeRiStorageItem((short) (x + blockOrigin.x), (short) (y + blockOrigin.y)) });
						}
					}
				}
			}
		}

		processDimensionSuper(ri, dim, dmap2, tmap2, new Box(blockOrigin, blockSize), dmap2.getDimensionality());

		// Write temporary file, if any
		if (dim < dmap2.getDimensionality() - 1) {
			// t.start();
			writeRiBlock(ri, riPrefix + "_dim" + dim, blockIndex, blockOrigin, dmap2.getDimensions());
			// IJ.log("Writing took " + t.stop() + " ms");
		}
	}

	private static Vec3i calculateBlockSize(Image dmap2, int dim, double meanr) {
		// Determine suitable block size
		Vec3i subDivisions = new Vec3i(1, 1, 1);
		Vec3i blockSize = dmap2.getDimensions();
		int distributionDirection = getDistributionDirection(dim);
		while (getMemoryRequirement(blockSize, meanr) >= IJ.maxMemory()) {
			subDivisions.inc(distributionDirection);
			blockSize = dmap2.getDimensions().divc(subDivisions).add(new Vec3i(1, 1, 1));
			MathUtils.clamp(blockSize, new Vec3i(0, 0, 0), dmap2.getDimensions());
		}

		return blockSize;
	}

	/**
	 * Subdivides image into smaller blocks and processes each block separately.
	 * 
	 * @param dmap2
	 * @param tmap
	 * @param dim
	 * @param riPrefix
	 * @param meanr
	 */
	private static void subdivideAndProcessDimension(Image dmap2, Image tmap2, int dim, String riPrefix, double meanr)
			throws InterruptedException, IOException {

		Vec3i blockSize = calculateBlockSize(dmap2, dim, meanr);

		// Count blocks for progress reporting
		int blockCount = 0;
		for (int blockZ = 0; blockZ < dmap2.depth(); blockZ += blockSize.z) {
			for (int blockY = 0; blockY < dmap2.height(); blockY += blockSize.y) {
				for (int blockX = 0; blockX < dmap2.width(); blockX += blockSize.x) {
					blockCount++;
				}
			}
		}

		if (blockCount > Short.MAX_VALUE)
			throw new IllegalArgumentException(
					"There is not enough memory to process this image. It must be divided to too many blocks. Consider increasing Maximu Memory in Edit->Options->Memory & Threads.");

		// Process all blocks
		int blockIndex = 0;
		for (int blockZ = 0; blockZ < dmap2.depth(); blockZ += blockSize.z) {
			for (int blockY = 0; blockY < dmap2.height(); blockY += blockSize.y) {
				for (int blockX = 0; blockX < dmap2.width(); blockX += blockSize.x) {

					IJ.showStatus("Processing block " + (blockIndex + 1) + " / " + blockCount);

					Vec3i blockPos = new Vec3i(blockX, blockY, blockZ);

					processDimensionBlock(dmap2, tmap2, dim, riPrefix, blockPos, blockSize, blockIndex);

					blockIndex++;
				}
			}
		}

	}

	/**
	 * Calculate squared local radius from squared distance map. Process the image
	 * in blocks in order to save RAM. Temporary results are saved to disk.
	 * 
	 * @param dmap2
	 * @param tmap2
	 * @param tempDirSuggestion Directory (suggestion) where temporary files can be
	 *                          saved.
	 * @param meanRadius        Mean radius, as returned by calcNonZeroMeanR method.
	 * @throws InterruptedException
	 */
	public static void thickmap2MultiBlock(Image dmap2, Image tmap2, String tempDirSuggestion, double meanRadius)
			throws InterruptedException, IOException {

		buildCircleLookup(dmap2);

		String riPrefix = createTempFilename(tempDirSuggestion);
		Path fullPath = Paths.get(riPrefix);
		Path riDir = fullPath.getParent();
		String globPrefix = fullPath.getFileName().toString();

		for (int dim = 0; dim < dmap2.getDimensionality(); dim++) {
			// Process this dimension
			subdivideAndProcessDimension(dmap2, tmap2, dim, riPrefix, meanRadius);

			// Delete temporary files from previous round
			System.gc(); // Try to induce GC to close open and unnecessary memory mapped files.
			ArrayList<Path> items = FileUtils.buildFileList(riDir, globPrefix + "_dim" + (dim - 1) + "_*");
			for (Path p : items) {
				if (!FileUtils.tryDelete(p))
					// We might not be able to delete the file if the Java runtime has not yet freed
					// all file mappings to that file.
					IJ.log("Unable to delete temporary file: " + p.toString() + " Please delete it manually.");
			}
		}

	}

	/**
	 * Calculate squared local radius from squared distance map. If the image is
	 * large it is processed in blocks.
	 * 
	 * @param dmap2
	 * @param tmap2
	 * @param tempDirSuggestion Directory (suggestion) where temporary files can be
	 *                          saved.
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void thickmap2(Image dmap2, Image tmap2, String tempDirSuggestion)
			throws InterruptedException, IOException {
		double meanr = calcNonZeroMeanR(dmap2);

		Vec3i blockSize = calculateBlockSize(dmap2, 0, meanr);
		if (blockSize.equals(dmap2.getDimensions())) {
			// Process in just one block
			// This is faster but requires more memory
			thickmap2SingleBlock(dmap2, tmap2);
		} else {
			thickmap2MultiBlock(dmap2, tmap2, tempDirSuggestion, meanr);
		}
	}

	/**
	 * Finds out where the given image is saved or where it is loaded from.
	 * 
	 * @param iplus
	 * @return Directory where the given image lives, or empty string if the
	 *         directory information is not available.
	 */
	public static String getImageDirectory(ImagePlus iplus) {
		String dir = iplus.getFileInfo() != null ? iplus.getFileInfo().directory : "";
		if (dir.isEmpty())
			dir = iplus.getOriginalFileInfo() != null ? iplus.getOriginalFileInfo().directory : "";
		return dir;
	}

	private ImagePlus iplus;

	@Override
	public void run(ImageProcessor arg0) {
		Image img = new Image(iplus.getStack());

		// Create output image
		ImageStack out = iplus.createEmptyStack();
		for (int z = 0; z < img.depth(); z++)
			out.addSlice(new FloatProcessor(img.width(), img.height(), new float[img.width() * img.height()]));
		Image outImg = new Image(out);

		String tempDir = getImageDirectory(iplus);

		try {
			thickmap2(img, outImg, tempDir);
			iplus.setStack(out);
		} catch (InterruptedException e) {

		} catch (IOException e) {
			IJ.showMessage("I/O exception while saving or loading temporary data: " + e.getMessage());
		}
	}

	@Override
	public int setup(String arg0, ImagePlus img) {

		iplus = img;
		return DOES_32;

	}

}
