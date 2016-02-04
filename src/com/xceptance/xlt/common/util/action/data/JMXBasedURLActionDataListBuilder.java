package com.xceptance.xlt.common.util.action.data;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
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

/**
 * Implementation of the {@link URLActionDataListBuilder} for Jmeters .jmx files. <br/>
 * Generally speaking, TSNC will translate tests following this structure: 
 *
 * <ul>
 * <li>Thread Group</li>
 * <ul> 
 * <li>(Variable Declaration)
 * <li>HTTP Request Sampler
 * <ul>
 * <li>XPath Extractions
 * <li>Response Assertions
 * </ul>
 * </ul>
 * </ul>
 * 
 * (See the documentation file for details.)
 * 
 * <p>If the Jmeter test does not follow this structure and/ or includes other 
 * important elements it won't be translated correctly.</p>
 * 
 * Load Test configurations like the number of threads or the ramp up time won't be translated. </br>
 * Listeners are ignored. TSNCs own result browser will be used.
 * 
 * On the technical side Jmeter saves it's tests in an xml file without a DTD. 
 * This class parses the xml file using StAX. The various constants are the names of the tags, the attributes
 * or their values. It doesn't do anything else, so there are some incongruities where Jmeters functionality
 * doesn't match TSNCs.
 */
public class JMXBasedURLActionDataListBuilder extends URLActionDataListBuilder {

	/*
	 * The following constant are used for the tagnames in Jmeter. 
	 * For example, in <Arguments ...> 'Arguments' is the tag name.
	 * 
	 * Their names follow the theme T (for tag) + NAME "_" + abbreviation of their function.
	 * An "S" at the end signifies plural. Example: TNAME_VARS for tagNameVariables.
	 * Underlines are used for readability.
	 */
	
	private final String TNAME_THREAD_GROUP = "ThreadGroup";
	
	private final String TNAME_ACTION = "HTTPSamplerProxy"; 
	
	private final String TNAME_VARS = "Arguments";
	
	private final String TNAME_VAR = "elementProp";
	
	private final String TNAME_PARAMS = "collectionProp";
	
	private final String TNAME_PARAM = "elementProp";
	
	private final String TNAME_ASSERT_RESP = "ResponseAssertion";
	
	private final String TNAME_ASSERT_ALL_VALUES = "collectionProp";
	
	private final String TNAME_ASSERT_ONE_CONTENT = "stringProp";
	
	private final String TNAME_XPATH_EXTRACT = "XPathExtractor";
	
	private final String TNAME_REGEX_EXTRACT = "RegexExtractor";
	
	/*
	 * Jmeter supports a tree structure, but the elements below a certain node
	 * are not inside the xml tag in the xml file. Instead a hashtree tag follows all tags with something 
	 * below them in the tree structure. Inside the hashtree tag are the nodes below the tag in question.
	 * For example: An action which contains an assertion 
	 * 
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
	
	private final String TNAME_TEST_CONFIG = "ConfigTestElement";
	
	private final String TNAME_HEADERS = "HeaderManager";
	
	private final String TNAME_HEADER = "elementProp";
	
	/*
	 * <p>The following constants are used for the attribute names in Jmeter. 
	 * For example, in <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" 
	 * testname="test for correcting the mistake afterwards 1" enabled="true"> 
	 * 'testname' is the attribute name for the name of the attribute which defines the action name.
	 * 
	 * Their names follow the theme ATTR (for attribute) + N (for name) + abbreviation of their function.
	 * An "S" at the end signifies plural. Example: ATTRNACTIONNAME for attributeNameActionName.
	 */
	
	/*
	 * The following is a name attribute often used to identify what kind of element the tag
	 * is supposed to stand for. It is basically used as a second tag name
	 * Example: 
	 * <stringProp name="HTTPSampler.domain">${host}/search</stringProp>
     * <stringProp name="HTTPSampler.port"></stringProp>
     * <stringProp name="HTTPSampler.connect_timeout"></stringProp>
     * <stringProp name="HTTPSampler.response_timeout"></stringProp>
     * <stringProp name="HTTPSampler.protocol"></stringProp>
	 */
	
	private final String ATTRN_NAME = "name";
		
	private final String ATTRN_ACTION_NAME = "testname";
	
