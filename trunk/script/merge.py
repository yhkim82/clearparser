#!/usr/bin/python
import sys

fin1 = open(sys.argv[1])
fin2 = open(sys.argv[2])
fout = open(sys.argv[3],'w')

for i,line in enumerate(fin1):
	l1 = line.split()
	l2 = fin2.readline().split()

	if not l1:
		fout.write('\n')
	else:
		l1.insert(3, l2[2])
		l1.insert(5, l2[1])
		fout.write('\t'.join(l1)+'\n')

