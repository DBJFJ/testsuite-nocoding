# Jmeter --> TSNC Translation

TSNC can recognize test files created with Jmeter and execute them.

While TSNC and Jmeter are both test tools, they concentrate on different areas and work in different ways. TSNC is a small test tool focused on calling urls, validating results and showing the results. Jmeter is a large load testing tool with a much wider set of functions. As a result, not all Jmeter tests can be translated to TSNC.

Load Test configurations and Listeners will be ignored since TSNC uses it's own properties and result browsers.

### Generally speaking, TSNC will translate tests following this structure: 

* Thread Group
* * (Variable Declaration)
* * HTTP Request Sampler
* * * XPath Extractions
* * * Response Assertions
* * (Variable Declaration)
* * HTTP Request Sampler
* * * XPath Extractions
* * * Response Assertions 
 
If the Jmeter test does not follow this structure and/ or includes other important elements it won't be translated correctly. 
Most notably logic elements like if or loops won't be translated. Extractors other then XPath Extractors won't be translated. Assertions other then Response Assertions won't be translated. Requests/ Samplers other then Http Requests won't be translated. Beanshell and JDBC connections won't be translated. There are minor differences in the Response Assertions/ the XPath Extractions.
Explanations and details for specific elements can be found below.
 
#### Config Elements

TSNC has it's own Load Test properties, so Jmeters Load Test properties are not translated. Even aside from that Jmeter features many configuration options that are not applicable or which have no counterpart in TSNC. There are also a small number of configuration options where a translation has not been implemented yet, for example default parameters.
The default protocol (http/ https) and the default headers from Jmeter are translated.

#### Variables

Variables are read with name and value as usual.Variables can be defined inside the Thread Group or inside an Http Request (not in an XPath Extractor). They can be overwritten. 
Jmeter variables are made to hold practically everything. TSNC variables are only made to hold Strings. 

#### Http Requests

Http Request Samplers are translated into TSNC actions and executed with name, protocol, url, method and parameters. 

### Extractions and Assertions

Extractors and Assertions in Jmeter are more strictly divided then in TSNC. Where Jmeter often extracts a variable to validate it afterwards TSNC does so in the same motion. The division between extractions and assertions isn't as clear in TSNC. As a sideeffect every TSNC extraction at least asserts that the object it is looking for exists. There is no default value like in Jmeter, the extractor will throw an error. Assertions essentially always extract and assert a something, it just so happens that they can extract everything from a variable.

###### XPath Extractions

XPath extractions are read with name and XPath. The big difference is that in TSNC, an extraction will always result in an error if it didn't find anything. That remains the case with extractions read from Jmeter. If the extractor doesn't find anything, there will be an error. There are some other comparatively small differences
* the extractor can't be used on variables
* the difference between 'Main sample and sub-samples', 'Main sample only' and 'Sub-samples only' is ignored, they are all mapped to the resulting web page
* 'Use Tidy', 'Quiet', 'Report errors' and 'Show warnings' don't work.

###### Regular Expression Extractions

While TSNC and Jmeter both use Regular Expression Extractors, they work in slightly different ways. The regular expression in Jmeter contains a set of round brackets and everything in these brackets is extracted.
For example: A regex extractor with "This is a great example(.*?)?" used on the String "This is a great example, isn't it? Yes it is." would extract ", isn't is".
A regex extractor in TSNC extracts the whole expression. Using the same values it would extract "This is a great example, isn't it?". **There is no workaround yet and so regular expression extractions can't be translated yet.** 

#### Response Assertions

Response Assertions can usually be translated to TSNC, but some functions again have no equivalent:
* the pattern matching rule 'not' can't be mapped
* the 'Ignore Status' field can't be mapped
* 'URL Sampled' isn't mapped
* both 'Text Response' and 'Document (Text)' are mapped to the response body
* the difference between 'Main sample and sub-samples', 'Main sample only' and 'Sub-samples only' is ignored, they are all mapped to the resulting web page

TSNC also adds a default HttpResponseCode=200 validation to every action, so redirections will cause errors unless an assertion for the expected response code was manually defined. 

**Assertions are mapped to Validations. (Except when asserting a Response Code.)**

The mapping for Response Assertions from Jmeter to TSNC is as such:

* 'Apply To' is mapped to 'selectionMode'
* * Main sample and sub-samples/ Main sample only/ Sub-samples only -> Regex 
* * Jmeter Variable -> Var  

* 'Response Field to Test' is also mapped to 'selectionMode'
* * Text Response/ Document(text) -> Regex 
* * URL Sampled can't be mapped yet
* * Response Code -> Http Response Code (not a validation object. There can only be one response code per action but something else woudn't make sense anyway.)	
* * Response Message can't be mapped
* * Response Headers -> can't be mapped, validations won't be created  
* * Ignore Status -> can't be mapped, validations won't be created 

The selectionContent from TSNC is always mapped to '.*' unless a variable should be asserted. In that case is is mapped to ${variablename}.

* 'Pattern Matching Rules' are mapped to TSNCs validationMode
* * Contains -> Exists
* * Matches -> Matches
* * Equals -> Text
* * Substring -> Exists
* * Not can't be mapped, validations won't be created

* 'Patterns to Test' is always mapped to 'validationContent'. If there are multiple patterns to test TSNC makes multiple validation objects.

#### Listeners 

Listeners are not read. Results are saved and shown in the same way as in other TSNC tests cases.
