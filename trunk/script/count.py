#!/usr/bin/python
import sys
import operator
fin = open(sys.argv[1])
d = dict()

for line in fin:
	l = line.split()
	if not l: continue
	if l[3] != 'IN' and l[3] != 'TO': continue
#	if l[6] != POS: continue

	if l[2] in d: d[l[2]] += 1
	else        : d[l[2]]  = 1

l = sorted(d.iteritems(), key=operator.itemgetter(1), reverse=True)

for item in l:
        print item[0],'\t',item[1]
