# Streaming QR
This project is an effort to create a protocol (and supporting library) to use streams of QR codes to transmit data between mobile devices using a visual protocol, but without the capacity constraints of individual QR codes.

## Building Streaming QR

The streaming QR project uses gradle to build a stand alone java
library, an android library, and an android app that uses these
libraries to send and receive data via sequences of QR codes.

You will need to install gradle 1.11 to run the builds.

These documents assume that you have checked out the Streaming QR code
source into a directory called `StreamingQR`.

### Building the java library

Open a shell in the `StreamingQR/development` directory.

From this location, run:

    $ gradle uploadArchives

This will compile the java library and place the resulting jar in the
correct location in the filesystem for the Android builds.

### Building the Android components

Note: You must run the `uploadArchives` target for the Java library
component before building the Android components.

Open a shell in the `StreamingQR/development/android` directory.

From this location, run:

    $ gradle build

This will build both the Android library and the Android
application. After the build completes, the android application will
be located at:
`StreamingQR/development/android/qrstream/build/outputs/apk/qrstream-debug-unaligned.apk`


