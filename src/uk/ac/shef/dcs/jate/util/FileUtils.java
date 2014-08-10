package uk.ac.shef.dcs.jate.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

/**
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 *         <p>
 *         Loads files in a directory recursively
 *         </p>
 */

public class FileUtils {

    private static final int BUFFER_SIZE = 2 << 13;
    public static final char[] _readBuffer = new char[BUFFER_SIZE];

    /**
     * <p>Reads content from an URL into a string buffer.</p>
     *
     * @param url the url to get the content from.
     * @return string buffer with the contents of the file.
     */
    public static StringBuilder getContent(final URL url) throws IOException {
        final StringBuilder buffer = new StringBuilder(BUFFER_SIZE);
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(url.openStream());
            for (int numRead = 0; numRead >= 0;) {
                int offset = 0;
                for (; offset < _readBuffer.length && (numRead = reader.read(_readBuffer, offset, _readBuffer.length - offset)) >= 0; offset += numRead) ;
                buffer.append(_readBuffer, 0, offset);
            }
        } finally {
            if (reader != null) reader.close();
        }
        buffer.trimToSize();
        return buffer;
    }

	/**
	 * Load files recursively from a directory
	 *
	 * @param dir
	 * @return
	 * @throws FileNotFoundException
	 */
	public static List<File> getFileRecursive(File dir) throws FileNotFoundException {
		List<File> result = new ArrayList<File>();

		File[] filesAndDirs = dir.listFiles();
		List filesDirs = Arrays.asList(filesAndDirs);
		Iterator filesIter = filesDirs.iterator();
		File file;
		while (filesIter.hasNext()) {
			file = (File) filesIter.next();
			if (file.isFile()) result.add(file); //always add, even if directory
			else {
				List<File> deeperList = getFileRecursive(file);
				result.addAll(deeperList);
			}

		}
		return result;
	}

	/**
	 * Load directories recursively from a directory
	 *
	 * @param dir
	 * @return
	 * @throws FileNotFoundException
	 */
	public static List<File> getFolderRecursive(File dir) throws FileNotFoundException {
		Set<File> result = new HashSet<File>();

		File[] filesAndDirs = dir.listFiles();
		List filesDirs = Arrays.asList(filesAndDirs);
		Iterator filesIter = filesDirs.iterator();
		File file;
		while (filesIter.hasNext()) {
			file = (File) filesIter.next();
			if (file.isDirectory()) {
				result.add(file); //always add, even if directory
				List<File> deeperList = getFolderRecursive(file);
				result.addAll(deeperList);
			}

		}
		result.add(dir);

		List<File> toreturn = new ArrayList<File>(result);
		Collections.sort(toreturn);
		return toreturn;
	}
}
