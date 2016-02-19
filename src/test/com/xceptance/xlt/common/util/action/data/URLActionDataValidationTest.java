package test.com.xceptance.xlt.common.util.action.data;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import bsh.EvalError;

import com.xceptance.xlt.api.data.GeneralDataProvider;
import com.xceptance.xlt.api.util.XltProperties;
import com.xceptance.xlt.common.util.action.data.URLActionDataValidation;
import com.xceptance.xlt.common.util.bsh.ParameterInterpreter;

public class URLActionDataValidationTest
{
    ParameterInterpreter interpreter;
    private XltProperties properties;

    private GeneralDataProvider dataProvider;

    List<String> selectionModes;

    List<String> validationModes;

    List<URLActionDataValidation> validations;

    @Before
    public void setup()
    {
        properties = XltProperties.getInstance();
        dataProvider = GeneralDataProvider.getInstance();
        interpreter = new ParameterInterpreter(properties, dataProvider);
        
        selectionModes = new ArrayList<String>();
        validationModes = new ArrayList<String>();

        selectionModes.addAll(URLActionDataValidation.PERMITTEDSELECTIONMODE);
        validationModes.addAll(URLActionDataValidation.PERMITTEDVALIDATIONMODE);

    }
    @Test 
    public void constructorTest(){
        for (final String selectionMode : selectionModes)
        {
            for (final String validationMode : validationModes)
            {
                @SuppressWarnings({
                        "unused"
                    })
                final URLActionDataValidation validation = new URLActionDataValidation("name",
                                                                               selectionMode,
                                                                               "content",
                                                                               validationMode,
                                                                               "content",
                                                                               interpreter);

            }
        }
    }
    @Test(expected = IllegalArgumentException.class)
    public void illegalSetupName()
    {
        @SuppressWarnings({
                "unused"
            })
        final URLActionDataValidation validation = new URLActionDataValidation(null, null, null, null,
                                                                       null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalSetupSelectionMode()
    {
        @SuppressWarnings({
                "unused"
            })
        final URLActionDataValidation validation = new URLActionDataValidation("name", null, "something",
                                                                       URLActionDataValidation.EXISTS,
                                                                       null, interpreter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalSetupValidationMode()
    {
        @SuppressWarnings({
                "unused"
            })
        final URLActionDataValidation validation = new URLActionDataValidation("name",
                                                                       URLActionDataValidation.XPATH,
                                                                       "something", null, null,
                                                                       interpreter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalInterpreter()
    {
        @SuppressWarnings({
                "unused"
            })
        final URLActionDataValidation validation = new URLActionDataValidation("name",
                                                                       URLActionDataValidation.XPATH,
                                                                       "something",
                                                                       URLActionDataValidation.MATCHES,
                                                                       null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalSelectionMode()
    {
        final URLActionDataValidation validation = new URLActionDataValidation("name", "x", "something",
                                                                       URLActionDataValidation.MATCHES,
                                                                       null, interpreter);
        @SuppressWarnings("unused")
		final String selectionMode = validation.getSelectionMode();


    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalValidationMode()
    {
        final URLActionDataValidation validation = new URLActionDataValidation("name",
                                                                       URLActionDataValidation.XPATH,
                                                                       "something", "x", null,
                                                                       interpreter);
        @SuppressWarnings("unused")
		final String validationMode = validation.getValidationMode();
    }
    
    
    /**
     * Tests if the normal getters and the raw getters are working as they should.
     * 
     * @throws EvalError
     */
    @Test
    public void testGetters() throws EvalError {
    	String name = "name";
    	String value = "value";
    	String parName = "${name}";
    	interpreter.set(name, value);
    	
    	String nameSMode = "nameSMode";
    	String valueSMode = URLActionDataValidation.REGEXP;
    	String parNameSMode = "${nameSMode}";
    	interpreter.set(nameSMode, valueSMode);
    	
    	String nameSubSMode = "nameSubSMode";
    	String valueSubSMode = URLActionDataValidation.REGEXGROUP;
    	String parNameSubSMode = "${nameSubSMode}";
    	interpreter.set(nameSubSMode, valueSubSMode);
    	
    	String nameVMode = "nameVMode";
    	String valueVMode = URLActionDataValidation.MATCHES;
    	String parNameVMode = "${nameVMode}";
    	interpreter.set(nameVMode, valueVMode);
    	
    	URLActionDataValidation validation = new URLActionDataValidation("hi",
    			parNameSMode, parName, 
    			parNameSubSMode, parName, 
    			parNameVMode, parName,
    			interpreter);
    	
    	// test the normal methods
    	Assert.assertEquals(valueSMode, validation.getSelectionMode());
    	Assert.assertEquals(value, validation.getSelectionContent());
    	
    	Assert.assertEquals(valueSubSMode, validation.getSubSelectionMode());
    	Assert.assertEquals(value, validation.getSubSelectionContent());
    	
    	Assert.assertEquals(valueVMode, validation.getValidationMode());
    	Assert.assertEquals(value, validation.getValidationContent());
    	
    	// test the raw methods
    	Assert.assertEquals(parName, validation.getRawSelectionContent());
    	
    	Assert.assertEquals(parName, validation.getRawSubSelectionContent());
    	
    	Assert.assertEquals(parName, validation.getRawValidationContent());
    }
}
