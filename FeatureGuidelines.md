**This page is out-of-date, will be updated shortly.**

ClearParser requires a feature template file in XML.  The following shows a sample feature template (it is not the default feature template).


Currently allows upto 3-gram features.
l = l0 = l+0 = l-0
l1 = l+1
visible="false" -> false, everything else is true
if no visible -> true

```
<feature_template>
    <unigram field="f"  node="l0"/>
    <unigram field="m"  node="l0"/>
    <unigram field="p"  node="b0"/>
    <unigram field="d"  node="b0"/>
    <unigram field="f"  node="l-1"/>
    <unigram field="f"  node="b-2"/>
    <unigram field="f"  node="l1"/>
    <unigram field="f"  node="b2"/>
    <unigram field="f"  node="l0_hd"/>
    <unigram field="d"  node="l0_rm"/>
    <unigram field="d"  node="b0_lm"/>
    <unigram field="pm" node="l0"/>
		
    <bigram field="pp" node0="l0" node1="b0"></bigram>
    <bigram field="pm" node0="l0" node1="b0"></bigram>
    <bigram field="mp" node0="l0" node1="b0"></bigram>
    <bigram field="mm" node0="l0" node1="b0"></bigram>

    <trigram field="ppp" node0="l0_lm" node1="l0" node2="b0"></trigram>
    <trigram field="ppp" node0="b0"    node1="b1" node2="b2"></trigram>
</feature_template>
```

<**feature\_template**> is the top element that consists of 3 kinds of elements: <**unigram**>, <**bigram**>, and <**trigram**>.
<br><br>

<h2><<b>cutoff</b>></h2>

The <<b>cutoff</b>> element consist of 3 optional attributes: <i>label</i>, <i>ngram</i>, and <i>extra</i>.  The <i>label</i> and <i>ngram</i> attributes take integer values indicating cutoffs for class labels and n-gram features.  For example, if <i>label="2"</i>, only classes whose occurrences are greater than 2 are trained.  Similarly, if <i>ngram="4"</i>, only n-gram features whose occurrences are greater than 4 are trained.<br>
<br>
The <i>extra</i> attribute can take either integer values delimited by commas or a string value of 'ngram'.  For example, <i>extra="2,3"</i> indicates that the 1st and 2nd extra features use cutoffs of 2 and 3, respectively (extra features are system-specific).  If <i>extra="ngram"</i>, the n-gram cutoff is used for all extra features.<br>
<br>
When there is no cutoff specified for any attribute, the default value of 0 is assigned.<br>
<br>
<h2><<b>unigram</b>></h2>

The <<b>unigram</b>> element consists of 2 required attributes.<br>
<br>
<ul><li>attribute.<b>field</b> ::= f | m | p | d | pm<br>
<ul><li>f: word form<br>
</li><li>m: lemma<br>
</li><li>p: part-of-speech tag<br>
</li><li>d: dependency label<br>
</li><li>pm: joined feature of (POS tag, lemma)<br>
</li></ul></li><li>attribute.<b>node</b> ::= l# | b# | l#<code>_</code>(hd | lm | rm) | b#<code>_</code>(hd | lm | rm)<br>
<ul><li>'l' indicates the last token in <i>lambda<sub>1</sub></i>, and 'b' indicates the first token in <i>beta</i>.<br>
</li><li># indicates an offset from the token (e.g., 'l0' is the last token in <i>lambda<sub>1</sub></i>, 'b0' is the first token in <i>beta</i>, 'l-1' is the previous token of 'l0', and 'b1' is the next token of 'b0')<br>
</li><li><code>_</code>(hd | lm | rm) is optional and indicates the <i>head</i> | <i>leftmost dependent</i> | <i>rightmost dependent</i> of the token (e.g., 'l0<code>_</code>hd' is the head of 'l0')</li></ul></li></ul>

<h2><<b>bigram</b>></h2>

The <<b>bigram</b>> element consists of 3 required attributes.<br>
<br>
<ul><li>attribute.<b>field</b> ::= pp | pm | mp | mm<br>
<ul><li>pp: joined feature of (POS tag of node0, POS tag of node1)<br>
</li><li>pm: joined feature of (POS tag of node0, lemma of node1)<br>
</li><li>mp: joined feature of (lemma of node0, POS tag of node1)<br>
</li><li>mm: joined feature of (lemma of node0, lemma of node1)<br>
</li></ul></li><li>attribute.<b>node0</b> ::= same as attribute.<b>node</b>
<ul><li>left node<br>
</li></ul></li><li>attribute.<b>node1</b> ::= same as attribute.<b>node</b>
<ul><li>right node</li></ul></li></ul>

<h2><<b>trigram</b>></h2>

The <<b>trigram</b>> element consists of 4 required attributes.<br>
<br>
<ul><li>attribute.<b>field</b> ::= ppp<br>
<ul><li>ppp: joined feature of (POS tag of node0, POS tag of node1, POS tag of node2)<br>
</li></ul></li><li>attribute.<b>node0</b> ::= same as attribute.<b>node</b>
<ul><li>left node<br>
</li></ul></li><li>attribute.<b>node1</b> ::= same as attribute.<b>node</b>
<ul><li>middle node<br>
</li></ul></li><li>attribute.<b>node2</b> ::= same as attribute.<b>node</b>
<ul><li>right node