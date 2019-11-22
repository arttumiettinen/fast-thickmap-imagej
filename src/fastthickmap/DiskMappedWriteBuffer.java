package fastthickmap;

import java.io.FileNotFoundException;
import java.io.IOException;

public class DiskMappedWriteBuffer extends DiskMappedBufferBase {

	public DiskMappedWriteBuffer(String filename, long length) throws FileNotFoundException, IOException {
		super(filename, false, length);
	}
	
	/**
	 * Writes long to file. The write is made to byte position index * 8 = index * sizeof(long).
	 * @param index
	 * @return
	 */
	public void writeLong(long index, long value) {
		long byteIndex = index * 8;
		getBuffer(byteIndex).putLong(getOffset(byteIndex), value);
	}

}
