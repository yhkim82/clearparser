#!/usr/bin/python
import sys
import glob

DIR = sys.argv[1]

g_trn_pos   = 0
g_trn_lemma = 0
g_trn_total = 0

g_tst_pos   = 0
g_tst_lemma = 0
g_tst_total = 0


filenames = glob.glob(DIR+'/*.mrg')
filenames.sort()

for filename in filenames:
	fin1 = open(filename)
	filename = filename[:filename.rfind('.')]
	fin2 = open(filename+'.srl')
	
	l_pos   = 0
	l_lemma = 0
	l_total = 0
	
	for line in fin1:
		l1 = line.split()
		l2 = fin2.readline().split()
		if not l1: continue
		
		if l1[2] != l2[2] or l1[4] != l2[3]:
			print 'Not gold annotations'
			sys.exit(1)
		
		if l1[2] == l1[3]: l_lemma += 1
		if l1[4] == l1[5]: l_pos   += 1
		l_total += 1
	
	print filename
	print 'pos  :', 100.0 * l_pos   / l_total
	print 'lemma:', 100.0 * l_lemma / l_total
	print '---------------------'

	if filename.endswith('trn'):
		g_trn_pos   += l_pos
		g_trn_lemma += l_lemma
		g_trn_total += l_total
	else:
		g_tst_pos   += l_pos
		g_tst_lemma += l_lemma
		g_tst_total += l_total

print 'average-trn'
print 'pos  :', 100.0 * g_trn_pos   / g_trn_total
print 'lemma:', 100.0 * g_trn_lemma / g_trn_total
print '---------------------------'
print 'average-tst'
print 'pos  :', 100.0 * g_tst_pos   / g_tst_total
print 'lemma:', 100.0 * g_tst_lemma / g_tst_total
