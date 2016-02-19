package test.com.xceptance.xlt.common.util.action.data.Jmeter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import bsh.EvalError;

import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.xceptance.xlt.common.util.action.data.URLActionData;
import com.xceptance.xlt.common.util.action.data.URLActionDataStore;
import com.xceptance.xlt.common.util.action.data.URLActionDataValidation;
import com.xceptance.xlt.common.util.action.data.Jmeter.JmeterTranslater;
import com.xceptance.xlt.common.util.action.data.Jmeter.JmeterTranslater.MappingException;
import com.xceptance.xlt.common.util.bsh.ParameterInterpreter;

/**
 * Tests the JMXBasedURLActionDataListBuilder. Tests:
 * 
 * <ul>
 * <li>tests output for an unexisting file 
 * <li>tests output for an empty file	
 * <li>tests with many example test files ...
 * </ul>
 */
public class JmeterTranslaterTest {

	private final static String rootPath = "./config/data/test/JmeterStuff/";
	
	private final String filePath1 = rootPath + "TSearchDidYouMean.jmx";
	private final String filePath2 = rootPath + "HTTP Request.jmx";
	private final String filePath3 = rootPath + "regex.jmx";
	private final String filePath4 = rootPath + "/impossible/";
	private final String stringNotExistingFile = "notExistingFile";
    private final String fileEmptyFile = rootPath + "emptyFile.yml";
	   
	private final JmeterTranslater translater = new JmeterTranslater();
   
    @Test(expected = IllegalArgumentException.class)
    public void testOutputForUnExistingFile()
    {
    	final LinkedHashMap<String, List<URLActionData>> TGroups = 
    						translater.translateFile(stringNotExistingFile);
        Assert.assertTrue(TGroups.isEmpty());
    }
    
    @Test(expected = IllegalArgumentException.class) 
    public void testOutputForEmptyFile()
    {       
    	final LinkedHashMap<String, List<URLActionData>> TGroups = 
				translater.translateFile(fileEmptyFile);
    	Assert.assertTrue(TGroups.isEmpty());
    }
	
