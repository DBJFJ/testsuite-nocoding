package com.xceptance.xlt.common.util.action.data;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import bsh.EvalError;

import com.gargoylesoftware.htmlunit.util.NameValuePair;

import com.xceptance.xlt.api.util.XltLogger;
import com.xceptance.xlt.common.util.bsh.ParameterInterpreter;

/*
 * Implementation of the {@link URLActionDataListBuilder} for Jmeters .jmx files. <br/>
 * Only works for a subset of Jmeter functions. Those are:
 * <ul>
 * <li> Storing variables
 * <li> HttpSamplers with url, method and parameters 
 * <li> Response Assertions with ...... TODO
 * <li> RegEx and XPath extractors with .......	TODO
 * </ul>
 * 
 * Configs and properties are handled TODO - How ?
 * Listeners are ignored. TSNCs own result browser will be used.
 */
public class JMXBasedURLActionDataListBuilder extends URLActionDataListBuilder {

	/*
	 * if the getAttributeValue(ATTRNNAME) method was used but the attribute wasn't found,
	 * it will return this value instead
	 */
	private final String NOT_FOUND = "Attribute not found";
	
	/*
	 * if the respnse code should be validated (instead of the response data
	 * or the response header), the selectionMode is temporarily set to this
	 */
	private final String VALIDATE_RESP_CODE = "respCode";
	
	/*
	 * <p>The following constant are used for the tagnames in Jmeter. 
	 * For example, in <Arguments ...> 'Arguments' is the tag name for 
	 * the place where variables are defined.</p>
	 * 
	 * Their names follow the theme T (for tag) + NAME + abbreviation of their function.
	 * An "S" at the end signifies plural. Example: TNAMEVARS for tagNameVariables.
	 */
	
	private final String TNAME_ACTION = "HTTPSamplerProxy"; 
	
	private final String TNAME_VARS = "Arguments";
	
	private final String TNAME_VAR = "elementProp";
	
	private final String TNAME_PARAMS = "collectionProp";
	
	private final String TNAME_PARAM = "elementProp";
	
	private final String TNAME_ASSERT_RESP = "ResponseAssertion";
	
	private final String TNAME_ASSERT_ALL_CONTENT = "collectionProp";
	
	private final String TNAME_ASSERT_ONE_CONTENT = "stringProp";
	
	private final String TNAME_XPATH_EXTRACT = "XPathExtractor";

	/*
	 * Jmeter supports a tree structure, but the elements below a certain node
	 * are not inside the xml tag in the .jmx file. Instead the following tag follows many tags.
	 * Inside that tag are the nodes below the node corresponding to the tag.</br>
	 * For example: An action which contains an assertion
	 * <HTTPSamplerProxy ...> ...</HTTPSamplerProxy> 
	 * <hashtree> 
	 * 		<ResponseAssertion ...> ... </ResponseAssertion> </br>
	 *		<hashtree> ... </hashtree> 
	 * </hashtree> 
	 * 
	 * In this example the inner 'hashtree' tag is for everything below the assertion, the 
	 * outer 'hashtree' tag for everything below the action.
	 */
	private final String TNAME_CONTENT = "hashTree";
	
	/*
	 * <p>The following constant are used for the attribute names in Jmeter. 
	 * For example, in <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" 
	 * testname="test for correcting the mistake afterwards 1" enabled="true"> 
	 * 'testname' is the attribute name for the name of the attribute which defines the actin name.
	 * 
	 * Their names follow the theme ATTR (for attribute) + N (for name) + abbreviation of their function.
	 * An "S" at the end signifies plural. Example: ATTRNACTIONNAME for attributeNameActionName.
	 */
	
	private final String ATTRN_ACTION_NAME = "testname";
	
	private final String ATTRN_ASSERT_NAME = "testname";
	
	private final String ATTRN_ELE_TYPE = "elementType";
	
	/*
	 * <p>the following is a name attribute used to identify what kind of element the tag
	 * is supposed to stand for. It is basically used as a second tag name.</p>
	 * <ul>Example: 
	 * <li> <stringProp name="HTTPSampler.domain">${host}/search</stringProp>
     * <li> <stringProp name="HTTPSampler.port"></stringProp>
     * <li> <stringProp name="HTTPSampler.connect_timeout"></stringProp>
     * <li> <stringProp name="HTTPSampler.response_timeout"></stringProp>
     * <li> <stringProp name="HTTPSampler.protocol"></stringProp>
     * </ul>
	 */
	private final String ATTRN_NAME = "name";
	
