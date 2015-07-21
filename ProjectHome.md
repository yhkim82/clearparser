# <font color='red'>Important Notice</font> #

The ClearParser project has moved to a bigger project called [ClearNLP](https://clearnlp.googlecode.com).  Here are some of new features in ClearNLP.

  * Constituent-to-dependency converter supports the Stanford dependency format.
  * Built-in tokenizer and sentence segmenter for English.
  * Built-in part-of-speech tagger.
  * Improved morphological analyzer for English.
  * Improved dependency parser.
  * Improved semantic role labeler (_coming soon_).
  * Built-in named entity recognizer (_coming soon_).
  * LibLinear for NLP applications.
  * And more!

---


This project provides several NLP tools such as a dependency parser, a semantic role labeler, a penn-to-dependency converter, a prop-to-dependency converter, and a morphological analyzer. All tools are written in Java and developed by the Computational Language and EducAtion Research ([CLEAR](http://clear.colorado.edu)) group at the University of Colorado at Boulder.

## Installation ##

  1. Download the [ClearParser system file](http://clearparser.googlecode.com/files/clearparser-sys.tar.gz), and uncompress it.
    * `tar -zxvf clearparser-sys.tar.gz`
  1. Download the latest version of the [ClearParser jar-file](http://clearparser.googlecode.com/files/clearparser-0.31.jar) and put it under the 'lib' directory.
    * `cp clearparser-version.jar lib/`
  1. Add all jar files in '[lib/](http://code.google.com/p/clearparser/source/browse/#svn%2Ftrunk%2Flib)' to your classpath.  If you are using the Bash shell, it can be something like this.
```
CLEAR_PATH="path of the [clearparser] directory"
export CLASSPATH=$CLEAR_PATH/lib/opennlp-maxent-3.0.1-incubating.jar:$CLEAR_PATH/lib/opennlp-tools-1.5.1.jar:$CLEAR_PATH/lib/args4j-2.0.12.jar:$CLEAR_PATH/lib/commons-compress-1.1.jar:$CLEAR_PATH/lib/hppc-0.4.0.jar:$CLEAR_PATH/lib/clearparser-version.jar:.
```

## Clear Dependency Parser ##

Clear dependency parser uses two transition-based dependency parsing algorithms, shift-eager algorithm (Choi & Nicolov, 2009) and shift-pop algorithm (default algorithm; Choi & Palmer, 2011a).  These algorithms show near state-of-the-art performance in both speed and accuracy.
  * [Guidelines](DepParserGuidelines.md)


## Clear Semantic Role Labeler ##

Clear semantic role labeler is a dependency-based SRL system.  The labeler uses a transition-based SRL algorithm and shows near state-of-the-art performance in accuracy  (Choi & Palmer, 2011b).
  * [Guidelines](SRLabelerGuidelines.md)

## Clear Penn-to-Dependency Converter ##

Clear penn-to-dependency converter takes Penn Treebank style phrase structure trees and generates CoNLL style dependency trees.  The converter is up-to-date for the new Treebank guidelines (as in 2011), and shows robust results across different corpora (Choi & Palmer 2010).
  * [Guidelines](Phrase2DepGuidelines.md)

## Clear Prop-to-Dependency Converter ##

Clear prop-to-dependency converter takes PropBank instances and generates CoNLL style dependency trees with semantic roles.  The converter is up-to-date for the new PropBank guidelines (as in 2011).
  * [Guidelines](PropToDepGuidelines.md)

## Clear Morphological Analyzer ##

Clear morphological analyzer takes a pair of word-form and its part-of-speech tag, and generates the lemma of the word-form.  It is a dictionary-based analyzer developed on top of the [WordNet](http://wordnet.princeton.edu/man/morphy.7WN.html) morphy.
  * [Guidelines](MorphAnalyzerGuidelines.md)

## References ##

  * K-best, Locally Pruned, Transition-based Dependency Parsing Using Robust Risk Minimization, Jinho D. Choi, Nicolas Nicolov, Collections of Recent Advances in Natural Language Processing V, 205-216, John Benjamins, Amsterdam & Philadelphia, 2009

  * Robust Constituent-to-Dependency Conversion for English, Jinho D. Choi, Martha Palmer, Proceedings of the 9th International Workshop on Treebanks and Linguistic Theories (TLT'9), 55-66, Tartu, Estonia, 2010

  * Getting the Most out of Transition-based Dependency Parsing, Jinho D. Choi, Martha Palmer, Proceedings of the 49th Annual Meeting of the Association for Computational Linguistics: Human Language Technologies (ACL:HLT'11), 687-692, Portland, Oregon, 2011a

  * Transition-based Semantic Role Labeling Using Predicate Argument Clustering, Jinho D. Choi, Martha Palmer, Proceedings of ACL workshop on Relational Models of Semantics (RELMS'11), 37-45, Portland, Oregon, 2011b