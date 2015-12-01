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

import org.jfree.util.Log;

import bsh.EvalError;

import com.gargoylesoftware.htmlunit.util.NameValuePair;

import com.xceptance.xlt.api.util.XltLogger;
import com.xceptance.xlt.common.util.bsh.ParameterInterpreter;

/*
 * Implementation of the {@link URLActionDataListBuilder} for Jmeters .jmx files. <br/>
 * Only works for a subset of Jmeter functions. Those are:
 * <ul>
 * <li> Storing variables
 * <li> HttpSamplers with url, method and parameters TODO
 * <li> Response Assertions with ...... TODO
 * <li> RegEx and XPath extractors with .......	TODO
 * <li> Storage of custom variables TODO
 * </ul>
 * 
 * Configs and properties are handled TODO - How ?
 * Listeners are ignored. TSNCs own result browsers will be used.
 */
public class JMXBasedURLActionDataListBuilder extends URLActionDataListBuilder {
	
	private List<URLActionData> actions = new ArrayList<URLActionData>();

    public JMXBasedURLActionDataListBuilder(final String filePath,
    										final ParameterInterpreter interpreter,
    										final URLActionDataBuilder actionBuilder)
    {
    		super(filePath, interpreter, actionBuilder);
    		XltLogger.runTimeLogger.debug("Creating new Instance");
    }
   
	/*
	 * Transforms the .jmx file into a list of URLActionData objects.
	 * Returns said list.
	 * 
	 * (non-Javadoc)
	 * @see com.xceptance.xlt.common.util.action.data.URLActionDataListBuilder#buildURLActionDataList()
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
			//read the next event in the XML stream
			XMLEvent event = reader.nextEvent();
			// if the reader comes upon a new (start) element
			if(event.isStartElement()){
				
				// check it's name and delegate to a subfunction accordingly
				StartElement se = event.asStartElement();
				QName name = se.getName();
				// DEBUG
//				System.out.println("Attributename: " + name.getLocalPart());
				
				if (name.toString().equals("HTTPSamplerProxy")) {
					// an HTTPSamplerProxy is aquivalent to an HttpRequest is aquivalent to an action
					
					// get and set the testname
					Attribute actionName = se.getAttributeByName(new QName("testname"));
					String testname = actionName.getValue();
					
					URLActionData action = readAction(reader, actionBuilder, testname);
					actions.add(action);
				}
				if (name.toString().equals("Arguments")) {
					// reads the arguments and stores the variables		
					readArguments(actionBuilder, reader);
				}
			}
		}
		
		// TODO for now, exceptionhandling should be changed later 
		inputstream.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		
		return actions;
	}
	
	/*
	 * Is called when the main parsing method {@link #buildURLActionDataList} comes upon a
	 * startelement for an action (HTTPSamplerProxy)<br/>
	 * Parses the file and creates the action with the appropriate parameters <br/>
	 * Returns the action when it comes upon the endelement.
	 */
	private URLActionData readAction(XMLEventReader reader,URLActionDataBuilder actionBuilder, String testName) throws XMLStreamException {
		
		// set the testname and the interpreter
		actionBuilder.setName(testName); 	
		actionBuilder.setInterpreter(interpreter);
		
		// keep reading until the endelement with the tag name of HTTPSamplerProxy
		boolean notEndOfAction = true;
		while (notEndOfAction) {
			XMLEvent event = reader.nextEvent();
			
			// look for startelements
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();
				
				// and get the attribute for 'name' 
				Attribute attributeName = se.getAttributeByName(new QName("name"));
				
				// if it exists and has a certain value
				if (attributeName != null) {
					String name = attributeName.getValue();
					
					// if the name attribute corresponds to an action parameter, 
					// set the appropriate action parameter to the content
					switch (name) {
					// make separate methods here ?
					// could also set a tag and read in characters depending on the tags value.
					// That might be safer.
					case "HTTPSampler.domain" : {				
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
					}
					default : {
						break;
					}
					}
				}
			}
			
			// stop the loop if the next element is the end of the action
			// it's an endelement with the tag name of HTTPSamplerProxy
			if (reader.peek().isEndElement()) {
				if (reader.peek().asEndElement().getName().toString().equals("HTTPSamplerProxy")) {
					notEndOfAction = false;
				}
			}
		}
		// build the action and reset the URLActionDataBuilder 
		URLActionData action = actionBuilder.build();	
		return action;
	}
	
	/*
	 * is called if the tag name of a startelement equals 'Arguments'
	 * parses multiple arguments and saves them as in the TODO
	 */
	private void readArguments(URLActionDataBuilder actionBuilder, XMLEventReader reader) throws XMLStreamException {
		boolean notEndOfArguments = true;
		while (notEndOfArguments) {
			XMLEvent event = reader.nextEvent(); 
			
			// look for startelements ...
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();
				//DEBUG
//				System.out.println("Name" + se.getName().getLocalPart());
				
				// and has an 'elementType' attribute 
				Attribute attributeElementType = se.getAttributeByName(new QName("elementType"));
				if (attributeElementType != null) {
					
					// and get it if it exists
					String elementType = attributeElementType.getValue();
					
					// and get the elements tag name
					QName qname = se.getName();
					String name = qname.getLocalPart();
					
					// if they both fit and it looks like a single argument, 
					// call the readArgument method to let it read it
					if (name.equals("elementProp") && elementType.equals("Argument")) {
						//DEBUG
//						System.out.println("Call readArgument ...");
						readArgument(reader);
					}
				}
			}
			
			// stop the loop if the next element closes the Arguments tag
			// it's an endelement with the tag name of Arguments
			if (reader.peek().isEndElement()) {
				if (reader.peek().asEndElement().getName().toString().equals("Arguments")) {
					notEndOfArguments = false;
				}
			}
		}
	}
	
	/*
	 * is called inside the readArguments method to parse a single argument
	 * parses a single Argument and saves it in TODO
	 */
	private void readArgument(XMLEventReader reader) throws XMLStreamException {
		
		String argsName = null;
		String argsValue = null;
		boolean notEndOfArgument = true;
		while (notEndOfArgument) {
			XMLEvent event = reader.nextEvent(); 
			
			// look for startelements
			if (event.isStartElement()) {
				
				StartElement se = event.asStartElement();
				
				// if the attribute for 'name' exists
				Attribute nameAttribute = se.getAttributeByName(new QName("name"));
				if (nameAttribute != null) {
					String name = nameAttribute.getValue();
					
					// and it is the right String, get the content of the tag
					// and save it as name or value, depending
					switch (name) {
					case "Argument.name" : {
						event = reader.nextEvent();
						argsName = event.asCharacters().getData();
						break;
					}
					case "Argument.value": {
						event = reader.nextEvent();
						argsValue = event.asCharacters().getData();
						//DEBUG
						System.out.println("value: " + argsValue);
						break;
					}

					default: {
						break;
					}
					}
				}
			}
			
			// stop the loop if the next element closes the Arguments tag
			// it's an endelement with the tag name of Arguments
			if (reader.peek().isEndElement()) {
				if (reader.peek().asEndElement().getName().toString().equals("elementProp")) {
					notEndOfArgument = false;
				}
			}
		}
		// save the aquired arguments TODO errorsearch
		NameValuePair nvp = new NameValuePair(argsName, argsValue);
		try {
			this.interpreter.set(nvp);
		} catch (EvalError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//DEBUG
//		System.out.println("VARIABLE FOUND!");
//		System.out.println("name: " + variable.getName() + ", " + "value: " + variable.getValue());	
	}
}
