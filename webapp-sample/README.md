This sample webapp exposes a book library management REST API with following methods :  
* add a book to the library,
* get book infos given its id,
* remove a book from the library given its id

# Running the webapp
NB : be sure to properly configure idempotence before building and launching the webapp

	> mvn clean install 
	> cd webapp-sample
	> mvn spring-boot:run

Once started, the book library REST API documentation should be available at <http://localhost:8080/swagger-ui.html>  
Also take a look at produced logs (especially, `TRACE` level logs created from `com.github.dgrandemange.idempotencereceiver.*` logger).  

# Playing with book creation idempotent method
Play the sample scenario below using the `curl` command line tool (can also be run through the Swagger UI provided at <http://localhost:8080/swagger-ui.html>, or any Soap UI, Postman ...) :

1) submit a book creation request (`Idempotency-Key:0000000001`)

	> curl -v -X POST localhost:8080/books -H "Content-type:application/json" -H "Idempotency-Key:0000000001" -d "{\"isbn\": \"1234567890\", \"title\": \"Eloge de l'ombre\", \"author\": \"Tanizaki\"}"

	Note: Unnecessary use of -X or --request, POST is already inferred.
	* Uses proxy env variable no_proxy == 'localhost,192.168.99.100,10.242.87.220,127.0.0.*,192.168.*'
	*   Trying 127.0.0.1...
	* TCP_NODELAY set
	* Connected to localhost (127.0.0.1) port 8080 (#0)
	> POST /books HTTP/1.1
	> Host: localhost:8080
	> User-Agent: curl/7.61.1
	> Accept: */*
	> Content-type:application/json
	> Idempotency-Key:0000000001
	> Content-Length: 73
	>
	* upload completely sent off: 73 out of 73 bytes
	< HTTP/1.1 201
	< Location: http://localhost:8080/books/1
	< Content-Type: application/json;charset=UTF-8
	< Transfer-Encoding: chunked
	< Date: Fri, 01 Feb 2019 11:32:52 GMT
	<
	{"id":1}

* status CREATED (201), 
* the `Location` header is set and indicates the newly created entity location,
* payload indicates newly created entity identifier value (let's call it _{id1}_)

2) submit a book read request; <u>in the request below, take care of replacing _{id1}_ placeholder by the id value returned at step 1</u>

	> curl -v -X GET localhost:8080/books/{id1} -H "Content-type:application/json"

	Note: Unnecessary use of -X or --request, GET is already inferred.
	* Uses proxy env variable no_proxy == 'localhost,192.168.99.100,10.242.87.220,127.0.0.*,192.168.*'
	*   Trying 127.0.0.1...
	* TCP_NODELAY set
	* Connected to localhost (127.0.0.1) port 8080 (#0)
	> GET /books/1 HTTP/1.1
	> Host: localhost:8080
	> User-Agent: curl/7.61.1
	> Accept: */*
	> Content-type:application/json
	>
	< HTTP/1.1 200
	< Content-Type: application/json;charset=UTF-8
	< Transfer-Encoding: chunked
	< Date: Fri, 01 Feb 2019 11:33:50 GMT
	<
	{"isbn":"1234567890","title":"Eloge de l'ombre","author":"Tanizaki"}

* status OK (200), 
* payload : book _{id1}_ JSON representation,

3) submit the same creation request as the one of step 1 
	
	> curl -v -X POST localhost:8080/books -H "Content-type:application/json" -H "Idempotency-Key:0000000001" -d "{\"isbn\": \"1234567890\", \"title\": \"Eloge de l'ombre\", \"author\": \"Tanizaki\"}"

	Note: Unnecessary use of -X or --request, POST is already inferred.
	* Uses proxy env variable no_proxy == 'localhost,192.168.99.100,10.242.87.220,127.0.0.*,192.168.*'
	*   Trying 127.0.0.1...
	* TCP_NODELAY set
	* Connected to localhost (127.0.0.1) port 8080 (#0)
	> POST /books HTTP/1.1
	> Host: localhost:8080
	> User-Agent: curl/7.61.1
	> Accept: */*
	> Content-type:application/json
	> Idempotency-Key:0000000001
	> Content-Length: 73
	>
	* upload completely sent off: 73 out of 73 bytes
	< HTTP/1.1 201
	< Location: http://localhost:8080/books/1
	< Content-Type: application/json;charset=UTF-8
	< Transfer-Encoding: chunked
	< Date: Fri, 01 Feb 2019 11:34:30 GMT
	<
	{"id":1}

* as the submitted idempotence key is the same as the one submitted at step 1, no book creation should have occured at all, and the response (status, headers, payload) should be the same as the one returned at step 1

4) submit another creation request with the same payload as the one of step 1, but with an incremented idempotence key (`Idempotency-Key:0000000002`)
	
	> curl -v -X POST localhost:8080/books -H "Content-type:application/json" -H "Idempotency-Key:0000000002" -d "{\"isbn\": \"1234567890\", \"title\": \"Eloge de l'ombre\", \"author\": \"Tanizaki\"}"

	Note: Unnecessary use of -X or --request, POST is already inferred.
	* Uses proxy env variable no_proxy == 'localhost,192.168.99.100,10.242.87.220,127.0.0.*,192.168.*'
	*   Trying 127.0.0.1...
	* TCP_NODELAY set
	* Connected to localhost (127.0.0.1) port 8080 (#0)
	> POST /books HTTP/1.1
	> Host: localhost:8080
	> User-Agent: curl/7.61.1
	> Accept: */*
	> Content-type:application/json
	> Idempotency-Key:0000000002
	> Content-Length: 73
	>
	* upload completely sent off: 73 out of 73 bytes
	< HTTP/1.1 409
	< Content-Type: text/plain;charset=UTF-8
	< Content-Length: 30
	< Date: Fri, 01 Feb 2019 11:35:16 GMT
	<
	Book already registered (id=1)

* as the submitted idempotence key is different from the one submitted at step 1, book creation processing starts, but here, it fails due to a business control (that is : a book cannot be created if it already exists in the library),
* status CONFLICT (409)

5) submit another creation request with a new book (see request data payload), using an incremented idempotence key (`Idempotency-Key:0000000003`)
	
	> curl -v -X POST localhost:8080/books -H "Content-type:application/json" -H "Idempotency-Key:0000000003" -d "{\"isbn\": \"0987654321\", \"title\": \"Moby dick\", \"author\": \"Herman Melville\"}"

	Note: Unnecessary use of -X or --request, POST is already inferred.
	* Uses proxy env variable no_proxy == 'localhost,192.168.99.100,10.242.87.220,127.0.0.*,192.168.*'
	*   Trying 127.0.0.1...
	* TCP_NODELAY set
	* Connected to localhost (127.0.0.1) port 8080 (#0)
	> POST /books HTTP/1.1
	> Host: localhost:8080
	> User-Agent: curl/7.61.1
	> Accept: */*
	> Content-type:application/json
	> Idempotency-Key:0000000003
	> Content-Length: 73
	>
	* upload completely sent off: 73 out of 73 bytes
	< HTTP/1.1 201
	< Location: http://localhost:8080/books/2
	< Content-Type: application/json;charset=UTF-8
	< Transfer-Encoding: chunked
	< Date: Fri, 01 Feb 2019 11:36:00 GMT
	<
	{"id":2}

* status CREATED (201), 
* http header `Location` is set and indicates the newly created resource location,
* payload indicates newly created entity(book) identifier value (let's call it _{id2}_)

6) submit a book read request (replace _{id2}_ placeholder by the id value returned at step 5)

	> curl -v -X GET localhost:8080/books/{id2} -H "Content-type:application/json"
	
	Note: Unnecessary use of -X or --request, GET is already inferred.
	* Uses proxy env variable no_proxy == 'localhost,192.168.99.100,10.242.87.220,127.0.0.*,192.168.*'
	*   Trying 127.0.0.1...
	* TCP_NODELAY set
	* Connected to localhost (127.0.0.1) port 8080 (#0)
	> GET /books/2 HTTP/1.1
	> Host: localhost:8080
	> User-Agent: curl/7.61.1
	> Accept: */*
	> Content-type:application/json
	>
	< HTTP/1.1 200
	< Content-Type: application/json;charset=UTF-8
	< Transfer-Encoding: chunked
	< Date: Fri, 01 Feb 2019 11:36:34 GMT
	<
	{"isbn":"0987654321","title":"Moby dick","author":"Herman Melville"}

* status OK (200), 
* payload : book _{id2}_ JSON representation,