#!/bin/bash

# A very simple check to find files with no copyright headers:

# If nothing is found, we'll exit with this successful exit code:
exitCode=0

echo "Checking java files for copyright headers"
for file in `find . -name "*.java" | xargs grep -L "opyright" | grep -v build/generated`; do
    # If we're in this loop, then the script should exit with an error condition:
    exitCode=1
    echo ${file}
done

exit ${exitCode}

