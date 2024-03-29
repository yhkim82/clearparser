#!/usr/bin/python
import sys
fin  = open(sys.argv[1])
fout = open(sys.argv[2],'w')
vpos = sys.argv[3]

tree = []
pred = []

for line in fin:
	l = line.split()
	
	if not l:
		rTree = []
		for node in tree:
			args = []
			for i,arg in enumerate(node[8:]):
				if arg == '_': continue
				predId = pred[i]
				if not tree[predId][3].startswith(vpos): continue
				args.append(str(predId+1)+':'+arg)

			del node[8:]
			if args: node.append(';'.join(args))
			else   : node.append('_')
			rTree.append('\t'.join(node))

		fout.write('\n'.join(rTree)+'\n\n')
		tree = []
		pred = []
		continue

	node = []
	node.append(l[0])	# id
	node.append(l[1])	# form
	node.append(l[2])	# glemma
	node.append(l[4])	# gpos
	node.append(l[6])	# gfeat
	node.append(l[8])	# headId
	node.append(l[10])	# deprel
	node.extend(l[13:])	# roleset, arg*

	tree.append(node)
	if l[12] != '_': pred.append(int(l[0])-1)
