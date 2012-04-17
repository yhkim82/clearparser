#!/usr/bin/python
import sys
import math
import operator

fin  = open(sys.argv[1])
spos = set(['NN','NNS','NNP','NNPS'])

def retrieve(fin, d):
	for line in fin:
		l = line.split()
		if not l: continue
		if l[3] not in spos: continue
		if l[8] == '_': continue
		
		currId = int(l[0])
		for arg in l[8].split(';'):
			tmp = arg.split(':')
			predId = int(tmp[0])
			label  = tmp[1]
			if currId < predId: dir = '-'
			else              : dir = '+'

			arg = label+dir+l[2]
			if arg in d: d[arg] += 1
			else       : d[arg]  = 1

			d[T] += 1

d = dict()
T = 'TOTAL'
d[T] = 0

retrieve(fin, d)

total = d[T]
lt = list()
for key in d:
	if key == T: continue
	prob = float(d[key]) / total
	lt.append((key, math.log(prob)))
	
l = sorted(lt, key=operator.itemgetter(1), reverse=True)

for item in l:
	print item[0], item[1]
