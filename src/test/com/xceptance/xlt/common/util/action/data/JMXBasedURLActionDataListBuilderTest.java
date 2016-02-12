package test.com.xceptance.xlt.common.util.action.data;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

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

/**
 * Tests the JMXBasedURLActionDataListBuilder. Tests:
 * 
 * <ul>
 * <li>tests output for an unexisting file 
 * <li>tests output for an empty file	
 * <li>tests with example test case:
 * <li>tests if variables are stored correctly
 * <li>tests if actions (Http Requests) are executed correctly
 * <li>tests if XPath extractions work
 * <li>and if ResponseAssertions work 
 * </ul>
 */
public class JMXBasedURLActionDataListBuilderTest {

	private final String path = "./config/data/test/";
	private final String filePath1 = path + "TSearchDidYouMean.jmx";
	private final String filePath2 = path + "HTTP Request.jmx";
	private final String filePath3 = path + "regex.jmx";
	private final String stringNotExistingFile = "notExistingFile";
    private final String fileEmptyFile = path + "emptyFile.yml";
	
	private XltProperties properties = XltProperties.getInstance();
	private GeneralDataProvider dataProvider = GeneralDataProvider.getInstance();
	
    private ParameterInterpreter interpreter = new ParameterInterpreter(properties, dataProvider);
	private final URLActionDataBuilder actionBuilder = new URLActionDataBuilder();
	
	String[][] selectionModeExpected = {
			
			// action 1
			{"Var", "Var", "Regex", "Regex"},	
			
			// action 2
			{"Regex", "Var"},					
			
			// action 3
			{"Regex", "Var", "Var"},			
			
			// and so on
			{"Regex", "Regex", "Regex"},		
			{"Regex", "Var", "Var"},			
			{"Regex", "Regex", "Regex"}			
	};
	
	String[][] selectionContentExpected = {
			
			// action 1
			{"justSomeDummyText", "justSomeDummyText",  ".*", ".*"},	
			
			// action 2
			{".*", "${noHitsBanner}"},					
			
			// action 3
			{".*", "${noHitsBanner}", "${suggestedSearchTerm}"},			
			
			// and so on
			{".*", ".*", ".*"},		
			{".*", "${noHitsBanner}", "${suggestedSearchTerm}"},			
			{".*", ".*", ".*"}			
	};
	
	String[][] validationModeExpected = {
			
			// action 0
			{URLActionDataValidation.TEXT, URLActionDataValidation.MATCHES,
				URLActionDataValidation.EXISTS, URLActionDataValidation.EXISTS},	
			
			// action 1
			{URLActionDataValidation.EXISTS, URLActionDataValidation.MATCHES},					
			
			// action 2
			{URLActionDataValidation.EXISTS, URLActionDataValidation.MATCHES,
				URLActionDataValidation.MATCHES},			
			
			// and so on ...
			{URLActionDataValidation.EXISTS, URLActionDataValidation.EXISTS, 
					URLActionDataValidation.EXISTS},
					
			{URLActionDataValidation.EXISTS, URLActionDataValidation.MATCHES, 
						URLActionDataValidation.MATCHES},	
						
			{URLActionDataValidation.EXISTS, URLActionDataValidation.EXISTS, 
							URLActionDataValidation.EXISTS}			
	};
	
	String[][] validationContentExpected = {
			
			// action 1
			{"justSomeDummyText", "justSomeD..myT.*",  "id=\"q\"", 
				"<h1\\ class=\"primary-logo\">"},	
			
			// action 2
			{"class=\"no-hits-banner\">", "We're sorry, no products were found for " +
					"your search"},					
			
			// action 3
			{"class=\"no-hits-banner\">", "We're sorry, no products were " +
					"found for your search", "blue"},			
			
			// and so on
			{"<span class=\"breadcrumb-element breadcrumb-result-text\">", 
					"<div class=\"content-slot slot-grid-header\">", 
						"<div class=\"results-hits\""},		
			{"class=\"no-hits-banner\">", "We're sorry, no products were " +
					"found for your search", "dress%20flora"},			
			{"<span class=\"breadcrumb-element breadcrumb-result-text\">",
						"<div class=\"content-slot slot-grid-header\">", 
							"<div class=\"results-hits\""}			
	};		

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
    
    @Test(expected = IllegalArgumentException.class) 
    public void testOutputForEmptyFile()
    {
        final JMXBasedURLActionDataListBuilder listBuilder = new JMXBasedURLActionDataListBuilder(
        		this.fileEmptyFile, this.interpreter, this.actionBuilder);
        final List<URLActionData> actions = listBuilder.buildURLActionDataList();
        Assert.assertTrue(actions.isEmpty());
    }
	
