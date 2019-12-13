package fastthickmap;

import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.function.DoubleBinaryOperator;

public class ImageUtils {

	/**
	 * Calculates maximum value in the given image.
	 * @param img
	 * @return
	 */
	public static float max(Image img) throws InterruptedException {
//		float M = Float.NEGATIVE_INFINITY;
//		for (int z = 0; z < img.depth(); z++) {
//			for (int y = 0; y < img.height(); y++) {
//				for (int x = 0; x < img.width(); x++) {
//					float p = img.get(x, y, z);
//					if(p > M)
//						M = p;
//				}
//			}
//		}
//		return M;
		
		DoubleAccumulator acc = new DoubleAccumulator(new DoubleBinaryOperator() {
			
			@Override
			public double applyAsDouble(double left, double right) {
				return Math.max(left, right);
			}
		}, Double.NEGATIVE_INFINITY);
		
		Loop.withIndex(0, img.depth(), new Loop.Each() {
			
			@Override
			public void run(long zl) {
				
				int z = (int)zl;
				
				float myMax = Float.NEGATIVE_INFINITY;
				float[] slice = img.getSlice(z);
				for(int n = 0; n < slice.length; n++)
				{
					float val = slice[n];
					if(val > myMax)
						myMax = val;
				}
				
				acc.accumulate(myMax);
			}
		});
		
		return (float)acc.get();
	}
	
	/**
	 * Sets all pixels in the image to given value.
	 * @param img
	 * @param val
	 */
	public static void setValue(Image img, float val) throws InterruptedException {
//		for (int z = 0; z < img.depth(); z++) {
//			for (int y = 0; y < img.height(); y++) {
//				for (int x = 0; x < img.width(); x++) {
//					img.set(x, y, z, val);
//				}
//			}
//		}
		
		Loop.withIndex(0, img.depth(), new Loop.Each() {
			
			@Override
			public void run(long zl) {
				
				int z = (int)zl;
				
				float[] slice = img.getSlice(z);
				for(int n = 0; n < slice.length; n++)
					slice[n] = val;
			}
		});
	}
	
}
