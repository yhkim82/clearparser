#!/usr/bin/python
import sys

FILE1 = sys.argv[1]
FILE2 = sys.argv[2]

def dep2pos(inputFile, outputFile):
	fin  = open(inputFile)
	fout = open(outputFile,'w')
	tree = []

	for line in fin:
		l = line.split()
	
		if not l:
			fout.write(' '.join(tree)+'\n')
			tree = []
		else:
			tree.append(l[1]+'_'+l[3])
		#	tree.append(l[1])

def dep2raw(inputFile, outputFile):
	fin  = open(inputFile)
	fout = open(outputFile,'w')
	tree = []

	for line in fin:
		l = line.split()
	
		if not l:
			fout.write(' '.join(tree)+'\n')
			tree = []
		else:
			tree.append(l[1])

def pos2dep(inputFile, outputFile):
	fin  = open(inputFile)
	fout = open(outputFile,'w')

	for line in fin:
		l = line.split()

		for item in l:
			fout.write(item.replace('_','\t')+'\n')
		
		fout.write('\n')


		

#dep2raw(FILE1, FILE2)
#pos2dep(FILE1, FILE2)
