package com.xceptance.xlt.common.util.action.data.Jmeter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

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
import com.xceptance.xlt.api.data.GeneralDataProvider;
import com.xceptance.xlt.api.util.XltLogger;
import com.xceptance.xlt.api.util.XltProperties;
import com.xceptance.xlt.common.util.action.data.URLActionData;
import com.xceptance.xlt.common.util.action.data.URLActionDataBuilder;
import com.xceptance.xlt.common.util.action.data.URLActionDataStore;
import com.xceptance.xlt.common.util.action.data.URLActionDataStoreBuilder;
import com.xceptance.xlt.common.util.action.data.URLActionDataValidation;
import com.xceptance.xlt.common.util.bsh.ParameterInterpreter;

/**
 * A class to handle the actual translation from Jmeter to TSNC. Used in
 * higher level classes. </br>
 * TSNC only implements some of Jmeters functions and some of those can't
 * be translated perfectly. See the documentation file to see what can and
 * can't be done.
 * <p>
 * On the technical side Jmeter saves it's tests in an xml file without a DTD. This class parses the
 * xml file using StAX. The various constants are the names of the tags, the attributes or their
 * values.
 * </p>
 */
public class JmeterTranslater
{

	/**
	 * The following constant are used for the tagnames in Jmeter. For example,
	 * in <Arguments ...> 'Arguments' is the tag name.
	 */
	public enum Tagname
	{
		// General ...
		TEST_PLAN("TestPlan"),

		THREAD_GROUP("ThreadGroup"),

		TEST_CONFIG("ConfigTestElement"),

		// Action
		ACTION("HTTPSamplerProxy"),

		// Variables ...
		VARS("Arguments"),

		VAR("elementProp"),

		// Parameters and Headers ...
		PARAMS("collectionProp"),

		PARAM("elementProp"),

		HEADERS("HeaderManager"),

		HEADER("elementProp"),

		// Extractors ...
		XPATH_EXTRACT("XPathExtractor"),

		REGEX_EXTRACT("RegexExtractor"),

		// Response Assertion ...
		ASSERT_RESP("ResponseAssertion"),

		ASSERT_ALL_VALUES("collectionProp"),

		ASSERT_ONE_CONTENT("stringProp"),

		/**
		 * Jmeter uses a tree structure, but the elements below a certain node
		 * are not inside the xml tag in the .jmx file. Instead a hashtree tag
		 * follows all tags with something below them in the tree structure. Inside
		 * the hashtree tag are the nodes below the tag in question. For example: An
		 * action which contains an assertion
		 * 
		 * <HTTPSamplerProxy ...> ...</HTTPSamplerProxy> <hashtree>
		 * <ResponseAssertion ...> ... </ResponseAssertion> </br> <hashtree> ...
		 * </hashtree> </hashtree>
		 * 
		 * In this example the inner 'hashtree' tag is for everything below the
		 * assertion, the outer 'hashtree' tag for everything below the action.
		 */
		CONTENT("hashTree");

		/**
		 * The literal String that used in the XML file.
		 */
		private final String xmlName;

		Tagname(String name)
		{
			this.xmlName = name;
		}
	}

	/**
	 * <p>
	 * These constants are used for the attribute names in Jmeter. For example, in <HTTPSamplerProxy
	 * guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy"
	 * testname="test for correcting the mistake afterwards 1" enabled="true"> </br> 'testname' is
	 * the attribute name for the name of the attribute which defines the action name. The
	 * attributes are often used as a secondary name.
	 */

	public enum AttributeName
	{
		NAME("name"),

		ACTION_NAME("testname"),

		ASSERT_NAME("testname"),

		ELEMENT_TYPE("elementType");

		/**
		 * The literal String that used in the XML file.
		 */
		private final String xmlName;

		AttributeName(String name)
		{
			this.xmlName = name;
		}
	}

	/**
	 * The following constants are used for the attributes in Jmeter. For
	 * example, in <stringProp
	 * name="HTTPSampler.domain">${host}/search</stringProp> 'stringProp' is the
	 * tag name, 'name' an attribute name and 'HTTPSampler.domain' an attribute
	 * value.
	 * <p>
	 * The elements of AttributeNameValue consist of a name, a {@link AttributeName},
	 * and a String value. 
	 * </p>
	 */
	public enum AttributeNameValue
	{
		ELEMENT_IS_PARAM(AttributeName.ELEMENT_TYPE, "HTTPArgument"),

		ELEMENT_IS_VAR(AttributeName.ELEMENT_TYPE, "Argument"),

		VAR_NAME(AttributeName.NAME, "Argument.name"),

		VAR_VALUE(AttributeName.NAME, "Argument.value"),

		ACTION_PROTOC(AttributeName.NAME, "HTTPSampler.protocol"),

		ACTION_WEBSITE(AttributeName.NAME, "HTTPSampler.domain"),

		ACTION_PATH(AttributeName.NAME, "HTTPSampler.path"),

		ACTION_METHOD(AttributeName.NAME, "HTTPSampler.method"),

		// variables and parameters ...

		ACTION_PARAM(AttributeName.NAME, "Arguments.arguments"),

		ENCODE_PARAM(AttributeName.NAME, "HTTPArgument.always_encode"),

		PARAM_NAME(AttributeName.NAME, "Argument.name"),

		PARAM_VALUE(AttributeName.NAME, "Argument.value"),

		// headers ...

		HEADER_NAME(AttributeName.NAME, "Header.name"),

		HEADER_VALUE(AttributeName.NAME, "Header.value"),

		// assertions ...

		ASSERT_VARIABLE(AttributeName.NAME, "Scope.variable"),

		ASSERT_FIELD_TO_TEST(AttributeName.NAME, "Assertion.test_field"),

		ASSERT_VAL_MODE(AttributeName.NAME, "Assertion.test_type"),

		ASSERT_IGNORE_STATUS(AttributeName.NAME, "Assertion.assume_success"),

		// XPath Extractor ...

		XPATH_EXT_REFNAME(AttributeName.NAME, "XPathExtractor.refname"),

		XPATH_EXT_XPATH(AttributeName.NAME, "XPathExtractor.xpathQuery"),

		XPath_EXT_FROM_VAR(AttributeName.NAME, "Scope.variable"),

		// Regex Extractor ...

		REGEX_EXT_REFNAME(AttributeName.NAME, "RegexExtractor.refname"),

		REGEX_EXT_REGEX(AttributeName.NAME, "RegexExtractor.regex"),

		REGEX_EXT_GROUP(AttributeName.NAME, "RegexExtractor.template"),

