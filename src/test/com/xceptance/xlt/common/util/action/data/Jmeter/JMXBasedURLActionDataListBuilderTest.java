package test.com.xceptance.xlt.common.util.action.data.Jmeter;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.xceptance.xlt.api.data.GeneralDataProvider;
import com.xceptance.xlt.api.util.XltProperties;
import com.xceptance.xlt.common.util.action.data.URLActionData;
import com.xceptance.xlt.common.util.action.data.URLActionDataBuilder;
import com.xceptance.xlt.common.util.action.data.Jmeter.JMXBasedURLActionDataListBuilder;
import com.xceptance.xlt.common.util.bsh.ParameterInterpreter;

public class JMXBasedURLActionDataListBuilderTest {

	private final static String rootPath = "./config/data/test/JmeterStuff/";
	private final String filePath1 = rootPath + "TSearchDidYouMean.jmx";
	
	private ParameterInterpreter interpreter;
	private URLActionDataBuilder actionBuilder;
	private XltProperties properties;
	private GeneralDataProvider dataProvider;
	
    @Before
    public void setup()
    {
        properties = XltProperties.getInstance();
        dataProvider = GeneralDataProvider.getInstance();
        interpreter = new ParameterInterpreter(properties, dataProvider);
        actionBuilder = new URLActionDataBuilder();
    }
    
    @Test
    public void testCorrectConstructor()
    {       
		@SuppressWarnings("unused")
     	final JMXBasedURLActionDataListBuilder builder = new JMXBasedURLActionDataListBuilder(
     				filePath1, this.interpreter, actionBuilder);
    }
    
    /**
     * Tests if the first Thread Group and only the first Thread Group is returned.
     * Tests with an example file.
     */
    @Test
    public void testFirstThGroup() {
    	final int ACTNUM = 6;
    	final String ACTNAME = "Open Homepage";
    	
     	final JMXBasedURLActionDataListBuilder builder = new JMXBasedURLActionDataListBuilder(
 				filePath1, this.interpreter, actionBuilder);
     	final List<URLActionData> actions = builder.buildURLActionDataList();
     	
     	Assert.assertEquals(ACTNUM, actions.size());
     	Assert.assertEquals(ACTNAME, actions.get(0).getName());
    }

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
}
