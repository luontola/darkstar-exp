Project Darkstar Server, Source Distribution
Version 0.9.6-r4193

INTRODUCTION

Project Darkstar lets developers write scalable, reliable, persistent
and fault-tolerant game server applications using simple
single-threaded, event-driven code.

The source distribution of the Project Darkstar Server contains the
source code for developing game servers that run as applications in the
Project Darkstar Server environment.  Information about building client
applications is available as a separate download.

In addition to the documents, APIs and examples, this distribution
contains the source code for a complete single-system version of the
Project Darkstar Server.

See the CHANGELOG file for a summary of recent changes, and the
API-CHANGES file for a detailed list of recent changes to the API.

CONTENTS

This distribution contains the following subdirectories:

- doc
  Server tutorial

- etc
  Miscellaneous files for building the source code and for running the
  server

- example
  Source code for example games

- lib
  An empty directory for installing third party JAR files

- src
  Source code for the server implementation

- test
  Source code for tests

BUILDING

The Project Darkstar Server makes use of several other open source
projects.

To build the software, you should install and use Ant:

- Apache Ant, version 1.6.5 (or better):

  http://ant.apache.org/

You can use the build-server, jar-server, and javadoc-api-server targets to
build the server classes, JAR file, and documentation.

The JAR files for the following three projects need to be installed in
the lib subdirectory:

- Apache MINA, version 1.1:

  http://mina.apache.org/

- Simple Logging Facade for Java (SLF4J), version 1.4:

  http://www.slf4j.org/

- Berkeley DB Java Edition, version 3.2.23:

  http://www.oracle.com/database/berkeley-db/je/index.html

The binaries for Berkeley DB are also needed:

- Berkeley DB, version 4.5.20:

  http://www.oracle.com/database/berkeley-db/db/index.html

These binaries need to be installed in a directory that you specify as
the value of the bdb.lib.dir property when using Ant.

The JAR file for the Junit project needs to be installed in the lib
subdirectory in order to run the tests:

- JUnit version 4.1 (or better)

  http://junit.org/


KNOWN ISSUES

This version of the Project Darkstar Server has the following
limitations:

- Multi-node operation is currently not scalable.  Depending on the
  application, adding additional application nodes to a multi-node
  cluster may reduce performance, not improve it.  We expect this
  behavior to improve in future releases.

- This release supports only limited I/O throttling, so it is possible
  for clients to overwhelm the server with network traffic and cause the
  server to run out of memory.  As a temporary workaround, developers
  must ensure that clients do not send network traffic faster than the
  server application can process it.

- It does not support pluggable I/O transports.

- The API for customizing managers and services is incomplete and still
  under development.

- The transaction coordinator implementation does not support more than
  one persistent service at a time.  This restriction prevents using
  additional services that modify the contents of databases.


MORE INFORMATION

For more information, please see the tutorial in the tutorial directory
and the documentation in the doc directory.  Also check the
http://www.projectdarkstar.com website, which has links to more
information and to the Project Darkstar forums.

If you cannot find an answer to your question in any of these places,
please email the Darkstar team at:

   ProjectDarkstar@games.dev.java.net
