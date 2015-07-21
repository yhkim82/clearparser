ClearParser currently supports 5 data formats: raw, part-of-speech, dependency, CoNLL-X, and semantic role formats.

## Raw format (raw) ##

The raw format requires one sentence per line.

```
Pricing details weren't immediately available.
She bought a car.
```

## Part-of-speech format (pos) ##

The part-of-speech format requires 2 fields.

  * FORM: word form or punctuation symbol
  * POSTAG: fine-grained part-of-speech tag

Each field is delimited by a tab character (`'\t'`).  Each sentence is delimited by a blank line.  This format is only for decoding and requires to specify a dictionary for the lemmatizer (see [configuration guidelines](ConfigGuidelines.md) for more details).

```
Pricing        NN
details        NNS
were           VBD
n't            RB
immediately    RB
available      JJ
.              .

She            PRP
bought         VBD
a              DT
car            NN
.              .
```

## Dependency format (dep) ##

The dependency format requires 5 fields.

  * ID: token counter, starting at 1 for each new sentence
  * FORM: word form or punctuation symbol
  * LEMMA: lemma or stem of word form
  * POSTAG: fine-grained part-of-speech tag
  * FEATS: extra features ('`_`' indicates no extra feature)
  * HEAD: head ID of the current token
  * DEPREL: dependency relation to the HEAD

Each field is delimited by a tab character (`'\t'`).  Each sentence is delimited by a blank line.  The last two columns (HEAD, DEPREL) are optional for decoding.

```
1    Pricing        pricing        NN     _    2    NMOD
2    details        detail         NNS    _    3    SBJ
3    were           be             VBD    _    0    ROOT
4    n't            not            RB     _    3    ADV
5    immediately    immediately    RB     _    6    AMOD
6    available      available      JJ     _    3    PRD
7    .              .              .      _    3    P

1    She            she            PRP    _    2    SBJ
2    bought         buy            VBD    _    0    ROOT
3    a              a              DT     _    4    NMOD
4    car            car            NN     _    2    OBJ
5    .              .              .      _    2    P
```

## CoNLL-X format (conll) ##

Detailed descriptions about the CoNLL-X data format can be found from [here](http://nextens.uvt.nl/~conll/#dataformat).  Among all the fields, the parser requires values for

  * ID: token counter, starting at 1 for each new sentence
  * FORM: word form or punctuation symbol
  * LEMMA: lemma or stem of word form
  * POSTAG: fine-grained part-of-speech tag
  * FEATS: extra features ('`_`' indicates no extra feature)
  * HEAD: head ID of the current token
  * DEPREL: dependency relation to the HEAD

Each field is delimited by a tab character (`'\t'`).  Each sentence is delimited by a blank line.  The last two columns (HEAD, DEPREL) are optional for decoding.

```
1    Pricing        pricing        _    NN     _    2    NMOD
2    details        detail         _    NNS    _    3    SBJ
3    were           be             _    VBD    _    0    ROOT
4    n't            not            _    RB     _    3    ADV
5    immediately    immediately    _    RB     _    6    AMOD
6    available      available      _    JJ     _    3    PRD
7    .              .              _    .      _    3    P

1    She            she            _    PRP    _    2    SBJ
2    bought         buy            _    VBD    _    0    ROOT
3    a              a              _    DT     _    4    NMOD
4    car            car            _    NN     _    2    OBJ
5    .              .              _    .      _    2    P
```

## Semantic role format (srl) ##

The semantic role labeling format requires 7 fields.

  * ID: token counter, starting at 1 for each new sentence
  * FORM: word form or punctuation symbol
  * LEMMA: lemma or stem of word form
  * POSTAG: fine-grained part-of-speech tag
  * HEAD: head ID of the current token
  * DEPREL: dependency relation to the HEAD
  * ROLESET: roleset ID of the current token ('`_`' indicates that this token is not a predicate)
  * SHEADS: semantic heads with labels ('`_`' indicates that this token has no semantic head)

Each field is delimited by a tab character (`'\t'`).  Each sentence is delimited by a blank line.  The last column (SHEADS) is optional for decoding.  During decoding, the semantic role labeler considers only tokens whose ROLESET values are not '`_`' as predicates.  SHEADS takes the following format.

```
SHEAD  ::= headId:label
SHEADS ::= _ | SHEAD(;SHEAD)*
```

```
1     Mickey       mickey       NNP    _    2     NMOD    _            _
2     Mouse        mouse        NNP    _    5     NMOD    _            _
3     's           's           POS    _    2     NMOD    _            _
4     new          new          JJ     _    5     NMOD    _            _
5     home         home         NN     _    7     NMOD    _            7:A0;17:A0
6     ,            ,            ,      _    5     P       _            _
7     settling     settle       VBG    _    16    ADV     settle.03    17:AM-PRD
8     on           on           IN     _    7     ADV     _            7:A4
9     Chinese      chinese      JJ     _    10    NMOD    _            _
10    land         land         NN     _    8     PMOD    _            _
11    for          for          IN     _    7     TMP     _            7:AM-TMP
12    the          the          DT     _    14    NMOD    _            _
13    first        $#ORD#$      JJ     _    14    NMOD    _            _
14    time         time         NN     _    11    PMOD    _            _
15    ,            ,            ,      _    16    P       _            _
16    has          have         VBZ    _    0     ROOT    _            _
17    captured     capture      VBN    _    16    VC      capture.01   _
18    worldwide    worldwide    RB     _    19    NMOD    _            _
19    attention    attention    NN     _    17    OBJ     _            17:A1
20    .            .            .      _    16    P       _            _
```