	/*
	 * <p>The following constant are used for the attribute values in Jmeter. 
	 * For example, in  <stringProp name="HTTPSampler.domain">${host}/search</stringProp>
	 * 'stringProp' is the tag name, 'name' an attribute name and 'HTTPSampler.domain' an 
	 * attribute value.</p>
	 * 
	 * Their names follow the theme ATTR (for attribute) + V (for value) + abbreviation of their function.
	 * An "S" at the end signifies plural. Example: ATTRVACTIONURL for attributeValueActionUrl.
	 */
	
	private final String ATTRV_ACTION_URL = "HTTPSampler.domain";
	
	private final String ATTRV_ACTION_METHOD = "HTTPSampler.method";
	
	private final String ATTRV_ACTION_PARAM = "Arguments.arguments";
	
	private final String ATTRV_ELE_ISVAR = "Argument";
	
	private final String ATTRV_ELE_ISPARAM = "HTTPArgument";
	
	private final String ATTRV_VAR_NAME = "Argument.name";
	
	private final String ATTRV_VAR_VALUE = "Argument.value";
	
	private final String ATTRV_ENCODE_PARAM = "HTTPArgument.always_encode";
	
	private final String ATTRV_PARAM_NAME = "Argument.name";
	
	private final String ATTRV_PARAM_VALUE = "Argument.value";
	
	// Assertions ...
	
	// if a variable should be validated it's name is inside this tag
	private final String ATTRV_ASSERT_VARIABLE = "Scope.variable";
	
	private final String ATTRV_ASSERT_FIELD_TO_TEST = "Assertion.test_field";
	
	private final String ATTRV_ASSERT_VAL_MODE = "Assertion.test_type";
	
	//Extractors ...
	
	private final String ATTRV_XPATH_EXT_REFNAME = "XPathExtractor.refname";
	
	private final String ATTRV_XPATH_EXT_XPATH = "XPathExtractor.xpathQuery";
	
	
	/*
	 * The following constants are used when important values are in the character
	 * parts of the xml. 
	 * For example in <stringProp name="XPathExtractor.refname">noHitsBanner</stringProp>
	 * 'noHisBanner' would be in the character part of the XML.
	 * .
	 * .
	 * . TODO
	 */
	
	private final String CHAR_ASSERT_RESP_HEADER = "Assertion.response_headers";
	
	private final String CHAR_ASSERT_RESP_CODE = "Assertion.response_code";
	
	
	private List<URLActionData> actions = new ArrayList<URLActionData>();

	public JMXBasedURLActionDataListBuilder(final String filePath,
			final ParameterInterpreter interpreter,
			final URLActionDataBuilder actionBuilder) {
		super(filePath, interpreter, actionBuilder);
		XltLogger.runTimeLogger.debug("Creating new Instance");
	}

	/*
	 * Transforms the .jmx file into a list of URLActionData objects. Returns
	 * said list.
	 * 
	 * (non-Javadoc)
	 * 
	 * @see com.xceptance.xlt.common.util.action.data.URLActionDataListBuilder#
	 * buildURLActionDataList()
	 */
	@Override
	public List<URLActionData> buildURLActionDataList() {
		try {
			InputStream inputstream = new FileInputStream(filePath);
			XMLInputFactory factory = XMLInputFactory.newInstance();
			factory.setProperty(XMLInputFactory.IS_COALESCING, true);
			XMLEventReader reader = factory.createXMLEventReader(inputstream);

			URLActionDataBuilder actionBuilder = new URLActionDataBuilder();
			actionBuilder.setInterpreter(interpreter);

			// start reading and keep reading until the end
			while (reader.hasNext()) {
				
				// look for the next startElement
				XMLEvent event = reader.nextEvent();			
				if (event.isStartElement()) {

					// check it's name and delegate to a subfunction accordingly
					StartElement se = event.asStartElement();			//TODO getTagName
					String name = getTagName(se);					

					switch (name) {
					
					case TNAME_ACTION : {
						
						// an TNAMEACTION is aquivalent to an HttpRequest
						// is aquivalent to an action get and set the testname
						String actionName = getAttributeValue(ATTRN_ACTION_NAME, se);
						URLActionData action = readAction(reader,
								actionBuilder, actionName);
						actions.add(action);
						break;
					}
					case TNAME_VARS : {
						
						// read the variables and stores them
						readVariables(actionBuilder, reader);
						break;
					}
					default : {
						break;
					}
					}
				}
			}

			// TODO for now, exceptionhandling should be changed later
			inputstream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}

		return actions;
	}

