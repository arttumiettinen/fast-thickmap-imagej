package fastthickmap;

import java.util.concurrent.atomic.AtomicInteger;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

/**
 * ImageJ plugin that rounds squared distance ridge (or distance map) values such that they represent
 * squares of integers.
 * @author miettinen_a
 *
 */
public class Round_Squared_Distance_Ridge_ implements PlugInFilter {

	
	public static void roundSquaredRidge(Image dmap2) throws InterruptedException {
		
		AtomicInteger progress = new AtomicInteger(0);
		Loop.withIndex(0, dmap2.depth(), new Loop.Each() {

			@Override
			public void run(long zl) {
				
				int z = (int)zl;
				
				for (int y = 0; y < dmap2.height(); y++) {
					for (int x = 0; x < dmap2.width(); x++) {
						float v = dmap2.get(x, y, z);
						v = Math.round(Math.sqrt(v));
						dmap2.set(x, y, z, v * v);
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

		try {
			roundSquaredRidge(img);
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
