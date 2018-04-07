# NEWS
**07 Apr 2018:** JATE 2.0 Beta.10 released. The main changes include: 1) migration to Solr 7.2.1. **WARNING**: the index files created by this version of Solr is not compatible with the previous versions; 2) fixing a couple of minor bugs documented in the Issues page; 3) added two more example configrations for the [TTC](https://github.com/ziqizhang/data#ate) corpora; 4) improved introduction page.

**02 Apr 2018:** JATE 2.0 Beta.9 released. The main change is migration to Solr 6.6.0 (thanks to [MysterionRise]) - JATE is now based on Solr 6.6.0. **WARNING**: the index files created by this version of Solr is not compatible with the previous versions. Please consider this before upgrading! 


  * [Introduction](#intro)
  * [Cite JATE](#cite)
  * [Reasons for using JATE](#why)
  * [Support](#support)
  * [Contributing](#contrib)
    - [Tell us your use case with JATE](#story)
    - [Collaboration](#collaboration)
    - [Code contribution](#code)
    - [Data contribution](#datac)
  * [Other downloads](#other)
  * [License](#license)
  * [Contact](#contact)
  * [Release history](#history)

# <a name="intro"> Introduction
JATE (Java Automatic Term Extraction) is an open source library for Automatic Term Extraction (or Recognition) from text corpora. It is implemented within the [Apache Solr] framework (currently Solr 7.2.1), currently supporting more than 10 ATE algorithms, and almost any kinds of term candidate patterns. The integration with Solr gives JATE potential to be easily customised and adapted to different document formats, domains, and languages. 

JATE is **not just a library for ATE**. It also implements several text processing utilities that can be easily used for other general-purpose indexing, such as tokenisation, advanced phrase and n-gram extraction. See [Reasons for using JATE](#why)


# <a name="cite"> Cite JATE
Please support us by citing JATE as below:

**If you use the version from this Git repository:** Zhang, Z., Gao, J., Ciravegna, F. 2016. JATE 2.0: Java Automatic Term Extraction with Apache Solr. In _The Proceedings of the 10th Language Resources and Evaluation Conference_, May 2016, Portorož, Slovenia

**If you use the old JATE 1.11** available [here] (no longer supported except an outdated [JATE 1.0 wiki page]): Zhang, Z., Iria, J., Brewster, C., and Ciravegna, F. 2008. A Comparative Evaluation of Term Recognition Algorithms. In Proceedings of The 6th Language Resources and Evaluation Conference, May 2008, Marrakech, Morocco. 


# <a name="why"> Reasons for using JATE
A wide range of ATE tools and libraries have been developed over the years. In comparison, there are **five reasons** why JATE is unique:

* [Free to use](https://github.com/ziqizhang/jate/blob/dev/LICENSE), for commercial or non-commercial purposes.
* Built on the Apache Solr framework to benefit from its [powerful text analysis libraries](https://lucene.apache.org/core/7_2_1/core/org/apache/lucene/analysis/package-summary.html#package.description), high compatibility and scalability, and rigorous community support. As examples, you can plug in the [Tika library](https://tika.apache.org/1.15/formats.html) to process different document formats, use different text preprocessing (e.g., character filtering, HTML entity conversion), tokenisation and normalisation methods available through Lucene, or [index your documents and boost your queries](https://github.com/ziqizhang/jate/wiki/Quick-start#plugin) with extracted terms easily thanks to its integration with Solr.
* Highly configurable [linguistic processors](https://github.com/ziqizhang/jate/wiki/Example-of-schema-setting-in-Solr-core) for candidate term extraction, such as noun phrases, PoS patterns, and n-grams.
* 10 state of the art [ATE scoring and ranking algorithms](https://github.com/ziqizhang/jate/tree/master/src/main/java/uk/ac/shef/dcs/jate/algorithm).
* A set of highly configurable, complex text processing utilities that can be used as Solr plugins for [general purpose text indexing and retrieval](https://github.com/ziqizhang/jate/tree/master/src/main/java/org/apache/lucene/analysis/jate). For example, sentence splitter, statistical tokeniser, lemmatiser, PoS tagger, phrase chunker and n-gram extractors that are sentence context aware and stopwords removable, etc.

For **terminology practitioners**, this means you can quickly build highly customisable ATE tools that suit your data and domain, at no cost. For **terminology researchers and developers**, this means that you have many necessary building blocks for developing novel ATE methods, and a uniform environment where you can evaluate and compare different methods. For **general information retrieval users**, you have a range of advanced text processing utilities that you can easily plug into your existing Solr or Lucene based indexing and retrieval applications. 


# <a name="support"> Support

JATE is currently maintained by a [team of two members](#contact), who have other full-time roles but use as much their spare time as possible on this work. We try our best to respond to your queries but we apologise for any potential delays for this reason. However there are many ways you can [contribute to JATE](#contrib) to potentially make it better. Currently you can obtain support from us in the following ways:

* A [wiki page] to get you started.
* A [Google Group] to ask questions about JATE. 
* An [issues] page to report bugs - only bug reporting please. For any questions please use the Google Group above.
* [Contact the team](#contact) directly - please use this **only** if your query does not fall into any of the above categories.

# <a name="contrib"> Contributing to JATE

JATE is a research software that originates from an [EPSRC](https://epsrc.ukri.org/) funded project '[Abraxas](http://gow.epsrc.ac.uk/NGBOViewGrant.aspx?GrantRef=GR/T22902/01)'. As you may appreciate, since the project termination, there is no more funding to support the software and therefore all subsequent development and its current maintenance have been undertaken voluntarily by the team. JATE is far from perfect and yet we are trilled to see it becoming one of the most popular free text mining tools in the community, thanks to your support. 1We are also keen to make it better and therefore, we would be grateful for your contributions in many forms:

### <a name="story"> Tell us your use case with JATE

We would be grateful if you tell us a little more of your use cases with JATE: are you using JATE to conduct cutting-edge research in another (or the same) subject area? Or are you using JATE to enable your business applications? By gathering as many detailed use cases as possible, you are helping us make a compelling case to apply for fundings from various institutions to support the development and maintenance of JATE. Please [get in touch with us by email](#contact) and share your story with us - it costs you no money but just a little of your time!

### <a name="collaboration"> Collaboration

We are keen to collaborate with any partners (academia or industry) to develop new project ideas. This can be, but not limited to, any of the following:

* further development of JATE, by adding new algorithms, text processing capacities, user friendly interface, support for other programming languages etc.
* integration with other, existing implementations of ATE methods, frameworks, or platforms.
* using ATE for downstream applications, such as ontology engineering, information retrieval etc.

Please [get in touch with us by email](#contact) to discuss your ideas.


### <a name="code"> Code contribution
We welcome bug fixes, improvements, new features etc. Before embarking on making significant changes, please open an issue and ask first so that you do not risk duplicating efforts or spending time working on something that may be out of scope. To contribute code, please follow:

###### 1. [Fork the project], clone your fork, and add the upstream to your remote:

```
$ git clone git@github.com:<your-username>/jate.git
$ cd jate
$ git remote add upstream https://github.com/ziqizhang/jate
```

###### 2. If you need to pull new changes committed upstream:

```
$ git checkout master
$ git fetch upstream
$ git merge upstream/master
```

###### 3. Create a feature branch for your fix or new feature:

```
$ git checkout -b <feature-branch-name>
```

###### 4. Please try to commit your changes in logical chunks and reference the issue number in your commit messages:

```
$ git commit -m "Issue #<issue-number> - <commit-message>"
```

###### 5. Push your feature branch to your fork.

```
$ git push origin <feature-branch-name>
```

###### 6. Open a Pull Request against the upstream master branch. Please give your pull request a clear title and description and note which issue(s) your pull request fixes.

**Important**: By submitting a patch, you agree to allow the project owners to license your work under the LGPLv3 license.


### <a name="datac"> Data contribution

A crucial resource for developing ATE methods is data, and particularly 'annotated' data that consists of text corpora as well as a list of expected 'real' terms to be found within the corpora. We call this 'gold standard'. This is critical for evaluating and improving the performance of ATE in particular domains.

If you would like to share any data you have created please also [get in touch by email](#contact). We will acknowledge your credits and share a download within the [Other downloads](#other) section, subject to your consent.



# <a name="other"> Other downloads

### Other versions of JATE (no longer supported)
This Git repository only hosts the most recent version of JATE. You can obtain some of the previous versions below: 

* JATE 1.11: download [here]
* Other JATE 2.0 based versions in the [Maven central repository](https://mvnrepository.com/artifact/uk.ac.shef.dcs/jate)

### Data
We share datasets used for the development and evaluation of ATE below.

* Ziqi Zhang's [research data page](https://github.com/ziqizhang/data#ate) contains 4 datasets used for ATE research. 


# <a name="license"> License

JATE is licensed with LGPL 3.0, which permits free commercial and non-commercial use. See [details](https://github.com/ziqizhang/jate/blob/master/LICENSE) here. 

# <a name="contact"> Contact

The team member's personal webpages contain their email contacts:

 - [Jie GAO](http://staffwww.dcs.shef.ac.uk/people/J.Gao/)
 - [Ziqi ZHANG](https://ziqizhang.github.com/)

# <a name="history"> JATE release history
*  JATE2.0 Beta.10 version - 7 Apr 2018
*  JATE2.0 Beta version - 20 May 2016
*  JATE2.0 Alpha version - 04 April 2016




[//]: # (These are reference links used in the body of this note and get stripped out when the markdown processor does its job.)
   [Apache Solr]: <http://lucene.apache.org/solr/>
   [here]: <https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/jatetoolkit/jate_1.11.zip>
   [JATE1.0 wiki]: <https://code.google.com/archive/p/jatetoolkit/wikis/JATEIntro.wiki>
   [wiki page]: <https://github.com/ziqizhang/jate/wiki>
   [Fork the project]: <https://help.github.com/articles/fork-a-repo/>
   [issue]: <https://github.com/ziqizhang/jate/issues/42>
   [email]: <mailto:ziqi.zhang@sheffield.ac.uk>
   [MysterionRise]: <https://github.com/MysterionRise>
   [Google Group]: <https://groups.google.com/d/forum/jate2>
   [JATE 1.0 wiki page]: <https://code.google.com/archive/p/jatetoolkit/wikis/JATEIntro.wiki>
   [issues]: <https://github.com/ziqizhang/jate/issues>
