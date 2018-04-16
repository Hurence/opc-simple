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

### Examples

A step by step series of examples to showcase basic use cases.


##### Connect to an OPC-DA server

As a prerequisite you should have an up an running OCP-DA server. In this example we'll use the
[Matrikon OPC simulation server](https://www.matrikonopc.com/products/opc-drivers/opc-simulation-server.aspx).

Please feel free to change connection settings reflecting your real environment.



```java


   //create a connection profile
   OpcDaConnectionProfile connectionProfile = new OpcDaConnectionProfile()
         //change with the appropriate clsid
        .withComClsId("F8582CF2-88FB-11D0-B850-00C0F0104305")
        //change with your domain
        .withDomain("OPC-DOMAIN")
        .withUser("OPC")
        .withPassword("opc")
        .withRefreshPeriodMillis(100)
        //use server cache
        .withDirectRead(false)
        .withHost("192.168.56.101")
        .withSocketTimeout(Duration.of(1, ChronoUnit.SECONDS));
        
    //Create an instance of a da operations
    OpcDaOperations opcDaOperations = new OpcDaOperations();
    //connect using our profile
    opcDaOperations.connect(connectionProfile);
    if (!opcDaOperations.awaitConnected()) {
        throw new IllegalStateException("Unable to connect");
    }
        

```


#### Browse a list of tags

Assuming a connection is already in place, just browse the tags and print to stdout.

````java

    opcDaOperations.browseTags().foreach(System.out::println);
````

#### Stream some tags readings

Assuming a connection is already in place, just stream tags values 
and as soon as possible print their values to stdout.

````java

    opcDaOperations.stream("Read Error.Int4", "Square Waves.Real8")
                    .forEach(System.out::println);

````

## Authors

* **Andrea Marziali** - *Initial work* - [amarziali](https://github.com/amarziali)

See also the list of [contributors](https://github.com/Hurence/opc-simple/contributors) who participated in this project.

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

* Thanks to OpenSCADA and Utgard project contributors for their great work.

