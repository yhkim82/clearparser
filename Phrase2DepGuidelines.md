# Clear penn-to-dependency converter #

**Clear penn-to-dependency converter** takes [Penn Treebank format](http://www.cis.upenn.edu/~treebank/) phrase structure trees and generates [CoNLL style](http://ilk.uvt.nl/conll/#dataformat) dependency trees.  The converter allows users to customize head-percolation rules.  It currently supports English and Korean.

## How to run ##

On the terminal, type the following command.

```
java clear.engine.PennToDep -i <input file> -o <output file> -h <headrule file> [-m <dictionary-file> -n <minimum sentence length> -l <language> -f -e]
java clear.engine.PennToDep -i dat/sample.phrase -o dat/sample.dep -h config/headrule_en_ontonotes.txt -m model/en_dict-1.0.jar -n 2 -l en -f
```

  * **input file** (required) - name of a file containing phrase structure trees ([dat/sample.phrase](http://code.google.com/p/clearparser/source/browse/trunk/dat/sample.phrase)).
  * **output file** (required) - name of a file containing dependency trees converted from the input file ([dat/sample.dep](http://code.google.com/p/clearparser/source/browse/trunk/dat/sample.dep)).
  * **headrule file** (required) - name of a file containing head-percolation rules ([config/headrule\_en\_ontonotes.txt](http://code.google.com/p/clearparser/source/browse/trunk/config/headrule_en_ontonotes.txt)).
  * **dictionary file** (optional) - name of a dictionary file for [Clear morphological analyzer](MorphAnalyzerGuidelines.md) ([model/en\_dict-1.0.jar](http://clearparser.googlecode.com/svn/trunk/model/en_dict-1.0.jar)).
  * **minimum sentence length** (optional) - minimum number of word-tokens in each sentence (inclusive, default = 1).
  * **language** (optional) - en: English (default), kr: Korean.
  * **-f** (optional) - if set, use function tags as dependency labels.
  * **-e** (optional) - if set, include empty categories.

## Input file ##

**Input file** contains [Penn Treebank style](http://www.cis.upenn.edu/~treebank/) phrase structure trees ([dat/sample.phrase](http://code.google.com/p/clearparser/source/browse/trunk/dat/sample.phrase)).  Each tree must start with either an empty phrase (example 1) or a `TOP`-phrase (example 2).

  * Example 1
```
((S (NP-SBJ (NP (DT The)
                    (ADJP (RBS most)
                          (JJ important))
                    (NN thing))
                (PP (IN about)
                    (NP (NNP Disney))))
        (VP (VBZ is)
            (SBAR-PRD (IN that)
                      (S (NP-SBJ (PRP it))
                         (VP (VBZ is)
                             (NP-PRD (DT a)
                                     (JJ global)
                                     (NN brand))))))
        (. .)))
```

  * Example 2
```
(TOP (S (NP-SBJ (NP (DT The)
                    (NN world)
                    (POS 's))
                (JJ fifth)
                (NNP Disney)
                (NN park))
        (VP (MD will)
            (ADVP-TMP (RB soon))
            (VP (VB open)
                (PP-CLR (IN to)
                        (NP (DT the)
                            (NN public)))
                (ADVP-LOC (RB here))))
        (. .)))
```

## Output file ##

**Output file** contains CoNLL style dependency trees in [dep format](DataFormat.md) ([dat/sample.dep](http://code.google.com/p/clearparser/source/browse/trunk/dat/sample.dep)).  Each tree is delimited by a blank line.

```
1     The          the          DT     _    4     NMOD
2     most         most         RBS    _    3     AMOD
3     important    important    JJ     _    4     NMOD
4     thing        thing        NN     _    7     SBJ
5     about        about        IN     _    4     NMOD
6     Disney       disney       NNP    _    5     PMOD
7     is           be           VBZ    _    0     ROOT
8     that         that         IN     _    7     PRD
9     it           it           PRP    _    10    SBJ
10    is           be           VBZ    _    8     SUB
11    a            a            DT     _    13    NMOD
12    global       global       JJ     _    13    NMOD
13    brand        brand        NN     _    10    PRD
14    .            .            .      _    7     P
```

## Headrule file ##

**Headrule file** contains head-percolation rules for all phrases/clauses.  Each line consists of rules for a phrase/clause.  The following shows a format of each line.

```
<pos-tag><tab><direction=l|r><tab><rule>(;<rule>)*
```
  * **pos-tag**: phrasal or clausal level [part-of-speech tag](http://bulba.sdsu.edu/jeanette/thesis/PennTags.html)
  * **direction**: `l` - find the leftmost head, `r` - find the rightmost head
  * **rule**: pos-tag/function-tag rules in [Java regular expression format](http://download.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html)

Here are sample rules for noun-phrases (`NP`) and verb-phrases (`VP`).

```
NP  r  NN.*|NML;NX;PRP;FW;CD;NP|QP|JJ.*|VB.*;ADJP;S;SBAR;.*
VP  l  TO;MD;VB.*;VP;-PRD;NN;NNS;NP;QP;JJ.*;ADJP;.*
```

  * For `NP`, it first looks for the rightmost '`NN.*|NML'`: the rule covers `NN`, `NNS`, `NNP`, `NNPS`, and `NML`.  If such a node exists, take it as the head of the phrase; if such a node doesn't exist, it searches for the next node (`NX`).
  * '`-`' is used to indicate a function tag.  For `VP`, after searching for `TO;MD;VB.*;VP`, it looks for any node with a function tag '`PRD`'.  The [link](http://groups.inf.ed.ac.uk/switchboard/subcattags.html) shows the list of function tags used for Penn Treebank.  A function tag can only be specified as a single unit (e.g., '`-PRD|SBJ`' or '`-PRD|-SBJ`' is not allowed).

For `S|VP`, whose child forms a gap-relation ('='), the following headrule is used instead.

```
GAP  l  MD;VB.*;VP;-SBJ;-TPC;-PRD
```