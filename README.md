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

Motivation
----------

If you have a play application which has a lot of Api-Calls (e.g. for your client-side-JavaScript framework), you will end up 
in a lot of entries to your routes-File which do not much more than service JSON-Content. 

As all route-entries run into Controller-Actions, which you also have to add them to controlles.

Controllers are a bit bad to test, because they mostly serve multiple purposes

- Decoding Input values
- Doing the actual action (often in a service)
- Encoding Result
- Maybe error handling

In the end this lead to the outsourcing of all real work into injected services which take typed arguments and return typed results.
 
This will make writing controllers a dull work because decoding/encoding/error-handling can be a lot generalized. 

GenApi wants to step down from this pain by generating the Controllers and the route-Files completely automatic from an Api-Definition file
which directly calls the Service.

Example
-------

An example can be found inside `PluginTest`. Just look at `conf/apidef.txt`.

This will generate controllers inside `PluginTest/target/scala-2.11/genapi/MainApi` and `PluginTest/target/scala-2.11/genapi/MainOverride`.

A routes file will be generated inside `PluginTest/target/scala-2.11/genapi/apidef.routes`

How to add to your project
--------------------------

GenApi is not yet pushed to maven. In order to use it, you have to check out and deploy to local Ivy-Repository using

    publishLocal
    
Afterwards you can add the dependency to your Play Application, just modify the `project/plugins.sbt`

    addSbtPlugin("net.reactivecore" % "genapi" % "0.1-SNAPSHOT")

Now you can fill a the `conf/apidef.txt` in the following format
   
    # This is a comment
    controller MainApi default # Generate a controller called MainApi using the default code generator (currently there is only this one)
    GET /api/get_user services.MyService.getUser() # Add calls to controller

   
The generated route file needs to get picked up by the play routes, for this the following is to be added to your main `conf/routes`

    -> / apidef.Routes

Note: The routes are scanned from up to bottom, so add the line above before you run into any catch-all-routes.

How to use
----------

You can add definitions to the file `conf/apidef.txt`

    controller MainApi default
    POST /api/echo_user services.MyService.echoUser(user: @model.User)
    
The file will be automatically picked up by the SBT-Plugin and it will create the controllers and routes files for you.

This will generate a controller called `MainApi` in `target/scala-2.11/genapi/MainApi.scala` which automatically injects the `services.MyService` dependency and calls it in `/api/echo_user`.

    package generated
    
    /* ... */
    class MainApi @Inject() (myService: services.MyService) extends DefaultGenApiControllerBase {
    
      def action_postApiEchoUser() = actionBuilder.async { implicit request =>
        catchErrors {
          val user = parseJsonInput[model.User](request)
          myService.echoUser(user).map (formatResult(_))
        }
      }
    }
    
`catchErrors`, `parseJsonInput` and `formatResult` are defined in the base class `DefaultGenApiControllerBase` and can also be overridden by the user.

The generated routes in `target/genapi/apidef.routes` file looks like this

    POST /api/echo_user generated.MainApi.action_postApiEchoUser()

You can override the base class of the generated controller, by passing another argument to the `controller` directive in `genapi.txt`, like this:

    controller MyController default controllers.MyBase

`controllers.MyBase` should derive from `generated.DefaultGenApiControllerBase` so that you do not have to declare everything by yourself.

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