		REGEX_EXT_MATCH(AttributeName.NAME, "RegexExtractor.match_number"),

		REGEX_EXT_FROM_VAR(AttributeName.NAME, "Scope.variable"),

		/**
		 * Determines whether to extract from the resonse message, just the response body,
		 * the resp. code or something else ...
		 */
		REGEX_EXT_SCOPE(AttributeName.NAME, "RegexExtractor.useHeaders");
		
		/**
		 * The name if the attribute.
		 */
		private AttributeName name;

		/**
		 * The literal String that used as the value in the XML file.
		 */
		private final String xmlValue;

		AttributeNameValue(AttributeName name, String value)
		{
			this.name = name;
			this.xmlValue = value;
		}
	}
	
	/*
	 * The following constants are used when important values are in the
	 * character parts of the xml. For example in <stringProp
	 * name="XPathExtractor.refname">noHitsBanner</stringProp> 'noHisBanner'
	 * would be in the character part of the XML.
	 * 
	 * The names follow the pattern CHAR + theme + abbreviation of their function.
	 * Underscores are used for readability.
	 */

	private final String CHAR_ASSERT_RESP_HEADER = "Assertion.response_headers";

	private final String CHAR_ASSERT_RESP_MESSAGE = "Assertion.response_message";

	private final String CHAR_ASSERT_RESP_CODE = "Assertion.response_code";

	private final String CHAR_ASSERT_TEXT = "Assertion.response_data";

	private final String CHAR_ASSERT_DOCUMENT = "Assertion.response_data_as_document";

	private final String CHAR_ASSERT_URL = "Assertion.sample_label";

	// finally all the xml constants used in this class are defined ...

	/**
	 * If the {{@link #getAttributeValue(StartElement, String)} method was used but the attribute
	 * wasn't found, it will return this value instead.
	 */
	private final String NOT_FOUND = "Attribute not found";

	/**
	 * If the respnse code should be validated (instead of the response data or
	 * the response header), the selectionMode is temporarily set to this.
	 */
	private final String VALIDATE_RESP_CODE = "respCode";

	/*
	 * The default settings for http protocol, website and path do not
	 * exist in TSNC. To simulate them, defaults are added to the interpreter with these
	 * constants as keys. 
	 */

	private final String DEF_PROTOCOL = "defaultProtocol";

	private final String DEF_WEBSITE = "defaultRootWebsite";

	private final String DEF_PATH = "defaultPath";

	private List<NameValuePair> defaultParameters = new ArrayList<NameValuePair>();

	/*
	 * the rest ...
	 */

	private ParameterInterpreter interpreter;

	/**
	 * The actionbuilder that is used to build the actions.
	 * Many methods add to the actionbuilder, it is resetted after
	 * an action is build.
	 */
	private URLActionDataBuilder actionBuilder;

	/**
	 * The reader used to parse the xml. Almost every method in this class moves
	 * the reader forwards.
	 */
	private XMLEventReader reader;

	/**
	 * The variables that are valid for the whole test plan, not just a single
	 * Thread Group.
	 */
	private List<NameValuePair> testplanVariables = new ArrayList<>();

	// TODO make it static ?
	public JmeterTranslater()
	{

	}

	/**
	 * Transforms the .jmx file from Jmeter into a map of a list of URLActionData
	 * objects. The map consists of name of the threadgroup - actions of the thread
	 * group and so on. Returns said list.
	 * 
	 * (non-Javadoc)
	 * 
	 * @param filePath
	 * @see com.xceptance.xlt.common.util.action.data.URLActionDataListBuilder#
	 *      buildURLActionDataList()
	 */
	public LinkedHashMap<String, List<URLActionData>> translateFile(String filePath)
	{

		LinkedHashMap<String, List<URLActionData>> threadGroups = new LinkedHashMap<>();
		XltLogger.runTimeLogger.debug("Starting Jmeter -> TSNC translation ...");

		try
		{
			InputStream inputstream = new FileInputStream(filePath);
			XMLInputFactory factory = XMLInputFactory.newInstance();
			factory.setProperty(XMLInputFactory.IS_COALESCING, true);
			reader = factory.createXMLEventReader(inputstream);

			// start reading and keep reading until the end
			while (reader.hasNext())
			{
				XMLEvent event = reader.nextEvent();

				// if it's the start of the test plan element
				if (isStart(event, Tagname.TEST_PLAN))
				{
					testplanVariables.addAll(readVarsInTestPlan());
				}

				// if it's a Thread Group, create a new action. Thread Group = Action
				if (isStart(event, Tagname.THREAD_GROUP))
				{
					String nameTGroup = getAttributeValue(event.asStartElement(), 
							AttributeName.ACTION_NAME.xmlName);
					XltLogger.runTimeLogger.info("Reading " + nameTGroup + "...");

					// we don't want to take stuff from the last threadgroup with us ...
					defaultParameters = new ArrayList<NameValuePair>();
					XltProperties properties = XltProperties.getInstance();
					GeneralDataProvider dataProvider = GeneralDataProvider.getInstance();
					this.interpreter = new ParameterInterpreter(properties, dataProvider);

					// but we do want the variables declared in the testplan
					for (NameValuePair nvp : testplanVariables)
					{
						try
						{
							interpreter.set(nvp);
						}
						catch (EvalError e)
						{
							XltLogger.runTimeLogger.error("Coudn't set variable " + nvp.getName()
									+ " : " + nvp.getValue() + " for some reason");
						}
					}

					List<URLActionData> testCase = readThreadGroup();
					threadGroups.put(nameTGroup, testCase);
				}
			}
		}
		catch (final FileNotFoundException e)
		{
			final String message = MessageFormat.format("File: \"{0}\" not found!", filePath);
			XltLogger.runTimeLogger.warn(message);
			throw new IllegalArgumentException(message + ": " + e.getMessage());
		}
		catch (XMLStreamException e)
		{
			XltLogger.runTimeLogger.error("Jmeters XML Stream was interrupted");
			throw new IllegalArgumentException(e.getMessage());
		}

		return threadGroups;
	}

	/**
	 * Reads the user defined variables defined inside the Test Plan element.
	 * 
	 * @return a list of {@link NameValuePair}
	 * @throws XMLStreamException
	 */
	private List<NameValuePair> readVarsInTestPlan() throws XMLStreamException
	{

		List<NameValuePair> variables = new ArrayList<>();

		XMLEvent event = reader.nextEvent();
		while (!isEnd(event, Tagname.TEST_PLAN))
		{
			event = reader.nextEvent();

			// the way the correct tag is found here is somewhat sloppy
			if (hasAttribute(event, AttributeNameValue.ELEMENT_IS_VAR))
			{
				variables.add(readVariable());
			}
		}

		return variables;
	}

