#!/usr/bin/python
import sys
fin  = open(sys.argv[1])
fout = open(sys.argv[2],'w')

for line in fin:
	l = line.split()
	
	if not l:
		fout.write('\n')
	else:
		l.insert(3, l[3])
		fout.write('\t'.join(l)+'\n')
