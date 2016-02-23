package com.xceptance.xlt.common.util.action.data.Jmeter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.FilenameUtils;

import com.xceptance.xlt.api.data.GeneralDataProvider;
import com.xceptance.xlt.api.util.XltLogger;
import com.xceptance.xlt.api.util.XltProperties;
import com.xceptance.xlt.common.util.action.data.URLActionData;

public class JmeterConverter {

	final static XltProperties properties = XltProperties.getInstance();
	final static GeneralDataProvider dataProvider = GeneralDataProvider.getInstance();
	static Path dumpFolder;
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String... args) throws IOException {
					
		// if there are no arguments ...
		if (args.length == 0 ) { 		
			throw new IllegalArgumentException("No input found");
		}
		
		for (String filePath : args) {
	
			// if the file doesn't exist ,,,
			if ( !Files.exists(Paths.get(filePath)) ) {
				XltLogger.runTimeLogger.error("File " + filePath + " not found");
				return;
			}
			
			// if the file is empty ...
			if ( Files.size(Paths.get(filePath)) == 0 ) {
				XltLogger.runTimeLogger.error("File " + filePath + " is empty");
				return;
			}
			
			// if it's not a .jmx file ...
			if ( !(FilenameUtils.getExtension(filePath).equals("jmx")) ) {
				XltLogger.runTimeLogger.error("File " + filePath + " is not a Jmeter (.jmx) file");
				return;
			}
				
			// finally the norm ...
				
			// get the folder in which to dump the files and the name of the test plan
			dumpFolder = Paths.get(filePath).getParent();
			if (dumpFolder == null) {
				dumpFolder = Paths.get("./");
			}		
			
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
					
				Path dumpThere = Paths.get(dumpFolder + "/" + fileName + ".yml");
				while (Files.exists(dumpThere)) {
					XltLogger.runTimeLogger.info(dumpThere.toString() + " already exists");
					
					fileName = fileName + "#";
					dumpThere = Paths.get(dumpFolder + "/" + fileName + ".yml");
				}
				XltLogger.runTimeLogger.info("Writing to " + dumpThere);
				
				// dump the actionlist
				YAMLBasedDumper.dumpActionsYaml(actions, dumpThere);	
			}		
		}
	}


}