	/**
	 * Reads a Jmeter threadgroup and all of it's content, which is equivalent
	 * to a TSNC test case.
	 * 
	 * @return a list of URLActionData objects to be returned by this builder
	 * @throws XMLStreamException
	 */
	private List<URLActionData> readThreadGroup() throws XMLStreamException
	{

		List<URLActionData> testCaseActions = new ArrayList<URLActionData>();
		this.actionBuilder = new URLActionDataBuilder();
		actionBuilder.setInterpreter(interpreter);

		XMLEvent event = reader.nextEvent();
		while (!isEnd(event, Tagname.THREAD_GROUP))
		{
			event = reader.nextEvent();

			// many of Jmeters Load Test Configurations are here
			// but we don't read them, so ...
		}

		// read the content of the ThreadGroup.
		// The first tag should be right here,
		// so there is no need to look for it

		// Increment for every Tagname.CONTENT tag that opens, decrement for every
		// tag that closes. Exit when you go down from one to zero.. (To make sure you exit
		// with the right tag.)
		int treeLevel = 0;
		while (true)
		{
			event = reader.nextEvent();

			if (isEnd(event, Tagname.CONTENT))
			{
				treeLevel--;

				if (treeLevel == 0)
				{
					break;
				}
			}

			// check the events tag name and delegate to a
			// subfunction accordingly
			if (isStart(event, Tagname.TEST_CONFIG))
			{
				// read the defaults defined here
				readDefaults();
			}
			if (isStart(event, Tagname.HEADERS))
			{
				// read default headers
				List<NameValuePair> defaultHeaders = readHeaders();
				actionBuilder.setDefaultHeaders(defaultHeaders);
			}
			if (isStart(event, Tagname.VARS))
			{
				// read the variables and stores them
				readVariables();
			}
			if (isStart(event, Tagname.ACTION))
			{
				// an Tagname.ACTION is equivalent to an HttpRequest,
				// which is in turn equivalent to an action. Get and set the testname
				String actionName = getAttributeValue(event.asStartElement(), 
						AttributeName.ACTION_NAME.xmlName);
				URLActionData action = readAction(actionName);
				testCaseActions.add(action);
			}
			if (isStart(event, Tagname.CONTENT))
			{
				treeLevel++;
			}
		}
		return testCaseActions;
	}

	/**
	 * Reads until it encounters a closing tag with the name of {@link Tagname#VARS}.
	 * Reads multiple variables with their names and values and stores
	 * them in the ParameterInterpreter.
	 * 
	 * @throws XMLStreamException
	 */
	private void readVariables() throws XMLStreamException
	{

		XltLogger.runTimeLogger.debug("Storing Variables ...");

		XMLEvent event = reader.nextEvent();
		while (!isEnd(event, Tagname.VARS))
		{
			event = reader.nextEvent();

			if (isStart(event, Tagname.VAR)
					&& hasAttribute(event, AttributeNameValue.ELEMENT_IS_VAR))
			{
				NameValuePair nvp = readVariable();

				try
				{
					interpreter.set(nvp);
				}
				catch (EvalError e)
				{
					XltLogger.runTimeLogger.error("Coudn't set variable " + nvp.getName() + " : "
							+ nvp.getValue() + " for some reason");
				}
			}
		}
	}

	/**
	 * Is called inside the {@link #readVariables} method to parse a single
	 * variable. Parses a single variable with name and value and saves it in the
	 * interpreter.
	 * 
	 * @return the variable as a {@link NameValuePair}
	 * @throws XMLStreamException
	 */
	private NameValuePair readVariable() throws XMLStreamException
	{

		String argsName = null;
		String argsValue = null;

		XMLEvent event = reader.nextEvent();
		while (!isEnd(event, Tagname.VAR))
		{
			event = reader.nextEvent();

			if (hasAttribute(event, AttributeNameValue.VAR_NAME))
			{
				event = reader.nextEvent();
				argsName = getTagContent(event);
			}
			if (hasAttribute(event, AttributeNameValue.VAR_VALUE))
			{
				event = reader.nextEvent();
				argsValue = getTagContent(event);
			}
		}

		// replace %20 with a whitespace, because that seems to match Jmeters and TSNCs behavior
		argsValue = argsValue.replace("%20", " ");

		// save the acquired arguments
		NameValuePair nvp = new NameValuePair(argsName, argsValue);
		return nvp;
	}

	/**
	 * Reads the Http defaults that can be read. Is called inside of {@link #readThreadGroup}.
	 * 
	 * @throws XMLStreamException
	 */
	private void readDefaults() throws XMLStreamException
	{
		XMLEvent event = reader.nextEvent();
		while (!isEnd(event, Tagname.TEST_CONFIG))
		{
			event = reader.nextEvent();

			try
			{
				// look for elements with the right attribute, delegate accordingly

				// a website field!
				if (hasAttribute(event, AttributeNameValue.ACTION_WEBSITE))
				{
					event = reader.nextEvent();
					if (event.isCharacters())
					{
						NameValuePair nvp = new NameValuePair(DEF_WEBSITE, getTagContent(event));
						interpreter.set(nvp);
					}
				}

				// the path field - ie the thing after the website,
				// "en" for somethign like "xceptance.com/en"
				if (hasAttribute(event, AttributeNameValue.ACTION_PATH))
				{
					event = reader.nextEvent();
					if (event.isCharacters())
					{
						NameValuePair nvp = new NameValuePair(DEF_PATH, getTagContent(event));
						interpreter.set(nvp);
					}
				}

				// the http protocol!
				if (hasAttribute(event, AttributeNameValue.ACTION_PROTOC))
				{
					event = reader.nextEvent();
					if (event.isCharacters())
					{
						NameValuePair nvp = new NameValuePair(DEF_PROTOCOL, getTagContent(event));
						interpreter.set(nvp);
					}
				}

				// and the http parameters!
				if (hasAttribute(event, AttributeNameValue.ACTION_PARAM))
				{
					defaultParameters = readParameters();
				}
			}
			catch (EvalError e)
			{
				XltLogger.runTimeLogger.error("Coudn't set variable default protocol,"
						+ " website or path for some reason");
			}
		}
	}

