@echo off
rem mvn -Dmaven.test.skip=true clean package javadoc:javadoc assembly:assembly
mvn -Dmaven.test.skip=true clean package
