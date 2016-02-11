# Jmeter to TSNC Translation

TSNC can recognize test a file created with Jmeter, execute it and convert it into the YAML test files TSNC usually uses. <TO_FINISH> 

While TSNC and Jmeter are both test tools, they concentrate on different areas and work in different ways. TSNC is a small test tool focused on calling urls, validating results and showing the results. Jmeter is a large load testing tool with a much wider set of functions working in a different way. As a result, not all Jmeter tests can be translated to TSNC.

Load Test configurations and Listeners will be ignored since TSNC uses it's own properties and result browsers. The Thread Groups are executed one after the other, not in parallel. Each Thread Group is translated to a seperate test file. 

### Generally speaking, TSNC will translate tests following this structure: 

* Thread Group
    * (Variable Declaration)
    * HTTP Request Sampler
        * XPath RegEx Extractions
        * Response Assertions
    * (Variable Declaration)
    * HTTP Request Sampler
        * XPath/RegEx Extractions
        * Response Assertions 
* Thread Group ...
 
If the Jmeter test does not follow this structure it may not run correctly on TSNC. Other elements are ignored.  
 
#### Config Elements

TSNC has it's own Load Test properties, so Jmeters Load Test properties are not translated. Even aside from that Jmeter features many configuration options that are not applicable or which have no counterpart in TSNC. There are also a small number of configuration options where a translation has not been implemented yet, for example default parameters.
The default protocol (http/ https) and the default headers from Jmeter are translated.

#### Variables

Variables are read with name and value as usual. Variables can be defined inside the Thread Group or inside an Http Request (not in an Extractor). They can be overwritten in later Actions/ Http Requests. 
Another difference is that Jmeter variables are made to hold practically everything. TSNC variables are only made to hold Strings.  

#### Http Requests

Http Request Samplers are translated into TSNC actions and executed with name, protocol, url, path, method and parameters. Redirects won't be followed.

### Extractions and Assertions

Extractors and Assertions in Jmeter are more strictly divided then in TSNC. Where Jmeter often extracts a variable to validate it afterwards TSNC does so in the same motion. The division between extractions and assertions isn't as clear in TSNC. As a sideeffect every TSNC extraction at least asserts that the object it is looking for exists. There is no default value like in Jmeter, the extractor will throw an error. Assertions essentially always extract and assert something too, they just don't save the extraction.

###### XPath Extractions

XPath extractions are read with name and XPath. The big difference is that in TSNC, an extraction will always result in an error if it didn't find anything. That remains the case with extractions read from Jmeter. If the extractor doesn't find anything, there will be an error. There are some other comparatively small differences:

* the difference between 'Main sample and sub-samples', 'Main sample only' and 'Sub-samples only' is ignored, they are all mapped to the resulting web page
* 'Use Tidy', 'Quiet', 'Report errors' and 'Show warnings' don't work.
* the extractor can't be used on variables

###### Regular Expression Extractions

While TSNC and Jmeter both use Regular Expression Extractors, they work in somewhat different ways.  Let's match a regular expression of "Re(.+?)gu(.+?)s(.+?)sion" as an example. In Jmeter you can decide if you want the first match, the second match, a random match or whichever match you'd like. You can also choose which bracket groups to extract and reorder them with a template like "$3$$1$$2$".

TSNC will always extract only one group. If TSNC detects a template it can't wholy match, like the aforementioned "$3$$1$$2$", it will only extract the first group, $3$ and log a warning. Likewise TSNC will always extract from the first match. If it should extract from a random one it will log a warning and extract from the first. If it should extract from the second, third or another later match, it will log an error and skip that extraction. <TODO_&_REALLY?>

#### Response Assertions

Response Assertions can usually be translated to TSNC, but some functions again have no equivalent:
* the pattern matching rule 'not' can't be mapped
* the 'Ignore Status' field can't be mapped
* 'URL Sampled' isn't mapped
* both 'Text Response' and 'Document (Text)' are mapped to the response body
* the difference between 'Main sample and sub-samples', 'Main sample only' and 'Sub-samples only' is ignored, they are all mapped to the resulting web page
* "%20" is automatically translated to a whitespace when used for a validation in Jmeter. But not in TSNC. Tests in which a variable is first used as a parameter, thus requiring %20, and later used for a validation will almost certainly fail. <What_TO_DO?>

TSNC also adds a default HttpResponseCode=200 validation to every action, so redirections will cause errors unless an assertion for the expected response code was manually defined. 

**Assertions are mapped to Validations. (Except when asserting a Response Code.)**

The mapping for Response Assertions from Jmeter to TSNC is as such:

* **Apply To** is mapped to 'selectionMode'
    * Main sample and sub-samples/ Main sample only/ Sub-samples only -> Regex 
    * Jmeter Variable -> Var  

* **Response Field to Test** is also mapped to 'selectionMode'
    * Text Response/ Document(text) -> Regex 
    * URL Sampled can't be mapped yet
    * Response Code -> Http Response Code (not a validation object. There can only be one response code per action but something else woudn't make sense anyway.)	
    * Response Message can't be mapped
    * Response Headers -> can't be mapped, validations won't be created  
    * Ignore Status -> can't be mapped, validations won't be created 

The selectionContent from TSNC is always mapped to '.*' unless a variable should be asserted. In that case is is mapped to ${variablename}.

* **Pattern Matching Rules** are mapped to TSNCs validationMode
    * Contains -> Exists
    * Matches -> Matches
    * Equals -> Text
    * Substring -> Exists
    * Not can't be mapped, validations won't be created

* **Patterns to Test** is always mapped to 'validationContent'. If there are multiple patterns to test TSNC makes multiple validation objects.

#### Listeners 

Listeners are not read. Results are saved and shown in the same way as in other TSNC tests cases.
