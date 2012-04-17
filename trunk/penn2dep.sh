#!/bin/bash
CORPORA_DIR=$1
DEP_EXT='dep'

for inputFile in $CORPORA_DIR/*/*/*/*.parse
do
    echo $inputFile
    outputFile=${inputFile%.*}.$DEP_EXT
    java clear.engine.PennToDep -f -h ../config/headrule_en_ontonotes.txt -m ../model/en_dict-1.0.jar -i $inputFile -o $outputFile
done

