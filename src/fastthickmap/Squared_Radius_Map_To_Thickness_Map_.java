package fastthickmap;

import java.util.concurrent.atomic.AtomicInteger;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class Squared_Radius_Map_To_Thickness_Map_ implements PlugInFilter {

	/**
	 * Calculate thickness map from squared local radius map.
	 * 
	 * @param img
	 * @throws InterruptedException
	 */
	public static void finalizeThickmap(Image rmap2) throws InterruptedException {

		AtomicInteger progress = new AtomicInteger(0);
		Loop.withIndex(0, rmap2.depth(), new Loop.Each() {

			@Override
			public void run(long zl) {

				int z = (int)zl;
				
				for (int y = 0; y < rmap2.height(); y++) {
					for (int x = 0; x < rmap2.width(); x++) {

						//Vec3c pos = new Vec3c(x, y, z);

						float r2 = rmap2.get(x, y, z);
						rmap2.set(x, y, z, (float) (2 * Math.sqrt(r2)));
					}
				}

				IJ.showProgress(progress.incrementAndGet(), rmap2.depth());
			}
		});

	}

	private ImagePlus iplus;

	@Override
	public void run(ImageProcessor arg0) {
		Image img = new Image(iplus.getStack());

		try {
			finalizeThickmap(img);
			iplus.setStack(iplus.getStack());
		} catch (InterruptedException e) {

		}
	}

	@Override
	public int setup(String arg0, ImagePlus img) {
		iplus = img;
		return DOES_32;
	}

}
