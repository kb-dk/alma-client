# KB's Alma Client

Implements the ALMA api with a complex error handling and retry capability.

## Usage

Create a `AlmaRestClient` object with the alma config you need

```java
AlmaRestClient almaRestClient = new AlmaRestClient(almaTarget, alma_apikey);
```

TODO document the capabilities of the AlmaRestClient in more detail.

TODO document the exceptions.

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

TODO document the odd SRU client. 