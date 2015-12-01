package test.com.xceptance.xlt.common.util.action.data;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import bsh.EvalError;
import bsh.Interpreter;

import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.xceptance.xlt.api.data.GeneralDataProvider;
import com.xceptance.xlt.api.util.XltProperties;
import com.xceptance.xlt.common.util.action.data.JMXBasedURLActionDataListBuilder;
import com.xceptance.xlt.common.util.action.data.URLActionData;
import com.xceptance.xlt.common.util.action.data.URLActionDataBuilder;
import com.xceptance.xlt.common.util.bsh.ParameterInterpreter;

/*
 * Tests the JMXBasedURLActionDataListBuilder. Tests:
 * 
 * <ul>
 * <li>tests output for an unexisting file TODO 
 * <li>tests output for an empty file	TODO
 * <li>test output for default values	TODO
 * <li>tests with an example test case
 * </u>
 */
public class JMXBasedURLActionDataListBuilderTest {

	private final String filePath = "/home/daniel/Desktop/TSearchDidYouMean.jmx";
	private XltProperties properties = XltProperties.getInstance();
	private GeneralDataProvider dataProvider = GeneralDataProvider.getInstance();
    private ParameterInterpreter interpreter = new ParameterInterpreter(properties, dataProvider);
	private final URLActionDataBuilder builder = new URLActionDataBuilder();

	/*
	 * tests a known file.
	 * tests if the number of actions is correct.
	 * tests if the names of the actions are as expected.
	 * tests if the urls of the actions are as expected
	 * tests if the methods of the actions are as expected
	 */
	@Test
	public void testActions() {
	
		JMXBasedURLActionDataListBuilder jmxBasedBuilder = new JMXBasedURLActionDataListBuilder(filePath, interpreter, builder);
		List<URLActionData> actions = jmxBasedBuilder.buildURLActionDataList();
		
		// check the number of actions 
		int numberOfActions = actions.size();
		Assert.assertEquals(6, numberOfActions);
		
		// check if the names of the actions are as expected
		String[] namesExpected = {
				"Open Homepage",
				"search for nonsense",
				"test for typing mistake in search 1",
				"test for correcting the mistake afterwards 1",
				"test for typing mistake in search 2",
				"test for correcting the mistake afterwards 2"
		};
		for (int i = 0; i <= 5; i++) {
			Assert.assertEquals(namesExpected[i], actions.get(i).getName());
		}
		
		// checks if the urls of the actions are as expected
		// this assertion also fails if the class doesn't store variables correctly
		String[] urlsExpected = {
				"http://production-stage02-dw.demandware.net/s/SiteGenesis/home",
				"http://production-stage02-dw.demandware.net/s/SiteGenesis/search",
				"http://production-stage02-dw.demandware.net/s/SiteGenesis/search",
				"http://production-stage02-dw.demandware.net/s/SiteGenesis/search",
				"http://production-stage02-dw.demandware.net/s/SiteGenesis/search",
				"http://production-stage02-dw.demandware.net/s/SiteGenesis/search"
		};
		for (int i = 0; i <= 5; i++) {
			Assert.assertEquals(urlsExpected[i], actions.get(i).getUrlString());
		}
		
		// checks the parameters TODO

		
		// checks if the method of the actions is as expected
		String[] expectedMethod = {
				"GET",
				"GET",
				"GET",
				"GET",
				"GET",
				"GET"
		};
		for (int i = 0; i <= 5; i++) {
			String realMethod = actions.get(i).getMethod().toString();
			boolean result = realMethod.contains(expectedMethod[i]);
			Assert.assertTrue(result);
		}
	}
	
	/*
	 * Checks if the variables were stored and used correctly. <br/>
	 * Tests with a known test case. <br/>
	 * Only tests the first action since all variables are stored at the start in this test case <br/>
	 * Only checks if names are mapped to the expected values, doesn't test the number of variables <br/>
	 */
	@Test
	public void testVariables() throws EvalError {
		JMXBasedURLActionDataListBuilder jmxBasedBuilder = new JMXBasedURLActionDataListBuilder(filePath, interpreter, builder);
		List<URLActionData> actions = jmxBasedBuilder.buildURLActionDataList();
		URLActionData action = actions.get(0);
		Interpreter interpreter = action.getInterpreter();
		
		
		// checks if the parameters of the actions are as expected
		String[][] parametersExpected = {
				{"host", "http://production-stage02-dw.demandware.net/s/SiteGenesis"},
				{"searchPhrase1", "iuzauz"},				
				{"searchPhrase2", "blut"},			
				{"suggestionPhrase2", "blue"},
				{"searchPhrase3", "dress%20flora"},
				{"suggestionPhrase3", "dress%20floral"}
			};
	
		for (int i = 0; i <= 5; i++) {
			String name = parametersExpected[i][0];
			String valueExpected = parametersExpected[i][1];
			String valueActual = interpreter.get(name).toString();
			Assert.assertEquals(valueExpected, valueActual);
		}
	}
}
