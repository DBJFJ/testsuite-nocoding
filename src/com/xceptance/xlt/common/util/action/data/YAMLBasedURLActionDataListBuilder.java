package com.xceptance.xlt.common.util.action.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import bsh.EvalError;

import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.xceptance.xlt.api.util.XltLogger;
import com.xceptance.xlt.common.util.ParameterUtils;
import com.xceptance.xlt.common.util.ParameterUtils.Reason;
import com.xceptance.xlt.common.util.bsh.ParameterInterpreter;

/**
 * Implementation of the {@link URLActionDataListBuilder} for files of type 'YAML'.
 * <ul>
 * <li>Takes a file of type yaml and build a List<{@link #URLActionData}> from it.
 * <li>The syntax of the file must follow the yaml 1.1 specification.
 * <li>The structure of the data is determined within this class and described in syntax.yml
 * <li>The names of the tags, whose values should be parsed into a URLActionData are also determined here.
 * <li>Since the used yaml parser ({@link #Yaml SnakeYaml})returns a monstrous {@link #HashMap}, this class is quite
 * busy with slaughtering this HashMap in small tasty pieces, doing some nasty type checking and converting, as well
 * as error handling. Therefore the structure and quality of the code is not very charming, but it works.
 * <li>If you want to change the names of the tags, you can do it easily.
 * <li>If you want to change the general structure of the data, you better write a new Builder and think about SRP
 * (Single responsibility principle).
 * </ul>
 * 
 * @author matthias mitterreiter
 */
public class YAMLBasedURLActionDataListBuilder extends URLActionDataListBuilder
{

    protected URLActionDataValidationBuilder validationBuilder;

    protected URLActionDataStoreBuilder storeBuilder;

    /*
     * The Following are the allowed syntactic tags. See "syntax.yml" for the structure.
     */
    private static final String ACTION = "Action";

    private static final String REQUEST = "Request";

    private static final String RESPONSE = "Response";

    private static final String BODY = "Body";

    private static final String STORE = "Store";

    private static final String SUBREQUESTS = "Subrequests";

    private static final String NAME = "Name";

    private static final String URL = "Url";

    private static final String METHOD = "Method";

    private static final String ENCODEPARAMETERS = "Encode-Parameters";

    private static final String ENCODEBODY = "Encode-Body";

    private static final String XHR = "Xhr";

    private static final String PARAMETERS = "Parameters";

    private static final String HTTPCODE = "Httpcode";

    private static final String VALIDATION = "Validate";

    private static final String STATIC = "Static";

    private static final String COOKIES = "Cookies";

    private static final String HEADERS = "Headers";

    private static final String DELETE = "Delete";

    /**
     * Default static URLs
     */
    private List<String> d_static = new ArrayList<String>();

    /**
     * @param filePath
     *            : path to the yaml file.
     * @param interpreter
     *            : {@link ParameterInterpreter}
     * @param actionBuilder
     *            : {@link URLActionDataBuilder}
     * @param validationBuilder
     *            :{@link URLActionDataValidationBuilder }
     * @param storeBuilder
     *            : {@link URLActionDataStoreBuilder }
     */
    public YAMLBasedURLActionDataListBuilder(final String filePath,
                                             final ParameterInterpreter interpreter,
                                             final URLActionDataBuilder actionBuilder,
                                             final URLActionDataValidationBuilder validationBuilder,
                                             final URLActionDataStoreBuilder storeBuilder)
    {
        super(filePath, interpreter, actionBuilder);

        setStoreBuilder(storeBuilder);
        setValidationBuilder(validationBuilder);

        XltLogger.runTimeLogger.debug("Creating new Instance");
    }

    private void setStoreBuilder(final URLActionDataStoreBuilder storeBuilder)
    {
        ParameterUtils.isNotNull(storeBuilder, "URLActionStoreBuilder");
        this.storeBuilder = storeBuilder;
    }

    private void setValidationBuilder(final URLActionDataValidationBuilder validationBuilder)
    {
        ParameterUtils.isNotNull(validationBuilder, "URLActionStoreBuilder");
        this.validationBuilder = validationBuilder;
    }

    /**
     * For debugging purpose. <br>
     * 'err-streams' the attributes of the object. <br>
     */
    public void outline()
    {
        System.err.println("YAMLBasedURLActionListBuilder");
        if (!d_static.isEmpty())
        {
            System.err.println("Static");
            for (final String s : d_static)
            {
                System.err.println("\t" + s);
            }
        }
        this.actionBuilder.outline();

        if (!actions.isEmpty())
        {
            System.err.println("URLAction actions:");
            for (final URLActionData action : actions)
            {
                action.outline();
            }
        }
    }

