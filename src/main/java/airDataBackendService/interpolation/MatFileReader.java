package airDataBackendService.interpolation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteOrder;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;


/**
 * MAT-file reader. Reads MAT-file into <code>MLArray</code> objects.
 * 
 * Usage:
 * <pre><code>
 * //read in the file
 * MatFileReader mfr = new MatFileReader( "mat_file.mat" );
 * 
 * //get array of a name "my_array" from file
 * MLArray mlArrayRetrived = mfr.getMLArray( "my_array" );
 * 
 * //or get the collection of all arrays that were stored in the file
 * Map content = mfr.getContent();
 * </pre></code>
 * 
 * @see ca.mjdsystems.jmatio.io.MatFileFilter
 * @author Wojciech Gradkowski (<a href="mailto:wgradkowski@gmail.com">wgradkowski@gmail.com</a>)
 */
/**
 * @author Wojciech Gradkowski (<a
 *         href="mailto:wgradkowski@gmail.com">wgradkowski@gmail.com</a>)
 * 
 */
public class MatFileReader
{
    public static final int MEMORY_MAPPED_FILE = 1;
    public static final int DIRECT_BYTE_BUFFER = 2;
    public static final int HEAP_BYTE_BUFFER   = 4;
    
    /* MAT-File Data Types */
    public static final int miUNKNOWN   = 0;
    public static final int miINT8      = 1;
    public static final int miUINT8     = 2;
    public static final int miINT16     = 3;
    public static final int miUINT16    = 4;
    public static final int miINT32     = 5;
    public static final int miUINT32    = 6;
    public static final int miSINGLE    = 7;
    public static final int miDOUBLE    = 9;
    public static final int miINT64     = 12;
    public static final int miUINT64    = 13;
    public static final int miMATRIX    = 14;
    public static final int miCOMPRESSED    = 15;
    public static final int miUTF8      = 16;
    public static final int miUTF16     = 17;
    public static final int miUTF32     = 18;

    public static final int miSIZE_INT64    = 8;
    public static final int miSIZE_INT32    = 4;
    public static final int miSIZE_INT16    = 2;
    public static final int miSIZE_INT8     = 1;
    public static final int miSIZE_UINT64   = 8;
    public static final int miSIZE_UINT32   = 4;
    public static final int miSIZE_UINT16   = 2;
    public static final int miSIZE_UINT8    = 1;
    public static final int miSIZE_DOUBLE   = 8;
    public static final int miSIZE_CHAR     = 1;
    
    /**
     * Return number of bytes for given type.
     * 
     * @param type - <code>MatDataTypes</code>
     * @return
     */
    public static int sizeOf(int type)
    {
        switch ( type )
        {
            case miINT8:
                return miSIZE_INT8;
            case miUINT8:
                return miSIZE_UINT8;
            case miINT16:
                return miSIZE_INT16;
            case miUINT16:
                return miSIZE_UINT16;
            case miINT32:
                return miSIZE_INT32;
            case miUINT32:
                return miSIZE_UINT32;
            case miINT64:
                return miSIZE_INT64;
            case miUINT64:
                return miSIZE_UINT64;
            case miDOUBLE:
                return miSIZE_DOUBLE;
            default:
                return 1;
        }
    }   
    
    
    /**
     * MAT-file header
     */
    private MatFileHeader matFileHeader;
    /**
     * Container for red <code>MLArray</code>s
     */
    private Map<String, MLDouble> data;
    /**
     * Tells how bytes are organized in the buffer.
     */
    private ByteOrder byteOrder;
    
    /**
     * Creates instance of <code>MatFileReader</code> and reads MAT-file 
     * from location given as <code>fileName</code>.
     * 
     * This method reads MAT-file without filtering.
     * 
     * @param fileName the MAT-file path <code>String</code>
     * @throws IOException when error occurred while processing the file.
     */
    
    
    public MatFileReader(File file) throws IOException
    {
        
        read(file);
    }
    
    public MatFileReader(InputStream str) throws IOException
    {
        
        read(str);
    }