	/*
	 * <p>Jmeter uses a tree structure. This method reads everything inside of an object,
	 * for example everything inside of an action (like validations).</p>
	 * 
	 * <p>TODO Which kind of things can have other things inside them?
	 * If it's just an action there's no need to bother ...
	 * Actions can't have actions inside them. Ditto for Assertions and Extractors.</p>
	 * 
	 * <p>If the structure gets complicated, we might have to define predecessor as an enum
	 * and call readContent at the end of major other functions. </p>
	 * 
	 * <p>For example: At the end of readAction readContent is called. It reads the content of the
	 * action, adds what it has to add and gives it back. Only then is the action created.
	 * The enum definition of predecessor includes actionBuilder.</p>
	 */
	private Object readContent (Object predecessor, XMLEventReader reader) {
		
		return null;
	}
	
	/*
	 * <p>Is called when the main parsing method {@link #buildURLActionDataList}
	 * comes upon a StartElement for an action (TNAMEACTION). Parses
	 * the file and creates the action with the appropriate properties </p>
	 * <p>Returns the action when it comes upon the EndElement.</p>
	 */
	private URLActionData readAction(XMLEventReader reader,
			URLActionDataBuilder actionBuilder, String testName)
			throws XMLStreamException {

		// set the testname and the interpreter
		actionBuilder.setName(testName);
		actionBuilder.setInterpreter(interpreter);

		// keep reading until an EndElement with the tag name of
		// TNAMEACTION is the next element
		while (true) {
			
			XMLEvent event = reader.nextEvent();
			
			if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				String name = getTagName(ee);
				if (name.equals(TNAME_ACTION)) {
					break;
				}
			}
			
			// look for StartElements
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();

				// and get the attribute for 'name'
				String name = getAttributeValue(ATTRN_NAME, se);

				// if the name attribute corresponds to an action parameter,
				// set the appropriate action parameter to the content
				switch (name) {
				case ATTRV_ACTION_URL: {
					// read the content
					event = reader.nextEvent();
					String content = event.asCharacters().getData();
					actionBuilder.setUrl(content);
					break;
				}
				case ATTRV_ACTION_METHOD: {
					// read the content
					event = reader.nextEvent();
					String content = event.asCharacters().getData();
					actionBuilder.setMethod(content);
					break;
				}
				case ATTRV_ACTION_PARAM: {
					// read the parameters					
					readParameters(actionBuilder, reader);
					break;
				}
				default: {
					break;
				}
				}
			}
		}
		
		//DEBUG
//		System.out.println("Location b4: " + reader.peek().getLocation());
		
		readActionContent(reader, interpreter, actionBuilder);
		
		//DEBUG
