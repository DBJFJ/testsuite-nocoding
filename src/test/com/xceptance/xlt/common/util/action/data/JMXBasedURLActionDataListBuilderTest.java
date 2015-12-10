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
import com.xceptance.xlt.common.util.action.data.URLActionDataStore;
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
		
		// DEBUG
//		for (int i = 0; i < actions.size(); i++) {
//			System.out.println("NAME: " + actions.get(i).getName());
//		}
		
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
		
		// checks the parameters ...
		
		// all parameters one after the other in a single String 
		// action1 + parameterName + parameterValue + action2 parameterName + 
		// parameterValue + parameterName + parameterValues + parameter3 + ...
		// what matters are the resolved values
		String allParametersInActions = "";
		String allParametersInActionsExpected = "01qiuzauztestsomthingsomething2qblut3"
												+"qblue4qdress%20flora5qdress%20floral";
		
		for (int i = 0; i <= 5; i++) {
			List<NameValuePair> parameters = actions.get(i).getParameters();
			allParametersInActions = allParametersInActions + i;
			for (NameValuePair parameter : parameters) {
				allParametersInActions = allParametersInActions + parameter.getName() + parameter.getValue();
			}
		}
		Assert.assertEquals(allParametersInActionsExpected, allParametersInActions);
		
		// checks parameter encoding 
		// Note: TSNC defaults to true, Jmeter to false
		// In Jmeter encoding can be defined for every parameter
		// in TSNC it's defined for every parameter pair in an action
		boolean[] encodeParametersExpected = {
				true,
				true,
				false,
				false,
				false,
				false
		};
		for (int i = 0; i <= 5; i++) {
			URLActionData action = actions.get(i);
			Assert.assertEquals(encodeParametersExpected[i], action.encodeParameters());
		}
		
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
	
	/*
	 * Checks if the ResponseAssertions were read correctly. <br/>
	 * Tests with a known test case. <br/>
	 * TODO
	 */
	@Test
	public void testResponseAssertion() {
		
	}
	
	/*
	 * Checks if the XPath-extractions were read correctly. <br/>
	 * Tests with a known test case. <br/>
	 */
	@Test
	public void testXPathExtraction() {
		JMXBasedURLActionDataListBuilder jmxBasedBuilder = new JMXBasedURLActionDataListBuilder(filePath, interpreter, builder);
		List<URLActionData> actions = jmxBasedBuilder.buildURLActionDataList();
		
		String[][][] extractedExpected = {
				
				// for action 1
				{}, 
				
				// for action 2
				{ {"//*[@class=\"section-header\"]/p", "noHitsBanner"} },
				
				// for action 3
				{ {"//*[@class=\"section-header\"]/p", "noHitsBanner"}, 
					{"//*[@class=\"no-hits-search-term-suggest\"]", "suggestedSearchTerm"} },
					
				// for action 4
					{},
					
				// for action 5
					{ {"//*[@class=\"section-header\"]", "noHitsBanner"}, 
						{"//*[@class=\"no-hits-search-term-suggest\"]", "suggestedSearchTerm"} },
						
				// for action 6
						{}
		};
		
		for (int iAction = 0; iAction < actions.size(); iAction++) {
			URLActionData action = actions.get(iAction);
			List<URLActionDataStore> extracted = action.getStore();
			int length = extracted.size();
			
			for (int iExtraction = 0; iExtraction < length; iExtraction++) {
				URLActionDataStore store = extracted.get(iExtraction);
				
				if (store.getSelectionMode() == URLActionDataStore.XPATH) {
					String actualValue = store.getSelectionContent();
					String actualName = store.getName();
					
					System.out.println("aaaaaaaa: " + iAction + ", " + iExtraction + ", " + actualValue + ", " + actualName);
					
					Assert.assertEquals(extractedExpected[iAction][iExtraction][0], actualValue);
					Assert.assertEquals(extractedExpected[iAction][iExtraction][1], actualName);	
					System.out.println("asdfgh: " + actualValue + ", " + actualName);
				}
			}
		}
	}
}
