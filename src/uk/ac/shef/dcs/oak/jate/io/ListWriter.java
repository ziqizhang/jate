package uk.ac.shef.dcs.oak.jate.io;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * An utility class which writes a list of strings to a file, with one entry on a line.
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */

public class ListWriter {

	private PrintWriter _writer;

	public ListWriter(String pathToFile) {
		try {
			_writer = new PrintWriter(new FileWriter(pathToFile));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ListWriter(String pathToFile, boolean append) {
		try {
			_writer = new PrintWriter(new FileWriter(pathToFile, append));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ListWriter(File f, boolean append) {
		try {
			_writer = new PrintWriter(new FileWriter(f, append));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void close() {
		if (_writer != null) _writer.close();
	}

	/**
	 * Write a line
	 * @param line
	 */
	public void appendLine(String line) {
		_writer.println(line);
	}


}