//		System.out.println("Location after: " + reader.peek().getLocation());
		
		// build the action and reset the URLActionDataBuilder
		URLActionData action = actionBuilder.build();
		return action;
	}

	/*
	 * Reads the things inside an action, namely. Called after an action. Reads until the
	 * TNAMECONTENT tag that closes the TNAMECONTENT right after the action.
	 * <li>XPath Extractions  
	 * <li>Response Assertions 
	 */
	private void readActionContent(XMLEventReader reader,
			ParameterInterpreter interpreter, URLActionDataBuilder actionBuilder) throws XMLStreamException {
		
		List<URLActionDataValidation> validations = new ArrayList<URLActionDataValidation>();
		URLActionDataValidationBuilder validationBuilder = new URLActionDataValidationBuilder();
		
		List<URLActionDataStore> variablesToExtract = new ArrayList<URLActionDataStore>();
		URLActionDataStoreBuilder storeBuilder = new URLActionDataStoreBuilder();
		

		// the first TNAMECONTENT tag should be right here, so there is no need to look for it
		
		// Increment for every TNAMECONTENT tag that opens, decrement for every TNAMECONTENT 
		// tag that closes. Exit when zero is reached. (To make sure you exit with the right
		// tag.)
		int treeLevel = 0;
	
		while (true) {	
			XMLEvent event = reader.nextEvent();
						
			if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				String name = getTagName(ee);

				if (name.equals("hashTree")) {
					treeLevel--;
					
					if (treeLevel == 0) {
						break;
					}
				}
			}
			
			// look for StartElements
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();
				
				// get the tagname
				String tagName = getTagName(se);
				
				switch (tagName) {
				case TNAME_CONTENT: {
				
					// we just went a bit further down in the tree
					treeLevel++;
					break;
				}
				case TNAME_ASSERT_RESP: {
					
					// check Response Assertion, add it to the actionBuilder
					String name = getAttributeValue(ATTRN_ASSERT_NAME, se);
					readResponseAssertion(name, reader, actionBuilder);
					break;
				}
				
				case TNAME_XPATH_EXTRACT: {
					
					// read the XPathExtractor
					String selectionMode = URLActionDataStore.XPATH;
					storeBuilder.setInterpreter(interpreter);
					URLActionDataStore variableToExtract = readExtractor(selectionMode, 
							reader, storeBuilder);
					if (variableToExtract != null) {
						variablesToExtract.add(variableToExtract);
					}
					storeBuilder.reset();
					break;
				}
		
				default:
					break;
				}
			}
		}
		
		// add all the validations and extractions to the actionBuilder
		// unless there are none, of course
		if (validations.size() > 0) {
			actionBuilder.setValidations(validations);
		}
		if (variablesToExtract.size() > 0) {
			actionBuilder.setStore(variablesToExtract);
		}
	}

	/*
	 * <p>Is called if the tag name of a StartElement equals TNAMEVARS. Reads
	 * multiple variables and stores them in the ParameterInterpreter</p>
	 */
	private void readVariables(URLActionDataBuilder actionBuilder,
			XMLEventReader reader) throws XMLStreamException {
		
		// loop until the next element is an EndElement that closes
		// the TNAMEVARS tag
		
		while (true) {
			
			XMLEvent event = reader.nextEvent();
			
			if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				String name = getTagName(ee);
				if (name.equals(TNAME_VARS)) {
					break;
				}
			}
			
			// look for startelements ...
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();

				// and has an 'elementType' attribute
				String elementType = getAttributeValue(ATTRN_ELE_TYPE, se);

				// and get the elements tag name
				String name = getTagName(se);

				// if they both fit and it looks like a single argument,
				// call the readArgument method to let it read it
				if (name.equals(TNAME_VAR)
						&& elementType.equals(ATTRV_ELE_ISVAR)) {
					readVariable(reader);
				}
			}
		}
	}

	/*
	 * is called inside the readArguments method to parse a single argument
	 * parses a single Argument and saves it in the interpreter
	 */
	private void readVariable(XMLEventReader reader) throws XMLStreamException {

		String argsName = null;
		String argsValue = null;
		
		// loop until the next Element is an EndElement with 
		// the tag name of TNAMEVAR
		while (true) {
			XMLEvent event = reader.nextEvent();
			
			if (event.isEndElement()) {
			EndElement ee = event.asEndElement();
			String name = getTagName(ee);
			if (name.equals(TNAME_VAR)) {
				break;
				}
			}

			// look for StartElements
			if (event.isStartElement()) {

				StartElement se = event.asStartElement();

				// if the attribute for 'name' exists
				String name = getAttributeValue(ATTRN_NAME, se);

				// and it is the right String, get the content of the tag
				// and save it as name or value, depending
				switch (name) {
				case ATTRV_VAR_NAME: {
					event = reader.nextEvent();
					argsName = getTagContent(event);
					break;
				}
				case ATTRV_VAR_VALUE: {
					event = reader.nextEvent();
					argsValue =  getTagContent(event);
					break;
				}
					default: {
					break;
				}
				}
			}
		}
		// save the aquired arguments
		NameValuePair nvp = new NameValuePair(argsName, argsValue);
		try {
			this.interpreter.set(nvp);
		} catch (EvalError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * Is called inside an action and parses the parameters 
	 */
	private void readParameters(URLActionDataBuilder actionBuilder,
			XMLEventReader reader) throws XMLStreamException {
		
		List<NameValuePair> parameters = new ArrayList<>();
		
		// loop until the loop is closed with an EndElement with the tag name TNAMEPARAMS
		// all parameters should be inside a single collectionProp tag
		while (true) {		
			XMLEvent event = reader.nextEvent();
			
			if (event.isEndElement()) {
			EndElement ee = event.asEndElement();
			String name = getTagName(ee);
			if (name.equals(TNAME_PARAMS)) {
				break;
				}
			}

			// look for startelements ...
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();

				// and it's tag name is ATTRNELETYPE
				String name = getTagName(se);
				if (name.equals(TNAME_PARAM)) {

					// if the elementType attribute is 'HTTPArgument'
					String elementType = getAttributeValue(ATTRN_ELE_TYPE, se);
					if (elementType.equals(ATTRV_ELE_ISPARAM)) {

						// read the single detected parameter
						parameters = readParameter(actionBuilder, reader, parameters);
					}
				}
			}
		}
		
		// set the parameters
		if (parameters != null) {
			actionBuilder.setParameters(parameters);
		}
	}

	/*
	 * Is called inside the readParameters method to parse a single parameter 
	 * <p>Parses a single Parameter with encoding, name and value. 
	 * Sets the former and adds the NameValuePair with name and value to the correct list.</p>
	 */
	private List<NameValuePair> readParameter(URLActionDataBuilder actionBuilder, 
			XMLEventReader reader, List<NameValuePair> parameters) throws XMLStreamException {

		String encoded = null;
		String parameterName = null;
		String parameterValue = null;
		
		// loop until the loop is closed with an EndElement with the tag name TNAMEPARAM
		// all parameters should be inside a single TNAMEPARAM tag
		while (true) {			
			XMLEvent event = reader.nextEvent();
			
			if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				String name = getTagName(ee);
				if (name.equals(TNAME_PARAM)) {
					break;
				}
			}

			// look for startelements
			if (event.isStartElement()) {

				StartElement se = event.asStartElement();

				String name = getAttributeValue(ATTRN_NAME, se);

				// if the name attribute is the right String, get the content of the tag
				// and set something, depending on the ATTRNNAME value
				switch (name) {
				case ATTRV_ENCODE_PARAM: {
					event = reader.nextEvent();
					encoded = getTagContent(event);
					break;
				}
				case ATTRV_PARAM_VALUE: {
					event = reader.nextEvent();
					parameterValue = getTagContent(event);
					break;
				}
				case "Argument.metadata": {
					// there doesn't seem to be an equivalent in TSNC
					break;
				}
				case "HTTPArgument.use_equals": {
					// there doesn't seem to be an equivalent in TSNC
					break;
				}
				case ATTRV_PARAM_NAME: {
					event = reader.nextEvent();
					parameterName = getTagContent(event);
					break;
				}
				default: {
					break;
				}
				}
			}
		}
		
		// set the encoding and the parameter values
		// the encoding is set for the whole action here whereas in Jmeter it was set for a single parameter
		actionBuilder.setEncodeParameters(encoded);
		NameValuePair nameValue = new NameValuePair(parameterName, parameterValue);
		parameters.add(nameValue);
		return parameters;
	}
	
	private URLActionDataValidation readResponseAssertion(String name, 
			XMLEventReader reader, URLActionDataBuilder actionBuilder) throws XMLStreamException {

		// variable check if the Response Code, the Response Headers or the Response Data
		// should be validated
		String selectionMode = null;
		String selectionContent = null;
		String validationMode = null;
		List<String> allValidationContent = new ArrayList<String>();
		
		// loop until the loop is closed with an EndElement with the tag name TNAME_ASSERT_RESP
		// all parameters should be inside a single TNAME_ASSERT_RESP tag
		while (true) {			
			XMLEvent event = reader.nextEvent();
			
			if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				String tagName = getTagName(ee);
				if (tagName.equals(TNAME_ASSERT_RESP)) {
					break;
				}
			}
			
			// look for StartElements
			if (event.isStartElement()) {

				StartElement se = event.asStartElement();
				String tagName = getTagName(se);
				
				if (tagName.equals(TNAME_ASSERT_ALL_CONTENT)) {
					
					// inside the TNAME_ASSERT_ALL_CONTENT are all the values that
					// should be validated. Read them
					allValidationContent = readallValidationContent(reader);
				}
				
				String attrName = getAttributeValue(ATTRN_NAME, se);
				switch (attrName) {
				case ATTRV_ASSERT_VARIABLE:
					
					// shows that a variable should be validated
					// and which variable to validate
					selectionMode = URLActionDataValidation.VAR;
					event = reader.nextEvent();
					selectionContent = getTagContent(event);
					selectionContent = "${" + selectionContent + "}";
					break;
					
				case ATTRV_ASSERT_FIELD_TO_TEST:
					
					// can determine the selectionMode 
					// if it's the code or header. The mapping can be awkward here
					event = reader.nextEvent();
					String selectionModeInJmt = getTagContent(event);
					selectionMode = getSelectionModeFromJmt(selectionModeInJmt, selectionMode);
					break;
					
				case ATTRV_ASSERT_VAL_MODE:
					
					// determines the validationMode
					event = reader.nextEvent();
					int valModeInInt = Integer.parseInt(getTagContent(event));
					validationMode = getValidationModeFromInt(valModeInInt);
					break;

				default:
					break;
				}
			}
		}
	
		createValidations(name, selectionMode, selectionContent, validationMode, 
				allValidationContent, actionBuilder);
		
		return null; 
	}
	
	/*
	 * Builds the validations from the given inputs and adds them to the action
	 */
	private void createValidations(String name,
			String selectionMode, String selectionContent,
			String validationMode,
			List<String> allValidationContent, URLActionDataBuilder actionBuilder) {
		
		// for all the cases that coudn't be mapped
		if (selectionMode == null) {
			selectionMode = URLActionDataValidation.REGEXP;
		}		
		if (selectionContent == null) {
			// this _should_ be everything
			selectionContent = ".*";
		}
		
		// if the response code should be validated
		if (selectionMode.equals(VALIDATE_RESP_CODE)) {
			String httpResponseCode = allValidationContent.get(0);
			actionBuilder.setHttpResponceCode(httpResponseCode);
		}
		else {
			for (String validationContent : allValidationContent) {
				
				// if the selectionMode is 'matches', make it so it means
				// 'contains a pattern which matches', not matches as a whole
				if (validationMode == URLActionDataValidation.MATCHES && 
						selectionMode != URLActionDataValidation.VAR) {
					validationContent = ".*" + validationContent + ".*";					
				}
				
				URLActionDataValidation validation = new URLActionDataValidation(name,
						selectionMode, selectionContent, validationMode, 
						validationContent, interpreter);
				actionBuilder.addValidation(validation);
			}
		}
	}

	/*
	 * Maps the value from Jmeters Pattern Matching Rules to TSNCs validationMode.
	 * Since a large part of them don't match it defaults to RegEx.
	 * 
	 */
	private String getSelectionModeFromJmt(String selectionContentInJmt, String selectionMode) {
		
		// if the selectionMode isn't set already
		if (selectionMode == null)	{	
			switch (selectionContentInJmt) {
			
			// and if a response code or a header should be validated
			// set the selectionMode
			case CHAR_ASSERT_RESP_CODE:
				selectionMode = VALIDATE_RESP_CODE;
				break;
			
			case CHAR_ASSERT_RESP_HEADER:
				selectionMode = URLActionDataValidation.HEADER;
				break;

			default:
				break;
			}
		}
		
		return selectionMode;
	}

	/*
	 * Maps the value from Jmeters Pattern Matching Rules to TSNCs validationMode
	 */
	private String getValidationModeFromInt(int valModeInInt) {
		
		String validationMode = null;
		
		switch (valModeInInt) {
		case 1:
			// Jmeter: Matches
			validationMode = URLActionDataValidation.MATCHES;
			break;
		
		case 2:
			// Jmeter: Contains
			validationMode = URLActionDataValidation.EXISTS;
			break;

		case 8:
			// Jmeter: Equal
			validationMode = URLActionDataValidation.TEXT;
			break;
			
		case 16:
			// Jmeter: Substring
			validationMode = URLActionDataValidation.EXISTS;
			break;
			
		default:
			//TODO log warning - validation mode not detected
			// set the validationMode to EXISTS so it has at least something
			validationMode = URLActionDataValidation.EXISTS;
			break;
		}
		return validationMode;
	}

	/*
	 * Called in the readResponseAssertion method. 
	 * Reads until the end of an TNAME_ASSERT_CONTENT tag and returns a list
	 * of the values that should be validated
	 */
	private List<String> readallValidationContent(XMLEventReader reader) throws XMLStreamException {
		
		List<String> allValidationContent = new ArrayList<>();
		
		while (true) {
			XMLEvent event = reader.nextEvent();
			
			if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				String tagName = getTagName(ee);
				if (tagName.equals(TNAME_ASSERT_ALL_CONTENT)) {
					break;
				}
			}
			
			// look for StartElements
			if (event.isStartElement()) {

				StartElement se = event.asStartElement();
				String tagName = getTagName(se);

				// if the tag name is equal to ...
				if (tagName.equals(TNAME_ASSERT_ONE_CONTENT)) {
					
					// one content was found. Read it and 
					// put it on the list
					event = reader.nextEvent();
					String content = getTagContent(event);
					allValidationContent.add(content);
				}
			}
		}
		
		return allValidationContent;
	}

	/*
	 * Should be called when the readActionContent method comes upon a TNAMEXPATHEXTRACT tag.
	 * Reads until the end of the TNAMEXPATHEXTRACT tag and returns an URLActionDataStore object.
	 */
	private URLActionDataStore readExtractor(String selectionMode, 
			XMLEventReader reader, URLActionDataStoreBuilder storeBuilder) throws XMLStreamException {
				
		String name = null;
		String selectionContent = null;
		
		// loop until the loop is closed with an EndElement with the tag name TNAMEXPATHEXTRACT
		// all parameters should be inside a single TNAMEXPATHEXTRACT tag
		while (true) {			
			XMLEvent event = reader.nextEvent();
			
			if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				String tagName = getTagName(ee);
				if (tagName.equals(TNAME_XPATH_EXTRACT)) {
					break;
				}
			}

			// look for StartElements
			if (event.isStartElement()) {

				StartElement se = event.asStartElement();

				String attrName = getAttributeValue(ATTRN_NAME, se);

				// if the name attribute is the right String, get the content of the tag
				// and set something, depending on the ATTRNNAME value
				switch (attrName) {
				
				// for the XPath ...
				case "XPathExtractor.default": {
					// there doesn't seem to be an equivalent in TSNC
					break;
				}
				case ATTRV_XPATH_EXT_REFNAME: {
					event = reader.nextEvent();
					name = getTagContent(event);
					break;
				}
				case ATTRV_XPATH_EXT_XPATH: {
					event = reader.nextEvent();
					selectionContent = getTagContent(event);
					break;
				}
				case "XPathExtractor.validate": {
					// there doesn't seem to be an equivalent in TSNC
					break;
				}
				case "XPathExtractor.tolerant": {
					// there doesn't seem to be an equivalent in TSNC
					break;
				}
				case "XPathExtractor.namespace": {
					// there doesn't seem to be an equivalent in TSNC
					break;
				}
				case "XPathExtractor.fragment": {
					// there doesn't seem to be an equivalent in TSNC
					break;
				}
				
				// "Sample.scope", "XPathExtractor.show_warnings" and "XPathExtractor.report_errors"
				// presumably don't have aquivalents either
				// ... no longer for the XPath
				
				//TODO just add the other extractors in here?
				
				default: {
					break;
				}
				}
			}
		}
		
		storeBuilder.setName(name);
		storeBuilder.setSelectionMode(selectionMode);
		storeBuilder.setSelectionContent(selectionContent);
		URLActionDataStore varToExtract = storeBuilder.build();
		return varToExtract;
	}
	
	/*
	 * Returns the tagname of a StartElement. Analogous to getTagName(EndElement ee).
	 */
	private String getTagName(StartElement se) {
		QName qname = se.getName();
		String name = qname.getLocalPart();
		return name;
	}
	
	/*
	 * Returns the tagname of an EndElement. Analogous to getTagName(StartElement se).
	 */
	private String getTagName(EndElement ee) {
		QName qname = ee.getName();
		String name = qname.getLocalPart();
		return name;
	}
	
	/*
	 * <p>Gets the attributeValue from a StartElement and an ATTRNNAME. </p>
	 * <p>Returns the value of the NOTFOUND constant if no attribute with that name exists.</p> 
	 */
	private String getAttributeValue(String attributeName, StartElement se) {
		
		String attributeValue = null;
		QName qname = new QName(attributeName); 
		if (se.getAttributeByName(qname) == null) {
			attributeValue = NOT_FOUND;
		}
		else {
			Attribute attribute = se.getAttributeByName(qname);
			attributeValue = attribute.getValue();
		}
		
		return attributeValue;
	}
	
	private String getTagContent(XMLEvent event) {
		if (event.isCharacters()) {
			Characters characters = event.asCharacters();
			String content = characters.getData();
			return content;
		}
		else {
			// this really shoudn't happen
			//TODO log a warning
			String warning = "An unexpected error occured during the Jmeter -> TSNC conversion." +
							"tried to get tag content when none was there";
			System.out.println(warning);
			return warning;
		}
	}
}