    /**
     * Read a mat file from a stream. Internally this will read the stream fully
     * into memory before parsing it.
     * 
     * @param stream
     *            a valid MAT-file stream to be read
     * @param filter
     *            the array filter applied during reading
     * 
     * @return the same as <code>{@link #getContent()}</code>
     * @see MatFileFilter
     * @throws IOException
     *             if error occurs during file processing
     */
    public synchronized Map<String, MLDouble> read(InputStream stream) throws IOException
    {
    	data    = new LinkedHashMap<String, MLDouble>();
    	
        ByteBuffer buf = null;
        
        ByteArrayOutputStream2 baos = new ByteArrayOutputStream2();
        copy(stream, baos);
        buf = ByteBuffer.wrap(baos.getBuf(), 0, baos.getCount());

        // read in file header
        readHeader(buf);

        while (buf.remaining() > 0)
        {
            readData(buf);
        }

        return data;
    }
    
    static class ByteArrayOutputStream2 extends ByteArrayOutputStream
	{
	    public ByteArrayOutputStream2(){super();}
	    public byte[] getBuf(){return buf;}
	    public int getCount(){return count;}
	}
    
    private void copy(InputStream stream, ByteArrayOutputStream2 output) throws IOException {
        final byte[] buffer = new byte[1024 * 4];
        int n = 0;
        while (-1 != (n = stream.read(buffer))) {
            output.write(buffer, 0, n);
        }
    }
    
    /**
     * Reads the content of a MAT-file and returns the mapped content.
     * <p>
     * Because of java bug <a
     * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4724038">#4724038</a>
     * which disables releasing the memory mapped resource, additional different
     * allocation modes are available.
     * <ul>
     * <li><code>{@link #MEMORY_MAPPED_FILE}</code> - a memory mapped file</li>
     * <li><code>{@link #DIRECT_BYTE_BUFFER}</code> - a uses
     * <code>{@link ByteBuffer#allocateDirect(int)}</code> method to read in
     * the file contents</li>
     * <li><code>{@link #HEAP_BYTE_BUFFER}</code> - a uses
     * <code>{@link ByteBuffer#allocate(int)}</code> method to read in the
     * file contents</li>
     * </ul>
     * <i>Note: memory mapped file will try to invoke a nasty code to relase
     * it's resources</i>
     * 
     * @param file
     *            a valid MAT-file file to be read
     * @param filter
     *            the array filter applied during reading
     * @param policy
     *            the file memory allocation policy
     * @return the same as <code>{@link #getContent()}</code>
     * @see MatFileFilter
     * @throws IOException
     *             if error occurs during file processing
     */
 
    public synchronized Map<String, MLDouble> read(File file) throws IOException
    {
        
    	data    = new LinkedHashMap<String, MLDouble>();
        
        FileChannel roChannel = null;
        RandomAccessFile raFile = null;
        ByteBuffer buf = null;
        WeakReference<MappedByteBuffer> bufferWeakRef = null;
        try
        {
            //Create a read-only memory-mapped file
            raFile = new RandomAccessFile(file, "r");
            roChannel = raFile.getChannel();
            // until java bug #4715154 is fixed I am not using memory mapped files
            // The bug disables re-opening the memory mapped files for writing
            // or deleting until the VM stops working. In real life I need to open
            // and update files
            
                
            buf = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int)roChannel.size());        
            bufferWeakRef = new WeakReference<MappedByteBuffer>((MappedByteBuffer)buf);                    
                
            // Do the actual work.
            parseData(buf);