	private final String ATTRN_ASSERT_NAME = "testname";
	
	private final String ATTRN_ELE_TYPE = "elementType";
	
	/*
	 * The following constant are used for the attribute values in Jmeter. 
	 * For example, in  <stringProp name="HTTPSampler.domain">${host}/search</stringProp>
	 * 'stringProp' is the tag name, 'name' an attribute name and 'HTTPSampler.domain' an 
	 * attribute value.
	 * 
	 * Their names follow the theme ATTR (for attribute) + V (for value) + abbreviation of their function.
	 * An "S" at the end signifies plural. Example: ATTRVACTIONURL for attributeValueActionUrl.
	 */
	
	private final String ATTRV_ACTION_PROTOC = "HTTPSampler.protocol";
	
	private final String ATTRV_ACTION_URL = "HTTPSampler.domain";
	
	private final String ATTRV_ACTION_METHOD = "HTTPSampler.method";
	
	private final String ATTRV_ELE_ISPARAM = "HTTPArgument";
			
	private final String ATTRV_ACTION_PARAM = "Arguments.arguments";
	
	private final String ATTRV_ENCODE_PARAM = "HTTPArgument.always_encode";
	
	private final String ATTRV_PARAM_NAME = "Argument.name";
	
	private final String ATTRV_PARAM_VALUE = "Argument.value";
	
	private final String ATTRV_ELE_ISVAR = "Argument";
	
	private final String ATTRV_VAR_NAME = "Argument.name";
	
	private final String ATTRV_VAR_VALUE = "Argument.value";
	
	// Assertions ...
	
	// if a variable should be validated it's name is inside this tag
	
	private final String ATTRV_ASSERT_VARIABLE = "Scope.variable";
	
	private final String ATTRV_ASSERT_FIELD_TO_TEST = "Assertion.test_field";
	
	private final String ATTRV_ASSERT_VAL_MODE = "Assertion.test_type";
	
	//Extractors ...
	
	private final String ATTRV_XPATH_EXT_REFNAME = "XPathExtractor.refname";
	
	private final String ATTRV_XPATH_EXT_XPATH = "XPathExtractor.xpathQuery";
	
	private final String ATTRV_REGEX_EXT_REFNAME = "RegexExtractor.refname";
	
	private final String ATTRV_REGEX_EXT_REGEX = "RegexExtractor.regex";
	
	
	/*
	 * The following constants are used when important values are in the character
	 * parts of the xml. 
	 * For example in <stringProp name="XPathExtractor.refname">noHitsBanner</stringProp>
	 * 'noHisBanner' would be in the character part of the XML.
	 */
	
	private final String CHAR_ASSERT_RESP_HEADER = "Assertion.response_headers";
	
	private final String CHAR_ASSERT_RESP_MESSAGE = "Assertion.response_message";
	
	private final String CHAR_ASSERT_RESP_CODE = "Assertion.response_code";
	
	// now finally all the xml constants used in this class are defined ...
	
	/*
	 * if the getAttributeValue(ATTRNNAME) method was used but the attribute wasn't found,
	 * it will return this value instead
	 */
	private final String NOT_FOUND = "Attribute not found";
	
	/*
	 * If the respnse code should be validated (instead of the response data
	 * or the response header), the selectionMode is temporarily set to this
	 */
	private final String VALIDATE_RESP_CODE = "respCode";
	
	private String defaultProtocol = null;
	
	private List<URLActionData> actions = new ArrayList<URLActionData>();

	/**
	 * 
	 * @param filePath the path to he Jmeter file to parse
	 * @param interpreter the ParameterInterpreter for variables {@link #ParameterInterpreter}
	 * @param actionBuilder {@link URLActionDataBuilder}
	 */
	public JMXBasedURLActionDataListBuilder(final String filePath,
			final ParameterInterpreter interpreter,
			final URLActionDataBuilder actionBuilder) {
		super(filePath, interpreter, actionBuilder);
		XltLogger.runTimeLogger.debug("Creating new Instance");
	}

