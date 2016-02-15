# Jmeter to TSNC Translation

TSNC can recognize a test file created with Jmeter, execute it and convert it into the YAML test files TSNC usually uses. 

While TSNC and Jmeter are both test tools, they concentrate on different areas and work in different ways. TSNC is a small test tool focused on calling urls, validating results and showing the results. Jmeter is a large load testing tool with a much wider set of functions working in a different way. As a result, not all Jmeter tests can be translated to TSNC.

Load Test configurations and Listeners will be ignored since TSNC uses it's own properties and result browsers. The Thread Groups are always executed one after the other, not in parallel. Each Thread Group is translated to yaml and saved in a separate test file. 

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

TSNC will always extract only one group. If TSNC detects a template it can't wholy match, like the aforementioned "$3$$1$$2$", it will only extract the first group, $3$ and log a warning. Likewise TSNC will always extract from the first match. If it should extract from a random one it will log a warning and extract from the first. If it should extract from the second, third or another later match, it will log an error and skip that extraction. 

Should template or match be empty, TSNC will default to 1.
#### Response Assertions

Response Assertions can usually be translated to TSNC, but some functions again have no equivalent:
* the pattern matching rule 'not' can't be mapped,  the assertion won't be created
* the 'Ignore Status' field can't be mapped,  the assertion won't be created
* 'URL Sampled' isn't mapped, TSNC, the assertion won't be created
* both 'Text Response' and 'Document (Text)' are mapped to the response body
* the difference between 'Main sample and sub-samples', 'Main sample only' and 'Sub-samples only' is ignored, they are all mapped to the resulting web page

TSNC also adds a default HttpResponseCode=200 validation to every action, so redirections will cause errors unless an assertion for the expected response code was manually defined. 

**Assertions are mapped to Validations. (Except when asserting a Response Code.)**

The mapping for Response Assertions from Jmeter to TSNC is as such:

* **Apply To** is mapped to 'selectionMode'
    * Main sample and sub-samples/ Main sample only/ Sub-samples only -> Regex 
    * Jmeter Variable -> Var  

