package test.com.xceptance.xlt.common.util.action.data.Jmeter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.xceptance.xlt.common.util.action.data.Jmeter.JmeterConverter;

public class JmeterConverterTest {
 
	

	
	private final static String rootFolder = "./config/data/test/JmeterStuff/";
	private static String folderTmp = rootFolder + "tmp/";	
		
	private final String fileTSearch = rootFolder + "TSearchDidYouMean.jmx";

	private final String fileRegex = rootFolder + "regex.jmx";
	
	private final String fileHttpR = rootFolder + "HTTP Request.jmx";
	
	private final String emptyFile = "./config/data/test/emptyFile.yml";
	
	private final String notExistingFile = "notExistingFile";
	
	private final String notJmxFile = rootFolder + "complexYamlDump.yml"; 


	/**
	 * Create the temporary folder where the yaml files will be dumped to.
	 * Make sure it doesn't exist already.
	 * 
	 * @throws Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		while (new File(folderTmp).isDirectory()) {
			folderTmp = folderTmp + "t";
		}
	}
	
	/**
	 * Tests what happens if the converter is executed without any input.
	 * 
	 * @throws IOException
	 */
	@Test(expected=IllegalArgumentException.class)
	public void noInput() throws IOException {
		JmeterConverter.main();
	}
	
	/**
	 * Tests what happens if the converter is executed with a single filepath, 
	 * and that file doesn't exist. Expected: an error was logged. How to test that?
	 * I don't know.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testNonexistingFile() throws IOException {
		JmeterConverter.main(notExistingFile);
	}

	/**
	 * Tests what happens if the converter is executed with a single filepath, 
	 * and that file is empty. Expected: an error was logged. How to test that?
	 * I don't know. TODO
	 * 
	 * @throws IOException
	 */
	@Test
	public void testEmptyFile() throws IOException {
		
		JmeterConverter.main(emptyFile);
	}
	
	
	/**
	 * Tests what happens if the converter is executed with a single filepath, 
	 * and that file is not a Jmeter file (not a .jmx ending). 
	 * Expected: an error was logged. How to test that?
	 * I don't know. TODO
	 * 
	 * @throws IOException
	 */
	@Test
	public void testFileNotJmeter() throws IOException {
		
		JmeterConverter.main(notJmxFile);
	}
	