	/**
	 * Should be called in {@link #readThreadGroup} to read the default headers
	 * or {@link #readActionContent} to read the normal headers of an action.
	 * 
	 * @return a List of NameValuePairs equivalent to the headers with name and
	 *         value
	 * @throws XMLStreamException
	 */
	private List<NameValuePair> readHeaders() throws XMLStreamException
	{

		List<NameValuePair> headers = new ArrayList<NameValuePair>();

		XMLEvent event = reader.nextEvent();
		while (!isEnd(event, Tagname.HEADERS))
		{
			event = reader.nextEvent();

			if (isStart(event, Tagname.HEADER))
			{
				NameValuePair header = readHeader();
				headers.add(header);
			}
		}
		return headers;
	}

	/**
	 * Called in {@link #readHeaders}. Reads a single header and returns it as a
	 * NameValuePair.
	 * 
	 * @return a NameValuePair equivalent to a single header with name and value
	 * @throws XMLStreamException
	 */
	private NameValuePair readHeader() throws XMLStreamException
	{
		String name = null;
		String value = null;

		XMLEvent event = reader.nextEvent();
		while (!isEnd(event, Tagname.HEADER))
		{
			event = reader.nextEvent();

			// read the name of the header
			if (hasAttribute(event, AttributeNameValue.HEADER_NAME))
			{
				event = reader.nextEvent();
				name = getTagContent(event);
			}

			// and it's value
			if (hasAttribute(event, AttributeNameValue.HEADER_VALUE))
			{
				event = reader.nextEvent();
				value = getTagContent(event);
			}
		}
		NameValuePair header = new NameValuePair(name, value);
		return header;
	}

	/**
	 * Is called when the main parsing method {@link #readThreadGroup} comes
	 * upon an Http-Request/ an action {@link #TNAME_ACTION}. Parses the file
	 * and creates the action with the appropriate properties. Returns the action
	 * when it comes upon the EndElement.
	 * 
	 * @param testName
	 *            and the name of the action to read
	 * 
	 * @return an {@link URLActionData} object
	 * @throws XMLStreamException
	 */
	private URLActionData readAction(String testName) throws XMLStreamException
	{

		XltLogger.runTimeLogger.debug("Reading new Action: " + testName + "...");

		// set the defaults if they exist ...
		String protocol = null, website = null, path = null;
		if (isVarInInterpreter(DEF_PROTOCOL))
		{
			protocol = "${" + DEF_PROTOCOL + "}";
		}
		if (isVarInInterpreter(DEF_WEBSITE))
		{
			website = "${" + DEF_WEBSITE + "}";
		}
		if (isVarInInterpreter(DEF_PATH))
		{
			path = "${" + DEF_PATH + "}";
		}

		// set the testname and the interpreter
		actionBuilder.setName(testName);
		actionBuilder.setInterpreter(interpreter);

		// keep reading until an EndElement with the
		// Tagname.ACTION is the next element
		XMLEvent event = reader.nextEvent();
		while (!isEnd(event, Tagname.ACTION))
		{
			event = reader.nextEvent();
			// if the name attribute corresponds to an action parameter,
			// set the appropriate action parameter to the content

			if (hasAttribute(event, AttributeNameValue.ACTION_WEBSITE))
			{
				// read the url
				event = reader.nextEvent();
				if (event.isCharacters())
				{
					website = event.asCharacters().getData();
				}
			}
			if (hasAttribute(event, AttributeNameValue.ACTION_PROTOC))
			{
				event = reader.nextEvent();
				if (event.isCharacters())
				{
					protocol = event.asCharacters().getData();
				}
			}
			if (hasAttribute(event, AttributeNameValue.ACTION_PATH))
			{
				// read the path. The path is added to the end of the URL.
				// Example: url=www.otto.de, path: /herrenmode/
				event = reader.nextEvent();
				if (event.isCharacters())
				{
					path = event.asCharacters().getData();
				}
			}
			if (hasAttribute(event, AttributeNameValue.ACTION_METHOD))
			{
				// read the method
				event = reader.nextEvent();
				String content = event.asCharacters().getData();
				actionBuilder.setMethod(content);
			}
			if (hasAttribute(event, AttributeNameValue.ACTION_PARAM))
			{
				// read the parameters
				List<NameValuePair> parameters = readParameters();
				// set the parameters
				if (parameters != null)
				{
					actionBuilder.setParameters(parameters);
				}
			}
		}

		// set the protocol to http if it wasn't specified yet
		if (protocol == null)
		{
			protocol = "http";
		}
		if (path == null)
		{
			path = "";
		}
		if (website == null)
		{
			String reason = "Coudn't detect website for " + testName;
			logAndThrow(reason);
		}

		// add protocol, url and path together, set the url
		String url = protocol + "://" + website + path;
		actionBuilder.setUrl(url);

		readActionContent();

		// build the action and reset the URLActionDataBuilder
		URLActionData action = actionBuilder.build();
		return action;
	}

	/**
	 * Is called inside {@link #readAction} to read the http parameters.
	 * 
	 * @return parameters
	 * @throws XMLStreamException
	 */
	private List<NameValuePair> readParameters() throws XMLStreamException
	{
		List<NameValuePair> parameters = new ArrayList<>();
		parameters.addAll(defaultParameters);

		// all parameters should be inside a single TNAME_PARAMS tag
		XMLEvent event = reader.nextEvent();
		while (!isEnd(event, Tagname.PARAMS))
		{
			event = reader.nextEvent();

			if (hasAttribute(event, AttributeNameValue.ELEMENT_IS_PARAM))
			{
				// read the single detected parameter
				parameters = readParameter(parameters);
			}
		}

		return parameters;
	}

	/**
	 * Is called inside the readParameters method to parse a single parameter.
	 * <p>
	 * Parses a single Parameter with encoding, name and value. Sets the former and adds the
	 * NameValuePair with name and value to the correct list.
	 * </p>
	 * 
	 * @param parameters
	 *            the List of parameters which were saved previously
	 * 
	 * @return
	 *         the List of parameters which were saved previously and
	 *         the newly saved ones
	 * 
	 * @throws XMLStreamException
	 */
	private List<NameValuePair> readParameter(List<NameValuePair> parameters)
			throws XMLStreamException
	{

		String encoded = null;
		String parameterName = null;
		String parameterValue = null;

		// read the parameter element one event at a time
		XMLEvent event = reader.nextEvent();
		while (!isEnd(event, Tagname.PARAM))
		{
			event = reader.nextEvent();

			if (hasAttribute(event, AttributeNameValue.ENCODE_PARAM))
			{
				event = reader.nextEvent();
				encoded = getTagContent(event);
			}
			if (hasAttribute(event, AttributeNameValue.PARAM_NAME))
			{
				event = reader.nextEvent();
				parameterName = getTagContent(event);
			}
			if (hasAttribute(event, AttributeNameValue.PARAM_VALUE))
			{
				event = reader.nextEvent();
				parameterValue = getTagContent(event);
			}
		}

		// set the encoding and the parameter values
		// the encoding is set for the whole action here whereas in Jmeter it
		// was set for a single parameter
		actionBuilder.setEncodeParameters(encoded);
		NameValuePair nameValue = new NameValuePair(parameterName, parameterValue);
		parameters.add(nameValue);
		return parameters;
	}

