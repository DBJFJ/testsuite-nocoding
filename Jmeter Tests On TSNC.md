# Executing Tests From Jmeter On TSNC

You want to try out a new test tool, but you don't want to rewrite all your tests? You want to take a look at a new workflow and result browser without setting everything up by hand? TSNC provides a way to execute simple test cases created in Jmeter. It also translates them into it's native YAML format, so you can get used to TSNC syntax more easily.

Not all tests can be translated, unfortunately TSNC and Jmeter work in different ways and Jmeter posesses far more features. FTP-Requests or Loops just do not exists in TSNC and it uses it's own result browser in place of Jmeters Listeners and it's own load test configurations. Only tests with the following structure can be translated:  

* Thread Group
    * (Variable Declarations)
    * HTTP Request Sampler
        * XPath/ RegEx Extractions
        * Response Assertions
        * (Variable Declarations)
    * HTTP Request Sampler
        * XPath/RegEx Extractions
        * Response Assertions
        * (Variable Declarations) 
* Thread Group ...

## Setup

Go to the the [Quickstart](https://github.com/Xceptance/testsuite-nocoding/wiki/Quickstart#setup) page and follow the instructions. When you come to the Define the Test Case section, just copy over your test cases .jmx file  instead of creating a new one. Follow the [Quickstart](https://github.com/Xceptance/testsuite-nocoding/wiki/Quickstart#setup) guide further to create your wrapper class. Execute it.  

The test should run out of the box in most cases. TSNC will also translate the test case to YAML and create new files for every Thread Group inside the test plan. They are saved in ./config/data/ with a name of "Test Plan-1.-Thread Group.yml". You can modify them, add them to the configurations and execute them separately. Please keep in mind TSNC will overwrite existing files without asking for clarification. You should definitely save important modifications under a different name.

Should the translation crash, your tests crash or should they refuse to work as they should, see the documentation below for possible reasons. Feel free to open an issue if a problem persists. 

## Possible errors from the translation

- **HTTP Requests:** The redirect and keep alive options from the HTTP Requests are ignored.  
- **Http response code:** TSNC automatically validates every actions Http response code. 200 -> Ok. 301 -> Failure.  

- **Extract-Validate Workflow:** TSNC executes all the extractions of an action before it starts the validations. This might cause some trouble if a variable is overwritten sooner then you thought. 
- **`Apply to` field:** The Apply to field of an Extractor or an Assertion doesn't really work.  
    - An Extractor can only extracts from the Response Body. If the Apply to field points to a variable, it will crash.  
    - An Assertion can Assert a variable, but uses the Response Body for the other values.   

- **Extractions**: If an extraction or a variable definition can't be translated, it is assumed the test case woudn't run and instead of translating it somehow, running and crashing the translation crashes right away.  
    - **Default values:** The default values of an Extractor don't work in TSNC. If the extractor doesn't find a match, the test will fail. 
    - **XPath Extractor:** The XML parsing options of an XPath Extractor are ignored

- **Regular Expression Extractor:**  
    - TSNC always applies a Regular Expression to the Response Body. Headers, URL, Code or Response Message in the Field to check will lead to a crash.  
    - TSNC can only extract from the first match. Match No. > 1 -> crash.  
    - TSNC can only extract from one group. Template like $2$$1$$3$ -> crash.    

- **Response Assertion:**  
    - If an assertion can't be translated, it is assumed the test case as a whole still runs, so it creates the test without the assertion.
    - Response Assertions outside of actions are ignored.
    - It is impossible to validate by URL Sample or Response Message  
    - It is impossible to Ignore Status or match by "Not" 

- **Asserting Response Headers** is only sometimes possible. TSNC validates Headers by extracting a single Header by Name and validating it's value. Which is not always compatible with Jmeter, were a single pattern is validated against all response Headers. The translation splits the pattern at `: ` and assumes the first part is the name and the second the value. That works great with something like  
`Accept: text/html`       ->      `Name: Accept, Value: text/html`  
Or even
`Set-Cookie`              ->      `Name: Set-Cookie`    
In the later case TSNC will validate that a Header with the name `Set-Cookie` exists.

But if the Pattern contains a colon with a trailing whitespace more then once, if you intent to just validate a value without naming the header or if you use a regular expression instead of the name, the translation will be faulty.

# Technical Documentation

The following describes the translation from Jmeter to TSNC in more detail. It briefly touches on the structural differences between TSNC and Jmeter and lists all recognized the Jmeter elements. Then it maps the Jmeter elements with their fields to TSNCs internal format. 

## Structural differences:

Jmeter represents it's test cases as a sequence of Requests. TSNC also represents a test case as a sequence of Requests, here called Actions, roughly aquivalent to it's YAML syntax. A single Jmeter file (a Test Plan) can hold many test cases whereas a TSNC test file always corresponds to a single test case.  

Jmeter uses more programming elements featuring a treelike structure, control elements and far more flexible variables. TSNC is a lot more straightforward. It lacks control elements and TSNC variables always hold a single String.

Jmeters divides Extractors and Assertions more strictly then TSNC. Where Jmeter often extracts a variable to validate it afterwards TSNC does so in the same motion. Validations essentially always extract and validate something, they just don't save the extraction. As a side effect every TSNC extraction also validates that the object it is looking for exists. 

In TSNCs internal structure User Defined Variables are saved inside an Interpreter, which is added to every action. An action object has many attributes, but the relevant ones here are headers, response code, interpreter, method, name, parameters, url, store, and validations. The last two attributes are themselves lists of store- (aquivalent to Jmeter extractions) or validation objects (aquivalent to Jmeter assertions).

A store object has an interpreter, a name and a selectionMode and -Content. It can have a subSelectionMode and -Content.  A validation also has these attributes, but a validationMode and -Content in addition to them. Take a look at the YAML documentation to see which values they can take.

## Recognized Jmeter elements

See above for the general structure of Jmeter test cases that TSNC can translate.

The elements that TSNC recognizes are:

- Test Plan 
- Thread Group 
- User Defined Variables
- HTTP Request Defaults
- HTTP Header Manager 
- HTTP Request
- XPath Extractor
- Regular Expression Extractor
- Response Assertion

Other elements are ignored. The elements are also only recognized if they are in the expected structure. Response Assertions or Extractions are only recognized if they are inside of an HTTP Request, Thread Groups only inside the Test Plan.

## Mapping by Element

#### Test Plan

The Test Plan only provides it's name for the YAML files and it's User Defined Variables.

#### Thread Group

The Thread Group similarily provides it's name for the YAML file. It also divides the Test Plan into different test cases. The Test Plan is executed as a whole with one Thread Group executed after the other, but every Thread Group is dumped into a single YAML file. 

#### User Defined Variables

User Defined Variables can be read inside the Test Plan, the Thread Group or inside an action. In either case the variables are saved into the interpreter. 

#### HTTP Request Defaults

Server/Website, Path, Protocol and Parameters are read and saved as defaults for later actions.

#### HTTP Header Manager

Reads the Headers with Name and Value. Can be read inside a Thread Group or inside an action. If read inside a Thread Group the headers are set as defaults from here on. If set inside an action they are added as the headers of that action.

#### HTTP Request

HTTP Requests are read with Name, Server, Path, Protocol, Method and Parameters. They are pretty much comparable to actions. Protocol, Server and Path are added together to form the URL of the action, the other attributes are mapped one to one to the action attributes.

#### XPath Extractor

XPath Extractors are read with Reference Name and XPath query. They are roughly comparable to TSNC stores with selectionMode: XPath. The Name, Reference Name and the XPath query are mapped one to one to Name, and selectionContent. The other attributes are ignored. 

#### Regular Expression Extractor

Jmeters Regular Expression Extractors are more high level then TSNC. They can extract more then one value, reorder them and one can specify whether to extract from the first, second, third or xed match. TSNCs validation are more low level and always extract one group from the first match.  

Regular Expression Extractors are read with the Reference Name, Regular Expression, Template and Match No. They are mapped to TSNC stores with selectionMode: Regex. The Reference Name and the Regular Expression are mapped one to one to the name and selectionContent of TSNCs stores. The template is comparable to TSNCs subSelectionMode: Group. 

`$x$` -> subSelectionMode: Group, subSelectionContent: x.   
`$0$` -> no subSelection   
`$x$$y$` -> log an error and crash    

`Match No. <= 1` -> extract first match
`Match No. > 1` -> log an error and crash with a MappingException. TSNC can't extract from a later match.

`Extract from a variable` -> log an error and crash with a MappingException. TSNC can't extract with Regex and from a variable.

`Field to check != Body` (in some form) -> log an error and crash with a MappingException. TSNC can't extract from a response code, message, headers....


#### Resonse Assertion

Response Assertions are usually mapped to TSNCs validations and can be translated. Usually. But there are exceptions. If a Response Assertion can't be translated to TSNC, it will log an error and fail to create the validation. The translation will not crash, since it is assumed the test can still function without the Assertion, just not as well. 

Every field of the Response Assertion is read (comments excepted): Name, Apply to, Response Field to Test, Pattern Matching Rules and Patterns to Test. The mapping kind of defaults to `selectionMode: Regex`, `selectionContent: .*`, `validationMode: Exists` for simple cases. But there are many exceptions and some options are difficult. See below for the mapping:

- Name -> validation name
- Apply to:
    - Main Sample and sub-samples, Main sample only, Sub samples only -> Make no difference, these options don't exist in TSNC -> ignore those
    - Jmeter Variable -> TSNC variable -> selectionMode: Var selectionContent: same as in Jmeter
    
- Response Field to Test:
    - Response Code -> Add an HttpResponseCode instead of a validation
    - Text Response, Document(text) -> take the whole content -> set selectionMode to Regex and set selectionContent to .+ to take everything unless it was set to VAR to validate a variable already
    - Response Headers -> Troublesome. Set selectionMode: Header. Split the pattern to test at ": ", take the first part as selectionContent and the second as validationContent. Should it only have one part (ie no ": ") set validationMode: EXISTS. Should it have more then two parts log an error, don't create validations
    - URL Sampled, Response Message, Ignore Status -> Can't be validated in TSNC -> log error, don't create the validations
    
- Pattern Matching Rules:
    - Matches -> validationMode: Matches
    - Contains -> validationMode: Exists 
    - Equal -> validationMode: Text
    - Substring -> validationMode: Exists 
    - Not -> Impossible in TSNC -> log error, don't create the validations
    - SelectionMode: Var and validationMode: Exists doesn't make sense. "Validate variable exists." So set validationMode: Matches in that case.
    
- Patterns to Test
    - Mapped to the validationContent
    - Multiple Patterns to Test -> Multiple validation Objects with different values for validationContent