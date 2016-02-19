package test.com.xceptance.xlt.common.util.action.data;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import bsh.EvalError;

import com.xceptance.xlt.api.data.GeneralDataProvider;
import com.xceptance.xlt.api.util.XltProperties;
import com.xceptance.xlt.common.util.action.data.URLActionDataStore;
import com.xceptance.xlt.common.util.action.data.URLActionDataValidation;
import com.xceptance.xlt.common.util.bsh.ParameterInterpreter;

public class URLActionDataStoreTest
{

	private static ParameterInterpreter interpreter;

	private static XltProperties properties;

	private static GeneralDataProvider dataProvider;

	private static List<String> selectionModes;

	private static List<String> subSelectionModes;

	@BeforeClass
	public static void setup()
	{
		properties = XltProperties.getInstance();
		dataProvider = GeneralDataProvider.getInstance();
		interpreter = new ParameterInterpreter(properties, dataProvider);

		selectionModes = new ArrayList<String>();
		subSelectionModes = new ArrayList<String>();

		subSelectionModes.addAll(URLActionDataStore.PERMITTEDSUBSELECTIONMODE);
		selectionModes.addAll(URLActionDataStore.PERMITTEDSELECTIONMODE);
	}

	@Test
	public void constructorTest()
	{
		for (final String selectionMode : selectionModes)
		{
			@SuppressWarnings(
			{"unused" })
			final URLActionDataStore store = new URLActionDataStore("name",
					selectionMode,
					"content",
					interpreter);
		}
	}

	@Test
	public void constructor2Test()
	{
		for (final String selectionMode : selectionModes)
		{
			for (final String subSelectionMode : subSelectionModes)
			{
				@SuppressWarnings(
				{"unused" })
				final URLActionDataStore store = new URLActionDataStore("name",
						selectionMode,
						"content",
						subSelectionMode,
						"nananan",
						interpreter);
			}
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void wrongSetupName()
	{
		@SuppressWarnings(
		{"unused" })
		final URLActionDataStore store = new URLActionDataStore(null,
				URLActionDataStore.REGEXP,
				"content",
				interpreter);
	}

	@Test(expected = IllegalArgumentException.class)
	public void wrongSetupSelectionMode()
	{
		@SuppressWarnings("unused")
		final URLActionDataStore store = new URLActionDataStore("name",
				null,
				"content",
				interpreter);
	}

	@Test(expected = IllegalArgumentException.class)
	public void wrongSetupInterpreter()
	{
		@SuppressWarnings(
		{"unused" })
		final URLActionDataStore store = new URLActionDataStore("name",
				URLActionDataStore.REGEXP,
				"content",
				null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void wrongSelectionMode()
	{
		final URLActionDataStore store = new URLActionDataStore("name",
				"bla",
				"content",
				interpreter);
		@SuppressWarnings("unused")
		final String name = store.getName();
		@SuppressWarnings("unused")
		final String mode = store.getSelectionMode();
		@SuppressWarnings("unused")
		final String content = store.getSelectionContent();
	}

	@Test(expected = IllegalArgumentException.class)
	public void wrongSubSelectionMode()
	{
		final URLActionDataStore store = new URLActionDataStore("name",
				URLActionDataStore.REGEXP,
				"content",
				"wrong Sub-Selection-Mode",
				"ssss",
				interpreter);
		@SuppressWarnings("unused")
		final String name = store.getName();
		@SuppressWarnings("unused")
		final String mode = store.getSelectionMode();
		@SuppressWarnings("unused")
		final String content = store.getSelectionContent();
		@SuppressWarnings("unused")
		final String subSelectionMode = store.getSubSelectionMode();
		@SuppressWarnings("unused")
		final String subSelectionContent = store.getSubSelectionContent();
	}

	@Test
	public void rightSubSelectionModeSetup()
	{
		final String name = "name";
		final String selectionMode = URLActionDataStore.REGEXP;
		final String selectionContent = "blabla";
		final String subSelectionMode = URLActionDataStore.REGEXGROUP;
		final String subSelectionContent = "blablablabla";


		final URLActionDataStore store = new URLActionDataStore(name,
				selectionMode,
				selectionContent,
				subSelectionMode,
				subSelectionContent,
				interpreter);
		Assert.assertEquals(store.getName(), name);
		Assert.assertEquals(store.getSelectionContent(), selectionContent);
		Assert.assertEquals(store.getSelectionMode(), selectionMode);
		Assert.assertEquals(store.getSubSelectionMode(), subSelectionMode);
		Assert.assertEquals(store.getSubSelectionContent(), subSelectionContent);
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
    	   	
    	URLActionDataStore store = new URLActionDataStore("hi",
    			parNameSMode, parName, 
    			parNameSubSMode, parName, 
    			interpreter);
    	
    	// test the normal methods
    	Assert.assertEquals(valueSMode, store.getSelectionMode());
    	Assert.assertEquals(value, store.getSelectionContent());
    	
    	Assert.assertEquals(valueSubSMode, store.getSubSelectionMode());
    	Assert.assertEquals(value, store.getSubSelectionContent());
    	   	
    	// test the raw methods
    	Assert.assertEquals(parName, store.getRawSelectionContent());
    	
    	Assert.assertEquals(parName, store.getRawSubSelectionContent());
    }
}
