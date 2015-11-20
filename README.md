genapi
======

GenApi is a controller and routes file generator for Play 2.4 (but also other Generators can be added).

It picks up a file called apidef.txt containing code like this:

    controller MainApi default
    
    GET /api/get_user services.MyService.getUser()
    GET /api/delete_user services.MyService.withoutResult()

and generates a controller `MainApi` which fetches the Service `MyService` via Play 2.4 Guice Dependency injection.
 
Any call to `/api/get_user` will be forwarded to `MyService.getUser`. The return value will be sent to the user.

Additionally a routes-File will be generated, which in turn can be compiled by the regular Play 2.4 routes compiler.

Supported Arguments
-------------------

* Query Parameters

        GET /api/echo_num services.MyService.echoInt(num: Int)

* (simple!) Parameters fetched from the path

        GET /api/echo_route/:path services.MyService.echoString(path)

* JSON-Parameters which will be decoded from the Request Body (if an implicit `Reads` object is available)
      
        POST /api/echo_user services.MyService.echoUser(user: @model.User)

Supported Return Values
-----------------------

* Unit
* String, Int
* Any class which provides an implicit `Writes` will be serialized into JSON
* Standard Play `Result` (this way it's e.g. possible to modify the session from a service)

Customization
-------------

* You can overwrite the base class of the Controller if you give an additional parameter inside `apidef.txt`

    controller MainOverride default controllers.MyBase

    GET /api/get_user services.MyService.getUser()

  This way it's possible to implement your own session-decoding, own exception handling etc.


