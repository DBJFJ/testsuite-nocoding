package test.com.xceptance.xlt.common.util.action.data.Jmeter;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.xceptance.xlt.common.util.action.data.Jmeter.JmeterConverter;

public class JmeterConverterTest {
 
	

	
	private final static String rootFolder = "./config/data/test/JmeterStuff/";
	private static String folderTmp = rootFolder + "/tmp/";	
		
	private final String filePath1 = rootFolder + "TSearchDidYouMean.jmx";
	private final String filePath2 = rootFolder + "HTTP Request.jmx";
	private final String filePath3 = rootFolder + "regex.jmx";
	
	private final String fileEmptyFile = rootFolder + "emptyFile.yml";
	private final String notExistingFile = "notExistingFile";


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
	
	@Test(expected=IllegalArgumentException.class)
	public void noInput() throws IOException {
		JmeterConverter.main();
	}
	
	@Test
	public void testNonexistingFile() throws IOException {
		JmeterConverter.main(notExistingFile);
		// TODO how to test it logs?
	}

	@Test(expected=IllegalArgumentException.class)
	public void testEmptyFile() throws IOException {
		
		JmeterConverter.main(fileEmptyFile);
	}
	
	@Test
	public void testFileNotJmeter() {
		
		// call the method with an existing file,
		// but that file is not a Jmeter file
		// what is supposed to happen here ... ?
		
		// how does it even recognize that ... - ending ? That's dump, but w/e
		fail("Not yet implemented");
	}
	
	/**
	 * tests with a sample Jmeter file that only contains one thrad group. 
	 * Tests against a previously checked file.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testOneTGroup() throws IOException {
		
		final File CORRECT = new File(rootFolder + "correct-regex.yml");
		
		JmeterConverter.main(filePath3);
		String yamlFileName = "regex" + "-" + "Thread Group" + ".yml"; 
		
		boolean dumpAsExpected = FileUtils.contentEquals(CORRECT, new File(yamlFileName));
		Assert.assertTrue(dumpAsExpected);
	}
	
	@Test
	public void testManyTGroups() throws IOException {
		
		final File CORRECT = new File(rootFolder + "correct-TSearch.yml");
		
		JmeterConverter.main(filePath1);
		String yamlFileName = "TSearchDidYouMean" + "-" + "Thread Group" + ".yml"; 
		
		boolean dumpAsExpected = FileUtils.contentEquals(CORRECT, new File(yamlFileName));
		Assert.assertTrue(dumpAsExpected);
	}
	
	@Test
	public void testManyFil() {
		
		// copy over the .jmx files
		// execute with the .jmx file, 
		// validate everything
		// can just use existing files
		
		fail("Not yet implemented");
	}
	
	@Test
	public void testManyFilOneWrong() {
		
		// execute with the .jmx file, 
		// validate everything
		// I want a warning, not an exception
		// control flow how?
		
	}

	/**
	 * Delete the temporary folder where the yaml files where dumped to
	 * 
	 * @throws Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		FileUtils.deleteDirectory(new File(folderTmp));
	}
}
