Eclipse Code Modifying Plugin
=============================

This Plugin can be used to convert JUnit 3 TestCases to JUnit 4 TestCases and to remove old non-javadoc comments from the source code.

Building
=============

To build, use the following command. Aftwards you can install the tooling via the local update site codemodify/de.simonscholz.codemodify.build.p2/target/repository

`mvn clean verify`

Features
========

- Removes TestCase parent class and the "junit.framework.TestCase" import
- setUp method
  - adds @Before annotation and its import
  - makes method public
- tearDown method
  - adds @After annotation and its import
  - makes method public
- test methods
  - adds @Test annotations and its import
  - Adds static imports for the assert* methods
  
- Remove old non-javadoc comments from the source code

Usage
=====

Just right click on a java class, a package or a whole Java project in Eclipse and click on the "Source"->"Convert to JUnit 4" menu entry.
