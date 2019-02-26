This project aims to help idempotence mechanism integration into a Spring MVC RESTful service.  
* provides a way to make relevant controller handler method _idempotent_ through a simple annotation,
* provides a dedicated Spring Boot Starter module that takes care of all dependencies declaration and Spring plumbing, while providing a set of dedicated Spring Boot properties to configure the idempotence mechanism.

# Idempotence : what, why, when
Generally speaking, idempotence is the ability of an operation to return the same result regardless of how many times it is invoked.

In a distributed service oriented architecture (like say a micro-service architecture), where the services communicate with each other through the network, we should not consider the network as fully reliable. Meaning our services should be designed in order to be resilient to network micro-failures :
* from a consumer service point of view, this means, for instance, being able to do some request retries when no response has been received from the invoked producer service within the allocated time (i.e. request timeout),
* from a producer service point of view, that means being able to handle those successive retries, meaning some times (and depending on the nature of operation) guaranteeing that the repeatedly requested operation is processed once and only once

Please consider reading resources below for detailed explanations :
* [Rest cookbook - What are idempotent and/or safe methods?](http://restcookbook.com/HTTP%20Methods/idempotency/)
* [Understanding why and how to add idempotent requests to your APIs](https://ieftimov.com/understand-how-why-add-idempotent-requests-api)
* [3 common pitfalls in microservice integration — and how to avoid them](https://blog.bernd-ruecker.com/3-common-pitfalls-in-microservice-integration-and-how-to-avoid-them-3f27a442cd07)

# Integration recipe
Here below are the steps to follow to integrate idempotence into an existing Spring MVC RESTful API.

See also a concrete integration example with the [webapp example](./webapp-sample).

## Add idempotence receiver spring boot starter dependency to your project 

		<dependency>
			<groupId>com.github.dgrandemange</groupId>
			<artifactId>idempotence-receiver-spring-boot-starter</artifactId>
			<version>${idempotence-receiver.version}</version>
		</dependency>

## Annotate service methods that need manual idempotence handling
Some methods doesn't need manual idempotent management, because there are already idempotent by nature.  
For instance, methods that only read resources, and do not affect their state, are natively idempotent and do not require manual idempotence handling. It may even reveal counter-productive to manually handle idempotence for such methods.  
On the other hand, methods that affect resources state, especially resource creation (usually through HTTP verb `POST` in a RESTful API) are good candidates to manual idempotence handling.  

To enable manual idempotence management of a method, simply annotate it with the `@Idempotent` annotation.  
As an example, look at method [com.github.dgrandemange.idempotencereceiver.examples.webapp.controller.BookResource.create(Book)](./webapp-sample/src/main/java/com/github/dgrandemange/idempotencereceiver/examples/webapp/controller/BookResource.java).

Please note : only methods with a `ResponseEntity<...>` return type are eligible to idempotence handling.

## Ensure your HTTP entities class are serializable
This is required in order for the HTTP method result to be stored in remote repositories (like for instance the remote infinispan cache).

## Configure idempotence management
Idempotence configuration is made through dedicated Spring Boot configuration properties.

### Common configuration
NB : for a standard usage, default values provided for those common properties don't need to be overridden.

Look at [IdempotentReceiverCommonConfiguration](./api/src/main/java/com/github/dgrandemange/idempotencereceiver/api/model/IdempotentReceiverCommonConfiguration.java) Javadoc to get an exhaustive list of available properties.  

### Idempotence repository configuration
Idempotence mechanism relies on a repository where idempotent method results are cached for a certain amount of time.

As there are different repositories implementations available, one of the available repository implementations must be chosen and specified via the `idempotence-receiver.repository.type` property (if not set, idempotence mechanism won't be enable).  
Each repository implementation requires its own set of configuration properties to be set. This will be detailed in the next sections.

#### Repository failure resiliency common configuration
The `idempotence-receiver.repository.resiliency.*` properties configure the retry and circuit breaker policies.  
These policies aim to offer resilience to idempotence repository potential failures.  

Look at [ResiliencyConfiguration](./api/src/main/java/com/github/dgrandemange/idempotencereceiver/api/model/ResiliencyConfiguration.java)  Javadoc to get an exhaustive list of available properties configurable in Spring Boot app configuration `application.yaml`.

NB : these properties are common to every repository implementation.

#### Internal memory repository configuration
Select this implementation by setting the `idempotence-receiver.repository.type` property to `internal-memory`.

It configures an in-memory idempotence repository, internally using a `WeakHashMap` as storage facility, meaning idempotent method results stored in this map are cleared every time a garbage collector is triggered (such GC can be manually triggered thanks to the Java Visual VM tooling).  

NB : this implementation is provided for test purpose only, and is quite not acceptable in production environments.

Excerpt of Spring Boot config `application.yaml` :

	spring :
	  application :
	    name : my-rest-api
	
	idempotence-receiver :
	  repository :
	    type : internal-memory
	
	    resiliency :
	      retry :
	        delay-ms : 250
	        max-retries : 2
	      circuit-breaker:
	        failure-threshold : 5
	        success-threshold : 3
	        delay-ms : 60000

#### Infinispan cache repository configuration
Select this implementation by setting the `idempotence-receiver.repository.type` property to `infinispan-cache`.

It configures an idempotence repository relying on a remote [infinispan](http://infinispan.org/) cache (or cluster of) that will be accessed via a [Hot Rod Java client](http://infinispan.org/docs/stable/user_guide/user_guide.html#hotrod_java_client).  

For instance, an infinispan server instance can be easily mounted via a docker environment (make sure to expose port 11222) :

	docker run --rm -p 11222:11222 -it jboss/infinispan-server

NB : in case docker host is a VM running on a Windows OS, you'll need to create a SSH tunnel and map target port `11222` to source `localhost:11222`. 

Look at [IdempotentReceiverInfinispanHotrodConfiguration](./impl/infinispan-hotrodclient/src/main/java/com/github/dgrandemange/idempotencereceiver/infinispan/hotrod/model/IdempotentReceiverInfinispanHotrodConfiguration.java) Javadoc to get an exhaustive list of available properties configurable in Spring Boot app configuration `application.yaml`.  

Here below is an configuration sample to configure a repository that will connect to an infinispan server providing a `default` cache on `localhost:11222` :

	spring :
	  application :
	    name : my-rest-api
	
	idempotence-receiver :
	  repository :
	    type : infinispan-cache
	
	    resiliency :
	      retry :
	        delay-ms : 250
	        max-retries : 2
	      circuit-breaker:
	        failure-threshold : 5
	        success-threshold : 3
	        delay-ms : 60000
	
	    infinispan-cache :
	      hotrod-client-configpath : classpath:/hotrod-client-config.properties
	      cache-name : default
	      ttl-ms : 120000

having a Hotrod client configuration `hotrod-client-config.properties` that should look like this :

	# =============================================================================
	# Connection properties
	# =============================================================================
	infinispan.client.hotrod.server_list=127.0.0.1:11222
	infinispan.client.hotrod.tcp_no_delay=true
	infinispan.client.hotrod.tcp_keep_alive=false
	infinispan.client.hotrod.client_intelligence=BASIC
	infinispan.client.hotrod.request_balancing_strategy=org.infinispan.client.hotrod.impl.transport.tcp.RoundRobinBalancingStrategy
	infinispan.client.hotrod.socket_timeout=2000
	infinispan.client.hotrod.connect_timeout=250
	# NB : max_retries is forced to 0 as retry mechanism is already handled at a higher level
	# (see 'idempotence-receiver.repository.resiliency.retry.*' properties in application.yaml)
	infinispan.client.hotrod.max_retries=0
	infinispan.client.hotrod.batch_size=10000
	#infinispan.client.hotrod.protocol_version=
	
	# =============================================================================
	# Connection pool properties
	# =============================================================================
	infinispan.client.hotrod.connection_pool.max_active=10
	infinispan.client.hotrod.connection_pool.exhausted_action=WAIT
	infinispan.client.hotrod.connection_pool.max_wait=500
	infinispan.client.hotrod.connection_pool.min_idle=5
	infinispan.client.hotrod.connection_pool.min_evictable_idle_time=60000
	infinispan.client.hotrod.connection_pool.max_pending_requests=-1
	
	# =============================================================================
	# Thread pool properties
	# =============================================================================
	infinispan.client.hotrod.async_executor_factory=org.infinispan.client.hotrod.impl.async.DefaultAsyncExecutorFactory
	infinispan.client.hotrod.default_executor_factory.pool_size=20
	infinispan.client.hotrod.default_executor_factory.threadname_prefix=HotRod-client-async-pool
	#infinispan.client.hotrod.default_executor_factory.threadname_suffix=

NB : for a list of available configuration properties (hotrod client v9.4.5), see :
* https://docs.jboss.org/infinispan/9.4/apidocs/org/infinispan/client/hotrod/configuration/package-summary.html#package.description
* https://github.com/infinispan/infinispan/blob/9.4.5.Final/client/hotrod-client/src/main/java/org/infinispan/client/hotrod/impl/ConfigurationProperties.java

## Update your RESTful API documentation and communicate it to consumers
All methods marked `@Idempotent` now require consumer services to provide a specific `Idempotency-Key` HTTP header in their requests in order to be able to consume your API.  
This header should stand as a _request unique identifier_ and therefore **must vary from one request to another, except in case of request re-presentation (i.e. retries)** where it MUST remain the same as the one initially set on request first presentation.  
Consumers should use something like a UUID/GUID generator for that purpose. UUID v4 is a generally a good choice, even if entropy source must be considered as UUID v4 is random based.

Here is a list of some available UUID implementations your consumers can use :
* java : [Guide to UUID in Java](https://www.baeldung.com/java-uuid),
* javascript : [uuid4](https://www.npmjs.com/package/uuid4), [kelektiv uuid](https://github.com/kelektiv/node-uuid),
* python : [uuid module](https://docs.python.org/2/library/uuid.html#uuid.uuid4),
* go : [UUID package for Go language](https://github.com/satori/go.uuid),
* ruby : [Standard library - uuid v4](https://ruby-doc.org/stdlib-2.6/libdoc/securerandom/rdoc/Random/Formatter.html#method-i-uuid),
* php : [PHP library for generating RFC 4122 version 1, 3, 4, and 5 universally unique identifiers (UUID)](https://github.com/ramsey/uuid),
* c : [libuuid](https://linux.die.net/man/3/libuuid)
* c#, vb : [Guid.NewGuid](https://docs.microsoft.com/en-us/dotnet/api/system.guid.newguid?redirectedfrom=MSDN&view=netframework-4.7.2#System_Guid_NewGuid)

NB : not providing the `Idempotency-Key` HTTP header in requests that requires it will lead to a `404` response, unless configuring property `idempotence-receiver.idempotencyKeyHeaderMandatory` to `false` in your RESTful application configuration. In this case, the idempotence mechanism will identify incoming requests by a hash dynamically computed using incoming request body contents, and other meta-informations (consumer IP address, URI, session id and principal identity when available). Nonetheless, this practice is discouraged.
