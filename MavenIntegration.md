# Maven Dependencies #

To use Darkstar EXP within a [Maven](http://maven.apache.org/) project, first add the following declaration to the `<repositories>` element in your POM file:

```
<repository>
    <id>orfjackal</id>
    <url>http://repo.orfjackal.net/maven2</url>
</repository>
```

The dependency to the latest Darkstar EXP server release is:

```
<dependency>
    <groupId>net.orfjackal.darkstar-exp</groupId>
    <artifactId>darkstar-server</artifactId>
    <version>0.9.7_1</version>
</dependency>
```

See ChangeLog and VersionNumbersExplained for details on available versions. For the server libraries it might be best to use the "provided" scope, if you start the server using the default startup scripts.

For the client libraries, you may use the official release which resides in the [java.net repository](http://download.java.net/maven/2/).

```
<repository>
    <id>java.net</id>
    <url>http://download.java.net/maven/2/</url>
</repository>

<dependency>
    <groupId>com.projectdarkstar.client</groupId>
    <artifactId>sgs-client</artifactId>
    <version>0.9.7</version>
</dependency>
```