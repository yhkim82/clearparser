#!/usr/bin/python
import sys
import glob

DIR = sys.argv[1]
EXT = sys.argv[2]
N   = int(sys.argv[3])

filelist = glob.glob(DIR+'/*.'+EXT)
filelist.sort()

for filename in filelist:
	fin  = open(filename)
	fout = open(filename+'.'+str(N),'w')
	tree = []
	tc = 0
	wc = 0
	
	for line in fin:
		line = line.strip()

		if not line:
			if len(tree) >= N:
				fout.write('\n'.join(tree)+'\n\n')
				tc += 1

			wc += len(tree)
			tree = []
		else:
			tree.append(line)

	print filename, tc, wc
