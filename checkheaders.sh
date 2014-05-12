#!/bin/bash

# A very simple check to find files with no copyright headers:
find . -name "*.java" | xargs grep -L "opyright"
