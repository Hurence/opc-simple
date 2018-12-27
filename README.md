# OPC Simple

OPC java interop made simple by [Hurence](https://www.hurence.com).

This project is based on OpenSCADA UtGard and aims to provide you an easy to use java library harmonizing both
OPC-DA and OPC-UA standards.


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
    <version>2.0.1</version>
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


##### Connect to an OPC-DA server

As a prerequisite you should have an up an running OPC-DA server. In this example we'll use the
[Matrikon OPC simulation server](https://www.matrikonopc.com/products/opc-drivers/opc-simulation-server.aspx).

Please feel free to change connection settings reflecting your real environment.



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
    opcDaOperations.connect(connectionProfile);
    if (!opcDaOperations.awaitConnected()) {
        throw new IllegalStateException("Unable to connect");
    }
        

```


##### Connect to an OPC-UA server

As a prerequisite you should have an up an running OPC-UA server. In this example we'll use the
[Prosys OPC-UA simulation server](https://www.prosysopc.com/products/opc-ua-simulation-server/).

Please feel free to change connection settings reflecting your real environment.



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
    opcUaOperations.connect(connectionProfile);
    if (!opcUaOperations.awaitConnected()) {
        throw new IllegalStateException("Unable to connect");
    }
        

```

#### Browse a list of tags

Assuming a connection is already in place, just browse the tags and print to stdout.

````java

    opcDaOperations.browseTags().foreach(System.out::println);
````


#### Browse the tree branch by branch

Sometimes browsing the whole tree is too much time and resource consuming.
As an alternative you can browse level by level. 

For instance you can browse what's inside the group _Square Waves_:
````java

    opcDaOperations.fetchNextTreeLevel("Square Waves")
                                    .forEach(System.out::println);
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


##### Create an OPC-DA session

An example:

````java

  OpcDaSessionProfile sessionProfile = new OpcDaSessionProfile()
        // direct read from device
        .withDirectRead(false)
        // refresh period
        .withRefreshInterval(Duration.ofMillis(100));

    try (OpcSession session = opcDaOperations.createSession(sessionProfile)) {
        //do something useful with your session
    }
````

##### Create an OPC-UA session

An example:

````java

  OpcUaSessionProfile sessionProfile = new OpcUaSessionProfile()
        //the publication window
        .withDefaultPublicationInterval(Duration.ofMillis(100));

        
    try (OpcSession session = opcUaOperations.createSession(sessionProfile)) {
        //do something useful with your session
    }
````

#### Stream some tags readings

Assuming a connection is already in place, just stream tags values 
and as soon as possible print their values to stdout.

````java
    
    try {
        session = opcDaOperations.createSession(sessionProfile);
        session.stream((new SubscriptionConfiguration().withDefaultSamplingInterval(Duration.ofMillis(100)),
                "Read Error.Int4", "Square Waves.Real8", "Random.ArrayOfString")
                .forEach(System.out::println);
    } finally {
        opcDaOperations.releaseSession(session);
    }


````


## Authors

* **Andrea Marziali** - *Initial work* - [amarziali](https://github.com/amarziali)

See also the list of [contributors](https://github.com/Hurence/opc-simple/contributors) who participated in this project.

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details

## Acknowledgments

* Thanks to OpenSCADA and Utgard project contributors for their great work.
* Thanks to Apache Milo for the great OPC-UA implementation.

