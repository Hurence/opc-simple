# OPC Simple

OPC DA/UA made simple by [Hurence](https://www.hurence.com).

An easy to use reactive and quite painless OPC UA/DA java library.

Main benefits:

- I support both OPC-DA and OPC-UA with a harmonized unique API.
- I'm reactive (based on ReactiveX) and nonblocking operations makes me performing very fast.
- I'm open source (Apache 2.0)
- I'm portable (java based. No native code needed)

## Getting Started

These instructions will help you to quick start using opc simple.

### Building

You can build on your machine using maven and a jdk >= 1.8.

Just trigger:

```
mvn clean install
```

### Include in your project (with maven)


Add The maven dependency
```

<dependency>
    <groupId>com.github.Hurence</groupId>
    <artifactId>opc-simple</artifactId>
    <version>develop-SNAPSHOT</version>
</dependency>

```


And the needed repositories

```

    <repositories>       
        repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
```

### Examples

A step by step series of examples to showcase basic use cases.


#### Preamble

The library is built as close as possible to the reactive manifesto paradigms and is based on the 
[RxJava](http://reactivex.io/) library.

If you are not familiar with reactive programming, observer patterns, backpressure or with the rx-java 
library in general, you can have further readings on the 
[RxJava wiki](https://github.com/ReactiveX/RxJava/wiki/Additional-Reading)


##### Connect to an OPC-DA server

As a prerequisite you should have an up an running OPC-DA server. In this example we'll use the
[Matrikon OPC simulation server](https://www.matrikonopc.com/products/opc-drivers/opc-simulation-server.aspx).

Please feel free to change connection settings reflecting your real environment.

Follows a simple blocking example (see after below for more complex reactive examples).


```java


   //create a connection profile      
   OpcDaConnectionProfile connectionProfile = new OpcDaConnectionProfile()
         //change with the appropriate clsid
        .withComClsId("F8582CF2-88FB-11D0-B850-00C0F0104305")
        .withCredentials(new NtLmCredentials()
            .withDomain("OPC-DOMAIN")
            .withUser("OPC")
            .withPassword("opc"))
        .withConnectionUri(new URI("opc.da://192.168.99.100"))
        .withSocketTimeout(Duration.of(5, ChronoUnit.SECONDS));
        
    //Create an instance of a da operations
    OpcDaOperations opcDaOperations = new OpcDaTemplate();
    //connect using our profile
    opcDaOperations.connect(connectionProfile).ignoreElement().blockingAwait();
        

```


##### Connect to an OPC-UA server

As a prerequisite you should have an up an running OPC-UA server. In this example we'll use the
[Prosys OPC-UA simulation server](https://www.prosysopc.com/products/opc-ua-simulation-server/).

Please feel free to change connection settings reflecting your real environment.

Follows a simple blocking example (see after below for more complex reactive examples).


```java


   //create a connection profile
   OpcUaConnectionProfile connectionProfile = new new OpcUaConnectionProfile()
      .withConnectionUri(URI.create("opc.tcp://localhost:53530/OPCUA/SimulationServer"))
      .withClientIdUri("hurence:opc-simple:client:test")
      .withClientName("Simple OPC test client")
      .withSocketTimeout(Duration.ofSeconds(5));
        
    //Create an instance of a ua operations
    OpcUaOperations opcUaOperations = new OpcUaTemplate();
    //connect using our profile
    opcUaOperations.connect(connectionProfile)
        .doOnError(throwable -> logger.error("Unable to connect", throwable))
        .ignoreElement().blockingAwait();
    
        

```

#### Browse a list of tags

Assuming a connection is already in place, just browse the tags and print to stdout.

Blocking example:

````java

    opcDaOperations.browseTags().foreachBlocking(System.out::println);
    //execution here is resumed when browse completed
````

Or in a "reactive way"

````java

    opcDaOperations.browseTags().subscribe(System.out::println);
    // code after is executed immediately without blocking (println is done asynchronously)
    System.out.println("I'm a reactive OPC-Simple application :-)");

````

#### Browse the tree branch by branch

Sometimes browsing the whole tree is too much time and resource consuming.
As an alternative you can browse level by level. 

For instance you can browse what's inside the group _Square Waves_:
````java
    opcDaOperations.fetchNextTreeLevel("Square Waves")
        .subscribe(System.out::println);
````

#### Using Sessions

Session are stateful abstractions sharing Connection. 
Hence multiple session can be created per connection.

Session is the main entry point for the following actions:

* Read
* Write
* Stream


When creating a session you should specify some parameters depending on the OPC standard you are using (e.g. direct read from hardware for OPC-DA).

Sessions should be created and released (beware leaks!) through the Connection object.

> SessionProfile and OpcOperations interface extends AutoCloseable interface.
> Hence you can use the handy *try-with-resources* syntax without taking care about destroying connection or sessions.


Reactive tips:

> - Close your sessions in a *doFinally* block if you want to avoid leaks and you do not need anymore the session 
> after downstream completes. 
> - You can use *flatmap* operator to chain flows after creation of a connection or a session.
> - You can handle backpressure and tune up the scheduler to be used for observe/subscribe operations.
> The library itself does not make any assumption on it.

##### Create an OPC-DA session

An example (blocking version):

````java

  OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
        // direct read from device
        .withDirectRead(false)
        // refresh period
        .withRefreshInterval(Duration.ofMillis(100));

    try (OpcSession session = opcDaOperations.createSession(sessionProfile).blockingGet()) {
        //do something useful with your session
    }
````

##### Create an OPC-UA session

An example (still blocking):

````java

  OpcUaSessionProfile sessionProfile = new OpcUaSessionProfile()
        //the publication window
        .withPublicationInterval(Duration.ofMillis(100));

        
    try (OpcSession session = opcUaOperations.createSession(sessionProfile).blockingGet()) {
        //do something useful with your session
    }
````

##### Create an OPC-UA session (reactive way)

A more efficient nonblocking example here:

````java

    final OpcUaTemplate opcUaTemplate = new OpcUaTemplate()
        
        // first create a session with the desired profile
        opcUaTemplate.createSession(new OpcUaSessionProfile()
                        .withPublicationInterval(Duration.ofMillis(100))))
                 // we got a single. Encapsulate in a flowable and chain
                .toFlowable()
                .flatMap(opcUaSession -> 
                        //do something more interesting with your session
                        Flowable
                            .empty()
                            //avoid open session leaks
                            .doFinally(opcUaSession::close)
                )         
                .subscribe(...);
````

#### Stream some tags readings

Assuming a connection is already in place, just stream tags values 
and as soon as possible print their values to stdout.


````java

    final OpcDaTemplate opcDaTemplate = new OpcDaTemplate()
        
        // first create a session with the desired profile
        opcDaTemplate
            .createSession(new OpcDaSessionProfile()
                // direct read from device
                .withDirectRead(false)
                // refresh period
                .withRefreshInterval(Duration.ofMillis(100))
             )             
             // we got a single. Encapsulate in a flowable and chain
            .toFlowable()
            .flatMap(opcUaSession -> 
                    // attach a stream to the session
                    opcUaSession.stream("Square Waves.Real8", Duration.ofMillis(100))
                        // close the session upon completion or error
                        .doFinally(opcUaSession::close)
            )                        
            //buffer in case of backpressure (but you can also discard or keep latest)
            .onBackpressureBuffer()
            //avoid blocking current thread for iowaits
            .subscribeOn(Schedulers.io())
            //take only first 100 elements
            .limit(100)
            //subscribe to events (upstream will start emitting events)
            .subscribe(opcData-> doSomethingWithData(opcData));
````

#### Advanced: managing automatic reconnection

With ReactiveX you can handle your stream as you want and even do some retry on error.

A quick example:

````java

    //assumes connectionProfile and sessionProfile have already been defined.
   daTemplate
        //establish a connection
        .connect(connectionProfile)
        .toFlowable()
        .flatMap(client -> client.createSession(sessionProfile)
            //when ready create a subscription and start streaming some data
            .toFlowable()
            .flatMap(session ->
                    session.stream("Saw-toothed Waves.UInt4", Duration.ofMillis(100))                                
            )
            //do not forget to close connections
            .doFinally(client::close)
        )
        //log upstream failures
        .doOnError(throwable -> logger.warn("An error occurred. Retrying: " + throwable.getMessage()))
        // Retry anything in case something failed failed
        // You can use exp backoff or immediate as well
        .retryWhen(throwable -> throwable.delay(1, TimeUnit.SECONDS))
        // handle schedulers
        .subscribeOn(Schedulers.io())
        // handle backpressure
        .onBackpressureBuffer()
        // finally do something with this data :-)
        .subscribe(opcData-> doSomethingWithData(opcData));

````

### Integrate with other reactive frameworks

Rx-Java uses its Scheduler and Threading models but sometimes there is the need to use another 
already in place thread pool.

When constructing an instance of OpcOperations (DA or UA) you can optionally provide a *SchedulerFactory* 
in order to tell the library which pool to use for blocking and computation tasks.

Here below you will find some examples.

#### Integrate with Vert.x

In order to best integrate with[Vert.x](https://vertx.io/) you should tell OPC simple to use the 
already in-place netty event loops provided by Vert.x

First of all, you need to import the rx-fied version of Vertx:

```
    <dependency>
     <groupId>io.vertx</groupId>
     <artifactId>vertx-rx-java2</artifactId>
     <!-- REPLACE WITH YOUR VERTX VERSION -->
     <version>3.6.2</version>
    </dependency>
```

Then, let's start defining the following Scheduler factory (given as example):

````java
    public class VertxSchedulerFactory implements SchedulerFactory {    
    
        private final Vertx vertx;
        /**
         * Hidden constructor.
         */
        public DefaultRxSchedulerFactory(Vertx vertx) {
            this.vertx = vertx;
        }
    
       
        @Nonnull
        @Override
        public Scheduler forBlocking() {
            return io.vertx.reactivex.core.RxHelper.blockingScheduler(vertx);
        }
    
        @Nonnull
        @Override
        public Scheduler forComputation() {
            return io.vertx.reactivex.core.RxHelper.scheduler(vertx);
        }
    }
````

This scheduler will be used internally to pick and choose the right scheduler for the right operation.

When you subscribe for flowables, you can as well use this scheduler factory to provide Schedulers for 
subscribeOn and observeOn operations.

## Authors

* **Andrea Marziali** - *Initial work* - [amarziali](https://github.com/amarziali)

See also the list of [contributors](https://github.com/Hurence/opc-simple/contributors) who participated in this project.

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details

## Changelog

Everything is tracked on a dedicate [CHANGELOG](CHANGELOG.md) file.

## Acknowledgments

* Thanks to OpenSCADA and Utgard project contributors for their great work.
* Thanks to Apache Milo for the great OPC-UA implementation.

