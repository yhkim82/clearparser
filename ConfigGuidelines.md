The following shows a default configuration file for English ('[config/config\_dep\_en.xml](http://code.google.com/p/clearparser/source/browse/trunk/config/config_dep_en.xml)').

```
<configuration>
    <common>
        <language>en</language>
        <format>dep</format>
        <parser>shift-pop</parser>
    </common>
    <classify>
        <algorithm name="lib" l="1" c="0.1" e="0.1" b="-1"/>
        <threads>2</threads>
    </classify>
    <predict>
        <tok_model>model/en_tok_opennlp.jar</tok_model>
        <pos_model>model/en_pos_ontonotes.jar</pos_model>
        <morph_dict>lib/en_dict-1.0.jar</morph_dict>
    </predict>
</configuration>
```

<**configuration**> is the top element that consists of 3 elements: <**common**>, <**train**>, and <**predict**>.
<br>

<h2><<b>common</b>></h2>

The <<b>common</b>> element is required for both training and decoding.  It consists of 2 required elements (<b>language</b>, <b>format</b>) and 1 optional element (<b>parser</b>).<br>
<br>
<ul><li><<b>language</b>> ::= cz | en | kr<br>
<ul><li>Specifies a language (cz: Czech, en: English, kr: Korean)<br>
</li></ul></li><li><<b>format</b>> ::= raw | pos | dep | conll | srl<br>
<ul><li>Specifies a <a href='DataFormat.md'>data format</a>
</li></ul></li><li><<b>parser</b>> ::= shift-eager | shift-pop<br>
<ul><li>Specifies a dependency parsing algorithm (required only for training a dependency parsing)</li></ul></li></ul>

<h2><<b>classify</b>></h2>

The <<b>classify</b>> element is required for training.  It consists of 2 required element (<b>algorithm</b>, <b>parser</b>) and 1 optional element (<b>threads</b>).<br>
<br>
<ul><li><<b>algorithm</b>> specifies a learning algorithm.  It requires an attribute <b>name</b>.<br>
<ul><li><b>name</b> ::= lib (LibLinear L2-SVM; default) | rrm (Robust Risk Minimization)<br>
</li><li><b>l</b> ::= lib: 1 (L1-loss; default) | 2 (L2-loss)<br>
</li><li><b>c</b> ::= lib: penalty (default = 0.1), rrm: regularization (default = 0.1)<br>
</li><li><b>e</b> ::= lib: termination criterion (default = 0.1), rrm: learning rate (default = 0.001)<br>
</li><li><b>b</b> ::= lib: bias (default = -1)<br>
</li><li><b>k</b> ::= rrm: max # of iterations (default = 40)<br>
</li><li><b>m</b> ::= rrm: initial weights (default = 1.0)<br>
</li></ul></li><li><<b>threads</b>> ::= # of threads to be used during training (default = 2)</li></ul>

<h2><<b>predict</b>></h2>

The <<b>predict</b>> element is required for decoding.  It consists of 4 elements (<b>tok_model</b>, <b>pos_model</b>, <b>dep_model</b>, <b>morph_dict</b>).<br>
<br>
<ul><li><<b>tok_model</b>> ::= absolute or relative path<br>
<ul><li>Specifies a model file (<a href='https://clearparser.googlecode.com/svn/trunk/model/en_tok_opennlp.jar'>model/en_tok_opennlp.jar</a>) for the OpenNLP tokenizer (required for the <a href='DataFormat.md'>raw</a> format).<br>
</li></ul></li><li><<b>pos_model</b>> ::= absolute or relative path<br>
<ul><li>Specifies a model file (<a href='http://verbs.colorado.edu/~choijd/clearparser/models/en_pos_ontonotes.jar'>model/en_pos_ontonotes.jar</a>) for the OpenNLP part-of-speech tagger (required for the <a href='DataFormat.md'>raw</a> format).<br>
</li></ul></li><li><<b>dep_model</b>> ::= absolute or relative path<br>
<ul><li>Specifies a model file (<a href='http://verbs.colorado.edu/~choijd/clearparser/models/en_dep_ontonotes.jar'>model/en_dep_ontonotes.jar</a>) for the <a href='DepParserGuidelines.md'>Clear dependency parser</a> (required for the <a href='DataFormat.md'>raw, pos</a> format).<br>
</li><li>This element is required only for semantic role labeling.<br>
</li></ul></li><li><<b>morph_dict</b>> ::= absolute or relative path<br>
<ul><li>Specifies a dictionary file (<a href='http://clearparser.googlecode.com/svn/trunk/model/en_dict-1.0.jar'>model/en_dict-1.0.jar</a>) for the <a href='MorphAnalyzerGuidelines.md'>Clear morphological analyzer</a> (required for the <a href='DataFormat.md'>raw, pos</a> format).