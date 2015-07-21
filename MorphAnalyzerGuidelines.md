**Clear morphological analyzer** takes a pair of word-form and its part-of-speech tag, and generates the lemma of the word-form.  It is a dictionary-based analyzer developed on top of the [WordNet](http://wordnet.princeton.edu/man/morphy.7WN.html) morphy.  Besides the WordNet rules, it finds lemmas for some abbreviations (& -> and, 're -> be), generalizes ordinals (21st -> $#ORD#$), and shortens all numbers (1900s -> 0s, 3.14 -> 0.0).  These generalized lemmas found to be useful for some NLP tasks (e.g., dependency parsing).  Currently, Clear morphological analyzer supports only English, but more language supports are expected soon.
<br>

<h2>How to run Clear morphological analyzer</h2>

<pre><code>java clear.engine.MorphAnalyze -i &lt;input file&gt; -o &lt;output file&gt; -d &lt;dictionary jar-file&gt;<br>
java clear.engine.MorphAnalyze -i dat/morph.in -o dat/morph.out -d model/en_dict-1.0.jar<br>
</code></pre>

<h2>Input file</h2>

<ul><li>The input file needs to be in the <a href='DataFormat.md'>part-of-speech</a> format with or without blank lines.  A sample input file can be found from '<a href='http://clearparser.googlecode.com/svn/trunk/dat/morph.in'>dat/morph.in</a>'.</li></ul>

<h2>Output file</h2>

<ul><li>The output file adds one more column for lemmas to the input file, delimited by a tab character.  A sample output file can be found from '<a href='http://clearparser.googlecode.com/svn/trunk/dat/morph.out'>dat/morph.out</a>'.</li></ul>

<h2>Dictionary file</h2>

<ul><li>English dictionary file can be found from '<a href='http://clearparser.googlecode.com/svn/trunk/model/en_dict-1.0.jar'>model/en_dict-1.0.jar</a>'.