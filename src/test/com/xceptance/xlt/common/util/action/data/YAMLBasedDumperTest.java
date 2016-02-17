package test.com.xceptance.xlt.common.util.action.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.xceptance.xlt.api.data.GeneralDataProvider;
import com.xceptance.xlt.api.util.XltProperties;
import com.xceptance.xlt.common.util.action.data.URLActionData;
import com.xceptance.xlt.common.util.action.data.URLActionDataBuilder;
import com.xceptance.xlt.common.util.action.data.URLActionDataStoreBuilder;
import com.xceptance.xlt.common.util.action.data.URLActionDataValidationBuilder;
import com.xceptance.xlt.common.util.action.data.YAMLBasedDumper;
import com.xceptance.xlt.common.util.action.data.YAMLBasedURLActionDataListBuilder;
import com.xceptance.xlt.common.util.bsh.ParameterInterpreter;


/**
 * Tests the {@link YAMLBasedDumper} with example files.
 * 
 * @author daniel
 *
 */
public class YAMLBasedDumperTest {

    private final String path = "./config/data/test/";
    private final Path fileTmp = Paths.get(path + "tmpfile.yml");	
    
    private final String simpleTest = path + "testYamlDumperSimple.yml";
    private final String complexTest = path + "testYamlDumperComplex.yml"; 	
    private final String simpleDump = path + "simpleYamlDump.yml";
    private final String complexDump = path + "complexYamlDump.yml"; 	
    
    private ParameterInterpreter interpreter;
    private XltProperties properties;
    private GeneralDataProvider dataProvider;
    
    private final URLActionDataBuilder actionBuilder = new URLActionDataBuilder();
    private final URLActionDataStoreBuilder storeBuilder = new URLActionDataStoreBuilder();
    private final URLActionDataValidationBuilder validationBuilder = new URLActionDataValidationBuilder();

    /**
     * Creates the interpreter.
     */
    @Before
    public void setup()
    {
        properties = XltProperties.getInstance();
        dataProvider = GeneralDataProvider.getInstance();
        interpreter = new ParameterInterpreter(properties, dataProvider);
    }
    

	
	/**
	 * Tests if a simple example test case is correctly dumped. Tests with a simple example test file 
	 * and checks the resulting file against a given dump that was manually checked. </br>
	 * Also feeds the dump back into the {@link YAMLBasedURLActionDataListBuilder} and checks if it remains
	 * constant. 
	 * @throws IOException 
	 * 
	 */
	@Test
	public void testSimplyDump() throws IOException {
			testYamlWithDump(simpleTest, simpleDump);
		}	
	
	/**
	 * Tests if a complicated example test case is correctly dumped. Tests with a complex example test file 
	 * and checks the resulting file against an complex dump that was manually checked. </br>
	 * Also feeds the dump back into the {@link YAMLBasedURLActionDataListBuilder} and checks if it remains
	 * constant. 
	 * @throws IOException 
	 * 
	 */
	@Test
	public void testComplexDump() throws IOException {
			testYamlWithDump(complexTest, complexDump);
		}	

	/**
	 * Checks if the a the given yaml file, after being constructed by the 
	 * {@link YAMLBasedURLActionDataListBuilder} and dumped by the {@link YAMLBasedDumper} 
	 * matches the given dump. Also tests if the dump stays constant over multiple 
	 * construct - dump rotations. </br>
	 * Used for the actual tests.
	 * 
	 * @param yamlFile
	 * @param dump
	 * @throws IOException
	 */
	private void testYamlWithDump(String yamlFile, String dump) throws IOException {
		
		// check if the first dump is as expected
		YAMLBasedURLActionDataListBuilder builder1 = new YAMLBasedURLActionDataListBuilder(yamlFile,
																						interpreter, 
																						actionBuilder, 
																						validationBuilder, 
																						storeBuilder);
		List<URLActionData> actions = builder1.buildURLActionDataList();
		YAMLBasedDumper.dumpActionsYaml(actions, fileTmp);
		boolean dumpAsExpected = FileUtils.contentEquals(fileTmp.toFile(), new File(dump));
		Assert.assertTrue(dumpAsExpected);
		
		// check if the dump remains constant
		YAMLBasedURLActionDataListBuilder builder2 = new YAMLBasedURLActionDataListBuilder(fileTmp.toString(), 
				interpreter, 
				actionBuilder, 
				validationBuilder, 
				storeBuilder);
		for (int i = 0; i < 10; i++) {
			
			// there is a bug in which the DataListBuilder keeps the actions he has previously 
			// returned unless he is reconstructed
			builder2 = new YAMLBasedURLActionDataListBuilder(fileTmp.toString(), 
					interpreter, 
					actionBuilder, 
					validationBuilder, 
					storeBuilder);
			
			actions = builder2.buildURLActionDataList();
			YAMLBasedDumper.dumpActionsYaml(actions, fileTmp);
			dumpAsExpected = FileUtils.contentEquals(fileTmp.toFile(), new File(dump));
			Assert.assertTrue(dumpAsExpected);
		}
	}
	
	@After
	public void deleteTemp() {
		fileTmp.toFile().delete();
	}
}