	/**
	 * Reads the stuff inside an action, like Extractions and Assertions. Called
	 * in {@link #readAction}. Reads until the {@link Tagname#CONTENT} tag that
	 * closes the {@link Tagname#CONTENT} tag right after the action.
	 * 
	 * @throws XMLStreamException
	 */
	private void readActionContent() throws XMLStreamException
	{

		XltLogger.runTimeLogger.debug("Reading new actions content: " + actionBuilder.getName()
				+ "...");

		List<URLActionDataValidation> validations = new ArrayList<URLActionDataValidation>();
		List<URLActionDataStore> storeList = new ArrayList<URLActionDataStore>();
		URLActionDataStoreBuilder storeBuilder = new URLActionDataStoreBuilder();

		// the first TNAME_CONTENT tag should be right here, so there is no need
		// to look for it

		// Increment for every TNAME_CONTENT tag that opens, decrement for every
		// TNAME_CONTENT
		// tag that closes. Exit when zero is reached. (To make sure you exit
		// with the right
		// tag.)

		int treeLevel = 0;

		while (true)
		{
			XMLEvent event = reader.nextEvent();

			if (isEnd(event, Tagname.CONTENT))
			{
				treeLevel--;

				if (treeLevel == 0)
				{
					break;
				}
			}

			// look for StartElements
			if (isStart(event, Tagname.CONTENT))
			{
				// we just went a bit further down in the tree
				treeLevel++;
			}
			if (isStart(event, Tagname.VARS))
			{
				// read the variables and stores them
				readVariables();
			}
			if (isStart(event, Tagname.ASSERT_RESP))
			{
				// check Response Assertion, add it to the actionBuilder
				String name = getAttributeValue(event.asStartElement(),
						AttributeName.ASSERT_NAME.xmlName);
				readResponseAssertion(name);
			}
			if (isStart(event, Tagname.XPATH_EXTRACT))
			{
				// read the XPathExtractor
				storeBuilder.setInterpreter(interpreter);
				storeBuilder.setSelectionMode(URLActionDataStore.XPATH);
				URLActionDataStore store = readXPathExtractor(reader, storeBuilder);

				storeList.add(store);
				storeBuilder.reset();
			}
			if (isStart(event, Tagname.REGEX_EXTRACT))
			{
				// read regexExtractor
				String selectionMode = URLActionDataStore.REGEXP;
				storeBuilder.setInterpreter(interpreter);
				URLActionDataStore store = readRegexExtractor(selectionMode, reader, storeBuilder);

				storeList.add(store);
				storeBuilder.reset();
			}
			if (isStart(event, Tagname.HEADERS))
			{
				// read and set the headers for the action
				List<NameValuePair> headers = readHeaders();
				for (NameValuePair header : headers)
				{
					actionBuilder.addHeader(header);
				}
			}
		}

		// add all the validations and extractions to the actionBuilder
		// unless there are none, of course
		if (validations.size() > 0)
		{
			actionBuilder.setValidations(validations);
		}
		if (storeList.size() > 0)
		{
			actionBuilder.setStore(storeList);
		}
	}

	/**
	 * Reads a single ResponseAssertion and translates it into {@link #URLActionDataValidation}
	 * objects. Adds these validations to the {@link #URLActionDataBuilder}</br> (Or adds an
	 * HttpResponceCode to the
	 * actionBuilder if a Response Code should be asserted).
	 * 
	 * @param name
	 *            the name of the validation(s)
	 * @throws XMLStreamException
	 */
	private void readResponseAssertion(String name) throws XMLStreamException
	{

		XltLogger.runTimeLogger.debug("Reading validation: " + name + "...");

		// variable check if the Response Code, the Response Headers or the
		// Response Data
		// should be validated
		String selectionMode = null;
		String selectionContent = null;
		String validationMode = null;
		List<String> allValidationContent = new ArrayList<String>();

		try
		{
			// loop until the loop is closed with an EndElement with the tag
			// name TNAME_ASSERT_RESP
			// all parameters should be inside a single TNAME_ASSERT_RESP tag
			XMLEvent event = reader.nextEvent();
			while (!isEnd(event, Tagname.ASSERT_RESP))
			{
				event = reader.nextEvent();

				if (isStart(event, Tagname.ASSERT_ALL_VALUES))
				{
					// inside this tag are all the values that
					// should be validated. Read them
					allValidationContent = readAllValuesToValidate();
				}

				if (hasAttribute(event, AttributeNameValue.ASSERT_VARIABLE))
				{
					// shows that a variable should be validated
					// and which variable to validate
					selectionMode = URLActionDataValidation.VAR;
					event = reader.nextEvent();
					selectionContent = getTagContent(event);
					selectionContent = "${" + selectionContent + "}";
				}
				if (hasAttribute(event, AttributeNameValue.ASSERT_FIELD_TO_TEST))
				{
					// determines the selectionMode unless it's VAR (from a
					// variable)
					event = reader.nextEvent();
					String selectionModeInJmt = getTagContent(event);
					selectionMode = getSelectionModeFromJmt(selectionModeInJmt, selectionMode);
				}
				if (hasAttribute(event, AttributeNameValue.ASSERT_VAL_MODE))
				{
					// determines the validationMode
					event = reader.nextEvent();
					int valModeInInt = Integer.parseInt(getTagContent(event));
					validationMode = getValidationModeFromInt(valModeInInt);
				}
				if (hasAttribute(event, AttributeNameValue.ASSERT_IGNORE_STATUS))
				{
					// determines if the assertion should "Ignore Status"
					// TSNC doesn't have an aquivalent. If this is true, it
					// can't be mapped
					event = reader.nextEvent();
					String ignore = getTagContent(event);
					if (ignore.equals("true"))
					{
						XltLogger.runTimeLogger.warn("Ignore Status at can't be mapped "
								+ "to TSNC.");
					}
				}
			}

			if (selectionMode == null || validationMode == null)
			{

				// this is impossible, don't create the validation
				String reason = "There was a problem in " + name;
				logAndThrow(reason);
			}

			// In case selectionMode == VAR and validationMode == EXISTS,
			// because that doesn't make sense.

			if (selectionMode == URLActionDataValidation.VAR
					&& (validationMode == URLActionDataValidation.EXISTS))
			{
				validationMode = URLActionDataValidation.MATCHES;
			}

			createValidations(name, selectionMode, selectionContent, validationMode,
					allValidationContent);
		}
		catch (MappingException e)
		{
			// there was obviously an error, don't create the validation/ Assertion
			XltLogger.runTimeLogger.error("Jmeter -> TSNC mapping of " + name + ""
					+ "coudn't be finished correctly");
			XltLogger.runTimeLogger.error("Assertion won't be created");
		}
	}

