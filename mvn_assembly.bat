@echo off
mvn -Dmaven.test.skip=true clean package && pause
