
Instructions for building QRLib
--------

The project is setup to build with Gradle. If you do not have Gradle
installed do that first [http://www.gradle.org/].

Then from `development/qrlib` directory you can run
    gradle build

This will compile the source in `src/main/java` and also run the unit
tests in `src/test/java`.  JUnit XML test results will be dumped into
the `build/test-results` directory and a prettier html report gets
dumped into `build/tests`.

If you want to build and run the project within Eclipse, you can run
    gradle eclipse

It will generate the necessary `.project` and `.classpath` files
so qrlib can be imported into Eclipse.



