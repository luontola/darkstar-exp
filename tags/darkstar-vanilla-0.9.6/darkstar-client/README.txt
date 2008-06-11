Project Darkstar Client SDK, Source Distribution
Version 0.9.6-r4193

INTRODUCTION

Project Darkstar lets developers write scalable, reliable, persistent
and fault-tolerant game server applications using simple
single-threaded, event-driven code.

The source distribution of the Project Darkstar Client SDK contains the
source code needed to develop and run game clients that communicate with
each other and with game servers implemented with the Project Darkstar
Server.  The Project Darkstar Server is available as a separate
download.

See the CHANGELOG file for a summary of recent changes, and the
API-CHANGES file for a detailed list of recent changes to the API.

CONTENTS

This distribution contains the following subdirectories:

- doc
  Documentation for the client tutorial

- example
  Source code for the client tutorial and the example C client

- src
  Source code for the client implementation

- test
  Source code for tests

BUILDING

The client makes use of two other open source projects, whose JAR files
should be downloaded and included in the classpath:

- Apache MINA, version 1.1:

  http://mina.apache.org/

- Simple Logging Facade for Java (SLF4J), version 1.4:

  http://www.slf4j.org/


KNOWN ISSUES

This version of the Project Darkstar Client SDK has the following
limitations:

- It only includes a J2SE(TM) client implementation.  Developers can use
  the documentation of the network protocol to develop other
  implementations, including ones to support different languages.

- This release supports only limited I/O throttling, so it is possible
  for clients to overwhelm the server with network traffic and cause the
  server to run out of memory.  As a temporary workaround, developers
  must ensure that clients do not send network traffic faster than the
  server application can process it.


MORE INFORMATION

For more information, please see the tutorial in the doc directory.
Also check the http://www.projectdarkstar.com website, which has links
to more information and to the Project Darkstar forums.

If you cannot find an answer to your question in any of these places,
please email the Darkstar team at:

   ProjectDarkstar@games.dev.java.net
