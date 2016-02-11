package com.xceptance.xlt.common.util.action.data;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import bsh.Variable;

import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.xceptance.xlt.api.util.XltLogger;
import com.xceptance.xlt.common.util.bsh.ParameterInterpreter;

import com.xceptance.xlt.common.util.action.data.YAMLBasedURLActionDataListBuilder;


/**
 * The class contains static methods to dump a test case (a list of {@link URLActionData}
 * to a yaml file. It uses the YAML constants declared in {@link YAMLBasedURLActionDataListBuilder}
 * 
 * @author daniel
 *
 */

public class YAMLBasedDumper {
	
	/**
	 * A private constructor, there is no need to instantiate the class since it only has static
	 * methods.
	 */
	private YAMLBasedDumper() {
		throw new UnsupportedOperationException("Can't instantiate class YAMLBasedDumper");
	}

	/**
	 * Dump the given list of {@link URLActionData) into the given file in yaml.
	 * Uses snakeyaml for the dumping, and {@link #restructureActionListForDumping(List)} 
	 * to bring the data into a format snakeyaml can use.
	 * </br>
	 * Since the method is used in the {@link JMXBasedURLActionDataListBuilder} it doesn't support attributes 
	 * that arn't supported in the {@link JMXBasedURLActionDataListBuilder}. The formatting causes some slight differences:
	 * The indention may be odd at times especially with dashes and there's no whitespace 
	 * in front of colons. </br>
	 * If variables were defined at two different times (not inside actions, but inside the main test case), the 
	 * dumper will put both of them at the top. </br>
	 * Logs an info and overwrites the dumpfile if it already exists.
	 *  
	 * @param file 
	 * @param actions 
	 */
	public static void dumpActionsYaml(List<URLActionData> actions, Path dumpThere) throws FileNotFoundException {
		
		XltLogger.runTimeLogger.info("Writing Test Case to YAML file ...");
		
		if (Files.exists(dumpThere, LinkOption.NOFOLLOW_LINKS)) {
			XltLogger.runTimeLogger.info(dumpThere.toString() + " already exists. " +
					"Overwriting ...");
		}
		PrintWriter printwriter = new PrintWriter(dumpThere.toFile());
	
	    DumperOptions dumperoptions = new DumperOptions();
	    dumperoptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
	    dumperoptions.setIndent(4);    
	    
		Yaml yaml = new Yaml(dumperoptions);
		List<Map<String, Object>> restructuredActionList = restructureActionListForDumping(actions);	
		String s = yaml.dump(restructuredActionList);
										
		printwriter.print(s);
		printwriter.close();
		
	}
	

	
	/**
	 * <p>
	 * Takes a list of {@link URLActionData} and transforms it into a nested list full of 
	 * {@link LinkedHashMap}s so the list can be processed and dumped with ({@link #Yaml SnakeYaml}). 
	 * </p>
	 * The method creates a horrible mix of nested {@link LinkedHashMap}s and {@link List}s, because that's what 
	 * snakeyaml needs to create the correct syntax. Take a look at the yaml cheatsheet to get an idea of the 
	 * intended result. For example validations in yaml look like this: 
	 * <pre>
	 * {@code
	 * Validation:
	 * 	- validationName
	 * 		SelectionMode: SelectionContent
	 * 		ValidationMode: ValidationContent
	 * 	- validationName
	 * 		...
	 * }
	 * </pre>
	 * <p>
	 * The blocks are maps and the blocks with a dash are lists. With {name=value, name=value} for a map and 
	 * [value,value] for a list it comes down to: 
	 * Validation=[{validationName={SelMode=SelContent, ValMode=ValContent}}, ...].
	 * 
	 * TODO add Parameters and Headers and default Protocol and default Headers and 
	 * @param actions 
	 *  
	 */
	private static List<Map<String, Object>> restructureActionListForDumping(List<URLActionData> actions) {
		
		List<Map<String, Object>> root = new ArrayList<>();
		
		// get the variables to store out of the interpreter ...
		LinkedHashMap<String, Object> storeMap = new LinkedHashMap<String, Object>();
		List<Map<String, Object>> innerStoreList =  new ArrayList<>();
		
		// since they are all put together in the interpreter, somehow, just
		// check the interpreter of the first action
		ParameterInterpreter interpreter = actions.get(0).getInterpreter();
		Variable[] variables = interpreter.getNameSpace().getDeclaredVariables();
		for (Variable variable : variables) {
			String name = variable.getName();
			
			// if it's not a default constant ...
			if (name.toUpperCase() != name && name != "bsh") {
				LinkedHashMap<String, Object> varMap = new LinkedHashMap<String, Object>();
				String value = interpreter.processDynamicData("${" + name +"}");
				putUnlessEmpty(varMap, name, (Object) value);
				addUnlessEmpty(innerStoreList, varMap);
			}
		}
		putUnlessEmpty(storeMap, YAMLBasedURLActionDataListBuilder.STORE, (Object) innerStoreList);
		addUnlessEmpty(root, storeMap);

		// iterate over the actions
		for (int i = 0; i < actions.size(); i++) {
			URLActionData action = actions.get(i);
			LinkedHashMap<String, Object> outerActionMap = new LinkedHashMap<String, Object>();
			LinkedHashMap<String, Object> innerActionMap = new LinkedHashMap<String, Object>();
			innerActionMap.put(YAMLBasedURLActionDataListBuilder.NAME, action.getName());
			
			// build request map ...
			LinkedHashMap<String, Object> requestMap = new LinkedHashMap<String, Object>();
			requestMap.put(YAMLBasedURLActionDataListBuilder.URL, action.getUrlString());
			requestMap.put(YAMLBasedURLActionDataListBuilder.METHOD, action.getMethod().toString());
			innerActionMap.put(YAMLBasedURLActionDataListBuilder.REQUEST, requestMap);
			
			// get parameters
			List<Map<String, Object>> parameterList = new ArrayList<>();
			
			List<NameValuePair> parameters = action.getParameters();
			for (NameValuePair parameter : parameters) {
				LinkedHashMap<String, Object> parameterMap = new LinkedHashMap<String, Object>();
				parameterMap.put(parameter.getName(), parameter.getValue());
				addUnlessEmpty(parameterList, parameterMap);
			}
			
			putUnlessEmpty(requestMap, YAMLBasedURLActionDataListBuilder.PARAMETERS, parameterList);
			
			// get Headers
			List<Map<String, Object>> headerList = new ArrayList<>();
			
			List<NameValuePair> headers = action.getHeaders();
			for (NameValuePair header : headers) {
				LinkedHashMap<String, Object> headerMap = new LinkedHashMap<String, Object>();
				headerMap.put(header.getName(), header.getValue());
				addUnlessEmpty(headerList, headerMap);
			}
			
			putUnlessEmpty(requestMap, YAMLBasedURLActionDataListBuilder.HEADERS, headerList);
			
			// build response map ...
			LinkedHashMap<String, Object> responseMap = new LinkedHashMap<String, Object>();
			responseMap.put(YAMLBasedURLActionDataListBuilder.HTTPCODE, action.getHttpResponseCode());
			
			
			// Store/ Extractions are performed analogous to validations, just before them
			List<Map<String, Object>> storeVarsList = new ArrayList<>();
			
			for (URLActionDataStore store : action.getStore()) {
				LinkedHashMap<String, Object> outerStoreMap = new LinkedHashMap<String, Object>();
				LinkedHashMap<String, Object> innerStoreMap = new LinkedHashMap<String, Object>();
				
				putUnlessEmpty(innerStoreMap, store.getSelectionMode(), store.getSelectionContent());
				putUnlessEmpty(innerStoreMap, store.getSubSelectionMode(), store.getSubSelectionContent());
				
				putUnlessEmpty(outerStoreMap, store.getName(), innerStoreMap);
				addUnlessEmpty(storeVarsList, outerStoreMap);
			}
			
			putUnlessEmpty(responseMap, YAMLBasedURLActionDataListBuilder.STORE, storeVarsList);
			putUnlessEmpty(innerActionMap, YAMLBasedURLActionDataListBuilder.RESPONSE, responseMap);
			
			// Validations ...			
			List<Map<String, Object>> validationsList = new ArrayList<>();
			
			for (URLActionDataValidation validation : action.getValidations()) {
				LinkedHashMap<String, Object> outerValidationMap = new LinkedHashMap<String, Object>();
				LinkedHashMap<String, Object> innerValidationMap = new LinkedHashMap<String, Object>();
				
				putUnlessEmpty(innerValidationMap, validation.getSelectionMode(), validation.getSelectionContent());
				putUnlessEmpty(innerValidationMap, validation.getSubSelectionMode(), validation.getSubSelectionContent());
				putUnlessEmpty(innerValidationMap, validation.getValidationMode(), validation.getValidationContent());
				
				putUnlessEmpty(outerValidationMap, validation.getName(), innerValidationMap);
				addUnlessEmpty(validationsList, outerValidationMap);
			}
			
			putUnlessEmpty(responseMap, YAMLBasedURLActionDataListBuilder.VALIDATION, validationsList);
			putUnlessEmpty(innerActionMap, YAMLBasedURLActionDataListBuilder.RESPONSE, responseMap);

			outerActionMap.put(YAMLBasedURLActionDataListBuilder.ACTION, innerActionMap);
			root.add(outerActionMap);
		}
		return root; 
	}
	
	/**
	 * The usual put method except it checks if the key or the object to put in are empty 
	 * or null or the key is "null" before putting. If they are, it doesn't put and just
	 * returns the given map. Also logs a debug message if the object is null while the 
	 * key isn't. 
	 * 
	 * @param map
	 * @param key
	 * @param value
	 * @return
	 */
	private static void putUnlessEmpty(LinkedHashMap<String, Object> map, 
								String key, Object value) {
		
		if (key == null || key.equals("null")) {
			return;
		}
		
		if (value == null) {
			return;
		}
		
		if (value instanceof List<?>) {
			List<?> list = (List<?>) value;
			if (list.isEmpty()) {
				return;
			}
		}
		
		// already, there's actually a value
		map.put(key, value);
		return;
	}
	
	
	/**
	 * The usual add method except it checks if the object to add in are empty 
	 * or null or the key is "null" before putting. If they are, it doesn't put and just
	 * returns the given list. 
	 * 
	 * @param map
	 * @param key
	 * @param value
	 * @return
	 */
	private static void addUnlessEmpty(List<Map<String, Object>> list, 
									Map<String, Object> map) {
		if (map == null || map.isEmpty()) {
			return;
		}

		// alright, there's actually a value
		list.add(map);
		return;
	}
}
