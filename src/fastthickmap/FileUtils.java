package fastthickmap;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class FileUtils {

	/**
	 * Creates folders in the path of a given file if they don't exist. Does not
	 * create the file, only directories.
	 * 
	 * @throws IOException
	 */
	public static void createFoldersFor(String filename) throws IOException {
		Path p = Paths.get(filename);
		Files.createDirectories(p.getParent());
	}

	/**
	 * Builds list of files that corresponds to the given template.
	 * @param template
	 * @return
	 */
	public static ArrayList<Path> buildFileList(Path directory, String glob) throws IOException {
		ArrayList<Path> items = new ArrayList<Path>();
		
		try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory, glob)) {
			dirStream.forEach(pth -> items.add(pth));
		}
		
		return items;
	}

	/**
	 * Tries to delete file identified by given path.
	 * @param p Path to the file to delete.
	 * @return True if the file could be deleted, false otherwise.
	 */
	public static boolean tryDelete(Path p) {
		try {
			Files.delete(p);
			return true;
		} catch (IOException e) {
			return false; // Could not delete the file.
		}
	}

}