    /**
     * Parses the data of the yaml file into List<{@link #URLActionData}>.
     * 
     * @return List<{@link #URLActionData}>
     */
    public List<URLActionData> buildURLActionDataList()
    {
        final List<Object> dataList = loadDataFromFile();
        createActionList(dataList);
                
      try {
    	  	dumpActionsYaml(actions, new File("/home/daniel/Desktop/dump.yml"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return this.actions;
    }

	@SuppressWarnings("unchecked")
    private List<Object> loadDataFromFile()
    {
        List<Object> resultList = Collections.emptyList();
        try
        {
            final Object o = loadParsedYamlObject();
            if (o != null)
            {
                ParameterUtils.isArrayListMessage(o,
                                                  "YAML-Data",
                                                  "See the no-coding syntax sepecification!");
                resultList = (List<Object>) o;
                XltLogger.runTimeLogger.info(MessageFormat.format("Loading YAML data from file: \"{0}\" ",
                                                                  this.filePath));
            }
            else
            {
                XltLogger.runTimeLogger.warn(MessageFormat.format("Empty file: \"{0}\" ",
                                                                  this.filePath));
            }
        }
        catch (final FileNotFoundException e)
        {
            final String message = MessageFormat.format("File: \"{0}\" not found!",
                                                        this.filePath);
            XltLogger.runTimeLogger.warn(message);
            throw new IllegalArgumentException(message + ": " + e.getMessage());
        }
        return resultList;
    }

    @Nullable
    private Object loadParsedYamlObject() throws FileNotFoundException
    {
        final InputStream input = new FileInputStream(new File(this.filePath));
        final Yaml yaml = new Yaml();
        return yaml.load(input);
    }

    @SuppressWarnings("unchecked")
    private void createActionList(final List<Object> dataList)
    {
        XltLogger.runTimeLogger.info("Start building URLAction list");
        for (final Object listObject : dataList)
        {
            ParameterUtils.isLinkedHashMapMessage(listObject,
                                                  "YAML - List",
                                                  SEESPEC);            
            final LinkedHashMap<String, Object> listItem = (LinkedHashMap<String, Object>) listObject;
            handleListItem(listItem);
        }
        XltLogger.runTimeLogger.info("Finished building URLAction list");
    }

    private void handleListItem(final LinkedHashMap<String, Object> listItem)
    {
        final String tagName = determineTagName(listItem);

        XltLogger.runTimeLogger.debug("Handling tag: " + tagName);

        switch (tagName)
        {
            case ACTION:
                handleActionListItem(listItem);
                break;
            case NAME:
                setDefaultName(listItem);
                break;
            case BODY:
                setDefaultBody(listItem);
                break;
            case HTTPCODE:
                setDefaultHttpCode(listItem);
                break;
            case URL:
                setDefaultUrl(listItem);
                break;
            case METHOD:
                setDefaultMethod(listItem);
                break;
            case ENCODEPARAMETERS:
                setDefaultEncodeParameters(listItem);
                break;
            case ENCODEBODY:
                setDefaultEncodeBody(listItem);
                break;
            case XHR:
                setDefaultXhr(listItem);
                break;
            case PARAMETERS:
                setDefaultParameters(listItem);
                break;
            case COOKIES:
                setDefaultCookies(listItem);
                break;
            case STATIC:
                setDefaultStatic(listItem);
                break;
            case HEADERS:
                setDefaultHeaders(listItem);
                break;
            case STORE:
                setDynamicStoreVariables(listItem);
                break;
            default:
                XltLogger.runTimeLogger.warn(MessageFormat.format("Ignoring invalid list item : \"{0}\"",
                                                                  tagName));
        }
    }

    private void setDefaultName(final LinkedHashMap<String, Object> nameItem)
    {
        final Object nameObject = nameItem.get(NAME);
        ParameterUtils.isString(nameObject, NAME);
        final String name = (String) nameObject;
        if (name.equals(DELETE))
        {
            actionBuilder.setDefaultName(null);
        }
        else
        {
            actionBuilder.setDefaultName(name);
        }
    }

    private void setDefaultBody(final LinkedHashMap<String, Object> bodyItem)
    {
        final Object bodyObject = bodyItem.get(BODY);
        ParameterUtils.isString(bodyObject, BODY);
        final String body = (String) bodyObject;
        if (body.equals(DELETE))
        {
            actionBuilder.setDefaultBody(null);
        }
        else
        {
            actionBuilder.setDefaultBody(body);
        }
    }

    private void setDefaultHttpCode(final LinkedHashMap<String, Object> codeItem)
    {
        final Object codeObject = codeItem.get(HTTPCODE);
        if (codeObject instanceof Integer)
        {
            final Integer code = (Integer) codeObject;
            actionBuilder.setDefaultHttpResponceCode(code.toString());
        }
        else if (codeObject instanceof String)
        {
            final String code = (String) codeObject;
            if (code.equals(DELETE))
            {
                actionBuilder.setDefaultHttpResponceCode(null);
            }
            else
            {
                actionBuilder.setDefaultHttpResponceCode(code);
            }
        }
        else
        {
            ParameterUtils.doThrow(HTTPCODE, Reason.UNSUPPORTED_TYPE);
        }
    }

    private void setDefaultUrl(final LinkedHashMap<String, Object> urlItem)
    {
        final Object urlObject = urlItem.get(URL);
        ParameterUtils.isString(urlObject, URL);
        final String url = (String) urlObject;
        if (url.equals(DELETE))
        {
            actionBuilder.setDefaultUrl(null);
        }
        else
        {
            actionBuilder.setDefaultUrl(url);
        }
    }

    private void setDefaultMethod(final LinkedHashMap<String, Object> methodItem)
    {
        final Object methodObject = methodItem.get(METHOD);
        ParameterUtils.isString(methodObject, METHOD);
        final String method = (String) methodObject;
        if (method.equals(DELETE))
        {
            actionBuilder.setDefaultMethod(null);
        }
        else
        {
            actionBuilder.setDefaultMethod(method);
        }
    }

    private void setDefaultEncodeParameters(final LinkedHashMap<String, Object> encodedItem)
    {
        final Object encodedObject = encodedItem.get(ENCODEPARAMETERS);
        if (encodedObject instanceof Boolean)
        {
            final Boolean encoded = (Boolean) encodedObject;
            actionBuilder.setDefaultEncodeParameters(encoded.toString());
        }
        else if (encodedObject instanceof String)
        {
            final String encoded = (String) encodedObject;
            if (encoded.equals(DELETE))
            {
                actionBuilder.setDefaultEncodeParameters(null);
            }
            else
            {
                actionBuilder.setDefaultEncodeParameters(encoded);
            }
        }
        else
        {
            ParameterUtils.doThrow(ENCODEPARAMETERS, Reason.UNSUPPORTED_TYPE);
        }
    }

    private void setDefaultEncodeBody(final LinkedHashMap<String, Object> encodedItem)
    {
        final Object encodedObject = encodedItem.get(ENCODEBODY);
        if (encodedObject instanceof Boolean)
        {
            final Boolean encoded = (Boolean) encodedObject;
            actionBuilder.setDefaultEncodeBody(encoded.toString());
        }
        else if (encodedObject instanceof String)
        {
            final String encoded = (String) encodedObject;
            if (encoded.equals(DELETE))
            {
                actionBuilder.setDefaultEncodeBody(null);
            }
            else
            {
                actionBuilder.setDefaultEncodeBody(encoded);
            }
        }
        else
        {
            ParameterUtils.doThrow(ENCODEBODY, Reason.UNSUPPORTED_TYPE);
        }
    }

    private void setDefaultXhr(final LinkedHashMap<String, Object> xhrItem)
    {
        final Object xhrObject = xhrItem.get(XHR);
        if (xhrObject instanceof Boolean)
        {
            final Boolean xhr = (Boolean) xhrObject;
            if (xhr)
            {
                actionBuilder.setDefaultType(URLActionData.TYPE_XHR);
            }
            else
            {
                actionBuilder.setDefaultType(URLActionData.TYPE_ACTION);
            }
        }
        else if (xhrObject instanceof String)
        {
            final String xhr = (String) xhrObject;
            if (xhr.equals(DELETE))
            {
                actionBuilder.setDefaultType(null);
            }
            else
            {
                actionBuilder.setDefaultType(xhr);
            }
        }
        else
        {
            ParameterUtils.doThrow(XHR, Reason.UNSUPPORTED_TYPE);
        }
    }

    private void setDefaultParameters(final LinkedHashMap<String, Object> parametersItem)
    {
        final Object parametersObject = parametersItem.get(PARAMETERS);
        if (parametersObject instanceof String)
        {
            final String parameters = (String) parametersObject;
            if (parameters.equals(DELETE))
            {
                actionBuilder.setDefaultParameters(Collections.<NameValuePair> emptyList());
            }
            else
            {
                ParameterUtils.doThrow(PARAMETERS,
                                       parameters,
                                       Reason.UNSUPPORTED_VALUE);
            }
        }
        else if (parametersObject instanceof ArrayList)
        {
            @SuppressWarnings({ "unchecked", "rawtypes" })
			final List<Object> objectList = (ArrayList) parametersObject;
            final List<NameValuePair> newList = new ArrayList<NameValuePair>();
            for (final Object object : objectList)
            {
                ParameterUtils.isLinkedHashMap(object, PARAMETERS);
                @SuppressWarnings("unchecked")
				final LinkedHashMap<Object, Object> lhm = (LinkedHashMap<Object, Object>) object;
                final NameValuePair nvp = createPairfromLinkedHashMap(lhm);
                newList.add(nvp);
            }
            actionBuilder.setDefaultParameters(newList);
        }
        else
        {
            ParameterUtils.doThrow(PARAMETERS, Reason.UNSUPPORTED_TYPE);
        }
    }

    private void setDefaultCookies(final LinkedHashMap<String, Object> cookiesItem)
    {
        final Object cookiesObject = cookiesItem.get(COOKIES);
        if (cookiesObject instanceof String)
        {
            final String cookies = (String) cookiesObject;
            if (cookies.equals(DELETE))
            {
                actionBuilder.setDefaultCookies(Collections.<NameValuePair> emptyList());
            }
            else
            {
                ParameterUtils.doThrow(COOKIES,
                                       cookies,
                                       Reason.UNSUPPORTED_VALUE);
            }
        }
        else if (cookiesObject instanceof ArrayList)
        {
            @SuppressWarnings({ "unchecked", "rawtypes" })
			final List<Object> objectList = (ArrayList) cookiesObject;
            final List<NameValuePair> newList = new ArrayList<NameValuePair>();
            for (final Object object : objectList)
            {
                ParameterUtils.isLinkedHashMap(object, COOKIES);
                @SuppressWarnings("unchecked")
				final LinkedHashMap<Object, Object> lhm = (LinkedHashMap<Object, Object>) object;
                final NameValuePair nvp = createPairfromLinkedHashMap(lhm);
                newList.add(nvp);
            }
            actionBuilder.setDefaultCookies(newList);
        }
        else
        {
            ParameterUtils.doThrow(COOKIES, Reason.UNSUPPORTED_TYPE);
        }
    }

    private void setDefaultHeaders(final LinkedHashMap<String, Object> headersItem)
    {
        final Object headersObject = headersItem.get(HEADERS);
        if (headersObject instanceof String)
        {
            final String headersString = (String) headersObject;
            if (headersString.equals(DELETE))
            {

                actionBuilder.setDefaultHeaders(Collections.<NameValuePair> emptyList());

            }
            else
            {
                ParameterUtils.doThrow(HEADERS,
                                       headersString,
                                       Reason.UNSUPPORTED_VALUE);
            }
        }
        else if (headersObject instanceof ArrayList)
        {
            @SuppressWarnings({ "unchecked", "rawtypes" })
			final List<Object> objectList = (ArrayList) headersObject;
            final List<NameValuePair> newList = new ArrayList<NameValuePair>();
            for (final Object object : objectList)
            {
                ParameterUtils.isLinkedHashMap(object, HEADERS);
                @SuppressWarnings("unchecked")
				final LinkedHashMap<Object, Object> lhm = (LinkedHashMap<Object, Object>) object;
                final NameValuePair nvp = createPairfromLinkedHashMap(lhm);
                newList.add(nvp);
            }
            actionBuilder.setDefaultHeaders(newList);
        }
        else
        {
            ParameterUtils.doThrow(HEADERS, Reason.UNSUPPORTED_TYPE);
        }
    }

    private void setDefaultStatic(final LinkedHashMap<String, Object> headersItem)
    {
        final Object staticObject = headersItem.get(STATIC);
        if (staticObject instanceof String)
        {
            final String staticString = (String) staticObject;
            if (staticString.equals(DELETE))
            {
                this.d_static = Collections.emptyList();
            }
            else
            {
                ParameterUtils.doThrow(STATIC,
                                       staticString,
                                       Reason.UNSUPPORTED_VALUE);
            }
        }
        else if (staticObject instanceof ArrayList)
        {
            @SuppressWarnings({ "unchecked", "rawtypes" })
			final List<Object> objectList = (ArrayList) staticObject;
            final List<String> newList = new ArrayList<String>();
            for (final Object object : objectList)
            {
                ParameterUtils.isString(object, STATIC);
                final String staticUrl = (String) object;
                newList.add(staticUrl);
            }
            this.d_static = newList;
        }
        else
        {
            ParameterUtils.doThrow(HEADERS, Reason.UNSUPPORTED_TYPE);
        }
    }

    private void setDynamicStoreVariables(final LinkedHashMap<String, Object> headersItem)
    {
        final Object storeObject = headersItem.get(STORE);
        if (storeObject instanceof String)
        {
            final String storeString = (String) storeObject;
            if (storeString.equals(DELETE))
            {
                XltLogger.runTimeLogger.warn("CANNOT DELETE DATA IN STORE (YET)");
            }
            else
            {
                ParameterUtils.doThrow(STORE,
                                       storeString,
                                       Reason.UNSUPPORTED_VALUE);
            }
        }
        else if (storeObject instanceof ArrayList)
        {
            @SuppressWarnings({ "unchecked", "rawtypes" })
			final List<Object> objectList = (ArrayList) storeObject;
            @SuppressWarnings("unused")
			final List<NameValuePair> newList = new ArrayList<NameValuePair>();
            for (final Object object : objectList)
            {
                ParameterUtils.isLinkedHashMap(object, STORE);
                @SuppressWarnings("unchecked")
				final LinkedHashMap<Object, Object> lhm = (LinkedHashMap<Object, Object>) object;
                final NameValuePair nvp = createPairfromLinkedHashMap(lhm);
                final NameValuePair nvp2 = new NameValuePair(interpreter.processDynamicData(nvp.getName()),
                                                             interpreter.processDynamicData(nvp.getValue()));
                try
                {
                    this.interpreter.set(nvp2);
                }
                catch (final EvalError e)
                {
                    // We just Set Values, so NP
                }
            }
        }
        else
        {
            ParameterUtils.doThrow(STORE, Reason.UNSUPPORTED_TYPE);
        }
    }

    private void handleActionListItem(final LinkedHashMap<String, Object> listItem)
    {
        final Object actionObject = listItem.get(ACTION);
        ParameterUtils.isNotNull(actionObject, ACTION);
        ParameterUtils.isLinkedHashMapMessage(actionObject,
                                              ACTION,
                                              "Missing Content");
        @SuppressWarnings("unchecked")
		final LinkedHashMap<String, Object> rawAction = (LinkedHashMap<String, Object>) actionObject;

        fillURLActionBuilder(rawAction);
        
        final URLActionData action = actionBuilder.build();
        this.actions.add(action);

        handleSubrequests(rawAction);

    }

    private void handleSubrequests(final LinkedHashMap<String, Object> rawAction)
    {
        final Object subrequestObject = rawAction.get(SUBREQUESTS);
        if (subrequestObject != null)
        {
            ParameterUtils.isArrayListMessage(subrequestObject, SUBREQUESTS, "");

            @SuppressWarnings("unchecked")
			final List<Object> subrequests = (List<Object>) subrequestObject;

            for (final Object subrequestItem : subrequests)
            {
                ParameterUtils.isLinkedHashMapMessage(subrequestItem,
                                                      STATIC,
                                                      "");

                @SuppressWarnings("unchecked")
				final LinkedHashMap<String, Object> subrequest = (LinkedHashMap<String, Object>) subrequestItem;
                createSubrequest(subrequest);
            }
        }
    }

    private void createSubrequest(final LinkedHashMap<String, Object> subrequest)
    {
        final Object staticSubrequestObject = subrequest.get(STATIC);
        if (staticSubrequestObject != null)
        {
            ParameterUtils.isArrayListMessage(staticSubrequestObject,
                                              SUBREQUESTS,
                                              "");
            @SuppressWarnings("unchecked")
			final List<Object> staticSubrequest = (List<Object>) staticSubrequestObject;
            handleStaticSubrequests(staticSubrequest);
        }
        else if (!d_static.isEmpty())
        {
            for (int i = 0; i < d_static.size(); i++)
            {
                actionBuilder.reset();
                actionBuilder.setUrl(d_static.get(i));
                actionBuilder.setType(URLActionData.TYPE_STATIC);
                actionBuilder.setMethod(URLActionData.METHOD_GET);
                actionBuilder.setName("static-subrequest" + i);
                actionBuilder.setInterpreter(this.interpreter);
                actions.add(actionBuilder.build());
            }
        }
        final Object xhrSubrequestObject = subrequest.get(XHR);
        if (xhrSubrequestObject != null)
        {
            ParameterUtils.isLinkedHashMapMessage(xhrSubrequestObject,
                                                  SUBREQUESTS,
                                                  "");
            @SuppressWarnings("unchecked")
			final LinkedHashMap<String, Object> xhrSubrequest = (LinkedHashMap<String, Object>) xhrSubrequestObject;
            handleXhrSubrequests(xhrSubrequest);
        }
    }

    private void handleXhrSubrequests(final LinkedHashMap<String, Object> xhrSubrequest)
    {
        fillURLActionBuilder(xhrSubrequest);
        actionBuilder.setType(URLActionData.TYPE_XHR);
        final URLActionData xhrAction = actionBuilder.build();
        actions.add(xhrAction);
        handleSubrequests(xhrSubrequest);
    }

    private void handleStaticSubrequests(final List<Object> staticUrls)
    {
        for (int i = 0; i < staticUrls.size(); i++)
        {
            final Object o = staticUrls.get(i);
            ParameterUtils.isStringMessage(o, STATIC, "");

            final String urlString = (String) o;

            actionBuilder.reset();
            actionBuilder.setType(URLActionData.TYPE_STATIC);
            actionBuilder.setMethod(URLActionData.METHOD_GET);
            actionBuilder.setUrl(urlString);
            actionBuilder.setName("static-subrequest" + i);
            actionBuilder.setInterpreter(this.interpreter);
            actions.add(actionBuilder.build());
        }
    }

    private void fillURLActionBuilder(final LinkedHashMap<String, Object> rawAction)
    {
        actionBuilder.reset();

        actionBuilder.setInterpreter(this.interpreter);
        fillUrlActionBuilderWithName(rawAction);
        fillUrlActionBuilderWithRequestData(rawAction);
        fillUrlActionBuilderWithResponseData(rawAction);
    }

    private void fillUrlActionBuilderWithName(final LinkedHashMap<String, Object> rawAction)
    {
        final Object nameObject = rawAction.get(NAME);
        if (nameObject != null)
        {
            ParameterUtils.isString(nameObject, NAME);
            final String name = (String) nameObject;
            actionBuilder.setName(name);
        }

    }

    private void fillUrlActionBuilderWithRequestData(final LinkedHashMap<String, Object> rawAction)
    {
        final Object requestObject = rawAction.get(REQUEST);
        if (requestObject != null)
        {
            ParameterUtils.isLinkedHashMapMessage(requestObject, REQUEST, "");
            @SuppressWarnings("unchecked")
			final LinkedHashMap<String, Object> rawRequest = (LinkedHashMap<String, Object>) requestObject;

            fillURLActionBuilderWithBodyData(rawRequest);
            fillURLActionBuilderWithHeaderData(rawRequest);
            fillURLActionBuilderWithEncodeParametersData(rawRequest);
            fillURLActionBuilderWithEncodeBodyData(rawRequest);
            fillURLActionBuilderWithMethodData(rawRequest);
            fillURLActionBuilderWithParameterData(rawRequest);
            fillURLActionBuilderWithCookieData(rawRequest);
            fillURLActionBuilderWithXhrData(rawRequest);
            fillURLActionBuilderWithUrlData(rawRequest);
        }
    }

    private void fillURLActionBuilderWithBodyData(final LinkedHashMap<String, Object> rawRequest)
    {
        final Object bodyObject = rawRequest.get(BODY);
        if (bodyObject != null)
        {
            ParameterUtils.isString(bodyObject, BODY);
            final String body = (String) bodyObject;
            actionBuilder.setBody(body);
        }
    }

    private void fillURLActionBuilderWithHeaderData(final LinkedHashMap<String, Object> rawRequest)
    {
        final Object headersObject = rawRequest.get(HEADERS);
        if (headersObject != null)
        {
            if (headersObject instanceof ArrayList)
            {
                @SuppressWarnings({ "unchecked", "rawtypes" })
				final List<Object> objectList = (ArrayList) headersObject;

                final List<NameValuePair> newList = new ArrayList<NameValuePair>();

                for (final Object object : objectList)
                {
                    ParameterUtils.isLinkedHashMap(object, HEADERS);
                    @SuppressWarnings("unchecked")
					final LinkedHashMap<Object, Object> lhm = (LinkedHashMap<Object, Object>) object;
                    final NameValuePair nvp = createPairfromLinkedHashMap(lhm);
                    newList.add(nvp);
                }
                actionBuilder.setHeaders(newList);
            }
            else
            {
                ParameterUtils.doThrow(HEADERS, Reason.UNSUPPORTED_TYPE);
            }
        }
    }

    private void fillURLActionBuilderWithParameterData(final LinkedHashMap<String, Object> rawRequest)
    {
        final Object parametersObject = rawRequest.get(PARAMETERS);
        if (parametersObject != null)
        {
            if (parametersObject instanceof ArrayList)
            {
                @SuppressWarnings({ "unchecked", "rawtypes" })
				final List<Object> objectList = (ArrayList) parametersObject;
                final List<NameValuePair> newList = new ArrayList<NameValuePair>();
                for (final Object object : objectList)
                {
                    ParameterUtils.isLinkedHashMap(object, PARAMETERS);
                    @SuppressWarnings("unchecked")
					final LinkedHashMap<Object, Object> lhm = (LinkedHashMap<Object, Object>) object;
                    final NameValuePair nvp = createPairfromLinkedHashMap(lhm);
                    newList.add(nvp);
                }
                actionBuilder.setParameters(newList);
            }
            else
            {
                ParameterUtils.doThrow(PARAMETERS, Reason.UNSUPPORTED_TYPE);
            }
        }
    }

    private void fillURLActionBuilderWithCookieData(final LinkedHashMap<String, Object> rawRequest)
    {
        final Object cookiesObject = rawRequest.get(COOKIES);
        if (cookiesObject != null)
        {
            if (cookiesObject instanceof ArrayList)
            {
                @SuppressWarnings({ "rawtypes", "unchecked" })
				final List<Object> objectList = (ArrayList) cookiesObject;
                final List<NameValuePair> newList = new ArrayList<NameValuePair>();
                for (final Object object : objectList)
                {
                    ParameterUtils.isLinkedHashMap(object, COOKIES);
                    @SuppressWarnings("unchecked")
					final LinkedHashMap<Object, Object> lhm = (LinkedHashMap<Object, Object>) object;
                    final NameValuePair nvp = createPairfromLinkedHashMap(lhm);
                    newList.add(nvp);
                }
                actionBuilder.setCookies(newList);
            }
            else
            {
                ParameterUtils.doThrow(COOKIES, Reason.UNSUPPORTED_TYPE);
            }
        }
    }

    private void fillURLActionBuilderWithXhrData(final LinkedHashMap<String, Object> rawRequest)
    {
        final Object xhrObject = rawRequest.get(XHR);
        if (xhrObject != null)
        {
            if (xhrObject instanceof Boolean)
            {
                final Boolean xhr = (Boolean) xhrObject;
                if (xhr)
                {
                    actionBuilder.setType(URLActionData.TYPE_XHR);
                }
                else
                {
                    actionBuilder.setType(URLActionData.TYPE_ACTION);
                }
            }
            else if (xhrObject instanceof String)
            {
                final String xhr = (String) xhrObject;

                actionBuilder.setType(xhr);
            }
            else
            {
                ParameterUtils.doThrow(XHR, Reason.UNSUPPORTED_TYPE);
            }
        }
    }

    private void fillURLActionBuilderWithMethodData(final LinkedHashMap<String, Object> rawRequest)
    {
        final Object methodObject = rawRequest.get(METHOD);
        if (methodObject != null)
        {
            ParameterUtils.isString(methodObject, METHOD);
            final String method = (String) methodObject;
            actionBuilder.setMethod(method);
        }
    }

    private void fillURLActionBuilderWithUrlData(final LinkedHashMap<String, Object> rawRequest)
    {
        final Object urlObject = rawRequest.get(URL);
        if (urlObject != null)
        {
            ParameterUtils.isString(urlObject, URL);
            final String url = (String) urlObject;
            actionBuilder.setUrl(url);
        }
    }

    private void fillURLActionBuilderWithEncodeParametersData(final LinkedHashMap<String, Object> rawRequest)
    {
        final Object encodedObject = rawRequest.get(ENCODEPARAMETERS);
        if (encodedObject != null)
        {
            if (encodedObject instanceof Boolean)
            {
                final Boolean encoded = (Boolean) encodedObject;
                actionBuilder.setEncodeParameters(encoded.toString());
            }
            else if (encodedObject instanceof String)
            {
                final String encoded = (String) encodedObject;
                actionBuilder.setEncodeParameters(encoded);
            }
            else
            {
                ParameterUtils.doThrow(ENCODEPARAMETERS,
                                       Reason.UNSUPPORTED_TYPE);
            }
        }
    }

    private void fillURLActionBuilderWithEncodeBodyData(final LinkedHashMap<String, Object> rawRequest)
    {
        final Object encodedObject = rawRequest.get(ENCODEBODY);
        if (encodedObject != null)
        {
            if (encodedObject instanceof Boolean)
            {
                final Boolean encoded = (Boolean) encodedObject;
                actionBuilder.setEncodeBody(encoded.toString());
            }
            else if (encodedObject instanceof String)
            {
                final String encoded = (String) encodedObject;
                actionBuilder.setEncodeBody(encoded);
            }
            else
            {
                ParameterUtils.doThrow(ENCODEBODY, Reason.UNSUPPORTED_TYPE);
            }
        }
    }

    private void fillUrlActionBuilderWithResponseData(final LinkedHashMap<String, Object> rawAction)
    {
        final Object responseObject = rawAction.get(RESPONSE);
        if (responseObject != null)
        {
            ParameterUtils.isLinkedHashMapMessage(responseObject, RESPONSE, "");
            @SuppressWarnings("unchecked")
			final LinkedHashMap<String, Object> rawResponse = (LinkedHashMap<String, Object>) responseObject;

            fillURLActionBuilderWithHttpResponseCodeData(rawResponse);
            fillURLActionBuilderWithValidationData(rawResponse);
            fillURLActionBuilderWithStoreData(rawResponse);
        }
    }

    private void fillURLActionBuilderWithHttpResponseCodeData(final LinkedHashMap<String, Object> rawResponse)
    {
        final Object codeObject = rawResponse.get(HTTPCODE);
        if (codeObject != null)
        {
            if (codeObject instanceof Integer)
            {
                final Integer code = (Integer) codeObject;
                actionBuilder.setHttpResponceCode(code.toString());
            }
            else if (codeObject instanceof String)
            {
                final String code = (String) codeObject;
                actionBuilder.setHttpResponceCode(code);
            }
            else
            {
                ParameterUtils.doThrow(HTTPCODE, Reason.UNSUPPORTED_TYPE);
            }
        }
    }

    private void fillURLActionBuilderWithValidationData(final LinkedHashMap<String, Object> rawResponse)
    {
        final Object validationsObject = rawResponse.get(VALIDATION);
        if (validationsObject != null)
        {
            ParameterUtils.isArrayListMessage(validationsObject, VALIDATION, "");
            @SuppressWarnings("unchecked")
			final List<Object> validations = (List<Object>) validationsObject;
            for (final Object validationObject : validations)
            {
                ParameterUtils.isLinkedHashMapMessage(validationObject,
                                                      VALIDATION,
                                                      "");
                @SuppressWarnings("unchecked")
				final LinkedHashMap<String, Object> validationItem = (LinkedHashMap<String, Object>) validationObject;
                fillURLActionValidationBuilder(validationItem);
                final URLActionDataValidation validation = validationBuilder.build();
                actionBuilder.addValidation(validation);
            }
        }
    }

    private void fillURLActionValidationBuilder(final LinkedHashMap<String, Object> rawValidationItem)
    {
        validationBuilder.reset();

        final String validationName = getNameOfFirstElementFromLinkedHashMap(rawValidationItem);

        validationBuilder.setName(validationName);
        validationBuilder.setInterpreter(this.interpreter);

        final Object rawValidateSubObject = rawValidationItem.get(validationName);
        if (rawValidateSubObject != null)
        {
            ParameterUtils.isLinkedHashMapMessage(rawValidateSubObject,
                                                  validationName,
                                                  "");
            @SuppressWarnings("unchecked")
			final LinkedHashMap<String, Object> validateListSubItem = (LinkedHashMap<String, Object>) rawValidateSubObject;
            fillURLActionValidationBuilderWithDataFromLinkedHashMap(validateListSubItem);
        }
        else
        {
            ParameterUtils.doThrow(VALIDATION,
                                   validationName,
                                   Reason.UNCOMPLETE);
        }
    }

    private void fillURLActionValidationBuilderWithDataFromLinkedHashMap(final LinkedHashMap<String, Object> rawValidateSubItem)
    {
        final Set<?> entrySet = rawValidateSubItem.entrySet();
        final Iterator<?> it = entrySet.iterator();
        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) it.next();
        final String selectionMode = (String) entry.getKey();
        final String selectionContent = (String) entry.getValue();
        String validationMode = null;
        String validationContent = null;
        String subSelectionMode = null;
        String subSelectionContent = null;
        
        // deal with the subSelectionMode if it exists 
        while (it.hasNext())
        {
            entry = (Map.Entry<?, ?>) it.next();
            String key = entry.getKey().toString();
            if (URLActionDataValidation.PERMITTEDVALIDATIONMODE.contains(key)) {
            	validationMode = key;
                validationContent = entry.getValue().toString();
            }
            else {
            	subSelectionMode = key;
            	subSelectionContent = entry.getValue().toString();
            }
        }
        
        if (validationMode == null)
        {
            validationMode = URLActionDataValidation.EXISTS;
        }
        validationBuilder.setSelectionMode(selectionMode);
        validationBuilder.setValidationContent(validationContent);
        validationBuilder.setValidationMode(validationMode);
        validationBuilder.setSelectionContent(selectionContent);
        
        if(subSelectionMode != null)
        {
        	validationBuilder.setSubSelectionMode(subSelectionMode);
        	validationBuilder.setSubSelectionContent(subSelectionContent);
        }
        
    }

    private void fillURLActionBuilderWithStoreData(final LinkedHashMap<String, Object> rawResponse)
    {
        final Object storeObject = rawResponse.get(STORE);
        if (storeObject != null)
        {
            ParameterUtils.isArrayListMessage(storeObject, STORE, "");
            @SuppressWarnings("unchecked")
			final List<Object> storeObjects = (List<Object>) storeObject;
            for (final Object storeObjectsItem : storeObjects)
            {
                ParameterUtils.isLinkedHashMapMessage(storeObjectsItem,
                                                      STORE,
                                                      "");
                @SuppressWarnings("unchecked")
				final LinkedHashMap<String, Object> storeItem = (LinkedHashMap<String, Object>) storeObjectsItem;

                fillURLActionStoreBuilder(storeItem);

                final URLActionDataStore store = storeBuilder.build();
                actionBuilder.addStore(store);
            }
        }
    }

    private void fillURLActionStoreBuilder(final LinkedHashMap<String, Object> storeItem)
    {
        storeBuilder.reset();

        final String storeName = getNameOfFirstElementFromLinkedHashMap(storeItem);

        storeBuilder.setName(storeName);
        storeBuilder.setInterpreter(interpreter);

        final Object rawStoreSubObject = storeItem.get(storeName);
        if (rawStoreSubObject != null)
        {
            ParameterUtils.isLinkedHashMapMessage(rawStoreSubObject,
                                                  storeName,
                                                  "");
            @SuppressWarnings("unchecked")
			final LinkedHashMap<Object, Object> rawStoreSubItem = (LinkedHashMap<Object, Object>) rawStoreSubObject;
            fillStoreBuilderWithDataFromLinkedHashMap(rawStoreSubItem);
        }
        else
        {
            ParameterUtils.doThrow(STORE, storeName, Reason.UNCOMPLETE);
        }

    }

    private void fillStoreBuilderWithDataFromLinkedHashMap(final LinkedHashMap<Object, Object> rawStoreSubItem)
    {
    	final Set<?> entrySet = rawStoreSubItem.entrySet();
        final Iterator<?> it = entrySet.iterator();
        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) it.next();
        final String selectionMode = (String) entry.getKey();
        final String selectionContent = (String) entry.getValue();
        String subSelectionMode = null;
        String subSelectionContent = null;

        if (it.hasNext())
        {
            entry = (Map.Entry<?, ?>) it.next();
            subSelectionMode = entry.getKey().toString();
            subSelectionContent = entry.getValue().toString();
        }
        storeBuilder.setSelectionMode(selectionMode);
        storeBuilder.setSelectionContent(selectionContent);
        
        if(subSelectionMode != null)
        {
        	storeBuilder.setSubSelectionMode(subSelectionMode);
        	storeBuilder.setSubSelectionContent(subSelectionContent);
        }
    }

    private String determineTagName(final LinkedHashMap<String, Object> tag)
    {
        return getNameOfFirstElementFromLinkedHashMap(tag);
    }

    private NameValuePair createPairfromLinkedHashMap(final LinkedHashMap<Object, Object> lhm)
    {
        final Set<?> entrySet = lhm.entrySet();
        final Iterator<?> it = entrySet.iterator();
        final Map.Entry<?, ?> entry = (Map.Entry<?, ?>) it.next();
        final String key = entry.getKey() != null ? entry.getKey().toString()
                                                 : null;
        final String value = entry.getValue() != null ? entry.getValue()
                                                             .toString() : null;
        final NameValuePair nvp = new NameValuePair(key, value);
        return nvp;
    }

    private String getNameOfFirstElementFromLinkedHashMap(final LinkedHashMap<String, Object> lhm)
    {
        final Set<?> entrySet = lhm.entrySet();
        final Iterator<?> it = entrySet.iterator();
        final Map.Entry<?, ?> entry = (Map.Entry<?, ?>) it.next();
        final String name = entry.getKey().toString();
        return name;
    }

    static private final String SPECIFICATION = "YAMLSyntaxSpecification.txt";

    static private final String SEESPEC = "See " + SPECIFICATION
                                          + " for the correct Syntax!";
    
	/**
	 * Dump the given list of {@link URLActionData) into the given file in yaml.
	 * Uses snakeyaml ({@link #Yaml SnakeYaml}) for the dumping, and {@link #restructureActionListForDumping(List)} 
	 * to bring the data into a format snakeyaml can use. <\br>
	 * <\br>
	 * Since the method is used in the {@link JMXBasedURLActionDataListBuilder} it doesn't support attributes 
	 * that arn't supported in the {@link JMXBasedURLActionDataListBuilder}. The formatting doesn't quite match
	 * the suggested format either. Attributes are included even if they are empty or null, the indention is odd 
	 * at times especially with dashes and there's no whitespace in front of colons.
	 *  
	 * @param file 
	 * @param actions 
	 */
	protected static void dumpActionsYaml(List<URLActionData> actions, File dumpThere) throws FileNotFoundException {
		
		PrintWriter printwriter = new PrintWriter(dumpThere);
	
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
	 * @param actions 
	 *  
	 */
	private static List<Map<String, Object>> restructureActionListForDumping(List<URLActionData> actions) {
		
		List<Map<String, Object>> root = new ArrayList<>();
		
		// get the variables that should be stored seperately
		// how access interpreter ...?
		
		// iterate over the actions
		for (int i = 0; i < actions.size(); i++) {
			URLActionData action = actions.get(i);
			Map<String, Object> outerActionMap = new LinkedHashMap<String, Object>();
			Map<String, Object> innerActionMap = new LinkedHashMap<String, Object>();
			innerActionMap.put(NAME, action.getName());
			
			Map<String, Object> requestMap = new LinkedHashMap<String, Object>();
			requestMap.put(URL, action.getUrlString());
			requestMap.put(METHOD, action.getMethod().toString());
			innerActionMap.put(REQUEST, requestMap);
			
			Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
			responseMap.put(HTTPCODE, action.getHttpResponseCode());
			
			// Validations ...			
			List<Map<String, Object>> validationsList = new ArrayList<>();
			
			for (URLActionDataValidation validation : action.getValidations()) {
				Map<String, Object> outerValidationMap = new LinkedHashMap<String, Object>();
				Map<String, Object> innerValidationMap = new LinkedHashMap<String, Object>();
				
				innerValidationMap.put(validation.getSelectionMode(), validation.getSelectionContent());
				innerValidationMap.put(validation.getSubSelectionMode(), validation.getSubSelectionContent());
				innerValidationMap.put(validation.getValidationMode(), validation.getValidationContent());
				
				outerValidationMap.put(validation.getName(), innerValidationMap);
				validationsList.add(outerValidationMap);
			}
			
			responseMap.put(VALIDATION, validationsList);			
			innerActionMap.put(RESPONSE, responseMap);
			
			// Store/ Extractions are performed analogous to validations
			List<Map<String, Object>> storeVarsList = new ArrayList<>();
			
			for (URLActionDataStore store : action.getStore()) {
				Map<String, Object> outerStoreMap = new LinkedHashMap<String, Object>();
				Map<String, Object> innerStoreMap = new LinkedHashMap<String, Object>();
				
				innerStoreMap.put(store.getSelectionMode(), store.getSelectionContent());
				innerStoreMap.put(store.getSubSelectionMode(), store.getSubSelectionContent());
				
				outerStoreMap.put(store.getName(), innerStoreMap);
				storeVarsList.add(outerStoreMap);
			}
			
			responseMap.put(STORE, storeVarsList);			
			innerActionMap.put(RESPONSE, responseMap);

			outerActionMap.put(ACTION, innerActionMap);
			root.add(outerActionMap);
		}
		
		return root; 
	}
}
