#!/usr/bin/python
import sys

INPUT_FILE = sys.argv[1]
N = int(sys.argv[2])

fin    = open(INPUT_FILE)
fout   = []
tree   = []
treeId = 0

for i in range(N-1):
	fout.append(open(INPUT_FILE+'.'+str(i),'w'))

for line in fin:
	line = line.strip()

	if not line:
		for i in range(treeId % N):
			t = '\n'.join(tree)+'\n\n'
			fout[i].write(t)

		tree = []
		treeId += 1
	else:
		tree.append(line)
