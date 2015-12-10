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
	 * if the getAttributeValue(attributeName) method was used but the attribute wasn't found,
	 * it will return this value instead
	 */
	private final String NOTFOUND = "Attribute not found";
	
	/*
	 * <p>The following constant are used for the tagnames in Jmeter. 
	 * For example, in <Arguments ...> 'Arguments' is the tag name for 
	 * the place where variables are defined.</p>
	 * 
	 * Their names follow the theme T (for tag) + NAME + abbreviation of their function.
	 * An "S" at the end signifies plural. Example: TNAMEVARS for tagNameVariables.
	 */
	
	private final String TNAMEACTION = "HTTPSamplerProxy"; 
	
	private final String TNAMEVARS = "Arguments";
	
	private final String TNAMEVAR = "elementProp";
	
	private final String TNAMEPARAMS = "collectionProp";
	
	private final String TNAMEPARAM = "elementProp";
	
	private final String TNAMERESPASSERT = "ResponseAssertion";
	
	private final String TNAMEXPATHEXTRACT = "XPathExtractor";

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
	private final String TNAMECONTENT = "hashTree";
	
	/*
	 * <p>The following constant are used for the attribute names in Jmeter. 
	 * For example, in <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" 
	 * testname="test for correcting the mistake afterwards 1" enabled="true"> 
	 * 'testname' is the attribute name for the name of the attribute which defines the actin name.
	 * 
	 * Their names follow the theme ATTR (for attribute) + N (for name) + abbreviation of their function.
	 * An "S" at the end signifies plural. Example: ATTRNACTIONNAME for attributeNameActionName.
	 */
	
	private final String ATTRNACTIONNAME = "testname";
	
	private final String ATTRNELETYPE = "elementType";
	
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
	private final String ATTRNNAME = "name";
	
	/*
	 * <p>The following constant are used for the attribute values in Jmeter. 
	 * For example, in  <stringProp name="HTTPSampler.domain">${host}/search</stringProp>
	 * 'stringProp' is the tag name, 'name' an attribute name and 'HTTPSampler.domain' an 
	 * attribute value.</p>
	 * 
	 * Their names follow the theme ATTR (for attribute) + V (for value) + abbreviation of their function.
	 * An "S" at the end signifies plural. Example: ATTRVACTIONURL for attributeValueActionUrl.
	 */
	
	private final String ATTRVACTIONURL = "HTTPSampler.domain";
	
	private final String ATTRVACTIONMETHOD = "HTTPSampler.method";
	
	private final String ATTRVACTIONPARAM = "Arguments.arguments";
	
	private final String ATTRVELEISVAR = "Argument";
	
	private final String ATTRVELEISPARAM = "HTTPArgument";
	
	private final String ATTRVVARNAME = "Argument.name";
	
	private final String ATTRVVARVALUE = "Argument.value";
	
	private final String ATTRVENCODEPARAM = "HTTPArgument.always_encode";
	
	private final String ATTRVPARAMNAME = "Argument.name";
	
	private final String ATTRVPARAMVALUE = "Argument.value";
	
	private final String ATTRVXPEXTREFNAME = "XPathExtractor.refname";
	
	private final String ATTRVXPEXTXPATH = "XPathExtractor.xpathQuery";
	
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
					
					case TNAMEACTION : {
						
						// an TNAMEACTION is aquivalent to an HttpRequest
						// is aquivalent to an action get and set the testname
						String actionName = getAttributeValue(ATTRNACTIONNAME, se);
						URLActionData action = readAction(reader,
								actionBuilder, actionName);
						actions.add(action);
						break;
					}
					case TNAMEVARS : {
						
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
				if (name.equals(TNAMEACTION)) {
					break;
				}
			}
			
			// look for StartElements
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();

				// and get the attribute for 'name'
				String name = getAttributeValue(ATTRNNAME, se);

				// if the name attribute corresponds to an action parameter,
				// set the appropriate action parameter to the content
				switch (name) {
				case ATTRVACTIONURL: {
					// read the content
					event = reader.nextEvent();
					String content = event.asCharacters().getData();
					actionBuilder.setUrl(content);
					break;
				}
				case ATTRVACTIONMETHOD: {
					// read the content
					event = reader.nextEvent();
					String content = event.asCharacters().getData();
					actionBuilder.setMethod(content);
					break;
				}
				case ATTRVACTIONPARAM: {
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
				case TNAMECONTENT: {
				
					// we just went a bit further down in the tree
					treeLevel++;
					break;
				}
				case TNAMERESPASSERT: {
					
					// read the Response Assertion
					validationBuilder.setInterpreter(interpreter);
					URLActionDataValidation validation = readResponseAssertion(validationBuilder, 
							reader);
					if (validation != null) {
						validations.add(validation);
					}
					validationBuilder.reset();
					break;
				}
				
				case TNAMEXPATHEXTRACT: {
					
					// read the XPathExtractor
					storeBuilder.setInterpreter(interpreter);
					URLActionDataStore variableToExtract = readXPathExtractor(reader, storeBuilder);
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
				if (name.equals(TNAMEVARS)) {
					break;
				}
			}
			
			// look for startelements ...
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();

				// and has an 'elementType' attribute
				String elementType = getAttributeValue(ATTRNELETYPE, se);

				// and get the elements tag name
				String name = getTagName(se);

				// if they both fit and it looks like a single argument,
				// call the readArgument method to let it read it
				if (name.equals(TNAMEVAR)
						&& elementType.equals(ATTRVELEISVAR)) {
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
			if (name.equals(TNAMEVAR)) {
				break;
				}
			}

			// look for StartElements
			if (event.isStartElement()) {

				StartElement se = event.asStartElement();

				// if the attribute for 'name' exists
				String name = getAttributeValue(ATTRNNAME, se);

				// and it is the right String, get the content of the tag
				// and save it as name or value, depending
				switch (name) {
				case ATTRVVARNAME: {
					event = reader.nextEvent();
					argsName = getTagContent(event);
					break;
				}
				case ATTRVVARVALUE: {
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
			if (name.equals(TNAMEPARAMS)) {
				break;
				}
			}

			// look for startelements ...
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();

				// and it's tag name is ATTRNELETYPE
				String name = getTagName(se);
				if (name.equals(TNAMEPARAM)) {

					// if the elementType attribute is 'HTTPArgument'
					String elementType = getAttributeValue(ATTRNELETYPE, se);
					if (elementType.equals(ATTRVELEISPARAM)) {

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
				if (name.equals(TNAMEPARAM)) {
					break;
				}
			}

			// look for startelements
			if (event.isStartElement()) {

				StartElement se = event.asStartElement();

				String name = getAttributeValue(ATTRNNAME, se);

				// if the name attribute is the right String, get the content of the tag
				// and set something, depending on the attributeName value
				switch (name) {
				case ATTRVENCODEPARAM: {
					event = reader.nextEvent();
					encoded = getTagContent(event);
					break;
				}
				case ATTRVPARAMVALUE: {
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
				case ATTRVPARAMNAME: {
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
	
	private URLActionDataValidation readResponseAssertion(
			URLActionDataValidationBuilder validationbuilder, XMLEventReader reader) {
		
		return null; 
	}
	
	/*
	 * Should be called when the readActionContent method comes upon a TNAMEXPATHEXTRACT tag.
	 * Reads until the end of the TNAMEXPATHEXTRACT tag and returns an URLActionDataStore object.
	 */
	private URLActionDataStore readXPathExtractor(XMLEventReader reader, 
			URLActionDataStoreBuilder storeBuilder) throws XMLStreamException {
				
		String name = null;
		String selectionMode = URLActionDataStore.XPATH;
		String selectionContent = null;
		
		// loop until the loop is closed with an EndElement with the tag name TNAMEXPATHEXTRACT
		// all parameters should be inside a single TNAMEXPATHEXTRACT tag
		while (true) {			
			XMLEvent event = reader.nextEvent();
			
			if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				String tagName = getTagName(ee);
				if (tagName.equals(TNAMEXPATHEXTRACT)) {
					break;
				}
			}

			// look for StartElements
			if (event.isStartElement()) {

				StartElement se = event.asStartElement();

				String attrName = getAttributeValue(ATTRNNAME, se);

				// if the name attribute is the right String, get the content of the tag
				// and set something, depending on the attributeName value
				switch (attrName) {
				case "XPathExtractor.default": {
					// there doesn't seem to be an equivalent in TSNC
					break;
				}
				case ATTRVXPEXTREFNAME: {
					event = reader.nextEvent();
					name = getTagContent(event);
					break;
				}
				case ATTRVXPEXTXPATH: {
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
	 * <p>Gets the attributeValue from a StartElement and an attributeName. </p>
	 * <p>Returns the value of the NOTFOUND constant if no attribute with that name exists.</p> 
	 */
	private String getAttributeValue(String attributeName, StartElement se) {
		
		String attributeValue = null;
		QName qname = new QName(attributeName); 
		if (se.getAttributeByName(qname) == null) {
			attributeValue = NOTFOUND;
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
