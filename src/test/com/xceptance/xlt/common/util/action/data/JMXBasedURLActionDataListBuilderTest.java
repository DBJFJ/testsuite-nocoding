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
import com.xceptance.xlt.common.util.action.data.URLActionDataValidation;
import com.xceptance.xlt.common.util.bsh.ParameterInterpreter;

/*
 * Tests the JMXBasedURLActionDataListBuilder. Tests:
 * 
 * <ul>
 * <li>tests output for an unexisting file 
 * <li>tests output for an empty file	
 * <li>tests with example test case:
 * <li>tests if variables are stored correctly
 * <li>tests if actions (Http Requests) are executed correctly
 * <li>tests if XPath extractions work
 * <li>and if ResponseAssertions work TODO
 * </u>
 */
public class JMXBasedURLActionDataListBuilderTest {

	private final String path = "./config/data/test/";
	private final String filePath1 = "/home/daniel/Desktop/TSearchDidYouMean.jmx";
	private final String filePath2 = "/home/daniel/Desktop/HTTP Request.jmx";
	private final String stringNotExistingFile = "notExistingFile";
    private final String fileEmptyFile = path + "emptyFile.yml";
	
	private XltProperties properties = XltProperties.getInstance();
	private GeneralDataProvider dataProvider = GeneralDataProvider.getInstance();
	
    private ParameterInterpreter interpreter = new ParameterInterpreter(properties, dataProvider);
	private final URLActionDataBuilder actionBuilder = new URLActionDataBuilder();

