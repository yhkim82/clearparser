#!/bin/bash
jar cf ../lib/clearparser-$1.jar clear
rsync -avc ../lib/clearparser-$1.jar choijd@verbs.colorado.edu:/data/choijd/opt/clearparser/lib
