Java Class Dependency Analyzer
==============================

Introduction
------------

This is a set of command-line utilities that scan class files and selected support files
in local Maven projects and generates reports about the dependencies between files.  The
purpose is to facilitate reorganizing code by moving it between Maven modules.

Installation
------------

Right now, all operations are done via local Maven commands:

1. Download the [source code] (https://github.com/shawnsmith/dependency-analyzer) locally

        $ git clone git://github.com/shawnsmith/dependency-analyzer.git

2. Build the source and run the tests:

        $ mvn verify

Commands
--------

### ShowMoveCandidates

Generates a "moves file" containing list of classes that may be moved from one Maven to another without
breaking any other Maven modules.  Generally, you will need to go though the output file and trim it
down to the subset of classes you actually want to move.

Usage:

    mvn -q exec:java
      -Dexec.mainClass=com.bazaarvoice.scratch.dependencies.ShowMoveCandidates
      -Dexec.args="<options>"

Options:

*   `-root <directory>` - directory containing all source files and compiled classes, including Maven pom.xml files.
*   `-src <maven-module-descriptor>` - the source Maven module.
*   `-dest <maven-module-descriptor>` - the destination Maven module.
*   `-moves <file-name` - the name of the output file to write the move candidates to, instead of the default stdout.
*   `-refs <file-name>` - an optional file in the "moves file" format that lists Maven modules and classes that
    must be visible to those modules.  This file can be used to add class dependencies that aren't discovered by
    the regular class scanner because of unsupported file formats, etc.
*   `-group <maven-groupId>` - default Maven group ID, used when parsing or displaying Maven module descriptors.
*   `-package <java-package-prefix>` - Java classes outside this package are ignored.

Example:

    mvn -q exec:java
      -Dexec.mainClass=com.bazaarvoice.scratch.dependencies.ShowMoves
      -Dexec.args="-root /home/scott/source -group com.example -package com.example -src product-api -dest product-app"

### ShowMoveErrors

Show a classes that likely won't compile or will have class dependency errors once a set of proposed moves
take place.  The proposed moves are described in a "moves file" (see below).

Usage:

    mvn -q exec:java
      -Dexec.mainClass=com.bazaarvoice.scratch.dependencies.ShowMoveErrors
      -Dexec.args="<options>"

Options:

*   `-root <directory>` - directory containing all source files and compiled classes, including Maven pom.xml files.
*   `-moves <file-name` - the name of a "move files" that list classes to move and their final destination.
*   `-refs <file-name>` - an optional file in the "moves file" format that lists Maven modules and classes that
    must be visible to those modules.  This file can be used to add class dependencies that aren't discovered by
    the regular class scanner because of unsupported file formats, etc.
*   `-group <maven-groupId>` - default Maven group ID, used when parsing or displaying Maven module descriptors.
*   `-package <java-package-prefix>` - Java classes outside this package are ignored.

Example:

    mvn -q exec:java
      -Dexec.mainClass=com.bazaarvoice.scratch.dependencies.ShowMoveErrors
      -Dexec.args="-root /home/scott/source -group com.example -package com.example -moves /home/scott/moves.txt"

### ShowMoves

Reads a set of moves in a "moves file" and displays them back in one of two formats: (1) as a de-dup'ed,
sorted "moves files" or (2) as a shell script of "svn mkdir" and "svn mv" commands.

Usage:

    mvn -q exec:java
      -Dexec.mainClass=com.bazaarvoice.scratch.dependencies.ShowMoves
      -Dexec.args="<options>"

Options:

*   `-root <directory>` - directory containing all source files and compiled classes, including Maven pom.xml files.
*   `-moves <file-name` - the name of a "move files" that list classes to move and their final destination.
*   `-group <maven-groupId>` - default Maven group ID, used when parsing or displaying Maven module descriptors.
*   `-package <java-package-prefix>` - Java classes outside this package are ignored.
*   `-svn` - Display output as a set of Subversion mkdir and move commands instead of in the default "moves file" format.

Example:

    mvn -q exec:java
      -Dexec.mainClass=com.bazaarvoice.scratch.dependencies.ShowMoves
      -Dexec.args="-root /home/scott/source -group com.example -package com.example -moves /home/scott/moves.txt"

Moves Files
-----------

A number of the tools accept or produce a "moves file", a file listing a set of class names or file names
grouped by Maven module.  The file format is simple:
*   Lines containing only whitespace are ignored, as are lines where the first non-whitespace character
    is a `#` (ie. comment lines)
*   Otherwise, lines where the first character is not whitespace must be a Maven module descriptor of
    the form `<groupId>:<artifactId>`, where if the `<groupId>:` prefix is missing the default group
    specified on the command-line is assumed.  For example:
       `com.example:product-dao`
       `product-app`
*   Otherwise, lines are assumed to contain a classname or a filename, relative to the classpath root.
    These identify a file or class that belongs to the preceeding Maven module descriptor.

For example:

    product-dao
        com.example.model.MyModel
        com/example/model/MyModel.hbm.xml
        com.example.dao.MyDao
        com.example.dao.hibernate.MyDaoHibernate

    product-app
        com.example.service.MyService
        com.example.service.impl.MyServiceImpl
        META-INF/spring.handlers

    product-web
        WEB-INF/web.xml
        WEB-INF/applicationContext.xml
        WEB-INF/tapestry.application
        WEB-INF/MyPage.page

For simulating moves, this file lists a set of classes and files and their final location after the
move has finished.  Note that the source of the moves are not specified--they are discovered by
scanning the files on disk.

Class Scanner
-------------

The dependency analyzer scans class files and selected support files to detect dependencies
between files.  Usually these dependencies are of the form "`classA` depends on `classB`".

### Java class files

Scans the following for references to other Java classes:
*   `target/classes/**/*.class`

This finds *most* runtime dependencies between classes.  Dependencies that are missed include
the following:

*   Dynamic class loading-related dependencies, eg. `Class.forName()`
*   Unused imports
*   References to static final primitive and String values.
*   Local variable types unless the .class files are compiled with debug information
*   Annotations on local variables (these are rare)
*   Some generic type parameters in expressions.

### Spring XML files

Scans the following for references to Java classes and other Spring XML files:
*   `target/classes/**/applicationContext*.xml`, `*-servlet*.xml`, `spring.handlers`
*   `src/main/resources/**/applicationContext*.xml`, `*-servlet*.xml`, `spring.handlers`
*   `src/main/webapp/WEB-INF/**/applicationContext*.xml`, `*-servlet*.xml`, `spring.handlers`

For the most part, Java class references are identified by looking at the `class` attribute on all XML elements.

The "spring.handlers" file is assumed to be a properties files where all key=value pairs
have Java class name values.

Spring annotations in the Java source files are ignored.

### Servlet web.xml files

Scans the following for references to Java classes and other Spring XML files:
*   `target/classes/**/web.xml`
*   `src/main/resources/**/web.xml`
*   `src/main/webapp/WEB-INF/**/web.xml`

Java class references are identified by looking at the text values of `*-class` XML elements.

### Hibernate XML files

Scans the following for references to Java classes:

*   `target/classes/**/*.hbm.xml`
*   `src/main/resources/**/*.hbm.xml`
*   `src/main/webapp/WEB-INF/**/*.hbm.xml`

Java class references are identified by looking at the `class` and `type` attributes on
all XML elements and by the `name` attribute on `class` elements.

### IBatis XML files

Scans the following for references to Java classes:

*   `target/classes/**/*.ibatis.xml`, `sqlmap-config*.xml`
*   `src/main/resources/**/*.ibatis.xml`, `sqlmap-config*.xml`
*   `src/main/webapp/WEB-INF/**/*.ibatis.xml`, `sqlmap-config*.xml`

Java class references are identified by looking at the `parameterClass` and `resultClass` attributes on
all XML elements.

### Tapestry page and component files

Scans the following for references to Java classes:

*   `target/classes/**/*.page`, `*.jwc*`, `*.script`
*   `src/main/resources/**/*.page`, `*.jwc*`, `*.script`
*   `src/main/webapp/WEB-INF/**/*.page`, `*.jwc*`, `*.script`

For the most part, Java class references are identified by looking at the `class` and `type` attributes on
all XML elements.

Currently supports only Tapestry 3.x DTDs.
