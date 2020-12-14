# KB's Alma Client

Implements the ALMA api with a complex error handling and retry capability.

## Usage

Create a `AlmaRestClient` object with the alma config you need

```java
AlmaRestClient almaRestClient = new AlmaRestClient(almaTarget, alma_apikey);
```

The AlmaRestClient exposes these methods

* `public WebClient constructLink()`: Construct a `org.apache.cxf.jaxrs.client.WebClient` for the Alma Interface.

* `public WebClient getWebClient(URI link)` / `public WebClient getWebClient(String link)`: Construct a `org.apache.cxf.jaxrs.client.WebClient` for an arbitrary URL

* `public <T> T get(final WebClient link, Class<T> type)` Send a GET request and parse the result as `type`

* `public <T, E> T put(final WebClient link, Class<T> type, E entity)` Send a PUT request with the given entity (use Null for no entity) and parse the result as `type`

* `public <T, E> T post(final WebClient link, Class<T> type, E entity)` Send a POST request with the given entity (use Null for no entity) and parse the result as `type`

* `public <T> T delete(final WebClient link, Class<T> type)` Send a delete request and parse the result as `type`

The methods above are NOT just wrappers of the corresponding CXF methods. Rather, a few manipulations of the request is done

1. GET requests have a cache. If the exact same URL have been retrieved before it returns the cached value.
This can be controlled by
   * As a parameter for a specific request: `public <T> T get(final WebClient link, Class<T> type, boolean useCache)`
   * getter/setter for cachingEnabled for global control.
    
2. Auth by API key is automatically handled. This means that logs listing the URL will NOT contain the api key.

3. Handles HTTP Redirection

4. Handles retries on Timeouts

5. Handles retries on ALMA HTTP 429 Rate Limit errors

6. Have ALL the (relevant) API objects as Java Classes.

7. Parses ALMA errors as exceptions

The Exceptions that can be thrown are all subtypes of `dk.kb.alma.client.exceptions.AlmaClientException`

The common types are

* `AlmaConnectionException`: When a network error was encountered
* `AlmaKnownException`: When the error is of a documented ALMA type. This exception will have these fields: 
```java
WebServiceResult result;
String errorCode;       
String errorMessage;
```
* `AlmaUnknownException`: When the error was not a known ALMA error
* `AlmaNotFoundException`: When you request something that could not be found.


The special structure of the `AlmaRestClient` allows us to create client methods corresponding to the ALMA api with a minimun of fuss. Example

See <https://developers.exlibrisgroup.com/alma/apis/docs/bibs/R0VUIC9hbG1hd3MvdjEvYmlicy97bW1zX2lkfQ==/>

This can be implemented with the method
```java
public Bib getBib(String mmsID) throws AlmaConnectionException, AlmaKnownException, AlmaUnknownException {
    return almaRestClient.get(almaRestClient.constructLink()
                                            .path("/bibs/")
                                            .path(mmsID), Bib.class);
}
```

Then create the nessesary lightweight clients from this object.

These lightweight clients can be created and destroyed as you like, they are just wrappers around the REST client and is thus very fast to create.

Note that not EVERY method from the ALMA api have been implemented, as we have added them on as-needed basis. Adding new methods is very fast, so just create an Issue or contact us if there is something else you need.

Corresponds to <https://developers.exlibrisgroup.com/alma/apis/conf/>
```java
AlmaConfClient almaConfClient = new AlmaConfClient(almaRestClient);
```

Corresponds to <https://developers.exlibrisgroup.com/alma/apis/bibs/>
```java
AlmaInventoryClient almaInventoryClient = new AlmaInventoryClient(almaRestClient);
```

Corresponds to <https://developers.exlibrisgroup.com/alma/apis/partners/>
```java
AlmaPartnersClient almaPartnersClient = new AlmaPartnersClient(almaRestClient);
```

Corresponds to <https://developers.exlibrisgroup.com/alma/apis/tasklists/>
```java
AlmaTasksClient almaTasksClient = new AlmaTasksClient(almaRestClient);
```

Corresponds to <https://developers.exlibrisgroup.com/alma/apis/users/>
```java
AlmaUsersClient almaUsersClient = new AlmaUsersClient(almaRestClient);
```

Corresponds to <https://developers.exlibrisgroup.com/alma/apis/analytics/>
```java
AlmaAnalyticsClient almaAnalyticsClient = new AlmaAnalyticsClient(almaRestClient);
```


Beside these clients, there is the odd `AlmaSRUClient`. The ALMA SRU interface is documented at <https://developers.exlibrisgroup.com/alma/integrations/sru/>

TODO document the odd SRU client. 