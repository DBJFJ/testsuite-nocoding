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
					QName name = se.getName();						

					if (name.toString().equals("HTTPSamplerProxy")) {			//TODO switch Case instead
						// an HTTPSamplerProxy is aquivalent to an HttpRequest
						// is aquivalent to an action											
						// get and set the testname
						Attribute actionName = se.getAttributeByName(new QName(			//TODO getAttributeName
								"testname"));
						String testname = actionName.getValue();

						URLActionData action = readAction(reader,
								actionBuilder, testname);
						actions.add(action);
					}
					if (name.toString().equals("Arguments")) {
						// read the variables and stores them
						readVariables(actionBuilder, reader);
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
	 * <p>Is called when the main parsing method {@link #buildURLActionDataList}
	 * comes upon a StartElement for an action (HTTPSamplerProxy). Parses
	 * the file and creates the action with the appropriate parameters </p>
	 * <p>Returns the action when it comes upon the EndElement. </p>
	 */
	private URLActionData readAction(XMLEventReader reader,
			URLActionDataBuilder actionBuilder, String testName)
			throws XMLStreamException {

		// set the testname and the interpreter
		actionBuilder.setName(testName);
		actionBuilder.setInterpreter(interpreter);

		// keep reading until an EndElement with the tag name of
		// HTTPSamplerProxy is the next element
		while (true) {
			
			if (reader.peek().isEndElement()) {
				if (reader.peek().asEndElement().getName().toString()
						.equals("HTTPSamplerProxy")) {
					break;
				}
			}
			
			XMLEvent event = reader.nextEvent();
			
			// look for StartElements
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();

				// and get the attribute for 'name'
				Attribute attributeName = se.getAttributeByName(new QName(		//TODO getAttributeName
						"name"));

				// if it exists and has a certain value
				if (attributeName != null) {
					String name = attributeName.getValue();

					// if the name attribute corresponds to an action parameter,
					// set the appropriate action parameter to the content
					switch (name) {
					case "HTTPSampler.domain": {
						// read the content
						event = reader.nextEvent();
						String content = event.asCharacters().getData();
						actionBuilder.setUrl(content);
						break;
					}
					case "HTTPSampler.method": {
						// read the content
						event = reader.nextEvent();
						String content = event.asCharacters().getData();
						actionBuilder.setMethod(content);
						break;
					}
					case "Arguments.arguments": {
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
		}
		// build the action and reset the URLActionDataBuilder
		URLActionData action = actionBuilder.build();
		return action;
	}

	/*
	 * is called if the tag name of a StartElement equals 'Arguments' parses
	 * multiple arguments and saves them in the ParameterInterpreter
	 */
	private void readVariables(URLActionDataBuilder actionBuilder,
			XMLEventReader reader) throws XMLStreamException {
		
		// loop until the next element is an EndElement that closes
		// the 'Arguments' tag
		while (true) {	
			if (reader.peek().isEndElement()) {		
				if (reader.peek().asEndElement().getName().toString()
						.equals("Arguments")) {
					break;
				}
			}
			
			XMLEvent event = reader.nextEvent();

			// look for startelements ...
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();

				// and has an 'elementType' attribute
				Attribute attributeElementType = se							//TODO getAttribute
						.getAttributeByName(new QName("elementType"));
				if (attributeElementType != null) {

					// and get it if it exists
					String elementType = attributeElementType.getValue();

					// and get the elements tag name
					QName qname = se.getName();								//TODO getTagName
					String name = qname.getLocalPart();

					// if they both fit and it looks like a single argument,
					// call the readArgument method to let it read it
					if (name.equals("elementProp")
							&& elementType.equals("Argument")) {
						readVariable(reader);
					}
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
		// the tag name of 'elementProp'
		while (true) {
			if (reader.peek().isEndElement()) {
				if (reader.peek().asEndElement().getName().toString()
						.equals("elementProp")) {
					break;
				}
			}
			
			XMLEvent event = reader.nextEvent();

			// look for startelements
			if (event.isStartElement()) {

				StartElement se = event.asStartElement();

				// if the attribute for 'name' exists
				Attribute nameAttribute = se.getAttributeByName(new QName(
						"name"));													//TODO getAttribute
				if (nameAttribute != null) {
					String name = nameAttribute.getValue();

					// and it is the right String, get the content of the tag
					// and save it as name or value, depending
					switch (name) {
					case "Argument.name": {
						event = reader.nextEvent();
						argsName = event.asCharacters().getData();					//TODO maybe getTagContent?
						break;
					}
					case "Argument.value": {
						event = reader.nextEvent();
						argsValue = event.asCharacters().getData();
						break;
					}

					default: {
						break;
					}
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
	 * is called inside an action and parses the parameters 
	 */
	private void readParameters(URLActionDataBuilder actionBuilder,
			XMLEventReader reader) throws XMLStreamException {
		
		List<NameValuePair> parameters = new ArrayList<>();
		
		// loop until the loop is closed with an EndElement with the tag name collectionProp
		// all parameters should be inside a single collectionProp tag
		while (true) {
			if (reader.peek().isEndElement()) {
				if (reader.peek().asEndElement().getName().toString()
						.equals("collectionProp")) {
					break;
				}
			}
			
			XMLEvent event = reader.nextEvent();

			// look for startelements ...
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();

				// and it's tag name is 'elementProb'
				QName qname = se.getName();
				String name = qname.getLocalPart();				//TODO getTagName
				if (name.equals("elementProp")) {

					// it has an 'elementType' attribute
					Attribute attributeElementType = se
							.getAttributeByName(new QName("elementType"));		//TODO getAttribute
					if (attributeElementType != null) {

						// and that elementType attribute is 'HTTPArgument'
						String elementType = attributeElementType.getValue();
						if (elementType.equals("HTTPArgument")) {

							// read the single detected parameter
							parameters = readParameter(actionBuilder, reader, parameters);
						}
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
	 * is called inside the readParameters method to parse a single parameter 
	 * <p>parses a single Parameter with encoding, name and value. Sets the former and adds the NameValuePair with name and value to the c
	 * correct list.</p>
	 */
	private List<NameValuePair> readParameter(URLActionDataBuilder actionBuilder, 
			XMLEventReader reader, List<NameValuePair> parameters) throws XMLStreamException {

		String encoded = null;
		String parameterName = null;
		String parameterValue = null;
		
		// loop until the loop is closed with an EndElement with the tag name elementProp
		// all parameters should be inside a single elementProp tag
		while (true) {
			if (reader.peek().isEndElement()) {
				if (reader.peek().asEndElement().getName().toString()
						.equals("elementProp")) {
					break;
				}
			}
			
			XMLEvent event = reader.nextEvent();

			// look for startelements
			if (event.isStartElement()) {

				StartElement se = event.asStartElement();

				// if the attribute for 'name' exists
				Attribute nameAttribute = se.getAttributeByName(new QName(					//TODO getAttribute
						"name"));
				if (nameAttribute != null) {			
					String name = nameAttribute.getValue();

					// and it is the right String, get the content of the tag
					// and set something, depending on the attributeName value
					switch (name) {
					case "HTTPArgument.always_encode": {
						event = reader.nextEvent();
						encoded = event.asCharacters().getData();							//TODO getTagContent
						break;
					}
					case "Argument.value": {
						event = reader.nextEvent();
						parameterValue = event.asCharacters().getData();
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
					case "Argument.name": {
						event = reader.nextEvent();
						parameterName = event.asCharacters().getData();
						break;
					}
					default: {
						break;
					}
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
}