* **Response Field to Test** is also mapped to 'selectionMode'
    * Text Response/ Document(text) -> Regex 
    * URL Sampled can't be mapped yet, validations won't be created  
    * Response Code -> Http Response Code (not a validation object. There can only be one response code per action but something else woudn't make sense anyway.)	
    * Response Message can't be mapped, validations won't be created  
    * Response Headers -> can't be mapped, validations won't be created  
    * Ignore Status -> can't be mapped, validations won't be created
<TODO write a test for all these validations that can't be mapped>
The selectionContent from TSNC is always mapped to '.*' unless a variable should be asserted. In that case is is mapped to ${variablename}.

* **Pattern Matching Rules** are mapped to TSNCs validationMode
    * Contains -> Exists
    * Matches -> Matches
    * Equals -> Text
    * Substring -> Exists
    * Not -> Exception 
* **Patterns to Test** is always mapped to 'validationContent'. If there are multiple patterns to test TSNC makes multiple validation objects.

#### Listeners 

Listeners are not read. Results are saved and shown in the same way as in other TSNC tests cases.


-------------------------------------------------------------------------------------------------------
# Developers Documentation.

The following describes the translation from Jmeter to TSNC. First it briefly describes the internal structure of TSNC. Afterwards it lists all the Jmeter elements that can be translated and describes how they are translated to TSNCs internal format. It also notes which differences and difficulties arise.

### TSNCs Internal structure:

TSNC represents a test case as a sequence of actions, roughly aquivalent to it's YAML syntax. User Defined Variables are saved inside an Interpreter, which is added to every action. An action object has many attributes, but the relevant ones here are headers, response code, interpreter, method, name, parameters, url, store, and validations. The last two attributes are themselves lists of store- (aquivalent to Jmeter extractions) or validation objects (aquivalent to Jmeter assertions).  

A store has an interpreter, a name, selectionMode and -Content as well as subSelectionMode and -Content. A validation also has these attributes, but also has a validationMode and -Content. To see which values they can take, see the general documentation.

### Jmeter elements:

See <insert link here> for the general structure of Jmeter test cases that TSNC can translate.

The elements that TSNC recognizes are:

* Test Plan with Name and User Defined Variables
* Thread Group with Name
* User Defined Variables
* HTTP Request Defaults
* HTTP Header Manager 
* HTTP Request
* XPath Extractor
* Regular Expression Extractor
* Response Assertion

other elements are ignored. Should other elements be vital for the test, the test will obviously not run correctly.

###### Test Plan

The Test Plan only provides his for the YAML files.

###### Thread Group

The Thread Group similarily provides it's name to the YAML file. It also divides the Test Plan into different test cases. The Test Plan is executed as a whole with one Thread Group executed after the other, but every Thread Group is dumped into a single YAML file. The files is saved in the ./config/data folder. It's name is constructed from the name of the Test Plan, the Number of the Thread Group and the Name of the Thread Group. For Example: "Test Plan-1-Thread Group". Should the file already exists, TSNC will log a warning, *but overwrite the file without user input.*

###### User Defined Variables

User Defined Variables can be read inside the Test Plan, the Thread Group or inside an action. In either case the variables are saved into the interpreter. 

###### HTTP Request Defaults

Server/Website, Path, Protocol and Parameters are read and saved as defaults for the actions to come.

###### HTTP Header Manager

Reads the Headers with Name and Value. Can be read inside a Thread Group or inside an action. If read inside a Thread Group the headers are set as defaults from here on. If set inside an action they are added as the headers of that action.

###### HTTP Request

HTTP Requests are read with Name, Server, Path, Protocol, Method and Parameters. They are pretty much comparable to actions. Protocol, Server and Path are added together to form the URL of the action, the other attributes are mapped one to one to the action attributes.
*There might be some trouble with Redirect Automatically, Follow Redirects or Use KeepAlive, since they don't exist in TSNC.*

###### XPath Extractor

XPath Extractors are read with Reference Name and XPath query. They are roughly comparable to TSNC stores with selectionMode: XPath. The Name, Reference Name and the XPath query are mapped one to one to Name, and selectionContent. *Difficulties can arise from multiple sources: The Apply to field is not read, since TSNC does not have these options. The XML Parsing Options arn't read, since TSNC doesn't have these options either. It is impossible to extract something by XPath and from a Variable in TSNC. The Default Value is ignored too, since TSNC doesn't have that option either. If it an extraction doesn't find anything in TSNC, it will cause the test case to crash with an error.*

###### Regular Expression Extractor

Regular Expression Extractors are read with the Reference Name, Regular Expression, Template and Match No. They are roughly comparable to TSNC stores with selectionMode: Regex. The Reference Name and the Regular Expression are mapped one to one to the name and selectionContent of TSNCs. The template is comparable to TSNCs subSelectionMode: Group.
*Jmeters Template and Match No. offer a bit more functionality then TSNC has here. It is assumed that a test can't run if the extractor doesn't work. Which is why TSNC will not only log an error and fail to translate the extractor if the Template and Match No. can't be translated to TSNC, but crash with a MappingException.* The problem with the template is that Jmeter allows one to extract multiple groups, reorder them and put them back together, TSNC is more low level and only extracts one group only. The mapping is:  
$x$ -> subSelectionMode: Group, subSelectionContent: x.
$0$ -> no subSelection
*$x$$y$ -> log an error and crash* 

*The problem with the Match No. is simply that TSNC always extract the first match and only the first match. So it has to crash if the Match No. is higher then 1.* 

*Other possible sources of problems: The Apply to field and the Field to check are ignored. Should they be important, the resulting test will be faulty. Finally the Default Value is ignored too, since TSNC doesn't have that option either. If it an extraction doesn't find anything in TSNC, it will cause the test case to crash with an error.*

###### Resonse Assertion

Response Assertions are often aquivalent to TSNCs validations and can be translated. Usually. But there are exceptions. *If a Response Assertion can't be translated to TSNC, it will log an error and fail to create the validation. It will not crash, since it is assumed the test can still function without the Assertion, just not as well.* Every field of the Response Assertion is read (the comments excepted): Name, Apply to, Response Field to Test, Pattern Matching Rules and Patterns to Test. Some values of these fields are troublesome. See below for the mapping:
*Name -> validation name
*Apply to:
    *Main Sample and sub-samples, Main sample only, Sub samples only -> Make no difference, options don't exist in TSNC -> ignore those
    *Jmeter Variable -> TSNC variable -> selectionMode: Var selectionContent: same as in Jmeter
*Response Field to Test:
    *Response Code -> Add an HttpResponseCode instead of a validation
    *Text Response, Document(text) -> take the whole content -> set selectionMode to Regex and set selectionContent to .+ to take everything unless it was set to VAR to validate a variable already
    *URL Sampled, Response Message, Response Headers -> Can't be validated in TSNC, at least not that way -> log error, don't create the validations
    *URL Sampled, Response Message, Response Headers -> Impossible in TSNC -> log error, don't create the validations
*Pattern Matching Rules:
    *Matches -> validationMode: Matches
    *Contains -> validationMode: Exists 
    *Equal -> validationMode: Text
    *Substring -> validationMode: Exists 
    *Not -> Impossible in TSNC -> log error, don't create the validations
    *SelectionMode: Var and validationMode: Exists doesn't make sense. "Validate variable exists." So set validationMode: Matches in that case.
*Patterns to Test
    *Mapped to the validationContent
    *Multiple Patterns to Test -> Multiple validation Objects with different values for validationContent