	/**
	 * tests a known file. </br>
	 * tests if the number of actions is correct. </br>
	 * tests if the names of the actions are as expected. </br>
	 * tests if the urls of the actions are as expected. </br>
	 * tests if the methods of the actions are as expected. </br>
	 * tests if the parameters of the actions are as expected </br>
	 */
	@Test
	public void testActions() {		
    	final LinkedHashMap<String, List<URLActionData>> TGroups = 
    												translater.translateFile(filePath1);
		List<URLActionData> actions = getAllActions(TGroups);
    	
		// check the number of actions 
		int numberOfActions = actions.size();
		Assert.assertEquals(9, numberOfActions);
		
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
	 * Tests if the default values for path, website, protocol and parameters in the Http Request 
	 * Defaults is read correctly. As usual, it tests with an example test file.
	 */
	@Test 
	public void testHttpDefaults() {
		LinkedHashMap<String, List<URLActionData>> TGroups = translater.translateFile(filePath1);
		
		List<URLActionData> actions = getAllActions(TGroups);
		
		// define default values, for an action without any other values
		String defaultUrl = "https://www.google.de/search";
		List<NameValuePair> defaultParameters = new ArrayList<NameValuePair>();
		defaultParameters.add(new NameValuePair("x-param", "parameter1"));
		
		// define the values for the action with more then just default values
		String manualUrl = "http://www.xceptance.com/en";
		List<NameValuePair> manualParameters = new ArrayList<NameValuePair>();
		manualParameters.addAll(defaultParameters);
		manualParameters.add(new NameValuePair("y-param", "parameter2"));
		
		// check the empty action for defaults
		URLActionData actionWithDefaults = actions.get(6);
		Assert.assertEquals(defaultUrl, actionWithDefaults.getUrlString());
		Assert.assertEquals(defaultParameters, actionWithDefaults.getParameters());
				
		// check the nonempty action. The url should have been overwritten,
		// the parameters just added.
		URLActionData actionWithManual = actions.get(7);
		Assert.assertEquals(manualUrl, actionWithManual.getUrlString());
		Assert.assertEquals(manualParameters, actionWithManual.getParameters());
		
	}
	
	/**
	 * Tests if the HeaderManager is read correctly and the custom headers are set correctly. </br>
	 * Also tests default headers. </br> 
	 */
	@Test
	public void testHeaders() {

		final LinkedHashMap<String, List<URLActionData>> TGroups = 
												translater.translateFile(filePath1);
		List<URLActionData> actions = getAllActions(TGroups);
		
		// check the default header
		String[] defaultHeader = {"Referer", "yahoo.com"};
		for (int i = 2; i < 6; i++) {
			URLActionData action = actions.get(i);
			NameValuePair header = action.getHeaders().get(0);
			
			Assert.assertEquals(defaultHeader[0], header.getName());
			Assert.assertEquals(defaultHeader[1], header.getValue());
		}
		
		// check if the default header can be overwritten
		NameValuePair headerExpected = new NameValuePair("Referer", "https://www.google.de");
		
		URLActionData action = actions.get(1);
		NameValuePair header = action.getHeaders().get(0);
		Assert.assertEquals(header.getName(), headerExpected.getName());
		Assert.assertEquals(header.getValue(), headerExpected.getValue());
		
		
		// check if default and normal headers are merged
		
		String[][] headersExpected = {
				{"User-Agent", "Mozilla/4.0 (X11; Ubuntu; Linux x86_64; rv:43.0) " +
						"Gecko/20100101 Firefox/43.0" },
				{ "Referer", "yahoo.com"},
		};
		
		action = actions.get(0);
		header = action.getHeaders().get(0);
		Assert.assertEquals(header.getName(), headersExpected[0][0]);
		Assert.assertEquals(header.getValue(), headersExpected[0][1]);
		
		// check the second header
		action = actions.get(0);
		header = action.getHeaders().get(1);
		Assert.assertEquals(header.getName(), headersExpected[1][0]);
		Assert.assertEquals(header.getValue(), headersExpected[1][1]);
	}
	
	/**
	 * Checks if the protocol is read correctly. Uses filePath2.
	 * Also checks if assertions that can't be translated are created (wrong) 
	 * or quietly left out (right).
	 */
	@Test
	public void testProtocol() {
		
		final LinkedHashMap<String, List<URLActionData>> TGroups = 
										translater.translateFile(filePath2);
		List<URLActionData> actions = getAllActions(TGroups);
		
		String[] urlExpected = {
				"http://www.xceptance.net",
				"https://www.xceptance.net",
				"https://www.xceptance.net",
				"http://blazemeter.com",
		};
		
		for (int i = 0; i < actions.size(); i++) {
			URLActionData action = actions.get(i);
			String realUrl = action.getUrlString();
			Assert.assertEquals(urlExpected[i], realUrl);
		}
		
		URLActionData action = actions.get(1);
		List<URLActionDataValidation> validations = action.getValidations();
		Assert.assertEquals(1, validations.size());
	}
	
	/**
	 * Checks if the variables were stored and used correctly. <br/>
	 * Tests with a known test case. <br/>
	 * Only tests the first action since all variables are stored at the start in this test case <br/>
	 * Only checks if names are mapped to the expected values, doesn't test the number of variables.</br>
	 * Tests User Defined Variables in the Thread Group as well as User Defined Variables in the Test Plan. 
	 */
	@Test
	public void testVariables() throws EvalError {
		
		LinkedHashMap<String, List<URLActionData>> TGroups = 
										translater.translateFile(filePath1);
		List<URLActionData> actions = getAllActions(TGroups);

		
		URLActionData action = actions.get(0);
		ParameterInterpreter interpreter = action.getInterpreter();
		
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
		
		// check if variables that were defined in the Test Plan element are also read ...
		
		TGroups = translater.translateFile(filePath2);
		actions = getAllActions(TGroups);
		
		action = actions.get(0);
		interpreter = action.getInterpreter();
		
		// checks if the parameters of the actions are as expected
		String[][] parametersExpected2 = {
			{"var1", "content1"},
			{"var2", "content2"},	
		};
	
		for (int i = 0; i <= 1; i++) {
			String name = parametersExpected2[i][0];
			String valueExpected = parametersExpected2[i][1];
			String valueActual = interpreter.get(name).toString();
			Assert.assertEquals(valueExpected, valueActual);
		}
	}
	
	/**
	 * Checks if the ResponseAssertions were read correctly. 
	 * Tests with an example test file. See the documentation for the intended mapping.
	 */
	@Test
	public void testResponseAssertion() {
	
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
		
		LinkedHashMap<String, List<URLActionData>> TGroups = 
									translater.translateFile(filePath1);
		List<URLActionData> actions = getAllActions(TGroups);
		
		// tests the sample test case
		for (int iAction = 0; iAction < 6; iAction++) {
			URLActionData action = actions.get(iAction);
			List<URLActionDataValidation> validations = action.getValidations();
			int length = validations.size();
			
			for (int iValidation = 0; iValidation < length; iValidation++) {
				URLActionDataValidation validation = validations.get(iValidation);
				
				// Note: validation.get(SomethingSomething) gets the dynamic interpretation
				// from the interpreter if there is one. That means a variable ${var} would
				// be returned with it's value if there is one. ( ${var} -> value )
				
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
				Assert.assertEquals(validationModeExpected[iAction][iValidation], validationMode);
				
				// assert validationContent
				Assert.assertEquals(validationContentExpected[iAction][iValidation], validationContent);
			}
		}
		
		// tests if the response code is validated
		TGroups = translater.translateFile(filePath2);
		actions = getAllActions(TGroups);
		
		URLActionData action = actions.get(0);
		
		int responseCodeExpected = 302;
		int responseCode = action.getHttpResponseCode(); 
		
		Assert.assertEquals(responseCodeExpected, responseCode);
	}
	
	/**
	 * Tests if the response headers can be verified. Tests - woudn't you believe it - with a sample file.
	 */
	@Test
	public void testValidateHeaders() {
			
		LinkedHashMap<String, List<URLActionData>> TGroups = 
						translater.translateFile(filePath1);
		List<URLActionData> actions = getAllActions(TGroups);

		
		URLActionData action = actions.get(8);
		List<URLActionDataValidation> validations = action.getValidations();
		
		String selectionModeExpected[] = {
				"Header", "Header", "Header", "Header", "Header"
		};
		String selectionContentExpected[] = {
				"Expires", "-1", "Expires", "Expires", "Vary"
		};
		String validationModeExpected[] = {
				URLActionDataValidation.EXISTS, URLActionDataValidation.EXISTS, URLActionDataValidation.MATCHES, 
				URLActionDataValidation.MATCHES, URLActionDataValidation.MATCHES
		};
		String validationContentExpected[] = {
				null, null, "-1", "-1", "Accept-Encoding"
		};
		
		for (int i = 0; i < 5; i++) {
			URLActionDataValidation validation = validations.get(i);
			
			String selectionMode = validation.getSelectionMode();
			String selectionContent = validation.getSelectionContent();
			String validationMode = validation.getValidationMode();
			String validationContent = validation.getValidationContent();
			
			Assert.assertEquals(selectionModeExpected[i], selectionMode);
			
			Assert.assertEquals(selectionContentExpected[i], selectionContent);
			
			Assert.assertEquals(validationModeExpected[i], validationMode);
			
			Assert.assertEquals(validationContentExpected[i], validationContent);
		}
		
	}
	
	/**
	 * Checks if the XPath-extractions were read correctly. <br/>
	 * Tests with a known test case. 
	 */
	@Test
	public void testXPathExtractions() {	
		LinkedHashMap<String, List<URLActionData>> TGroups = 
									translater.translateFile(filePath1);
		List<URLActionData> actions = getAllActions(TGroups);
		
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
	
	/**
	 * Tests a Regex extractor that can be mapped with a sample file.
	 */
	@Test
	public void testRegexExtractionPossible() {		
		
		LinkedHashMap<String, List<URLActionData>> TGroups = 
							translater.translateFile(filePath3);
		List<URLActionData> actions = getAllActions(TGroups);
		
		String subSelectionModeExpected[] = {
				URLActionDataStore.REGEXGROUP, null, null
		};
		String subSelectionContentExpected[] = {
				"2", null, null 
		};
		
		// test if subSelectionMode/-Value are read correctly (action 0),
		// if subSelectionMode/-Value are left out when they should be (action 1)
		// and if correct default values are set for subSelectionMode and /-Value (action 2= 
		
		for (int i = 0; i < actions.size(); i++) {
			URLActionData action = actions.get(i);
			URLActionDataStore store = action.getStore().get(0);
			
			String subSelectionModeActual = store.getSubSelectionMode();
			String subSelectionContentActual = store.getSubSelectionContent();
			
			Assert.assertEquals(subSelectionModeExpected[i], subSelectionModeActual);
			Assert.assertEquals(subSelectionContentExpected[i], subSelectionContentActual);

		}
	}
	
	/**
	 * Tests with files that include Regular Expression Extractors that can't be mapped to 
	 * TSNC. Tests if the program crashes for every one of them.
	 * <\br>
	 * A template like $3$$2$ doesn't cause a crash, just a warning and translation to 
	 * Group: 2,
	 */
	@Test
	public void impossibleRegex() {
		
		String genName = "-regex.jmx";
		int crashNum = 0;
		final int crashesExpected = 3;
		
		for (int i = 1; i < 5; i++ ) {
			String fileName = filePath4 + i + genName;
			
			try {				
				@SuppressWarnings("unused")
				LinkedHashMap<String, List<URLActionData>> TGroups = 
											translater.translateFile(fileName);				
			}
			catch (MappingException e) {
				crashNum++;
			}
		}
		Assert.assertEquals(crashesExpected, crashNum);
	}

	/**
	 * Tests with a test file that is full of assertions that can't be mapped to TSNC. 
	 * Tests if any of them were created.
	 */
	@Test
	public void impossibleValidations() {
		
		LinkedHashMap<String, List<URLActionData>> TGroups = 
													translater.translateFile(filePath2);
		List<URLActionData> actions = getAllActions(TGroups);
		
		List<URLActionDataValidation> validations = actions.get(1).getValidations();
		Assert.assertEquals(1, validations.size());
	}
	
	/**
	 * Puts all the actions inside a single list.
	 * 
	 * @return a list of all the actions
	 */
	private List<URLActionData> getAllActions(LinkedHashMap<String, 
													List<URLActionData>> TGroups) {
    	
    	List<URLActionData> actions = new ArrayList<>();
    	for (List<URLActionData> ThreadGroup : TGroups.values()) {
    		actions.addAll(ThreadGroup);
    	}
    	return actions;
	}
}