	/**
	 * tests a known file. </br>
	 * tests if the number of actions is correct. </br>
	 * tests if the names of the actions are as expected. </br>
	 * tests if the urls of the actions are as expected. </br>
	 * tests if the methods of the actions are as expected. </br>
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
	
	/**
	 * Tests if the HeaderManager is read correctly and the custom headers are set correctly. </br>
	 * Also tests default headers. </br> 
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
		
		// here for development TODO
		try {
			dumpYamlToFile(actions);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Checks if the protocol is read correctly. Uses filePath2.
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
	
	/**
	 * Checks if the variables were stored and used correctly. <br/>
	 * Tests with a known test case. <br/>
	 * Only tests the first action since all variables are stored at the start in this test case <br/>
	 * Only checks if names are mapped to the expected values, doesn't test the number of variables 
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
				{"suggestionPhrase3", "dress%20floral"},
				{"justADummyVariable", "justSomeDummyText"}
			};
	
		for (int i = 0; i <= 5; i++) {
			String name = parametersExpected[i][0];
			String valueExpected = parametersExpected[i][1];
			String valueActual = interpreter.get(name).toString();
			Assert.assertEquals(valueExpected, valueActual);
		}
	}
	
	/**
	 * Checks if the ResponseAssertions were read correctly. <br/>
	 * The mapping should be:
	 * <ul>
	 * <li> Main sample and sub-samples/ Main sample only/ Sub-samples only -> Regex with .*
	 * <li> Text Response/ Document(text) -> Regex with .*
	 * <li> Jmeter Variable -> Var with ${variable} 
	 * 
	 * <li> URL Sampled -> can't be mapped or isn't implemented yet
	 * <li> Response Message -> can't be matched or isn't implemented yet
	 * <li> Response Code -> Http Response Code (not a validation object in TSNC)	
	 * <li> Response Headers -> can't be mapped easily 
	 * <li> Ignore Status -> can't be mapped 
	 * </ul>
	 * 
	 * Pattern Matching Rules are mapped to TSNCs validationMode
	 * <ul>
	 * <li> Contains -> Exists
	 * <li> Matches -> Matches
	 * <li> Equals -> Text
	 * <li> Substring -> Exists
	 * <li> Not -> can't be matched
	 * </ul>
	 * <li>Patterns to Test -> validationContent. If there are multiple patterns to test 
	 * TSNC makes multiple validations.
	 */
	@Test
	public void testResponseAssertion() {
		JMXBasedURLActionDataListBuilder jmxBasedBuilder = new JMXBasedURLActionDataListBuilder(filePath1, 
				interpreter, actionBuilder);
		List<URLActionData> actions = jmxBasedBuilder.buildURLActionDataList();
		
		
		// tests the sample test case
		for (int iAction = 0; iAction < actions.size(); iAction++) {
			URLActionData action = actions.get(iAction);
			List<URLActionDataValidation> validations = action.getValidations();
			int length = validations.size();
			
			for (int iValidation = 0; iValidation < length; iValidation++) {
				URLActionDataValidation validation = validations.get(iValidation);
				
				// Note: validation.getSomethingSomething gets the dynamic interpretation
				// from the interpreter if there is one. That means a variable ${var} would
				// be returned with it's value if there is one. ( ${var} -> value
				
				// If the variable doesn't have a value yet (because the value is 
				// dynamically assigned at runtime) the raw variable is returned 
				// ( ${var} -> ${var}
				
				String selectionMode = validation.getSelectionMode();
				String selectionContent = validation.getSelectionContent();
				String validationMode = validation.getValidationMode();	
				String validationContent = validation.getValidationContent();

				// assert selectionMode	
				Assert.assertEquals(selectionModeExpected[iAction][iValidation], selectionMode);	
			
				// assert selectionContent
				Assert.assertEquals(selectionContentExpected[iAction][iValidation], selectionContent);
				
				// assert validationMode
				System.out.println("Action: " + iAction + ", " + validation.getName());
				Assert.assertEquals(validationModeExpected[iAction][iValidation], validationMode);
				
				// assert validationContent
				Assert.assertEquals(validationContentExpected[iAction][iValidation], validationContent);
			}
		}
		
		// tests if the response code is validated
		jmxBasedBuilder = new JMXBasedURLActionDataListBuilder(filePath2, 
				interpreter, actionBuilder);
		actions = jmxBasedBuilder.buildURLActionDataList();
		URLActionData action = actions.get(0);
		
		int responseCodeExpected = 302;
		int responseCode = action.getHttpResponseCode(); 
		
		Assert.assertEquals(responseCodeExpected, responseCode);
	}
	
	/**
	 * Checks if the XPath-extractions were read correctly. <br/>
	 * Tests with a known test case. 
	 */
	@Test
	public void testXPathExtractions() {
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
	
	/*
	 * Tests the regex extractor with a sample file. Right now it's just one regex 
	 * extractor with a group (round brackets) and which should extract the first 
	 * match. TODO expand. 
	 */
	@Test
	public void testRegexExtractions() {
		JMXBasedURLActionDataListBuilder jmxBasedBuilder = new JMXBasedURLActionDataListBuilder(filePath3, 
				interpreter, actionBuilder);
		List<URLActionData> actions = jmxBasedBuilder.buildURLActionDataList();
		
		String subSelectionModeExpected[] = {
				URLActionDataStore.REGEXGROUP, null, URLActionDataStore.REGEXGROUP
		};
		String subSelectionContentExpected[] = {
				"2", null, "1" 
		};
		
		// test if subSelectionMode/-Value are read correctly (action 0),
		// if subSelectionMode/-Value are left out when they should be (action 1)
		// and if correct default values are set for subSelectioMode and /-Value (action 2= 
		
		for (int i = 0; i < actions.size(); i++) {
			URLActionData action = actions.get(i);
			URLActionDataStore store = action.getStore().get(0);
			
			String subSelectionModeActual = store.getSubSelectionMode();
			String subSelectionContentActual = store.getSubSelectionContent();
			
			Assert.assertEquals(subSelectionModeExpected[i], subSelectionModeActual);
			Assert.assertEquals(subSelectionContentExpected[i], subSelectionContentActual);

		}
		
		// TODO add test for impossible values
	}
	/**
	 * Mainly just here for development. TODO
	 * 
	 * @param obj
	 * @throws FileNotFoundException
	 */
	private void dumpYamlToFile(Object obj) throws FileNotFoundException {
		PrintWriter printwriter = new PrintWriter("/home/daniel/Desktop/dump.yml");	
	    StringWriter stringwriter = new StringWriter();
	
		Yaml yaml = new Yaml();
		yaml.dump(obj, stringwriter);
		
		printwriter.print(stringwriter.toString());
		
		printwriter.close();
	}
}
