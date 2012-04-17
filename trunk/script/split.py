#!/usr/bin/python
import sys
import math

INPUT_FILE = sys.argv[1]
N = int(sys.argv[2])

fin   = open(INPUT_FILE)
total = len(fin.readlines())
cut   = int(math.ceil(float(total)/N))

fin = open(INPUT_FILE)
for i,line in enumerate(fin):
	if i%cut == 0: fout = open(INPUT_FILE+'.'+str(i/cut),'w')
	fout.write(line)	
