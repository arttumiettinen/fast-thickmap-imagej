package fastthickmap;

import java.io.IOException;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.process.StackConverter;
import ij.plugin.ContrastEnhancer;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

/**
 * ImageJ plugin that transforms image to its local thickness map.
 *  
 * @author miettinen_a
 *
 */
public class Thickness_Map_ implements PlugInFilter {

	private ImagePlus iplus;

	@Override
	public void run(ImageProcessor arg0) {
		
		boolean approximation = defaultIntApprox;
		String tempDir = defaultTempDir;

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
			
			if(approximation) {
				IJ.showStatus("Approximation by rounding distance values to the nearest integers...");
				Round_Squared_Distance_Ridge_.roundSquaredRidge(outImg);
			}

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

	static boolean defaultIntApprox = false;
	static String defaultTempDir = "";
	
	@Override
	public int setup(String arg0, ImagePlus img) {
		
		String tempDir = defaultTempDir;
		if(tempDir == null || tempDir.isEmpty())
			tempDir = Squared_Distance_Ridge_To_Squared_Radius_Map_.getImageDirectory(img);
		
		if (tempDir == null || tempDir.isEmpty())
			tempDir = System.getProperty("java.io.tmpdir");
		
		GenericDialog dlg = new GenericDialog("Thickness map settings");
		dlg.addCheckbox("Integer radius approximation", defaultIntApprox);
		dlg.addDirectoryField("Temporary directory", tempDir);
		dlg.addMessage("The temporary directory should be on a fast disk with plenty of free space.");
		dlg.showDialog();
		
		if(dlg.wasCanceled())
			return DONE;
		
		defaultIntApprox = dlg.getNextBoolean();
		String newTempDir = dlg.getNextText();
		
		// Only save the directory choice if the user changed the directory from the default.
		// If left to the default value (empty) the directory is updated for each image processed.
		if(newTempDir != tempDir)
			defaultTempDir = newTempDir;
		
		iplus = img;
		return DOES_8G + DOES_16 + DOES_32;
	}

}
