/*
 *                    BioJava development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the individual
 * authors.  These should be listed in @author doc comments.
 *
 * For more information on the BioJava project and its aims,
 * or to join the biojava-l mailing list, visit the home page
 * at:
 *
 *      http://www.biojava.org/
 *
 * created at Oct 18, 2008
 */
package org.biojava.bio.structure.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.biojava.bio.structure.Structure;
import org.biojava.bio.structure.align.util.UserConfiguration;
import org.biojava.bio.structure.io.StructureIOFile;
import org.biojava.bio.structure.io.mmcif.MMcifParser;
import org.biojava.bio.structure.io.mmcif.SimpleMMcifConsumer;
import org.biojava.bio.structure.io.mmcif.SimpleMMcifParser;
import org.biojava.bio.structure.io.util.FileDownloadUtils;
import org.biojava3.core.util.InputStreamProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** How to parse an mmCif file:
 * <pre>
  public static void main(String[] args) throws Exception {
        String filename =  "/path/to/something.cif.gz" ;

        StructureIOFile reader = new MMCIFFileReader();

        Structure struc = reader.getStructure(filename);
        System.out.println(struc);
    }
    </pre>
 *
 * @author Andreas Prlic
 * @since 1.7
 *
 */
public class MMCIFFileReader implements StructureIOFile {

	private static final Logger logger = LoggerFactory.getLogger(MMCIFFileReader.class);

	public static final String lineSplit = System.getProperty("file.separator");

	public static final String LOCAL_MMCIF_SPLIT_DIR    = "data"+lineSplit+"structures"+lineSplit+"divided" +lineSplit+"mmCIF";
	public static final String LOCAL_MMCIF_ALL_DIR      = "data"+lineSplit+"structures"+lineSplit+"all"     +lineSplit+"mmCIF";
	public static final String LOCAL_MMCIF_OBSOLETE_DIR = "data"+lineSplit+"structures"+lineSplit+"obsolete"+lineSplit+"mmCIF";

	private static final String CURRENT_FILES_PATH  = "/pub/pdb/data/structures/divided/mmCIF/";
	//private static final String OBSOLETE_FILES_PATH = "/pub/pdb/data/structures/obsolete/mmCIF/";


	private File path;
	private List<String> extensions;
	private boolean autoFetch;
	private boolean pdbDirectorySplit;

	private String serverName;

	private FileParsingParameters params;
	private SimpleMMcifConsumer consumer;

	public static void main(String[] args) throws Exception {

		MMCIFFileReader reader = new MMCIFFileReader();
		FileParsingParameters params = new FileParsingParameters();
		reader.setFileParsingParameters(params);


		Structure struc = reader.getStructureById("1m4x");
		System.out.println(struc);
		System.out.println(struc.toPDB());


	}

	/**
	 * Constructs a new MMCIFFileReader, initializing the extensions member variable.
	 * The path is initialized in the same way as {@link UserConfiguration}, 
	 * i.e. to system property/environment variable {@link UserConfiguration#PDB_DIR}.
	 * Both autoFetch and splitDir are initialized to false
	 */
	public MMCIFFileReader(){
		this(null);
	}

	/**
	 * Constructs a new PDBFileReader, initializing the extensions member variable.
	 * The path is initialized to the given path, both autoFetch and splitDir are initialized to false.
	 */
	public MMCIFFileReader(String path){
		extensions    = new ArrayList<String>();
		extensions.add(".cif");
		extensions.add(".mmcif");
		extensions.add(".cif.gz");
		extensions.add(".mmcif.gz");

		autoFetch     = false;		
		pdbDirectorySplit = false;

		params = new FileParsingParameters();

		if( path == null) {
			UserConfiguration config = new UserConfiguration();
			path = config.getPdbFilePath();
			logger.debug("Initialising from system property/environment variable to path: {}", path.toString());
		} else {
			path = FileDownloadUtils.expandUserHome(path);
			logger.debug("Initialising with path {}", path.toString());
		}
		this.path = new File(path);

		this.serverName = System.getProperty(PDBFileReader.PDB_FILE_SERVER_PROPERTY);

		if ( serverName == null || serverName.trim().isEmpty()) {
			serverName = PDBFileReader.DEFAULT_PDB_FILE_SERVER;
			logger.debug("Using default PDB file server {}",serverName);
		} else {
			logger.info("Using PDB file server {} read from system property {}",serverName,PDBFileReader.PDB_FILE_SERVER_PROPERTY);
		}


	}

