Eclipse Code Modifying Plugin
=============================

This Plugin can be used to convert JUnit 3 TestCases to JUnit 4 TestCases and to remove old non-javadoc comments from the soruce code.

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

Just right click a java class, a package or a whole javaproject in Eclipse and click on the "Convert to JUnit 4" menu entry.


![Package Explorer Context Menu](http://i58.tinypic.com/optdls.png)
![non-javadoc removal sample](https://pbs.twimg.com/media/By-tQEECQAAMoCR.png:large)