	/**
	 * Maps the value from Jmeters Pattern Matching Rules to TSNCs
	 * selectionMode. Called in {@link #readResponseAssertion}.
	 * 
	 * @param selectionModeInJmt
	 *            the rough aquivalent to the selectionMode in Jmeter, an
	 *            integer value
	 * @param selectionMode
	 *            the selectionMode if it was set already (there are multiple
	 *            places for that, it might have been set to {@link URLActionDataValidation#VAR}
	 *            already) null otherwise)
	 * @return the selectionMode in TSNC
	 */
	private String getSelectionModeFromJmt(String selectionModeInJmt, String selectionMode)
	{

		switch (selectionModeInJmt)
		{

		// the normal stuff ...

		case CHAR_ASSERT_TEXT:
			if (selectionMode == null)
			{
				selectionMode = URLActionDataValidation.REGEXP;
			}
			break;

		case CHAR_ASSERT_DOCUMENT:
			if (selectionMode == null)
			{
				selectionMode = URLActionDataValidation.REGEXP;
			}
			break;

		// somewhat troublesome, but usually possible ...

		case CHAR_ASSERT_RESP_CODE:

			if (selectionMode == null)
			{
				selectionMode = VALIDATE_RESP_CODE;
			}
			else
			{
				String reason = "Can't validate the response code of a variable.";
				logAndThrow(reason);
			}
			break;

		case CHAR_ASSERT_RESP_HEADER:
			if (selectionMode == null)
			{
				selectionMode = URLActionDataValidation.HEADER;
			}
			else
			{
				String reason = "Can't validate the response header of a variable.";
				logAndThrow(reason);
			}
			break;

		// and this stuff just isn't possible in TSNC ...

		case CHAR_ASSERT_RESP_MESSAGE:
			// can't validate the response message
			String reason = "Can't assert Response Message in TSNC";
			logAndThrow(reason);

		case CHAR_ASSERT_URL:
			// can't validate the url
			String s = "Can't assert URL Sample in TSNC";
			logAndThrow(s);

		default:
			// Something went wrong!
			String message = "Coudn't detect selectionMode/ Response Field to Test";
			logAndThrow(message);
		}

		return selectionMode;
	}

	/**
	 * Maps the value from Jmeters Pattern Matching Rules to TSNCs
	 * validationMode. Should be called in {@link #readResponseAssertion}. The
	 * validationMode in Jmeter has a few options that just arn't possible in TSNC.
	 * Throws a {@link MappingException} if it comes upon one. Also throws one if
	 * it doesn't recognize one, though that should never happen.
	 * 
	 * @param valModeInInt
	 *            the rough aquivalent to TSNC validationMode in Jmeter. An
	 *            integer value.
	 * @return the validationMode from TSNC
	 */
	private String getValidationModeFromInt(int valModeInInt)
	{

		String validationMode = null;

		switch (valModeInInt)
		{
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

		// the following values come from Jmeters "Not". Since there's no "Not"
		// option in TSNCs validations, log an error and throw an exception
		case 5:
			// Jmeter: Matches and Not.
			String reason = "Can't map \"Not\" in the Response Assertion, that option "
					+ "doesn't exist in TSNC.";
			logAndThrow(reason);
		case 6:
			// Jmeter: Contains and Not.
			String s = "Can't map \"Not\" in the Response Assertion, that option "
					+ "doesn't exist in TSNC.";
			logAndThrow(s);
		case 12:
			String message = "Can't map \"Not\" in the Response Assertion, that option "
					+ "doesn't exist in TSNC.";
			logAndThrow(message);
		case 20:
			// Jmeter: Substring and Not.
			String cause = "Can't map \"Not\" in the Response Assertion, that option "
					+ "doesn't exist in TSNC.";
			logAndThrow(cause);

		default:
			String string = "Coudn't detect validation mode in Jmeter";
			logAndThrow(string);
		}
		return validationMode;
	}

	/**
	 * Called in the {@link #readResponseAssertion} method. Reads until the end
	 * of an {@link Tagname#ASSERT_ALL_VALUES} tag and returns a list of the values
	 * that should be validated. Ie multiple validationContent values from
	 * {@link URLActionDataValidation} which will be used to build multiple
	 * validation objects
	 * 
	 * @return multiple String aquivalent to multiple validationContents
	 * @throws XMLStreamException
	 */
	private List<String> readAllValuesToValidate() throws XMLStreamException
	{

		List<String> allValidationContent = new ArrayList<>();

		XMLEvent event = reader.nextEvent();
		while (!isEnd(event, Tagname.ASSERT_ALL_VALUES))
		{
			event = reader.nextEvent();

			if (isStart(event, Tagname.ASSERT_ONE_CONTENT))
			{
				// one content was found. Read it and
				// put it on the list
				event = reader.nextEvent();
				String content = getTagContent(event);
				allValidationContent.add(content);
			}
		}

		return allValidationContent;
	}

