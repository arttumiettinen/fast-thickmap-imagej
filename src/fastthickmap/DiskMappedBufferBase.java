package fastthickmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;

/**
 * Memory-maps a file (of almost any size) and provides access to the file as an array.
 * The file size limit is 2^31 * MAPPING_SIZE bytes = 2^31 * 2^31 bytes =  2^62 bytes (enough for now).
 * This class is inspired by
 * https://vanillajava.blogspot.com/2011/12/using-memory-mapped-file-for-huge.html
 * 
 * 
 * @author miettinen_a
 *
 */
public abstract class DiskMappedBufferBase implements AutoCloseable {

	/**
	 * The file is mapped in chunks of this size. This size must be less than 2 gigabytes.
	 * NOTE: The MAPPING_SIZE must be divisible by sizeof(short). If accessor methods are added for other
	 * types than short, then MAPPING_SIZE must be divisible by size of all the types. 
	 */
	private static final int MAPPING_SIZE = 1 << 30;
	
	private RandomAccessFile rafile;
	
	private ArrayList<MappedByteBuffer> buffers;
	
	/**
	 * Constructor
	 * @param filename
	 * @param readOnly Set to true to open the file in read only mode.
	 * @param length Only used if readOnly is false. If non-negative, the size of the file is set to this value.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public DiskMappedBufferBase(String filename, boolean readOnly, long length) throws FileNotFoundException, IOException {
		File file = new File(filename);
		
		if(readOnly)
		{
			rafile = new RandomAccessFile(file, "r");
		}
		else
		{
			rafile = new RandomAccessFile(file, "rw");
			if(length >= 0)
				rafile.setLength(length);
		}

		buffers = new ArrayList<MappedByteBuffer>();

		try
		{
			long size = file.length();
			for(long start = 0; start < size; start += MAPPING_SIZE)
			{
				long realSize = Math.min(size - start, MAPPING_SIZE);
				MappedByteBuffer buffer = rafile.getChannel().map(readOnly ? MapMode.READ_ONLY : MapMode.READ_WRITE, start, realSize);
				buffers.add(buffer);
			}
		
		}
		catch(IOException e)
		{
			close();
			throw e;
		}
	}
	
	/**
	 * Calculates index of buffer where the byte in given byte position is stored.
	 * @param byteIndex
	 * @return
	 */
	protected int getBufferIndex(long byteIndex) {
		return (int) (byteIndex / MAPPING_SIZE);
	}

	/**
	 * Calculates index in buffer where the byte in given byte position is stored.
	 * @param byteIndex
	 * @return
	 */
	protected int getOffset(long byteIndex) {
		return (int) (byteIndex % MAPPING_SIZE);
	}
	
	/**
	 * Gets buffer where the given byte resides.
	 * @param byteIndex
	 * @return
	 */
	protected MappedByteBuffer getBuffer(long byteIndex) {
		return buffers.get(getBufferIndex(byteIndex));
	}
	
	/**
	 * Reads short from file. The read is made from byte position index * 2 = index * sizeof(short). 
	 * @param index
	 * @return
	 */
	public short readShort(long index) {
		long byteIndex = index * 2;
		return getBuffer(byteIndex).getShort(getOffset(byteIndex));
	}
	
	/**
	 * Reads long from file. The read is made from byte position index * 8 = index * sizeof(long).
	 * @param index
	 * @return
	 */
	public long readLong(long index) {
		long byteIndex = index * 8;
		return getBuffer(byteIndex).getLong(getOffset(byteIndex));
	}
	
	@Override
	public void close() throws IOException {
        buffers.clear();
        rafile.close();
        
        // NOTE: The mappings remain open until they get garbage collected.
        // Let's try to induce collection here.
        System.gc();
    }
}