	@Override
	public void addExtension(String ext) {
		extensions.add(ext);

	}

	@Override
	public void clearExtensions(){
		extensions.clear();
	}

	/** Opens filename, parses it and returns
	 * a Structure object .
	 * @param filename  a String
	 * @return the Structure object
	 * @throws IOException ...
	 */
	@Override
	public Structure getStructure(String filename)
			throws IOException
	{
		File f = new File(FileDownloadUtils.expandUserHome(filename));
		return getStructure(f);

	}

	/** Opens filename, parses it and returns a Structure object.
	 *
	 * @param filename a File object
	 * @return the Structure object
	 * @throws IOException ...
	 */
	@Override
	public Structure getStructure(File filename) throws IOException {

		InputStreamProvider isp = new InputStreamProvider();

		InputStream inStream = isp.getInputStream(filename);

		return parseFromInputStream(inStream);
	}



	public Structure parseFromInputStream(InputStream inStream) throws IOException{

		MMcifParser parser = new SimpleMMcifParser();

		consumer = new SimpleMMcifConsumer();

		consumer.setFileParsingParameters(params);


		// The Consumer builds up the BioJava - structure object.
		// you could also hook in your own and build up you own data model.
		parser.addMMcifConsumer(consumer);

		parser.parse(new BufferedReader(new InputStreamReader(inStream)));


		// now get the protein structure.
		Structure cifStructure = consumer.getStructure();

		return cifStructure;
	}

	@Override
	public void setPath(String path) {
		this.path = new File(FileDownloadUtils.expandUserHome(path));

	}


	@Override
	public String getPath() {
		return path.toString();
	}

	/** Get a structure by PDB code. This works if a PATH has been set via setPath, or if setAutoFetch has been set to true.
	 *
	 * @param pdbId a 4 letter PDB code.
	 */
	@Override
	public Structure getStructureById(String pdbId) throws IOException {
		InputStream inStream = getInputStream(pdbId);

		return parseFromInputStream(inStream);
	}

	private InputStream getInputStream(String pdbId) throws IOException{

		if ( pdbId.length() < 4)
			throw new IOException("The provided ID does not look like a PDB ID : " + pdbId);

		InputStream inputStream =null;

		String pdbFile = null ;
		File f = null ;


		File dir = getDir(pdbId);

		// this are the possible PDB file names...
		String fpath = new File(dir,pdbId).toString();
		String ppath = new File(dir,"pdb"+pdbId).toString();

		String[] paths = new String[]{fpath,ppath};

		for ( int p=0;p<paths.length;p++ ){
			String testpath = paths[p];
			//System.out.println(testpath);
			for (int i=0 ; i<extensions.size();i++){
				String ex = extensions.get(i) ;
				//System.out.println("PDBFileReader testing: "+testpath+ex);
				f = new File(testpath+ex) ;

				if ( f.exists()) {
					//System.out.println("found!");
					pdbFile = testpath+ex ;

					if ( params.isUpdateRemediatedFiles()){
						long lastModified = f.lastModified();

						if (lastModified < PDBFileReader.lastRemediationDate) {
							// the file is too old, replace with newer version
							logger.warn("Replacing file " + pdbFile +" with latest remediated file from PDB.");
							pdbFile = null;

							return null;
						}
					}


					InputStreamProvider isp = new InputStreamProvider();

					inputStream = isp.getInputStream(pdbFile);
					break;
				}

				if ( pdbFile != null) break;
			}
		}

		if ( pdbFile == null ) {
			if ( autoFetch)
				return downloadAndGetInputStream(pdbId);

			String message = "no structure with PDB code " + pdbId + " found!" ;
			throw new IOException (message);
		}

		return inputStream ;
	}


