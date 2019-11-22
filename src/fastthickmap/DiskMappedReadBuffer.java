package fastthickmap;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Disk mapped buffer that provides read-only access to the underlying file.
 * @author miettinen_a
 *
 */
public class DiskMappedReadBuffer extends DiskMappedBufferBase {

	public DiskMappedReadBuffer(String filename) throws FileNotFoundException, IOException {
		super(filename, true, 0);
	}

}