	/**
	 * Transforms the .jmx file from Jmeter into a list of URLActionData objects. 
	 * Returns said list.
	 * 
	 * (non-Javadoc)
	 * 
	 * @see com.xceptance.xlt.common.util.action.data.URLActionDataListBuilder#
	 * buildURLActionDataList()
	 */
	@Override
	public List<URLActionData> buildURLActionDataList() {
		
		XltLogger.runTimeLogger.debug("Starting Jmeter -> TSNC translation ...");
		
		try {
			InputStream inputstream = new FileInputStream(filePath);
			XMLInputFactory factory = XMLInputFactory.newInstance();
			factory.setProperty(XMLInputFactory.IS_COALESCING, true);
			XMLEventReader reader = factory.createXMLEventReader(inputstream);
		
			// start reading and keep reading until the end
			while (reader.hasNext()) {
			
			// look for the next startElement
				XMLEvent event = reader.nextEvent();			
				if (event.isStartElement()) {

					StartElement se = event.asStartElement();
					String tagName = getTagName(se);
					
					if (tagName.equals(TNAME_THREAD_GROUP)) {
						String name = getAttributeValue(ATTRN_ACTION_NAME, se);
						XltLogger.runTimeLogger.info("Reading " + name + "...");
						List<URLActionData> testCase = readThreadGroup(reader);
						
						// dump it into a yaml file
						Path dumpFile = Paths.get("./config/data" + name + ".yml");
						try {
							YAMLBasedURLActionDataListBuilder.dumpActionsYaml(testCase,	dumpFile);
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}	
						
						break;
					}
				}
			}
		}        
		catch (final FileNotFoundException e)  {
	    
			final String message = MessageFormat.format("File: \"{0}\" not found!",
	                                                       this.filePath);
	        XltLogger.runTimeLogger.warn(message);
	        throw new IllegalArgumentException(message + ": " + e.getMessage());
	    }
		catch (XMLStreamException e) {
	        XltLogger.runTimeLogger.error("Jmeters XML Stream was interrupted");
	        throw new IllegalArgumentException(e.getMessage());
		}
		
		return actions;
	}
	
	/**
	 * Reads a Jmeter threadgroup and all of it's content, which is aquivalent to a TSNC test case. 
	 * 
	 * @param reader the XMLEventReader with it's position
	 * @return a list of URLActionData objects to be returned by this builder
	 * @throws XMLStreamException
	 */
	private List<URLActionData> readThreadGroup (XMLEventReader reader) throws XMLStreamException {
		
		URLActionDataBuilder actionBuilder = new URLActionDataBuilder();
		actionBuilder.setInterpreter(interpreter);
			
		while (true) {
			XMLEvent event = reader.nextEvent();
			
			if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				String name = getTagName(ee);
				if (name.equals(TNAME_THREAD_GROUP)) {
					break;
				}
			}
			// many of Jmeters Load Test Configurations are here
		}
		
		// read the content of the ThreadGroup
		// the first  tag should be right here, so there is no need to look for it
		
		// Increment for every TNAME_CONTENT tag that opens, decrement for every TNAME_CONTENT 
		// tag that closes. Exit when zero is reached. (To make sure you exit with the right
		// tag.)
		int treeLevel = 0;
		while (true) {
			XMLEvent event = reader.nextEvent();
			
			if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				String name = getTagName(ee);
				if (name.equals(TNAME_CONTENT)) {
					if (name.equals(TNAME_CONTENT)) {
						treeLevel--;
						
						if (treeLevel == 0) {
							break;
						}
					}
				}
			}
			
			// check the events attribute name name and delegate to a subfunction accordingly
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();			
				String name = getTagName(se);					

