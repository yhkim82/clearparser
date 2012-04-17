#!/usr/bin/python
import sys
import operator

fin1 = open(sys.argv[1])
fin2 = open(sys.argv[2])

s1 = set()
for line in fin1:
	l = line.split()
	if not l      : continue
	if l[7] == '_': continue

	s1.add(l[2])

d2 = dict()
for line in fin2:
	l = line.split()
	if not l      : continue
	if l[7] == '_': continue
	if l[2] in s1 : continue

	if l[2] in d2: d2[l[2]] += 1
	else         : d2[l[2]]  = 1

l = sorted(d2.iteritems(), key=operator.itemgetter(1), reverse=True)

for item in l:
	print item[0], item[1]