	/**
	 * Builds the {@link URLActionDataValidation} objects from the given inputs
	 * and adds them to the actionBuilder. Should be called in {@link #readResponseAssertion}.
	 * 
	 * <p>
	 * Parameters are the necessary parameters to construct an {@link URLActionDataValidation}
	 * object and the actionBuilder.
	 * </p>
	 * 
	 * <p>
	 * Uses a few ifs for exceptional validations, like the HttpResponceCode, Headers or Variables.
	 * </p>
	 * 
	 * <p>
	 * The selectionContent can be null. In that case set it to ".*", ie everything.
	 * </p>
	 * 
	 * @param name
	 * @param selectionMode
	 * @param selectionContent
	 * @param validationMode
	 * @param allValidationContent
	 */
	private void createValidations(String name, String selectionMode, String selectionContent,
			String validationMode, List<String> allValidationContent)
	{

		// In the exceptional cases ...

		// if selectionMode: HEADER, the selectionContent needs to be transformed a bit

		if (selectionMode.equals(URLActionDataValidation.HEADER))
		{

			for (int i = 0; i < allValidationContent.size(); i++)
			{
				String validationContent = allValidationContent.get(i);
				String[] header = validationContent.split(":");

				if (2 < header.length)
				{

					String reason = "Pattern to test against the Header in " + name
							+ " coudn't be translated.";
					logAndThrow(reason);
				}
				if (header.length == 2)
				{
					// there's both a name and a value

					selectionContent = header[0]; // header name
					validationContent = header[1]; // header value
					validationMode = URLActionDataValidation.MATCHES;

					// for some reason Jmeter puts linebreaks here ...
					if (i < allValidationContent.size())
					{
						validationContent = validationContent.trim();
					}

				}
				if (header.length == 1)
				{
					// there's only a name (or a value, but there's no way to know which)

					selectionContent = header[0];
					validationMode = URLActionDataValidation.EXISTS;
					validationContent = null;
				}

				URLActionDataValidation validation = new URLActionDataValidation(name,
						selectionMode, selectionContent, validationMode, validationContent,
						interpreter);
				actionBuilder.addValidation(validation);
			}
			return;
		}

		// if the response code should be validated ...

		if (selectionMode.equals(VALIDATE_RESP_CODE))
		{
			String httpResponseCode = allValidationContent.get(0);
			actionBuilder.setHttpResponceCode(httpResponseCode);
			return;
		}

		// In the more normal cases ...

		if (selectionContent == null)
		{
			// this _should_ be everything
			selectionContent = ".*";
		}

		for (String validationContent : allValidationContent)
		{

			URLActionDataValidation validation = new URLActionDataValidation(name, selectionMode,
					selectionContent, validationMode, validationContent, interpreter);
			actionBuilder.addValidation(validation);
		}
		return;
	}

	/**
	 * Should be called when the {@link #readActionContent} method comes upon a
	 * {@link Tagname#XPATH_EXTRACT} tag. Reads until the end of the {@link #TNAME_XPATH_EXTRACT}
	 * tag and returns an {@link URLActionDataStore} object.
	 * 
	 * @param reader
	 *            the XMLEventReader with it's position
	 * @param storeBuilder
	 *            the storeBuilder with the interpreter and the selectionMode
	 *            already added
	 * @return a {@link URLActionDataStore} object
	 * @throws XMLStreamException
	 */
	private URLActionDataStore readXPathExtractor(XMLEventReader reader,
			URLActionDataStoreBuilder storeBuilder) throws XMLStreamException
	{

		String name = null;
		String selectionContent = null;

		// loop ...
		// all parameters should be inside a single Tagname.XPATH_EXTRACT tag
		XMLEvent event = reader.nextEvent();
		while (!isEnd(event, Tagname.XPATH_EXTRACT))
		{
			event = reader.nextEvent();

			if (hasAttribute(event, AttributeNameValue.XPATH_EXT_REFNAME))
			{
				event = reader.nextEvent();
				name = getTagContent(event);
			}
			if (hasAttribute(event, AttributeNameValue.XPATH_EXT_XPATH))
			{
				event = reader.nextEvent();
				selectionContent = getTagContent(event);
			}
			if (hasAttribute(event, AttributeNameValue.XPath_EXT_FROM_VAR))
			{
				// exists if it should extract from a variable.
				// that's impossible in TSNC, so just log and throw.

				String reason = "XPath Extractor " + name
						+ " should extract from a variable. Since TSNC always extracts from "
						+ "the first match. That is unfortunately impossible in TSNC.";
				logAndThrow(reason);
			}
		}

		storeBuilder.setName(name);
		storeBuilder.setSelectionContent(selectionContent);
		URLActionDataStore store = storeBuilder.build();
		return store;
	}

	/**
	 * Should be called when the {@link #readActionContent} method comes upon a
	 * {@link Tagname.REGEX_EXTRACT} tag. Reads until the end of the {@link Tagname.REGEX_EXTRACT}
	 * tag and returns an URLActionDataStore object.
	 * 
	 * @param selectionMode
	 * @param reader
	 * @param storeBuilder
	 * @return
	 * @throws XMLStreamException
	 */
	private URLActionDataStore readRegexExtractor(String selectionMode, XMLEventReader reader,
			URLActionDataStoreBuilder storeBuilder) throws XMLStreamException
	{

		String group = null;
		String name = null;
		String selectionContent = null;
		String subSelectionMode = null;
		String subSelectionContent = null;

		// start the loop ...
		XMLEvent event = reader.nextEvent();
		while (!isEnd(event, Tagname.REGEX_EXTRACT))
		{
			event = reader.nextEvent();

			if (hasAttribute(event, AttributeNameValue.REGEX_EXT_REFNAME))
			{
				// get the name of the variable to create
				event = reader.nextEvent();
				name = getTagContent(event);
			}
			if (hasAttribute(event, AttributeNameValue.REGEX_EXT_REGEX))
			{
				// get the regex
				event = reader.nextEvent();
				selectionContent = getTagContent(event);
			}
			if (hasAttribute(event, AttributeNameValue.REGEX_EXT_GROUP))
			{
				// get/ determine if a subSelectionMode is needed

				event = reader.nextEvent();
				if (event.isCharacters())
				{
					group = getTagContent(event);
				}
				else
				{
					// set the default = 0;
					group = "$0$";
				}
			}
			if (hasAttribute(event, AttributeNameValue.REGEX_EXT_MATCH))
			{
				// the match specifies from which match to extract from.
				// since TSNC always extracts from the first match it
				// logs a warning at "random" (=0) and an error at 2,3,4
				// ...

				event = reader.nextEvent();
				if (event.isCharacters())
				{
					int match = Integer.parseInt(getTagContent(event));

					if (match == 0)
					{
						XltLogger.runTimeLogger.warn("Regex Extractor " + name
								+ " should extract from a random match. That is impossible in "
								+ "TSNC. It will extract from the first match instead.");
					}
					if (1 < match)
					{
						String reason = "Can only extract from first match. " + name
								+ " intented to extract from a later match.";
						logAndThrow(reason);
					}
				}

				else
				{
					// no match was set, log a warning and use the first
					// match
					// not like TSNC can do anything else
					XltLogger.runTimeLogger.warn("No match was set for Regex Extractor " + name
							+ "." + " Extracting from first match ... ");
				}
			}
			if (hasAttribute(event, AttributeNameValue.REGEX_EXT_FROM_VAR))
			{
				// exists if it should extract from a variable.
				// that's impossible in TSNC, so just log and throw.

				if (event.isCharacters())
				{
					String reason = "Regex Extractor "
							+ name
							+ " should extract from a variable. That is unfortunately impossible in TSNC.";
					logAndThrow(reason);
				}
			}
			if (hasAttribute(event, AttributeNameValue.REGEX_EXT_SCOPE))
			{
				// if we should extract from header, code, URL or anything but the body.
				// we can't, so just log and throw

				event = reader.nextEvent();
				String content = getTagContent(event);
				if (!(content.equals("as document") || content.equals("unescaped") || content
						.equals("false")))
				{

					String reason = "Regex Extractor " + name
							+ " should extract from something other then the response body";
					logAndThrow(reason);
				}
			}
		}

		// if multiple groups should be should be extracted, log a warning
		// and
		// just extract the first one
		String templates[] = group.split(Pattern.quote("$$"));
		if (templates.length > 1)
		{

			String reason = "Regex Extractor " + name
					+ " should extract to a more complicated template";
			logAndThrow(reason);
		}
		group = templates[0];
		group = group.replace("$", "");

		if (Integer.parseInt(group) != 0)
		{

			// set the subSelectionMode
			subSelectionMode = URLActionDataStore.REGEXGROUP;
			subSelectionContent = group;
		}

		storeBuilder.setName(name);
		storeBuilder.setSelectionMode(selectionMode);
		storeBuilder.setSelectionContent(selectionContent);
		storeBuilder.setSubSelectionMode(subSelectionMode);
		storeBuilder.setSubSelectionContent(subSelectionContent);
		URLActionDataStore store = storeBuilder.build();
		return store;
	}