    @Test
    public void testCorrectConstructor()
    {

    	
        @SuppressWarnings("unused")
		final JMXBasedURLActionDataListBuilder listBuilder = new JMXBasedURLActionDataListBuilder(
				filePath1, this.interpreter, actionBuilder);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testOutputForUnExistingFile()
    {
        final JMXBasedURLActionDataListBuilder listBuilder = new JMXBasedURLActionDataListBuilder(
        		this.stringNotExistingFile, this.interpreter, this.actionBuilder);
        
        final List<URLActionData> actions = listBuilder.buildURLActionDataList();
        Assert.assertTrue(actions.isEmpty());
    }
    
    @Test(expected = IllegalArgumentException.class) //TODO really correct?
    public void testOutputForEmptyFile()
    {
        final JMXBasedURLActionDataListBuilder listBuilder = new JMXBasedURLActionDataListBuilder(
        		this.fileEmptyFile, this.interpreter, this.actionBuilder);
        final List<URLActionData> actions = listBuilder.buildURLActionDataList();
        Assert.assertTrue(actions.isEmpty());
    }
	
	/*
	 * tests a known file.
	 * tests if the number of actions is correct.
	 * tests if the names of the actions are as expected.
	 * tests if the urls of the actions are as expected
	 * tests if the methods of the actions are as expected
	 */
	@Test
	public void testActions() {
	
		JMXBasedURLActionDataListBuilder jmxBasedBuilder = new JMXBasedURLActionDataListBuilder(filePath1, 
				interpreter, actionBuilder);
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
	 * Tests if the HeaderManager is read correctly and the custom headers are set correctly.
	 * Doesn't test default headers, since default headers are not yet implemented.
	 */
	@Test
	public void testHeaders() {
		JMXBasedURLActionDataListBuilder jmxBasedBuilder = new JMXBasedURLActionDataListBuilder(
				filePath1, interpreter, actionBuilder);
		List<URLActionData> actions = jmxBasedBuilder.buildURLActionDataList();
		
		String[][] headerExpected = {
				{"User-Agent", "Mozilla/4.0 (X11; Ubuntu; Linux x86_64; rv:43.0) " +
						"Gecko/20100101 Firefox/43.0" },
				{ "Referer", "https://www.google.de"}
				
		};
		
		// check the first header
		URLActionData action = actions.get(0);
		NameValuePair header = action.getHeaders().get(0);
		Assert.assertEquals(header.getName(), headerExpected[0][0]);
		Assert.assertEquals(header.getValue(), headerExpected[0][1]);
		
		// check the second header
		action = actions.get(0);
		header = action.getHeaders().get(1);
		Assert.assertEquals(header.getName(), headerExpected[1][0]);
		Assert.assertEquals(header.getValue(), headerExpected[1][1]);
		
		// check the default header
		String[] defaultHeader = {"Referer", "yahoo.com"};
		for (int i = 1; i < actions.size(); i++) {
			action = actions.get(1);
			header = action.getHeaders().get(0);
			
			Assert.assertEquals(header.getName(), defaultHeader[0]);
			Assert.assertEquals(header.getValue(), defaultHeader[1]);
		}
	}
	
	/*
	 * Checks if the protocol is read correctly. Uses file2.
	 */
	@Test
	public void testProtocol() {
	
		JMXBasedURLActionDataListBuilder jmxBasedBuilder = new JMXBasedURLActionDataListBuilder(
				filePath2, interpreter, actionBuilder);
		List<URLActionData> actions = jmxBasedBuilder.buildURLActionDataList();
		
		String[] urlExpected = {
				"http://www.xceptance.net",
				"https://www.xceptance.net",
				"https://www.xceptance.net",
				"http://blazemeter.com"
		};
		
		for (int i = 0; i < actions.size(); i++) {
			URLActionData action = actions.get(i);
			String realUrl = action.getUrlString();
			Assert.assertEquals(urlExpected[i], realUrl);
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
		JMXBasedURLActionDataListBuilder jmxBasedBuilder = new JMXBasedURLActionDataListBuilder(filePath1, 
				interpreter, actionBuilder);
		List<URLActionData> actions = jmxBasedBuilder.buildURLActionDataList();
		URLActionData action = actions.get(0);
		Interpreter interpreter = action.getInterpreter();
		
		
		// checks if the parameters of the actions are as expected
		String[][] parametersExpected = {
				{"host", "production-stage02-dw.demandware.net/s/SiteGenesis"},
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
	 * The mapping is difficult here, so the the test case just checks that 
	 * the values are syntactically correct for now.
	 */
	@Test
	public void testResponseAssertion() {
		JMXBasedURLActionDataListBuilder jmxBasedBuilder = new JMXBasedURLActionDataListBuilder(filePath1, 
				interpreter, actionBuilder);
		List<URLActionData> actions = jmxBasedBuilder.buildURLActionDataList();
		
		String[][] selectionModeExpected = {
				{"Regex", "Regex"},
				{"Regex", "Var"},
				{"Regex", "Var", "Var"},
				{"Regex", "Regex", "Regex"},
				{"Regex", "Var", "Var"},
				{"Regex", "Regex", "Regex"}
		};
		
		for (int iAction = 0; iAction < actions.size(); iAction++) {
			URLActionData action = actions.get(iAction);
			List<URLActionDataValidation> validations = action.getValidations();
			int length = validations.size();
			
			for (int iValidation = 0; iValidation < length; iValidation++) {
				URLActionDataValidation validation = validations.get(iValidation);

				String selectionMode = validation.getSelectionMode();	
				Assert.assertEquals(selectionModeExpected[iAction][iValidation], selectionMode);
				
				//TODO the rest
				String selectionContent = validation.getSelectionContent();
				String validationMode = validation.getValidationMode();	
				String validationContent = validation.getValidationContent();
				boolean result = true;

				
				// TODO test selectionMode and validationMode and validationContent 
				// and HTTPResponseCode
								
				// testing whether the selection Content has any unexpected values
				// it should contain variables (${variable} or everything ".*"
				if ( !(selectionContent.contains("${") || selectionContent.equals(".*")) ) {
					result = false;
				}
				Assert.assertTrue(result);
			}
		}
	}
	
	/*
	 * Checks if the XPath-extractions were read correctly. <br/>
	 * Tests with a known test case. <br/>
	 */
	@Test
	public void testExtractions() {
		JMXBasedURLActionDataListBuilder jmxBasedBuilder = new JMXBasedURLActionDataListBuilder(filePath1, 
				interpreter, actionBuilder);
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
				
				String actualValue = store.getSelectionContent();
				String actualName = store.getName();					
				Assert.assertEquals(extractedExpected[iAction][iExtraction][0], actualValue);
				Assert.assertEquals(extractedExpected[iAction][iExtraction][1], actualName);	
			}
		}
	}
}