				switch (name) {
				case TNAME_TEST_CONFIG : {
					
					// read the defaults defined here(protocol)
					readDefaults(reader);
					break;
				}
				case TNAME_HEADERS : {
					
					// read default headers
					List<NameValuePair> d_headers = readHeaders(reader);
					actionBuilder.setDefaultHeaders(d_headers);
					break;
				}
				case TNAME_VARS : {
					
					// read the variables and stores them
					readVariables(actionBuilder, reader);
					break;
				}
				case TNAME_ACTION : {
				
					// an TNAME_ACTION is aquivalent to an HttpRequest
					// is aquivalent to an action get and set the testname
					String actionName = getAttributeValue(ATTRN_ACTION_NAME, se);
					URLActionData action = readAction(reader,
							actionBuilder, actionName);
					actions.add(action);
					break;
				}
				case TNAME_CONTENT : 
					treeLevel++;
				default : {
					break;
				}
				}
			}
		}
		return actions;
	}
	
	/**
	 * Is called inside of {@link #readThreadGroup} if the tag name of a StartElement equals {@link #TNAME_VARS}. 
	 * Reads multiple variables with their names and values and stores them 
	 * in the ParameterInterpreter.
	 * 
	 * @param actionBuilder the actionBuilder in which interpreters the variables will be stored
	 * @param reader the XMLEventReader with it's position
	 * @throws XMLStreamException
	 */
	private void readVariables(URLActionDataBuilder actionBuilder,
			XMLEventReader reader) throws XMLStreamException {
		
		XltLogger.runTimeLogger.debug("Storing Variables ...");
		
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
			
			// look for StartElements ...
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
	
	/**
	 * Is called inside the {@link #readVariables} method to parse a single variable.
	 * Parses a single vaiable with name and value and saves it in the interpreter.
	 * 
	 * @param reader the XMLEventReader with it's position
	 * @throws XMLStreamException
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
		} 
		catch (EvalError e) {
			XltLogger.runTimeLogger.warn("Coudn't set variable: " + argsName + "=" + argsValue);
		}
	}
	
	/**
	 * Reads the default values that can be read. Which is to say the default protocol.
	 * Is called inside of {@link #readThreadGroup}.
	 * 
	 * @param reader the XMLEventReader with it's position.
	 * @throws XMLStreamException
	 */
	private void readDefaults(XMLEventReader reader) throws XMLStreamException {
		
		while (true) {
			XMLEvent event = reader.nextEvent();
			
			if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				String name = getTagName(ee);
				if (name.equals(TNAME_TEST_CONFIG)) {
					break;
				}
			}
			
			// look for StartElements
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();

				// and get the attribute for 'name'
				String name = getAttributeValue(ATTRN_NAME, se);

				switch (name) {
				case ATTRV_ACTION_PROTOC:
					event = reader.nextEvent();
					defaultProtocol = getTagContent(event);
					break;

				default:
					break;
				}
			}
		}	
	}
	
	/**
	 * Should be called in {@link #readThreadGroup}  to read the default headers  
	 * or {@link #readActionContent} to read the normal headers of an action.
	 * 
	 * @param reader the XMLEventReader with it's position
	 * @return a List of NameValuePairs aquivalent to the headers with name and value
	 * @throws XMLStreamException
	 */
	private List<NameValuePair> readHeaders(XMLEventReader reader) throws XMLStreamException {
	
		List<NameValuePair> headers = new ArrayList<NameValuePair>();
		while (true) {			
			XMLEvent event = reader.nextEvent();
			
			if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				String tagName = getTagName(ee);
				if (tagName.equals(TNAME_HEADERS)) {
					break;
				}
			}
			
			// look for StartElements
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();
				
				String tagName = getTagName(se);
				if (tagName.equals(TNAME_HEADER)) {
					
					NameValuePair header = readHeader(reader);
					headers.add(header);
				}
			}
		}
		return headers;
	}
	
	/**
	 * Called in {@link #readHeaders}. Reads a single header and returns it as a NameValuePair 
	 * 
	 * @param reader the XMLEventReader with it's position
	 * @return a NameValuePair aquivalent to a single header with name and value
	 * @throws XMLStreamException
	 */
	private NameValuePair readHeader(XMLEventReader reader) throws XMLStreamException {
		String name = null;
		String value = null;
		
		while (true) {			
			XMLEvent event = reader.nextEvent();
			
			if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				String tagName = getTagName(ee);
				if (tagName.equals(TNAME_HEADER)) {
					break;
				}
			}
			
			// look for StartElements
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();
				
				String attributeName = getAttributeValue(ATTRN_NAME, se);
					
				switch (attributeName) {
				case "Header.name":
					event = reader.nextEvent();
					name = getTagContent(event);
					break;
				case "Header.value":
					event = reader.nextEvent();
					value = getTagContent(event);
					break;
				default:
					break;
				}
			}
		}
		NameValuePair header = new NameValuePair(name, value);
		return header;
	}
	
	/**
	 * Is called when the main parsing method {@link #readThreadGroup}
	 * comes upon a StartElement for an action {@link #TNAME_ACTION}. Parses
	 * the file and creates the action with the appropriate properties 
	 * Returns the action when it comes upon the EndElement.
	 * 
	 * @param reader the XMLEventReader with it's position
	 * @param actionBuilder the actionBuilder with possible default values
	 * @param testName and the name of the action to read
	 * @return an {@link URLActionData} object 
	 * @throws XMLStreamException
	 */
	private URLActionData readAction(XMLEventReader reader,
			URLActionDataBuilder actionBuilder, String testName)
			throws XMLStreamException {
		
		
		XltLogger.runTimeLogger.debug("Reading new Action: " + testName + "...");
		
		String protocol = defaultProtocol;
		String url = null;
		
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
				
				case ATTRV_ACTION_PROTOC :
					// read the protocol
					event = reader.nextEvent();
					if (event.isCharacters()) {
						protocol = event.asCharacters().getData();
					}
					break;
				case ATTRV_ACTION_URL: {
					// read the content
					event = reader.nextEvent();
					url = event.asCharacters().getData();
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
		
		// add protocol and url together, set the url
		
		// set the protocol to http if it wasn't specified yet
		if (protocol == null) {
			protocol = "http";
		}
		url = protocol + "://" + url;
		actionBuilder.setUrl(url);
		
		readActionContent(reader, interpreter, actionBuilder);
		
		// build the action and reset the URLActionDataBuilder
		URLActionData action = actionBuilder.build();
		return action;
	}

	/**
	 * Is called inside {@link #readAction} to read the parameters. 
	 * 
	 * @param actionBuilder the actionBuilder in which the parameters will be saved
	 * @param reader the XMLEventReader with it's position
	 * @throws XMLStreamException
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

	/**
	 * Is called inside the readParameters method to parse a single parameter 
	 * <p>Parses a single Parameter with encoding, name and value. 
	 * Sets the former and adds the NameValuePair with name and value to the correct list.</p>
	 * 
	 * @param actionBuilder the actionBuilder where the encoding option will be saved.
	 * @param reader the XMLEventReader with it's position
	 * @param parameters and the List of parameters which were already saved previously
	 * @return
	 * @throws XMLStreamException
	 */
	private List<NameValuePair> readParameter(URLActionDataBuilder actionBuilder, 
			XMLEventReader reader, List<NameValuePair> parameters) throws XMLStreamException {

		String encoded = null;
		String parameterName = null;
		String parameterValue = null;
		
		// loop until the loop is closed with an EndElement with the tag name TNAMEPARAM
		// all parameters should be inside a single TNAME_PARAM tag
		while (true) {			
			XMLEvent event = reader.nextEvent();
			
			if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				String name = getTagName(ee);
				if (name.equals(TNAME_PARAM)) {
					break;
				}
			}

			// look for StartElements
			if (event.isStartElement()) {

				StartElement se = event.asStartElement();

				String name = getAttributeValue(ATTRN_NAME, se);

				// if the name attribute is the right String, get the content of the tag
				// and set something, depending on the ATTRN_NAME value
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
	
	/**
	 * Reads the stuff inside an action, like Extractions and Assertions. Called in {@link #readAction}. 
	 * Reads until the {@link #TNAME_CONTENT} tag that closes the {@link #TNAME_CONTENT}
	 * tag right after the action.
	 * 
	 * @param reader the XMLEventReader with it's position
	 * @param interpreter the ParameterInterpreter for dynamic interpretation of variables
	 * @param actionBuilder and the actionBuilder to which the resulting validations 
	 * 						and extractions will be added
	 * @throws XMLStreamException
	 */
	private void readActionContent(XMLEventReader reader,
			ParameterInterpreter interpreter, URLActionDataBuilder actionBuilder) throws XMLStreamException {
		
		XltLogger.runTimeLogger.debug("Reading new actions content: " + 
							actionBuilder.getName() + "...");
		
		List<URLActionDataValidation> validations = new ArrayList<URLActionDataValidation>();	
		List<URLActionDataStore> variablesToExtract = new ArrayList<URLActionDataStore>();
		URLActionDataStoreBuilder storeBuilder = new URLActionDataStoreBuilder();
		

		// the first TNAME_CONTENT tag should be right here, so there is no need to look for it
		
		// Increment for every TNAME_CONTENT tag that opens, decrement for every TNAME_CONTENT 
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
				case TNAME_VARS : {
					
					// read the variables and stores them
					readVariables(actionBuilder, reader);
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
					storeBuilder.setInterpreter(interpreter);
					storeBuilder.setSelectionMode(URLActionDataStore.XPATH);
					URLActionDataStore variableToExtract = readXPathExtractor(reader, storeBuilder);
					if (variableToExtract != null) {
						variablesToExtract.add(variableToExtract);
					}
					storeBuilder.reset();
					break;
				}
				case TNAME_REGEX_EXTRACT: {
					
					// read regexExtractor
					String selectionMode = URLActionDataStore.REGEXP;
					storeBuilder.setInterpreter(interpreter);
					URLActionDataStore variableToExtract = readRegexExtractor(selectionMode, 
							reader, storeBuilder);
					if (variableToExtract != null) {
						variablesToExtract.add(variableToExtract);
					}
					storeBuilder.reset();
					
					XltLogger.runTimeLogger.debug("Reading RegexExtractor ..." );
					break;
				}
				case TNAME_HEADERS: {
					
					// read and set the headers for the action
					List<NameValuePair> headers = readHeaders(reader);
					actionBuilder.setHeaders(headers);
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
	
	/**
	 * Reads a single ResponseAssertion and translates it into {@link #URLActionDataValidation} 
	 * objects. Adds these validations to the {@link #URLActionDataBuilder}</br>
	 * (Or adds an HttpResponceCode to the actionBuilder if a Response Code should be asserted.
	 * 
	 * @param name the name of the validation(s)
	 * @param reader the XMLEventReader with it's position
	 * @param actionBuilder the actionBuilder to which the validations should be added
	 * @throws XMLStreamException
	 */
	private void readResponseAssertion(String name, 
			XMLEventReader reader, URLActionDataBuilder actionBuilder) throws XMLStreamException {

		XltLogger.runTimeLogger.debug("Reading validation: " + name + "...");
		
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
				
				if (tagName.equals(TNAME_ASSERT_ALL_VALUES)) {
					
					// inside the TNAME_ASSERT_ALL_CONTENT are all the values that
					// should be validated. Read them
					allValidationContent = readAllValuesToValidate(reader);
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
					
					// determines the selectionMode unless it's VAR (from a variable)
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
	
		if (selectionMode != null && validationMode != null) {		
			
			// in Jmeters "validate variable contains/ substring xyz" validations
			// contains and substring still get mapped to TSNCs exist.
			// but validate variable Exists xyz doesn't make any sense
			// so in that case, set validationMode to Matches
			if (selectionMode == URLActionDataValidation.VAR &&
					(validationMode == URLActionDataValidation.EXISTS)) {
				validationMode = URLActionDataValidation.MATCHES;
			}
			
			createValidations(name, selectionMode, selectionContent, validationMode, 
					allValidationContent, actionBuilder);
		}
		else {
			// Don't create the validations since the mapping 
			// was quite likely wrong impossible
			
			XltLogger.runTimeLogger.warn("Mapping Jmeter -> TSNC coudn't be finished correctly");
			XltLogger.runTimeLogger.warn("Validation " + name + "will not be created");
		}
	}
	
	/**
	 * Maps the value from Jmeters Pattern Matching Rules to TSNCs validationMode. Called in 
	 * {@link #readResponseAssertion}. </br>
	 * Since a significant part of them don't match it defaults to {@link URLActionDataValidation#REGEXP}.
	 * 
	 * @param selectionModeInJmt the rough aquivalent to the selectionMode in Jmeter, an integer value
	 * @param selectionMode the selectionMode if it was set already (there are multiple places for that, 
	 * 						it might have been set to {@link URLActionDataValidation#VAR} already) null otherwise)
	 * @return the selectionMode in TSNC
	 */
	private String getSelectionModeFromJmt(String selectionModeInJmt, String selectionMode) {
		
		// if the selectionMode isn't set already
		if (selectionMode == null)	{	
			switch (selectionModeInJmt) {
			
			// and if a response code or a header should be validated
			// set the selectionMode
			case CHAR_ASSERT_RESP_CODE:
				selectionMode = VALIDATE_RESP_CODE;
				break;
			
			case CHAR_ASSERT_RESP_HEADER:
				// this can't be mapped easily
				XltLogger.runTimeLogger.warn("Coudn't translate assertion from Jmeter to TSNC: " + 
											"Headers can't be asserted");
				break;	
				
			case CHAR_ASSERT_RESP_MESSAGE:
				// this can't be mapped easily
				XltLogger.runTimeLogger.warn("Coudn't translate assertion from Jmeter to TSNC: " + 
											"Response Messages can't be asserted");
				break;
				
			default:
				
				// set it to RegExp as the default
				selectionMode = URLActionDataValidation.REGEXP;
				break;
			}
		}
		
		return selectionMode;
	}
	
	/**
	 * Maps the value from Jmeters Pattern Matching Rules to TSNCs validationMode. Should be called
	 * in {@link #readResponseAssertion}. 
	 * 
	 * @param valModeInInt the rough aquivalent to TSNC validationMode in Jmeter. An integer value.
	 * @return the validationMode from TSNC
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
			
		case 6:
			// Jmeter: Not
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
	        XltLogger.runTimeLogger.warn("Coudn't detect validation mode in Jmeter");
			break;
		}
		return validationMode;
	}
	
	/**
	 * Called in the {@link #readResponseAssertion} method. 
	 * Reads until the end of an {@link #TNAME_ASSERT_CONTENT} tag and returns a list
	 * of the values that should be validated. Ie multiple validationContent values from
	 * {@link URLActionDataValidation} which will be used to build multiple validation objects
	 * 
	 * @param reader the XMLEventReader with it's content.
	 * @return multiple String aquivalent to multiple validationContents
	 * @throws XMLStreamException
	 */
	private List<String> readAllValuesToValidate(XMLEventReader reader) throws XMLStreamException {
		
		List<String> allValidationContent = new ArrayList<>();
		
		while (true) {
			XMLEvent event = reader.nextEvent();
			
			if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				String tagName = getTagName(ee);
				if (tagName.equals(TNAME_ASSERT_ALL_VALUES)) {
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
	
	/**
	 * Builds the {@link URLActionDataValidation} objects from the given inputs and adds 
	 * them to the actionBuilder. Should be called in {@link #readResponseAssertion}. </br>
	 * 
	 * Parameters are the necessary parameters to construct an {@link URLActionDataValidation} object 
	 * and the actionBuilder. </br>
	 * 
	 * The parameters for the selectionMode and/or the selectionContent can be null. In that case the
	 * the method sets the selectionMode to {@link URLActionDataValidation#REGEXP} and the
	 * selectionContent to ".*", ie everything.
	 * 
	 * @param name 
	 * @param selectionMode 
	 * @param selectionContent 
	 * @param validationMode 
	 * @param allValidationContent
	 * @param actionBuilder
	 */
	private void createValidations(String name,
			String selectionMode, String selectionContent,
			String validationMode,
			List<String> allValidationContent, URLActionDataBuilder actionBuilder) {

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
						
				URLActionDataValidation validation = new URLActionDataValidation(name,
						selectionMode, selectionContent, validationMode, 
						validationContent, interpreter);
				actionBuilder.addValidation(validation);
			}
		}
	}
	
	/**
	 * Should be called when the {@link #readActionContent} method comes upon a 
	 * {@link #TNAME_XPATH_EXTRACT} tag. Reads until the end of the {@link #TNAME_XPATH_EXTRACT}
	 * tag and returns an {@link URLActionDataStore} object.
	 * 
	 * @param reader the XMLEventReader with it's position
	 * @param storeBuilder the storeBuilder with the interpreter and the selectionMode already added
	 * @return a  {@link URLActionDataStore} object
	 * @throws XMLStreamException
	 */
	private URLActionDataStore readXPathExtractor(XMLEventReader reader, 
			URLActionDataStoreBuilder storeBuilder) throws XMLStreamException {
				
		String name = null;
		String selectionContent = null;
		
		// loop until the loop is closed with an EndElement with the tag name TNAMEXPATHEXTRACT
		// all parameters should be inside a single TNAME_XPATH_EXTRACT tag
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

				default: {
					break;
				}
				}
			}
		}
		
		storeBuilder.setName(name);
		storeBuilder.setSelectionContent(selectionContent);
		URLActionDataStore varToExtract = storeBuilder.build();
		return varToExtract;
	}

	/**
	 * Should be called when the {@link #readActionContent} method comes upon a 
	 * {@link TNAME_REGEX_EXTACT} tag. Reads until the end of the {@link TNAME_REGEX_EXTACT} tag and 
	 * returns an URLActionDataStore object. </br>
	 * 
	 * <p>In Jmeter the extractor extracts everything inside the parentheses of the regular expression.
	 * ( and ) - the round brackets enclose the portion of the match string to be returned. TSNC can do the 
	 * same. However there are differences if there is more then one match. Jmeter would return every match,
	 * TSNC only returns one. <\br>
	 * The way it's written, the first match is returned.
	 * </p>
	 * <p>
	 * Actually, it can be specified which group should be returned. But that isn't implemented yet TODO
	 * </p>
	 * 
	 * @param selectionMode
	 * @param reader
	 * @param storeBuilder
	 * @return
	 * @throws XMLStreamException
	 */
	private URLActionDataStore readRegexExtractor(String selectionMode, 
			XMLEventReader reader, URLActionDataStoreBuilder storeBuilder) throws XMLStreamException {
				
		String GROUP_NMB = "1";
		String name = null;
		String selectionContent = null;
		
		// loop until the loop is closed with an EndElement with the tag name 
		// all parameters should be inside a single  tag
		while (true) {			
			XMLEvent event = reader.nextEvent();
			
			if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				String tagName = getTagName(ee);
				if (tagName.equals(TNAME_REGEX_EXTRACT)) {
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

				case ATTRV_REGEX_EXT_REFNAME: {
					event = reader.nextEvent();
					name = getTagContent(event);
					break;
				}
				case ATTRV_REGEX_EXT_REGEX: {
					event = reader.nextEvent();
					selectionContent = getTagContent(event);
					
					break;
				}

				default: {
					break;
				}
				}
			}
		}
		
		storeBuilder.setName(name);
		storeBuilder.setSelectionMode(selectionMode);
		storeBuilder.setSelectionContent(selectionContent);
		
		// return whatever was in the round brackets in the first match
		storeBuilder.setSubSelectionMode(URLActionDataStore.REGEXGROUP);
		storeBuilder.setSubSelectionContent(GROUP_NMB);
		
		URLActionDataStore varToExtract = storeBuilder.build();
		return varToExtract;
	}

	
	/**
	 * Returns the tag name of a StartElement. Analogous to getTagName(EndElement ee).
	 * Convenience method.
	 * 
	 * @param se the StartElement
	 * @return it's tag name as a String
	 */
	private String getTagName(StartElement se) {
		QName qname = se.getName();
		String name = qname.getLocalPart();
		return name;
	}
	
	/**
	 * Returns the tag name of a EndElement. Analogous to getTagName(StartElement se).
	 * Convenience method.
	 * 
	 * @param ee the EndElement
	 * @return it's tag name as a String
	 */
	private String getTagName(EndElement ee) {
		QName qname = ee.getName();
		String name = qname.getLocalPart();
		return name;
	}
	
	/**
	 * Gets the attributeValue from a StartElement and an attribute name. </br>
	 * Returns the value of the {@link #NOTFOUND} constant if no attribute with that name exists. 
	 * 
	 * @param attributeName the name of the attribute 
	 * @param se the StartElement from which we want to get the value
	 * @return and the value of the attribute as a string
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
	
	/**
	 * Gets the content of a tag, it <tag>CONTENT</tag>. As a String. </br>
	 * Convenience method.
	 * 
	 * @param event the character event aquivalent to the content of the tag
	 * @return said event as a string
	 */
	private String getTagContent(XMLEvent event) {
		if (event.isCharacters()) {
			Characters characters = event.asCharacters();
			String content = characters.getData();
			return content;
		}
		else {
			// this really shoudn't happen
			String warning = "An unexpected error occured during the Jmeter -> TSNC conversion." +
							"tried to get tag content when none was there";
	        XltLogger.runTimeLogger.warn(warning);
	        return warning;
		}
	}
}