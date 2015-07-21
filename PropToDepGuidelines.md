# Clear prop-to-dependency converter #

**Clear prop-to-dependency converter** takes PropBank instances and generates [CoNLL style](http://ufal.mff.cuni.cz/conll2009-st/task-description.html) dependency trees with semantic roles. The converter uses the [Clear penn-to-dependency converter](Phrase2DepGuidelines.md) for the dependency conversion.  It currently supports Arabic, Chinese, and English.

## How to run ##

On the terminal, type the following command.

```
java clear.engine.PropToDep -i <input file> -o <output directory> -p <parse directory> -h <headrule file> [-m <dictionary-file> -n <minimum sentence length> -f -e -j]
java clear.engine.PropToDep -i dat/sample.prop -o dat/ -p dat/ -h config/headrule_en_ontonotes.txt -m model/en_dict-1.0.jar -n 2 -f
```

  * **input file** (required) - name of a file containing PropBank instances ([dat/sample.prop](http://code.google.com/p/clearparser/source/browse/trunk/dat/sample.prop)).
  * **output directory** (required) - name of a directory containing dependency output ([dat/](http://code.google.com/p/clearparser/source/browse/trunk/dat/)).
  * **parse directory** (required) - name of a directory containing parse trees ([dat/](http://code.google.com/p/clearparser/source/browse/trunk/dat)).
  * **headrule file** (required) - name of a file containing head-percolation rules ([config/headrule\_en\_ontonotes.txt](http://code.google.com/p/clearparser/source/browse/trunk/config/headrule_en_ontonotes.txt)).
  * **dictionary file** (optional) - name of a dictionary file for [Clear morphological analyzer](MorphAnalyzerGuidelines.md) ([model/en\_dict-1.0.jar](http://clearparser.googlecode.com/svn/trunk/model/en_dict-1.0.jar)).
  * **minimum sentence length** (optional) - minimum number of word-tokens in each sentence (inclusive, default = 1).
  * **-f** (optional) - if set, use function tags as dependency labels.
  * **-e** (optional) - if set, include empty categories.
  * **-j** (optional) - if set, consider adjectival predicates as verbal predicates.

## Input file ##

**Input file** contains PropBank instances ([dat/sample.prop](http://code.google.com/p/clearparser/source/browse/trunk/dat/sample.prop)).  Each PropBank instance follows the following format.

```
<parse file> <tree ID> <predicate ID> <annotator ID> <lemma-pos> <frameset ID> <aspects>( <argument>)+
<argument> ::= <terminal ID>:<height>-<label>

wsj_0001.parse 0 8 gold join-v join.01 ----- 8:0-rel 0:2-ARG0 7:0-ARGM-MOD 9:1-ARG1 11:1-ARGM-PRD 15:1-ARGM-TMP
```

  * **parse file** - path to the parse file.
  * **tree ID** - index of the tree containing the predicate (starts with 0, indicating the 1st tree in **parse file**).
  * **predicate ID** - terminal ID of the predicate (starts with 0; 8 indicates the 9th terminal node of the tree).
  * **annotator ID** - ID of the annotator worked on this instance.
  * **lemma** - lemma of the predicate.
  * **pos** - part-of-speech tag of the predicate (a: adjective, n: noun, v: verb).
  * **frameset ID** - sense ID of the predicate.
  * **aspects** - no longer used.
  * **argument**
    * **terminal ID** - terminal ID of the first argument node.
    * **height** - height of the argument phrase from the terminal node.
    * **label** - PropBank label
    * "9:1-ARG1" of the 1st tree in [dat/wsj\_0001.parse](http://code.google.com/p/clearparser/source/browse/trunk/dat/wsj_0001.parse) represents (NP (DT the) (NN board)) because (DT the) is the 10th terminal node and NP is the phrase with a height 1 from the terminal node.

## Output file ##

**Output file** contains dependency trees with semantic roles in [srl format](DataFormat.md) ([dat/wsj\_0001.parse.srl](http://code.google.com/p/clearparser/source/browse/trunk/dat/wsj_0001.parse.srl)).

```
1     Pierre          pierre          NNP    _    2     NMOD    _          _
2     Vinken          vinken          NNP    _    8     SBJ     _          9:A0
3     ,               ,               ,      _    2     P       _          _
4     61              0               CD     _    5     NMOD    _          _
5     years           year            NNS    _    6     AMOD    _          _
6     old             old             JJ     _    2     NMOD    _          _
7     ,               ,               ,      _    2     P       _          _
8     will            will            MD     _    0     ROOT    _          9:AM-MOD
9     join            join            VB     _    8     VC      join.01    _
10    the             the             DT     _    11    NMOD    _          _
11    board           board           NN     _    9     OBJ     _          9:A1
12    as              as              IN     _    9     ADV     _          9:AM-PRD
13    a               a               DT     _    15    NMOD    _          _
14    nonexecutive    nonexecutive    JJ     _    15    NMOD    _          _
15    director        director        NN     _    12    PMOD    _          _
16    Nov.            nov.            NNP    _    9     TMP     _          9:AM-TMP
17    29              0               CD     _    16    NMOD    _          _
18   .                .               .      _    8     P       _          _
```

## Headrule file ##

**Headrule file** contains head-percolation rules for all phrases/clauses.  See [penn-to-dependency converter guidelines](Phrase2DepGuidelines.md) for more details.