            return data;
        }
        catch ( IOException e )
        {
            throw e;
        }
        finally
        {
            if ( roChannel != null )
            {
                roChannel.close();
            }
            if ( raFile != null )
            {
                raFile.close();
            }
            if ( buf != null && bufferWeakRef != null )
            {
            	raFile.close();
            	roChannel.close();
            }
        }
        
    }

    private void parseData(ByteBuffer buf) throws IOException {
        //read in file header
        readHeader(buf);

        while ( buf.remaining() > 0 ) {
            readData( buf );
        }
    }

    
    /**
     * Returns the value to which the red file maps the specified array name.
     * 
     * Returns <code>null</code> if the file contains no content for this name.
     * 
     * @param - array name
     * @return - the <code>MLArray</code> to which this file maps the specified name, 
     *           or null if the file contains no content for this name.
     */
    public MLDouble getMLArray( String name )
    {
        return data.get( name );
    }
    
    
    /**
     * Reads data form byte buffer. Searches for either
     * <code>miCOMPRESSED</code> data or <code>miMATRIX</code> data.
     * 
     * Compressed data are inflated and the product is recursively passed back
     * to this same method.
     * 
     * Modifies <code>buf</code> position.
     * 
     * @param buf -
     *            input byte buffer
     * @throws IOException when error occurs while reading the buffer.
     */
    private void readData( ByteBuffer buf ) throws IOException
    {
        //read data
        ISMatTag tag = new ISMatTag(buf);
        switch ( tag.type )
        {
            
            case miMATRIX:
                
                //read in the matrix
                int pos = buf.position();
                
                MLDouble element = readMatrix( buf, true );
                
       
                if ( element != null && !data.containsKey( element.name ) )
                {
                    data.put( element.name, element );
                }
                else
                {
                    int red = buf.position() - pos;
                    int toread = tag.size - red;
                    buf.position( buf.position() + toread );
                }
                int red = buf.position() - pos;

                int toread = tag.size - red;
                
                if ( toread != 0 )
                {
                    throw new IOException("Matrix was not red fully! " + toread + " remaining in the buffer.");
                }
                break;
            default:
                throw new IOException("Incorrect data tag: " + tag);
                    
        }
    }
    /**
     * Reads miMATRIX from from input stream.
     * 
     * If reading was not finished (which is normal for filtered results)
     * returns <code>null</code>.
     * 
     * Modifies <code>buf</code> position to the position when reading
     * finished.
     * 
     * Uses recursive processing for some ML**** data types.
     * 
     * @param buf -
     *            input byte buffer
     * @param isRoot -
     *            when <code>true</code> informs that if this is a top level
     *            matrix
     * @return - <code>MLArray</code> or <code>null</code> if matrix does
     *         not match <code>filter</code>
     * @throws IOException when error occurs while reading the buffer.
     */
    private MLDouble readMatrix(ByteBuffer buf, boolean isRoot ) throws IOException
    {
        //result
    	MLDouble mlArray;
        ISMatTag tag;

        //read flags
        int[] flags = readFlags(buf);
        int attributes = ( flags.length != 0 ) ? flags[0] : 0;
        int type = attributes & 0xff;
        
        //read Array dimension
        int[] dims = readDimension(buf);
        
        //read array Name
        String name = readName(buf);
        
        
        

        //read data >> consider changing it to stategy pattern
        if(type==6)
        {
        	mlArray = new MLDouble(name, dims, type, attributes);
            //read real
            tag = new ISMatTag(buf);
            tag.readToByteBuffer( ((MLDouble) mlArray).getRealByteBuffer(),
                                        (MLDouble) mlArray );
        }
        else
        {
        	throw new IOException("Incorrect matlab array class");
        }
        
        return mlArray;
    }
    /**
     * Converts byte array to <code>String</code>. 
     * 
     * It assumes that String ends with \0 value.
     * 
     * @param bytes byte array containing the string.
     * @return String retrieved from byte array.
     * @throws IOException if reading error occurred.
     */
    private String zeroEndByteArrayToString(byte[] bytes) throws IOException
    {
        int i = 0;
        
        for ( i = 0; i < bytes.length && bytes[i] != 0; i++ );
        
        return new String( bytes, 0, i );
        
    }
    /**
     * Reads Matrix flags.
     * 
     * Modifies <code>buf</code> position.
     * 
     * @param buf <code>ByteBuffer</code>
     * @return flags int array
     * @throws IOException if reading from buffer fails
     */
    private int[] readFlags(ByteBuffer buf) throws IOException
    {
        ISMatTag tag = new ISMatTag(buf);
        
        int[] flags = tag.readToIntArray();
        
        return flags;
    }
    /**
     * Reads Matrix dimensions.
     * 
     * Modifies <code>buf</code> position.
     * 
     * @param buf <code>ByteBuffer</code>
     * @return dimensions int array
     * @throws IOException if reading from buffer fails
     */
    private int[] readDimension(ByteBuffer buf ) throws IOException
    {
        
        ISMatTag tag = new ISMatTag(buf);
        int[] dims = tag.readToIntArray();
        return dims;
        
    }
    /**
     * Reads Matrix name.
     * 
     * Modifies <code>buf</code> position.
     * 
     * @param buf <code>ByteBuffer</code>
     * @return name <code>String</code>
     * @throws IOException if reading from buffer fails
     */
    private String readName(ByteBuffer buf) throws IOException
    {
        ISMatTag tag = new ISMatTag(buf);

        return tag.readToString();
    }
    /**
     * Reads MAT-file header.
     * 
     * Modifies <code>buf</code> position.
     * 
     * @param buf
     *            <code>ByteBuffer</code>
     * @throws IOException
     *             if reading from buffer fails or if this is not a valid
     *             MAT-file
     */
    private void readHeader(ByteBuffer buf) throws IOException
    {
        //header values
        String description;
        int version;
        byte[] endianIndicator = new byte[2];

        // This part of the header is missing if the file isn't a regular mat file.  So ignore.
        
        //descriptive text 116 bytes
        byte[] descriptionBuffer = new byte[116];
        buf.get(descriptionBuffer);

        description = zeroEndByteArrayToString(descriptionBuffer);

        if (!description.matches("MATLAB 5.0 MAT-file.*")) {
            throw new IOException("This is not a valid MATLAB 5.0 MAT-file.");
        }

        //subsyst data offset 8 bytes
        buf.position(buf.position() + 8);
        
        
        byte[] bversion = new byte[2];
        //version 2 bytes
        buf.get(bversion);
        
        //endian indicator 2 bytes
        buf.get(endianIndicator);
        
        //program reading the MAT-file must perform byte swapping to interpret the data
        //in the MAT-file correctly
        if ( (char)endianIndicator[0] == 'I' && (char)endianIndicator[1] == 'M')
        {
            byteOrder = ByteOrder.LITTLE_ENDIAN;
            version = bversion[1] & 0xff | bversion[0] << 8;
        }
        else
        {
            byteOrder = ByteOrder.BIG_ENDIAN;
            version = bversion[0] & 0xff | bversion[1] << 8;
        }
        
        buf.order( byteOrder );
        
        matFileHeader = new MatFileHeader(description, version, endianIndicator, byteOrder);

        // After the header, the next read must be aligned.  Thus force the alignment.  Only matters with reduced header data,
        // but apply it regardless for safety.
        buf.position((buf.position() + 7) & 0xfffffff8);
    }
    /**
     * TAG operator. Facilitates reading operations.
     * 
     * <i>Note: reading from buffer modifies it's position</i>
     * 
     * @author Wojciech Gradkowski (<a href="mailto:wgradkowski@gmail.com">wgradkowski@gmail.com</a>)
     */
    private static class ISMatTag 
    {
    	private int type;
    	private int size;
        private final MatFileInputStream mfis;
        private final int padding;
		private final boolean compressed;
        
        public ISMatTag(ByteBuffer buf) throws IOException
        {
            
            type=0;
            size = 0;
            
            int tmp = buf.getInt();
            
            type = tmp;
            size = buf.getInt();
            compressed = false;
            
            padding = getPadding(size, compressed);
            mfis = new MatFileInputStream(buf, type);
        } 
        
        /**
         * Calculate padding
         */
        private int getPadding(int size, boolean compressed)
        {
            int padding;
            //data not packed in the tag
            if ( !compressed )
            {    
                int b;
                padding = ( b = ( ((size/sizeOf())%(8/sizeOf()))*sizeOf() ) ) !=0   ? 8-b : 0;
            }
            else //data _packed_ in the tag (compressed)
            {
                int b;
                padding = ( b = ( ((size/sizeOf())%(4/sizeOf()))*sizeOf() ) ) !=0   ? 4-b : 0;
            }
            return padding;
        }
        
        /**
         * Get size of single data in this tag.
         * 
         * @return - number of bytes for single data
         */
        public int sizeOf()
        {
            return MatFileReader.sizeOf(type);
        }
        
        public void readToByteBuffer( ByteBuffer buff, MLDouble mlArray ) throws IOException
        {
            int elements = size/sizeOf();
            mfis.readToByteBuffer( buff, elements, mlArray );
            mfis.skip( padding );
        }
        public byte[] readToByteArray() throws IOException
        {
            //allocate memory for array elements
            int elements = size/sizeOf();
            byte[] ab = new byte[elements];
            

            for ( int i = 0; i < elements; i++ )
            {
                ab[i] = mfis.readByte();
            }
            
            //skip padding
            mfis.skip( padding );

            return ab;
        }
        

        public int[] readToIntArray() throws IOException
        {
            //allocate memory for array elements
            int elements = size/sizeOf();
            int[] ai = new int[elements];
            
            for ( int i = 0; i < elements; i++ )
            {
                ai[i] = mfis.readInt();
            }
            
            //skip padding
            mfis.skip( padding );
            return ai;
        }
        public String readToString() throws IOException
        {
            //
            byte[] bytes = readToByteArray();
            
            return new String( bytes, "UTF-8" );
        	
        }
        
        class MatFileInputStream
        {
            private final int type;
            private final ByteBuffer buf;
            
            /**
             * Attach MAT-file input stream to <code>InputStream</code>
             * 
             * @param is - input stream
             * @param type - type of data in the stream
             * @see ca.mjdsystems.jmatio.common.MatDataTypes
             */
            public MatFileInputStream( ByteBuffer buf, int type )
            {
                this.type = type;
                this.buf = buf;
            }
            
            /**
             * Reads data (number of bytes red is determined by <i>data type</i>)
             * from the stream to <code>int</code>.
             * 
             * @return
             * @throws IOException
             */
            public int readInt() throws IOException
            {
                switch ( type )
                {
                    case miUINT8:
                        return (int)( buf.get() & 0xFF);
                    case miINT8:
                        return (int) buf.get();
                    case miUINT16:
                        return (int)( buf.getShort() & 0xFFFF);
                    case miINT16:
                        return (int) buf.getShort();
                    case miUINT32:
                        return (int)( buf.getInt() & 0xFFFFFFFF);
                    case miINT32:
                        return (int) buf.getInt();
                    case miUINT64:
                        return (int) buf.getLong();
                    case miINT64:
                        return (int) buf.getLong();
                    case miDOUBLE:
                        return (int) buf.getDouble();
                    default:
                        throw new IllegalArgumentException("Unknown data type: " + type);
                }
            }
            
            /**
             * Reads data (number of bytes red is determined by <i>data type</i>)
             * from the stream to <code>double</code>.
             * 
             * @return - double
             * @throws IOException
             */
            public double readDouble() throws IOException
            {
                switch ( type )
                {
                    case miUINT8:
                        return (double)( buf.get() & 0xFF);
                    case miINT8:
                        return (double) buf.get();
                    case miUINT16:
                        return (double)( buf.getShort() & 0xFFFF);
                    case miINT16:
                        return (double) buf.getShort();
                    case miUINT32:
                        return (double)( buf.getInt() & 0xFFFFFFFF);
                    case miINT32:
                        return (double) buf.getInt();
                    case miDOUBLE:
                        return (double) buf.getDouble();
                    default:
                        throw new IllegalArgumentException("Unknown data type: " + type);
                }
            }

            public byte readByte()
            {
                switch ( type )
                {
                    case miUINT8:
                        return (byte)( buf.get() & 0xFF);
                    case miINT8:
                        return (byte) buf.get();
                    case miUINT16:
                        return (byte)( buf.getShort() & 0xFFFF);
                    case miINT16:
                        return (byte) buf.getShort();
                    case miUINT32:
                        return (byte)( buf.getInt() & 0xFFFFFFFF);
                    case miINT32:
                        return (byte) buf.getInt();
                    case miDOUBLE:
                        return (byte) buf.getDouble();
                    case miUTF8:
                        return (byte) buf.get();
                    default:
                        throw new IllegalArgumentException("Unknown data type: " + type);
                }
            }

            /**
             * Reads the data into a <code>{@link ByteBuffer}</code>. This method is
             * only supported for arrays with backing ByteBuffer (<code>{@link ByteStorageSupport}</code>).
             * 
             * @param dest
             *            the destination <code>{@link ByteBuffer}</code>
             * @param elements
             *            the number of elements to read into a buffer
             * @param mlArray
             *            the backing <code>{@link ByteStorageSupport}</code> that
             *            gives information how data should be interpreted
             * @return reference to the destination <code>{@link ByteBuffer}</code>
             * @throws IOException
             *             if buffer is under-fed, or another IO problem occurs
             */
            public ByteBuffer readToByteBuffer(ByteBuffer dest, int elements,
                            MLDouble mlArray) throws IOException
            {
                
                int bytesAllocated = mlArray.getBytesAllocated();
                int size = elements * mlArray.getBytesAllocated();
                
                //direct buffer copy
                if ( MatFileReader.sizeOf(type) == bytesAllocated && buf.order().equals(dest.order()) )
                {
                    int bufMaxSize = 1024;
                    int bufSize = Math.min(buf.remaining(), bufMaxSize);
                    int bufPos = buf.position();
                    
                    byte[] tmp = new byte[ bufSize ];
                    
                    while ( dest.remaining() > 0 )
                    {
                        int length = Math.min(dest.remaining(), tmp.length);
                        buf.get( tmp, 0, length );
                        dest.put( tmp, 0, length );
                    }
                    buf.position( bufPos + size );
                }
                else
                {
                    //because Matlab writes data not respectively to the declared
                    //matrix type, the reading is not straight forward (as above)
                    Class<?> clazz = mlArray.getStorageClazz();
                    while ( dest.remaining() > 0 )
                    {
                        if ( clazz.equals( Double.class) )
                        {
                            dest.putDouble( readDouble() );
                        }
                        else if ( clazz.equals( Byte.class) )
                        {
                            dest.put( readByte() );
                        }
                        else if ( clazz.equals( Integer.class) )
                        {
                            dest.putInt( readInt() );
                        }
                        else
                        {
                            throw new RuntimeException("Not supported buffer reader for " + clazz );
                        }
                    }
                }
                dest.rewind();
                return dest;
            }

            
            
            

        	public void skip(int padding) 
        	{
        		buf.position( buf.position() + padding );
        	}
        }
    }
    
    public class MatFileHeader
    {
        
        private final ByteOrder byteOrder;

        private int version;
        private String description;
        private byte[] endianIndicator;
        
        /**
         * New MAT-file header
         * 
         * @param description - descriptive text (no longer than 116 characters)
         * @param version - by default is set to 0x0100
         * @param endianIndicator - byte array size of 2 indicating byte-swapping requirement
         */
        public MatFileHeader(String description, int version, byte[] endianIndicator, ByteOrder byteOrder)
        {
            this.description = description;
            this.version = version;
            this.endianIndicator = endianIndicator;
            this.byteOrder = byteOrder;
        }
        
        /**
         * Gets descriptive text
         * 
         * @return
         */
        public String getDescription()
        {
            return description;
        }
        /**
         * Gets endian indicator. Bytes written as "MI" suggest that byte-swapping operation is required
         * in order to interpret data correctly. If value is set to "IM" byte-swapping is not needed.
         * 
         * @return - a byte array size of 2
         */
        public byte[] getEndianIndicator()
        {
            return endianIndicator;
        }
        /**
         * When creating a MAT-file, set version to 0x0100
         * 
         * @return
         */
        public int getVersion()
        {
            return version;
        }
        
        
        

        public ByteOrder getByteOrder()
        {
            assert( (byteOrder != ByteOrder.LITTLE_ENDIAN || endianIndicator[0] == 'I') && (byteOrder != ByteOrder.BIG_ENDIAN || endianIndicator[0] == 'M') );
            return byteOrder;
        }
    }
    
    public class MLDouble 
    {
    	 /* Matlab Array Types (Classes) */
        
        protected int dims[];
        public String name;
        protected int attributes;
        protected int type;
        
        
    	private ByteBuffer real;
        /** The buffer for creating Number from bytes */
        private byte[] bytes;
    	
        /**
         * Normally this constructor is used only by MatFileReader and MatFileWriter
         * 
         * @param name - array name
         * @param dims - array dimensions
         * @param type - array type: here <code>mxDOUBLE_CLASS</code>
         * @param attributes - array flags
         */
        public MLDouble( String name, int[] dims, int type, int attributes )
        {
        	this.dims = new int[dims.length];
            System.arraycopy(dims, 0, this.dims, 0, dims.length);
            
            
            if ( name != null && !name.equals("") )
            {
                this.name = name;
            }
            else
            {
                this.name = "@"; //default name
            }
            
            
            this.type = type;
            this.attributes = attributes;
            allocate();
        }
        
        public void allocate( )
        {
            real = ByteBuffer.allocate( getSize()*getBytesAllocated());
            
            bytes = new byte[ getBytesAllocated() ];
        }
        
        /* (non-Javadoc)
         * @see ca.mjdsystems.jmatio.types.GenericArrayCreator#createArray(int, int)
         */
        public Double[] createArray(int m, int n)
        {
            return new Double[m*n];
        }
        /**
         * Gets two-dimensional real array.
         * 
         * @return - 2D real array
         */
        public double[][] getArray()
        {
            double[][] result = new double[getM()][];
            
            for ( int m = 0; m < getM(); m++ )
            {
               result[m] = new double[ getN() ];

               for ( int n = 0; n < getN(); n++ )
               {               
                   result[m][n] = getReal(m,n);
               }
            }
            return result;
        }
        /**
         * Casts <code>Double[]</code> to <code>double[]</code>
         * 
         * @param - source <code>Double[]</code>
         * @return - result <code>double[]</code>
         */
        /**
         * Gets single real array element of A(m,n).
         * 
         * @param m - row index
         * @param n - column index
         * @return - array element
         */
        public double getReal(int m, int n)
        {
            return getReal( getIndex(m,n) );
        }
        
        public double getReal( int index )
        {
            return _get(real, index);
        }
        
        private double _get( ByteBuffer buffer, int index )
        {
            buffer.position( getByteOffset(index) );
            buffer.get( bytes, 0, bytes.length );
            return buldFromBytes( bytes );
        }
        
        private int getByteOffset( int index )
        {
            return index*getBytesAllocated();
        }
        
        public ByteBuffer getRealByteBuffer()
        {
            return real;
        }
        
        
        
        
        public int getM()
        {
            int i = 0;
            if( dims != null )
            {
                i = dims[0];
            }
            return i;
        }

        public int getN()
        {
            int i = 0;
            if(dims != null)
            {
                if(dims.length > 2)
                {
                    i = 1;
                    for(int j = 1; j < dims.length; j++)
                    {
                        i *= dims[j];
                    }
                } 
                else
                {
                    i = dims[1];
                }
            }
            return i;
        }
        
        public int getSize()
        {
            return getM()*getN();
        }
        
        protected int getIndex(int m, int n)
        {
            return m+n*getM();
        }
        
        
        
        
        
        public int getBytesAllocated()
        {
            return Double.SIZE >> 3;
        }
        public Double buldFromBytes(byte[] bytes)
        {
            if ( bytes.length != getBytesAllocated() )
            {
                throw new IllegalArgumentException( 
                            "To build from byte array I need array of size: " 
                                    + getBytesAllocated() );
            }
            return ByteBuffer.wrap( bytes ).getDouble();
            
        }
        public byte[] getByteArray(Double value)
        {
            int byteAllocated = getBytesAllocated();
            ByteBuffer buff = ByteBuffer.allocate( byteAllocated );
            buff.putDouble( value );
            return buff.array();
        }
        
        public Class<Double> getStorageClazz()
        {
            return Double.class;
        }
    }
    
}