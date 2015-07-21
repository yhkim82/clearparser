**Clear semantic role labeler** takes dependency trees as input and labels semantic roles on a headword of each argument of predicates.

## Training ##

```
java clear.engine.SRLTrain -c <configuration file> -t <feature template file> -i <train file> -m <model file> [-n <bootstrapping level>]
java -XX:+UseConcMarkSweepGC -Xmx4g clear.engine.SRLTrain -c config/config_srl_en.xml -t config/feature_srl_en_conll09.xml -i dat/wsj_0001.parse.srl -m dat/sample.mod
```

  * <**configuration file**>: name of a file specifying [configurations](ConfigGuidelines.md) ([config/config\_srl\_en.xml](http://code.google.com/p/clearparser/source/browse/trunk/config/config_srl_en.xml))
  * <**feature template file**>: name of a file containing [feature templates](FeatureGuidelines.md) ([config/feature\_srl\_en\_conll09.xml](http://code.google.com/p/clearparser/source/browse/trunk/config/feature_srl_en_conll09.xml)).
  * <**train file**>: name of a file containing dependency trees with semantic roles in [srl](DataFormat.md) format ([dat/wsj\_0001.parse.srl](http://code.google.com/p/clearparser/source/browse/trunk/dat/wsj_0001.parse.srl)) for training.
  * <**model file**>: name of a file to contain a trained model.
  * <**bootstrapping level**>: number of iterations for bootstrapping (default = 1)

After running the command, it will generate _n+1_ models, where _n_ is the bootstrapping level: <model file> is a model trained without using bootstrapping and <model file>.boot\_n is a model generated from the _n_'th level of bootstrapping.

## Decoding ##

```
java clear.engine.SRLPredict -c <configuration file> -m <model file> -i <input file> -o <output file>
java -XX:+UseConcMarkSweepGC -Xmx1g clear.engine.SRLPredict -c config/config_srl_en.xml -m dat/sample.mod -i dat/wsj_0001.parse.srl -o dat/sample.out
```

  * <**configuration file**>: name of a file specifying [configurations](ConfigGuidelines.md) ([config/config\_srl\_en.xml](http://code.google.com/p/clearparser/source/browse/trunk/config/config_srl_en.xml))
  * <**model file**>: name of a file containing a trained model.  Pre-trained models can be downloaded from the [this](TrainedModels.md) page.
  * <**input file**>: name of a file containing dependency trees in either [raw, pos, dep, conll, or srl](DataFormat.md) format for decoding.
  * <**output file**>: name of a file containing system dependency trees in [srl](DataFormat.md) format ([dat/wsj\_0001.parse.srl](http://code.google.com/p/clearparser/source/browse/trunk/dat/wsj_0001.parse.srl)).

If you use our pre-trained models, we encourage you to use the corresponding part-of-speech tagging and dependency parsing models as well.  The part-of-speech and dependency parsing models can be specified in the configuration file ([config/config\_srl\_en.xml](http://code.google.com/p/clearparser/source/browse/trunk/config/config_srl_en.xml)).

## Evaluating ##

```
java clear.engine.SRLEvaluate -g <gold-standard file> -s <system file>
java clear.engine.SRLEvaluate -g dat/wsj_0001.parse.srl -s dat/sample.out
```

  * <**gold-standard file**>: name of a file containing gold-standard dependency trees with semantic roles in [srl](DataFormat.md) format.
  * <**system file**>: name of a file containing system dependency trees with semantic roles in [srl](DataFormat.md) format.