	private InputStream downloadAndGetInputStream(String pdbId)
			throws IOException{
		//PDBURLReader reader = new PDBURLReader();
		//Structure s = reader.getStructureById(pdbId);
		File tmp = downloadPDB(pdbId);
		if ( tmp != null ) {
			InputStreamProvider prov = new InputStreamProvider();
			return prov.getInputStream(tmp);


		} else {
			throw new IOException("Could not find PDB " + pdbId + " in file system and also could not download");
		}

	}

	public File downloadPDB(String pdbId){


		File dir = getDir(pdbId);

		File tempFile = new File(dir, getMmCifFileName(pdbId));


		String ftp = String.format("ftp://%s%s%s/%s.cif.gz", 
				serverName, CURRENT_FILES_PATH, pdbId.substring(1,3).toLowerCase(), pdbId.toLowerCase());

		logger.info("Fetching " + ftp);
		try {
			URL url = new URL(ftp);
			InputStream conn = url.openStream();

			// prepare destination
			logger.info("Writing to " + tempFile);

			FileOutputStream outPut = new FileOutputStream(tempFile);
			GZIPOutputStream gzOutPut = new GZIPOutputStream(outPut);
			PrintWriter pw = new PrintWriter(gzOutPut);

			BufferedReader fileBuffer = new BufferedReader(new InputStreamReader(new GZIPInputStream(conn)));
			String line;
			while ((line = fileBuffer.readLine()) != null) {
				pw.println(line);
			}
			pw.flush();
			pw.close();
			outPut.close();
			conn.close();
		} catch (IOException e){
			//e.printStackTrace();
			logger.warn("Could not fetch from ftp "+ftp+" or write file locally. Error: "+e.getMessage());
			return null;
		}
		return tempFile;
	}

	@Override
	public boolean isAutoFetch() {
		return autoFetch;
	}


	@Override
	public void setAutoFetch(boolean autoFetch) {
		this.autoFetch = autoFetch;

	}

	/** Flag that defines if the PDB directory is containing all PDB files or is split into sub dirs (like the FTP site).
	 *  
	 * @return boolean. default is false (all files in one directory)
	 */
	@Override
	public boolean isPdbDirectorySplit() {
		return pdbDirectorySplit;
	}

	/** Flag that defines if the PDB directory is containing all PDB files or is split into sub dirs (like the FTP site).
	 *  
	 * @param pdbDirectorySplit boolean. If set to false all files are in one directory.
	 */
	@Override
	public void setPdbDirectorySplit(boolean pdbDirectorySplit) {
		this.pdbDirectorySplit = pdbDirectorySplit;
	}



	@Override
	public FileParsingParameters getFileParsingParameters()
	{
		return params;
	}


	@Override
	public void setFileParsingParameters(FileParsingParameters params)
	{
		this.params=params;

	}

	public SimpleMMcifConsumer getMMcifConsumer(){
		return consumer;
	}

	public void setMMCifConsumer(SimpleMMcifConsumer consumer){
		this.consumer = consumer;
	}

	public String getMmCifFileName(String pdbId) {
		return pdbId.toLowerCase()+".cif.gz";
	}

	public File getDir(String pdbId) {

		File dir = null;

		if (pdbDirectorySplit) {

			String middle = pdbId.substring(1,3).toLowerCase();
			dir = new File(path, LOCAL_MMCIF_SPLIT_DIR + lineSplit + middle);

		} else {

			dir = new File(path, LOCAL_MMCIF_ALL_DIR);

		}


		if (!dir.exists()) {
			boolean success = dir.mkdirs();
			if (!success) logger.error("Could not create mmCIF dir {}",dir.toString());
		}

		return dir;
	}

}
