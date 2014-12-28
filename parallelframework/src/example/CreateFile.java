package example;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class CreateFile {

	 
	    public static long KBSIZE = 1024;
	    public static long MBSIZE1 = 1024 * 1024;
	    public static long MBSIZE10 = 1024 * 1024 * 10;
	 
	    public static boolean createFile(File file, long fileLength) {
	        FileOutputStream fos = null;
	        try {
	 
	            if (!file.exists()) {
	                file.createNewFile();
	            }
	 
	            long batchSize = 0;
	            batchSize = fileLength;
	            if (fileLength > KBSIZE) {
	                batchSize = KBSIZE;
	            }
	            if (fileLength > MBSIZE1) {
	                batchSize = MBSIZE1;
	            }
	            if (fileLength > MBSIZE10) {
	                batchSize = MBSIZE10;
	            }
	            long count = fileLength / batchSize;
	            long last = fileLength % batchSize;
	 
	 
	            fos = new FileOutputStream(file);
	            FileChannel fileChannel = fos.getChannel();
	            for (int i = 0; i < count; i++) {
	                ByteBuffer buffer = ByteBuffer.allocate((int) batchSize);
	                fileChannel.write(buffer);
	            }
	            ByteBuffer buffer = ByteBuffer.allocate((int) last);
	            fileChannel.write(buffer);
	            fos.close();
	            return true;
	        } catch (IOException e) {
	            e.printStackTrace();
	        } finally {
	            try {
	                if (fos != null) {
	                    fos.close();
	                }
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	        return false;
	    }
}
