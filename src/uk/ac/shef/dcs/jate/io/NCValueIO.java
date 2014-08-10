package uk.ac.shef.dcs.jate.io;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import uk.ac.shef.dcs.jate.model.NCTermwithOriginals;
import uk.ac.shef.dcs.jate.model.Term;

public class NCValueIO{
	public static boolean OutputFileExists(String fname, String path)
	 {
		// int indx = path.lastIndexOf( File.separator );
		 
		 File directory = new File( path );
		 boolean found = false;
		 for( File f : directory.listFiles() )
		 {
		    if( f.getName().equals( fname ) )
		    {
		        found = true;
		        break;
		    }
		 } 
		 return found;
	 }
	
	public static Term[] readFile(String filename)
	{
		Set<NCTermwithOriginals> result = new HashSet<NCTermwithOriginals>();
		
		try{
			  // Open the file that is the first 
			  // command line parameter
			  FileInputStream fstream = new FileInputStream(filename);
			  // Get the object of DataInputStream
			  DataInputStream in = new DataInputStream(fstream);
			  BufferedReader br = new BufferedReader(new InputStreamReader(in));
			  String strLine;
			  //Read File Line By Line
			  while ((strLine = br.readLine()) != null)   {
			  //Parse the sentence using tab as a delimiter.
				  String[] splitted_parts = strLine.split("\t\t\t");
				  if(splitted_parts.length==2)
				  {
					  
					  String[] variants = splitted_parts[0].trim().split("\\|");
					  String lemma = variants[0].trim();
					  if(variants.length<2)
					  {
						  result.add(new NCTermwithOriginals(lemma, null ,Double.parseDouble(splitted_parts[1].trim())));
					  }
					  else
					  {
						  Set<String> originals = new HashSet<String>();
						  originals.addAll(Arrays.asList(Arrays.copyOfRange(variants, 1, variants.length)));
						  
						  
						  //NCTermwithOriginals term = new NCTermwithOriginals(variants[0].trim(), originals ,Double.parseDouble(splitted_parts[1].trim()));
						  result.add(new NCTermwithOriginals(variants[0].trim(), originals ,Double.parseDouble(splitted_parts[1].trim())));
						  
						  result.size();
					  }
					  
						  
					  
				  }
				  else
				  {
					  System.out.println("something more than term and confidence is available...invalid.");
				  }
			  
			  }
			  //Close the input stream
			  in.close();
		}catch (Exception e){//Catch exception if any
			  System.err.println("Error: " + e.getMessage());
		}
		Term[] all = result.toArray(new Term[0]);
		return all;		
	}
	
	
}