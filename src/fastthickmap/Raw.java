package fastthickmap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

/**
 * Provides functionality for reading .raw files.
 * 
 * @author miettinen_a
 *
 */
public class Raw {

	/**
	 * Adds .raw image dimensions to file name.
	 */
	public static String concatDimensions(String baseName, Vec3c dimensions) {
		String suffix = "_" + dimensions.x + "x" + dimensions.y + "x" + dimensions.z + ".raw";

		if (baseName.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT)))
			return baseName;

		return baseName + suffix;
	}

	/**
	 * Writes a block of image to specified location in a raw file. The output file
	 * is not truncated if it exists. If the output file does not exist, it is
	 * created. Part of image extending beyond [0, fileDimensions[ is not written.
	 * 
	 * @param img             Image to write.
	 * @param filename        Name of file to write.
	 * @param filePosition    Position in the file to write to.
	 * @param fileDimension   Total dimensions of the output file.
	 * @param imagePosition   Position in the image where the block to be written
	 *                        starts.
	 * @param imageDimensions Dimensions of the block of the source image to write.
	 */
	public static void writeBlock(ImageI64 img, String filename, Vec3c filePosition, Vec3c fileDimensions,
			Vec3c imagePosition, Vec3c imageDimensions) throws IOException {

		Vec3c cStart = new Vec3c(filePosition);
		MathUtils.clamp(cStart, new Vec3c(0, 0, 0), fileDimensions);
		Vec3c cEnd = filePosition.add(imageDimensions);
		MathUtils.clamp(cEnd, new Vec3c(0, 0, 0), fileDimensions);

		if (!img.isInImage(imagePosition))
			throw new IllegalArgumentException("Block start position must be inside the image.");
		if (!img.isInImage(imagePosition.add(imageDimensions).sub(new Vec3c(1, 1, 1))))
			throw new IllegalArgumentException("Block end position must be inside the image.");

		FileUtils.createFoldersFor(filename);

		// Create file if it does not exist, otherwise set file size to correct value.
		final long LONG_SIZE = 8;
		long fileSize = fileDimensions.x * fileDimensions.y * fileDimensions.z * LONG_SIZE;

		// try (DataOutputStream out = new DataOutputStream(new
		// FileOutputStream(filename))) {
		try (DiskMappedWriteBuffer out = new DiskMappedWriteBuffer(filename, fileSize)) {

			for (long z = cStart.z; z < cEnd.z; z++) {
//				if (cStart.x == 0 && cEnd.x == fileDimensions.x)
//				{
//					// Writing whole scanlines.
//					// Write all scanlines in region [cStart.y, cEnd.y[ at once in order to increase write speed.
//					size_t filePos = (z * fileDimensions.x * fileDimensions.y + cStart.y * fileDimensions.x + cStart.x) * sizeof(pixel_t);
//					out.seekp(filePos);
//					
//	
//					size_t imgPos = img.getLinearIndex(imagePosition.x, imagePosition.y, z - cStart.z + imagePosition.z);
//					out.write((char*)&pBuffer[imgPos], (cEnd.x - cStart.x) * (cEnd.y - cStart.y) * sizeof(pixel_t));
//	
//					if (!out)
//					{
//						showProgress(cEnd.z - cStart.z, cEnd.z - cStart.z, showProgressInfo);
//						throw ITLException(std::string("Unable to write (fast) to ") + filename + string(", ") + getStreamErrorMessage());
//					}
//				}
//				else
//				{
//					// Writing partial scanlines.
//	
				for (long y = cStart.y; y < cEnd.y; y++) {
					for (long x = cStart.x; x < cEnd.x; x++) {
						long index = z * fileDimensions.x * fileDimensions.y + y * fileDimensions.x + x;

						Vec3c imgPos = new Vec3c(x - cStart.x + imagePosition.x, y - cStart.y + imagePosition.y,
								z - cStart.z + imagePosition.z);
						out.writeLong(index, img.get(imgPos));
					}

//						size_t filePos = (z * fileDimensions.x * fileDimensions.y + y * fileDimensions.x + cStart.x) * sizeof(pixel_t);
//						out.seekp(filePos);
//	
//						size_t imgPos = img.getLinearIndex(imagePosition.x, y - cStart.y + imagePosition.y, z - cStart.z + imagePosition.z);
//						out.write((char*)&pBuffer[imgPos], (cEnd.x - cStart.x) * sizeof(pixel_t));
//	
//						if (!out)
//						{
//							showProgress(cEnd.z - cStart.z, cEnd.z - cStart.z, showProgressInfo);
//							throw ITLException(std::string("Unable to write to ") + filename + string(", ") + getStreamErrorMessage());
//						}
				}
//				}

//				showProgress(z - cStart.z, cEnd.z - cStart.z, showProgressInfo);
			}
		}
	}

	/**
	 * Writes an image to a specified location in a .raw file. The output file is
	 * not truncated if it exists. If the output file does not exist, it is created.
	 * Part of image extending beyond [0, fileDimensions[ is not written.
	 * 
	 * @param img           Image to write.
	 * @param filename      Name of file to write.
	 * @param filePosition  Position in the file to write to.
	 * @param fileDimension Total dimensions of the output file.
	 */
	public static void writeBlock(ImageI64 img, String filename, Vec3c filePosition, Vec3c fileDimensions)
			throws IOException {
		writeBlock(img, filename, filePosition, fileDimensions, new Vec3c(0, 0, 0), img.getDimensions());
	}

	/**
	 * Reads part of a .raw file to given image. NOTE: Does not support out of
	 * bounds start position.
	 * 
	 * @param img        Image where the data is placed. The size of the image
	 *                   defines the size of the block that is read.
	 * @param filename   The name of the file to read.
	 * @param dimensions Dimensions of the whole file.
	 * @param start      Start location of the read. The size of the image defines
	 *                   the size of the block that is read.
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void readBlockNoParse(ImageI64 img, String filename, Vec3c start, Vec3c dimensions)
			throws FileNotFoundException, IOException {

		if (start.x < 0 || start.y < 0 || start.z < 0 || start.x >= dimensions.x || start.y >= dimensions.y
				|| start.z >= dimensions.z)
			throw new IllegalArgumentException("Out of bounds start position in Raw.readBlockNoParse.");

		Vec3c cStart = new Vec3c(start);
		MathUtils.clamp(cStart, new Vec3c(0, 0, 0), dimensions);
		Vec3c cEnd = start.add(img.getDimensions());
		MathUtils.clamp(cEnd, new Vec3c(0, 0, 0), dimensions);

//		if (cStart.equals(new Vec3c(0, 0, 0)) && cEnd.equals(dimensions))
//		{
//			// Reading whole file, use the whole file reading function.
//			raw::readNoParse(img, filename, bytesToSkip);
//			return;
//		}

		try (DiskMappedReadBuffer in = new DiskMappedReadBuffer(filename)) {
			for (long z = cStart.z; z < cEnd.z; z++) {
				for (long y = cStart.y; y < cEnd.y; y++) {
					for (long x = cStart.x; x < cEnd.x; x++) {
						long index = z * dimensions.x * dimensions.y + y * dimensions.x + x;
						Vec3c imagePos = new Vec3c(x - cStart.x, y - cStart.y, z - cStart.z);
						img.set(imagePos, in.readLong(index));
					}
				}
			}
		}

//		if (cStart.x == 0 && cEnd.x == dimensions.x)
//		{
//			// Reading whole scan lines.
//			// We can read one slice per one read call.
//			for (coord_t z = cStart.z; z < cEnd.z; z++)
//			{
//				size_t filePos = (z * dimensions.x * dimensions.y + cStart.y * dimensions.x + cStart.x) * sizeof(pixel_t);
//				in.seekg(filePos);
//
//				size_t imgPos = img.getLinearIndex(0, 0, z - cStart.z);
//				in.read((char*)&pBuffer[imgPos], (cEnd.x - cStart.x) * (cEnd.y - cStart.y) * sizeof(pixel_t));
//
//				if (in.bad())
//				{
//					showProgress(cEnd.z - cStart.z, cEnd.z - cStart.z, showProgressInfo);
//					throw ITLException(std::string("Failed to read (slice at a time) block of ") + filename + string(", ") + getStreamErrorMessage());
//				}
//
//				showProgress(z - cStart.z, cEnd.z - cStart.z, showProgressInfo);
//			}
//		}
//		else
//		{
//			// Reading partial scan lines.
//			for (coord_t z = cStart.z; z < cEnd.z; z++)
//			{
//				for (coord_t y = cStart.y; y < cEnd.y; y++)
//				{
//					size_t filePos = (z * dimensions.x * dimensions.y + y * dimensions.x + cStart.x) * sizeof(pixel_t);
//					in.seekg(filePos);
//
//					size_t imgPos = img.getLinearIndex(0, y - cStart.y, z - cStart.z);
//					in.read((char*)&pBuffer[imgPos], (cEnd.x - cStart.x) * sizeof(pixel_t));
//
//					if (in.bad())
//					{
//						showProgress(cEnd.z - cStart.z, cEnd.z - cStart.z, showProgressInfo);
//						throw ITLException(std::string("Failed to read block of ") + filename + string(", ") + getStreamErrorMessage());
//					}
//				}
//
//				
//				showProgress(z - cStart.z, cEnd.z - cStart.z, showProgressInfo);
//			}
//		}
	}

}
