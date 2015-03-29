Darkstar EXP is a modified version of the official [Project Darkstar Server](http://www.projectdarkstar.com/) distribution. It contains experimental new features which do not exist in the vanilla Darkstar. If experience shows that some of the features are useful, they may be included in the vanilla Darkstar by its developers. Individual features can be enabled and disabled by editing the `darkstar-exp.properties` file.

The project files are available in Maven format in a Maven repository (see MavenIntegration).

If you wish to contribute to Darkstar EXP, contact the project owner (username@gmail.com). You should provide a patch for your modifications and unit tests which verify the new functionality. You will then get write access to the source repository so that you can maintain your own features.


# Current Features #

  * TransparentReferences - Avoid manual management of ManagedReferences (by Esko Luontola, since 0.9.6\_2)
  * Decoupled AppContext from its implementation details to allow mocking the whole Darkstar (by Esko Luontola, since 0.9.6\_1)
  * [Mocking support](http://code.google.com/p/darkstar-exp/source/browse/trunk/darkstar-exp-mocks) to help testing Darkstar applications (by Esko Luontola, since 0.9.6\_1)
    * _Also have a look at related projects: [mocksgs](https://mocksgs.dev.java.net/) and [darkstar-integration-test](http://code.google.com/p/darkstar-contrib/wiki/DarkstarIntegrationTest)_
  * Interpret the application root in the application properties file as relative to the properties file, instead of the working directory (by Esko Luontola, since 0.9.6\_0)


# In Progress #

(None)


# Planned Features #

  * [Data garbage collection](http://www.projectdarkstar.com/component/option,com_smf/Itemid,99999999/topic,288.0)
  * [Refactoring of serialized data](http://www.projectdarkstar.com/component/option,com_smf/Itemid,99999999/topic,333.0)
  * [Indexing ManagedObjects and their fields](http://www.projectdarkstar.com/component/option,com_smf/Itemid,120/topic,573.msg3737#msg3737), similar to how RDBMSs index database table columns