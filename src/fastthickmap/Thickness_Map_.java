package fastthickmap;

import java.io.IOException;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.StackConverter;
import ij.plugin.ContrastEnhancer;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

public class Thickness_Map_ implements PlugInFilter {

	private ImagePlus iplus;

	@Override
	public void run(ImageProcessor arg0) {

		String tempDir = Squared_Distance_Ridge_To_Squared_Radius_Map_.getImageDirectory(iplus);

		if (iplus.isStack())
			(new StackConverter(iplus)).convertToGray32();
		else
			(new ImageConverter(iplus)).convertToGray32();

		Image img = new Image(iplus.getStack());

		ImageStack out = iplus.createEmptyStack();
		for (int z = 0; z < img.depth(); z++)
			out.addSlice(new FloatProcessor(img.width(), img.height(), new float[img.width() * img.height()]));
		Image outImg = new Image(out);

		try {
			IJ.showStatus("Squared distance map...");
			Squared_Distance_Map_.squaredDistanceMap(img, 0);

			IJ.showStatus("Squared distance ridge...");
			Squared_Distance_Map_To_Squared_Distance_Ridge_.danielsson(img, outImg);

			IJ.showStatus("Squared local radius...");
			Squared_Distance_Ridge_To_Squared_Radius_Map_.thickmap2(outImg, img, tempDir);

			IJ.showStatus("Finalization...");
			Squared_Radius_Map_To_Thickness_Map_.finalizeThickmap(img);

			IJ.showStatus("");
			iplus.setStack(iplus.getStack());
			(new ContrastEnhancer()).stretchHistogram(iplus.getProcessor(), 0.5);

		} catch (InterruptedException e) {

		} catch (IOException e) {
			IJ.showMessage("I/O exception while saving or loading temporary data: " + e.getMessage());
		}
	}

	@Override
	public int setup(String arg0, ImagePlus img) {
		iplus = img;
		return DOES_8G + DOES_16 + DOES_32;
	}

}
