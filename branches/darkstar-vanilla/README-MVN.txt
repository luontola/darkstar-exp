
    BUILDING

1. Install Berkeley DB files to your local repository (you need to do this only 
   once)

      cd bdb-4.5.20
      mvn_install_bdb.bat

2. Build the project with one of the following commands (the first one is 
   preferred if you can wait for the tests to run)

      mvn clean package
      mvn -Dmaven.test.skip=true clean package
      mvn_assembly.bat

3. Collect the ZIP files from the /target, /darkstar-client-dist/target and 
   /darkstar-server-dist/target directories


    RELEASING A NEW VERSION

1. Run these commands in the project's root directory to release a new version:

      svn update
      mvn clean
     (mvn -Dmaven.test.skip=true -DdryRun=true release:prepare)
      mvn -Dmaven.test.skip=true release:prepare
      mvn release:clean

   (Version number conventions: 1.2.3-SNAPSHOT -> 1.2.3 -> 1.2.4-SNAPSHOT)

2. Afterwards, export the new tag from SVN and build the artifacts as explained 
   in the preceding section.
