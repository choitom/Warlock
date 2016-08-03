#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR/..
javac warlock/*.java
java warlock.Controller
rm warlock/*.class
