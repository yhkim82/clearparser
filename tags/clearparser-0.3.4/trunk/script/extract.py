#!/usr/bin/python
import sys
import os

PROP_FILE = sys.argv[1]
PARSE_DIR = sys.argv[2]

fin = open(PROP_FILE)
s   = set()
for line in fin:
	l = line.split()
	s.add(l[0])

l = list(s)
l.sort()
for i in range(len(l)):
	l[i] = PARSE_DIR+'/'+l[i]

fout = open(PROP_FILE+'.parse','w')
for filename in l:
	fin = open(filename)
	for line in fin:
		fout.write(line)
