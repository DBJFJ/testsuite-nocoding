package com.xceptance.xlt.common.util.action.data.Jmeter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.xceptance.xlt.api.data.GeneralDataProvider;
import com.xceptance.xlt.api.util.XltLogger;
import com.xceptance.xlt.api.util.XltProperties;
import com.xceptance.xlt.common.util.action.data.URLActionData;

public class JmeterConverter {

	final static XltProperties properties = XltProperties.getInstance();
	final static GeneralDataProvider dataProvider = GeneralDataProvider.getInstance();
	static String dumpFolder;
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String... args) throws IOException {
					
		if (args.length == 0 || args == null) { 		// check that TODO
			throw new IllegalArgumentException("No input found");
		}
		
		for (String filePath : args) {
				
			if ( !Files.exists(Paths.get(filePath)) ) {
				XltLogger.runTimeLogger.error("File " + filePath + " not found");
			}
			
			else {
				
				// this should be the norm
				dumpFolder = System.getProperty("user.dir") + "/";
				String testplanName = FilenameUtils.getBaseName(filePath);
				
				LinkedHashMap<String, List<URLActionData>> ThreadGroups = 
												new JmeterTranslater().translateFile(filePath);
				
				// iterate over the ThreadGroups
				for (Entry<String, List<URLActionData>> ThreadGroup : ThreadGroups.entrySet()) {
					
					String ThName = ThreadGroup.getKey();
					List<URLActionData> actions = ThreadGroup.getValue();
					
					// see if we can't get a fileName
					String fileName = testplanName + "-" + ThName;	 
					
					// If the file already exists. Say, because the names were the same
					
					while (Files.exists(Paths.get(dumpFolder + fileName + ".yml"))) {
						XltLogger.runTimeLogger.info("File " + dumpFolder + fileName + ".yml" + " already exists");
						fileName = fileName + "#";
					}
					Path dumpThere = Paths.get(dumpFolder + fileName + ".yml");
					XltLogger.runTimeLogger.info("Writing to " + dumpThere);
					
					// dump the actionlist
					YAMLBasedDumper.dumpActionsYaml(actions, dumpThere);	
				}		
			}
		}
	}

}