	/**
	 * True if the passed event is a {@link StartElement} with the passed tagname.
	 * False otherwise.
	 * 
	 * @param event
	 * @param tagname
	 * @return
	 */
	private boolean isStart(XMLEvent event, Tagname tagname)
	{
		if (event.isStartElement())
		{
			StartElement startElement = event.asStartElement();
			String name = getTagName(startElement);

			if (name.equals(tagname.xmlName))
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * True if the passed event is an {@link EndElement} with the passed tagname.
	 * False otherwise.
	 * 
	 * @param event
	 * @param tagname
	 * @return
	 */
	private boolean isEnd(XMLEvent event, Tagname tagname)
	{
		if (event.isEndElement())
		{
			EndElement endElement = event.asEndElement();
			String name = getTagName(endElement);

			if (name.equals(tagname.xmlName))
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks if the passed event is a start element and has the given attribute.
	 * The latter means the start element has an attribute with the correct name and
	 * value.
	 * 
	 * @param event
	 * @param attribute
	 * @return
	 */
	private boolean hasAttribute(XMLEvent event, AttributeNameValue attribute)
	{
		if (event.isStartElement())
		{
			StartElement se = event.asStartElement();
			String attrValue = getAttributeValue(se, attribute.name.xmlName);

			if (attrValue.equals(attribute.xmlValue))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the tag name of a {@link StartElement}. Analogous to
	 * getTagName(EndElement ee). Convenience method.
	 * 
	 * @param se
	 *            the StartElement
	 * @return it's tag name as a String
	 */
	private String getTagName(StartElement se)
	{
		QName qname = se.getName();
		String name = qname.getLocalPart();
		return name;
	}

	/**
	 * Returns the tag name of a {@link EndElement}. Analogous to
	 * getTagName(StartElement se). Convenience method.
	 * 
	 * @param ee
	 *            the EndElement
	 * @return it's tag name as a String
	 */
	private String getTagName(EndElement ee)
	{
		QName qname = ee.getName();
		String name = qname.getLocalPart();
		return name;
	}

	/**
	 * Gets the attributeValue from a StartElement and an attribute name. </br>
	 * Returns the value of the {@link #NOTFOUND} constant if no attribute with
	 * that name exists.
	 * 
	 * @param se
	 *            the StartElement from which we want to get the value
	 * @param attributeName
	 *            the name of the attribute
	 * 
	 * @return and the value of the attribute as a string
	 */
	private String getAttributeValue(StartElement se, String attributeName)
	{

		String attributeValue = null;
		QName qname = new QName(attributeName);
		if (se.getAttributeByName(qname) == null)
		{
			attributeValue = NOT_FOUND;
		}
		else
		{
			Attribute attribute = se.getAttributeByName(qname);
			attributeValue = attribute.getValue();
		}

		return attributeValue;
	}

	/**
	 * Gets the content of a tag, for example: {@code<tag>content<tag>} -->
	 * "content". Will log a warning if the tag was empty: {@code<tag></tag>} --> log warning and
	 * return a warning. String.</br>
	 * Convenience method.
	 * 
	 * @param event
	 *            the character event aquivalent to the content of the tag
	 * @return said event as a string. If there was no content, return the
	 *         warning as a string.
	 */
	private String getTagContent(XMLEvent event)
	{
		if (event.isCharacters())
		{
			Characters characters = event.asCharacters();
			String content = characters.getData();
			return content;
		}
		else
		{
			// this really shoudn't happen
			String warning = "An unexpected error occured during the Jmeter -> TSNC conversion."
					+ "tried to get a tags content when none was there";
			XltLogger.runTimeLogger.warn(warning);
			return warning;
		}
	}

	/**
	 * Tests if a certain variable is inside the interpreter. Takes a name, checks if a variable
	 * with that name is registered with it and returns true of it is, false otherwise.
	 * 
	 * @param var
	 * @return
	 */
	private boolean isVarInInterpreter(String name)
	{

		for (String var : interpreter.getNameSpace().getVariableNames())
		{
			if (name.equals(var))
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Declares the custom mapping exception for better error handling.
	 */
	@SuppressWarnings("serial")
	public class MappingException extends RuntimeException
	{

		public MappingException()
		{

		}

		public MappingException(String message)
		{
			super(message);
		}

		public MappingException(Throwable cause)
		{
			super(cause);
		}

		public MappingException(String message, Throwable cause)
		{
			super(message, cause);
		}
	}

	/**
	 * Takes a reason to abort as a string.
	 * 
	 * Logs an error and throws a {@link MappingException} with a
	 * general message + the given reason.
	 * 
	 * @param reason
	 */
	private void logAndThrow(String reason)
	{
		String message = "Coudn't map the test case from Jmeter to TSNC." + "Reason: " + reason;

		XltLogger.runTimeLogger.error(message);
		throw new MappingException(message);
	}
}