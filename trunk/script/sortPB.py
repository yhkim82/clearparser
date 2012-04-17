#!/usr/bin/python
import sys
import operator

fin  = open(sys.argv[1])
fout = open(sys.argv[2],'w')
d    = dict()

for line in fin:
	l      = line.split()
	key    = (l[0],int(l[1]),int(l[2]))
	d[key] = ' '.join(l[3:])

keys = sorted(list(d.keys()), key=operator.itemgetter(0,1,2))

for key in keys:
	ls = list()
	ls.append(key[0])
	ls.append(str(key[1]))
	ls.append(str(key[2]))
	ls.append(d[key])

	fout.write(' '.join(ls)+'\n')
