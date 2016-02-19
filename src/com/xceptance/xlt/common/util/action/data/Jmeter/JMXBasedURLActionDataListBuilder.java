package com.xceptance.xlt.common.util.action.data.Jmeter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.xceptance.xlt.api.util.XltLogger;
import com.xceptance.xlt.common.util.action.data.URLActionData;
import com.xceptance.xlt.common.util.action.data.URLActionDataBuilder;
import com.xceptance.xlt.common.util.action.data.URLActionDataListBuilder;
import com.xceptance.xlt.common.util.action.data.YAMLBasedURLActionDataListBuilder;
import com.xceptance.xlt.common.util.bsh.ParameterInterpreter;

/**
 * Implementation of the {@link URLActionDataListBuilder} for Jmeters .jmx
 * files. <br/>
 * TSNC only implements a subset of Jmeters functions and of those only a subset is translated.
 * The tests are dumped into a YAML file in ./config/data before execution.
 * See the documentation file for details
 * <p>
 * On the technical side Jmeter saves it's tests in an xml file without a DTD.
 * This class parses the xml file using StAX. The various constants are the
 * names of the tags, the attributes or their values. 
 * </p>
 */
public class JMXBasedURLActionDataListBuilder extends URLActionDataListBuilder {

	private List<URLActionData> actions = new ArrayList<URLActionData>();

	/**
	 * Implementing the builders constructor. Similar to the 
	 * {@link YAMLBasedURLActionDataListBuilder}.
	 * 
	 * The interpreter and the actionBuilder arn't actually used, but 
	 * 
	 * @param filePath
	 *            the path to he Jmeter file to parse
	 * @param interpreter
	 *            the ParameterInterpreter for variables
	 *            {@link #ParameterInterpreter}
	 * @param actionBuilder
	 *            {@link URLActionDataBuilder}
	 */
	public JMXBasedURLActionDataListBuilder(final String filePath,
			final ParameterInterpreter interpreter,
			final URLActionDataBuilder actionBuilder) {
		super(filePath, interpreter, actionBuilder);

		XltLogger.runTimeLogger.debug("Creating new Instance");
	}

	/**
	 * Transforms the .jmx file from Jmeter into a list of URLActionData
	 * objects. Returns said list. Uses {@link JmeterTranslater}.
	 * 
	 * (non-Javadoc)
	 * 
	 * @see com.xceptance.xlt.common.util.action.data.URLActionDataListBuilder#
	 *      buildURLActionDataList()
	 */
	@Override
	public List<URLActionData> buildURLActionDataList() {

		XltLogger.runTimeLogger
				.debug("Starting Jmeter -> TSNC translation ...");

		JmeterTranslater translater = new JmeterTranslater();
		LinkedHashMap<String, List<URLActionData>> everything = 
							translater.translateFile(filePath);
		
		// get the first thread group
		actions = everything.entrySet().iterator().next().getValue();
		
		return actions;
	}
}