	/**
	 * tests with a sample Jmeter file that only contains one thrad group. 
	 * Tests against a previously checked file.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testOneTGroup() throws IOException {
		
		// copy the files over to the tmpFolder
		Path toConvert = copyFileToTmp(fileRegex, "regex.jmx");
		
		// convert it
		JmeterConverter.main(toConvert.toString());
		
		// compare it
		String yamlFileName = "regex" + "-" + "Thread Group" + ".yml"; 
		String correct = "correct-regex.yml";
		assertFilesEqual(correct, yamlFileName);
	}
	
	@Test
	public void testManyTGroups() throws IOException {
		
		// copy the files over to the tmpFolder
		Path toConvert = copyFileToTmp(fileTSearch, "TSearch.jmx");
		
		// convert it
		JmeterConverter.main(toConvert.toString());
		
		// compare first thread group ...
		String yamlFileName = "TSearch" + "-" + "Thread Group1" + ".yml"; 
		String correctF = "correct-TSearch1.yml";
		assertFilesEqual(correctF, yamlFileName);
		
		// compare first thread group ...
		yamlFileName = "TSearch" + "-" + "Thread Group2" + ".yml"; 
		correctF = "correct-TSearch2.yml";
		assertFilesEqual(correctF, yamlFileName);
		
		// compare first thread group ...
		yamlFileName = "TSearch" + "-" + "Validate Response Headers" + ".yml"; 
		correctF = "correct-TSearch3.yml";
		assertFilesEqual(correctF, yamlFileName);
	}
	
	@Test
	public void testManyFiles() throws IOException {
		
		// copy over the .jmx files

		Path regexTConvert = copyFileToTmp(fileRegex, "regex.jmx");
		Path tSearchTConvert = copyFileToTmp(fileTSearch, "TSearch.jmx");
		Path httpRTConvert = copyFileToTmp(fileHttpR, "HttpR.jmx");
		
		// execute with the .jmx files, 
		String[] args = {
				regexTConvert.toString(), tSearchTConvert.toString(), httpRTConvert.toString()
		};
		JmeterConverter.main(args);
		
		// validate everything ...
		
		// validate regex
		String yamlFileName = "regex" + "-" + "Thread Group" + ".yml"; 
		String correctF = "correct-regex.yml";
		assertFilesEqual(correctF, yamlFileName);
		
		// validate TSearch ( this is exactly the same as in 
		// compare first thread group ...
		yamlFileName = "TSearch" + "-" + "Thread Group1" + ".yml"; 
		correctF = "correct-TSearch1.yml";
		assertFilesEqual(correctF, yamlFileName);
		
		// compare first thread group ...
		yamlFileName = "TSearch" + "-" + "Thread Group2" + ".yml"; 
		correctF = "correct-TSearch2.yml";
		assertFilesEqual(correctF, yamlFileName);
		
		// compare first thread group ...
		yamlFileName = "TSearch" + "-" + "Validate Response Headers" + ".yml"; 
		correctF = "correct-TSearch3.yml";
		assertFilesEqual(correctF, yamlFileName);
		
		// validate HttpR 
		yamlFileName = "HttpR" + "-" + "Thread Group" + ".yml"; 
		correctF = "correct-HttpR.yml";
		assertFilesEqual(correctF, yamlFileName);
	}
	
	@Test
	public void testManyFilesOneWrong() throws IOException {
		
		Path regexTConvert = copyFileToTmp(fileRegex, "regex.jmx");
		Path tSearchTConvert = copyFileToTmp(fileTSearch, "TSearch.jmx");
		Path httpRTConvert = copyFileToTmp(fileHttpR, "HttpR.jmx");
		
		// execute with the .jmx files, 
		String[] args = {
				regexTConvert.toString(), notExistingFile, tSearchTConvert.toString(), 
				emptyFile, httpRTConvert.toString()
		};
		JmeterConverter.main(args);
		
		
		// don't validate, that was done in testManyFiles
		
		// how to test a warning was logged?
	}

	/**
	 * Delete the temporary folder where the yaml files where dumped to
	 * 
	 * @throws Exception
	 */
	@After
	public void deleteTmpFolder() throws Exception {
		FileUtils.deleteDirectory(new File(folderTmp));
	}
	
	/**
	 * Copies a file over to the temp Folder, so it can be converted there and 
	 * the converted file will also end up there.
	 * 
	 * @param source the file to copy
	 * @param name the name the copy should have
	 * @return the path of the copy
	 * @throws IOException
	 */
	private Path copyFileToTmp(String source, String name) throws IOException {
		
		Path copy = Paths.get(folderTmp + name);
		Files.createDirectories(copy.getParent());
		Files.copy(Paths.get(source), copy);
		
		return copy;
	}
	
	/**
	 * Takes two filenames and asserts that a file with the corrName exist in the
	 * {@link #rootFolder}, a file with the checkName exists in the {@link #folderTmp} 
	 * and their content is equal.
	 * 
	 * @param corrName
	 * @throws IOException 
	 */
	private void assertFilesEqual(String corrName, String checkName) throws IOException {
		
		final String convertedFile = folderTmp.toString() + checkName;
		final File CORRECT = new File(rootFolder + corrName);
		
		// validate exists
		Assert.assertTrue(Files.exists(Paths.get(convertedFile)));
		Assert.assertTrue(CORRECT.exists());
		
		// validate content equal
		boolean equal = FileUtils.contentEquals(CORRECT, new File(convertedFile));
		Assert.assertTrue(equal);
	}
}
