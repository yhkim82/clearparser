#!/usr/bin/python
import sys
import glob

PATH = sys.argv[1]

filelist = glob.glob(PATH)
filelist.sort()
print filelist

for filename in filelist:
	fin = open(filename)
	wc  = 0

	for line in fin:
		line = line.strip()
		if line: wc += 1

	print filename, wc
