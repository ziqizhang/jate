# JATE
JATE (Java Automatic Term Extraction) is an open source library for automatic domain specific term extraction from text corpora. It is implemented within the [Apache Solr] framework (currently Solr 5.3), currently supporting more than 10 ATE algorithms, and almost any kinds of term candidate patterns. The seamless integration with Solr gives JATE potential to be easily customised and adapted to different documents (including binary), domains, and languages. 

For details of how to use JATE2, please follow the [wiki on the JATE website].

General introduction of JATE is also available in [JATE1.0 wiki].

**To cite JATE:**
Zhang, Z., Gao, J., Ciravegna, F. 2016. JATE 2.0: Java Automatic Term Extraction with Apache Solr. In _The Proceedings of the 10th Language Resources and Evaluation Conference_, May 2016, Portorož, Slovenia

**If you are after the old JATE 1.11 previously hosted on Google Code Project, you can download it from [here]. However the old version is no longer supported. To cite the old version, please use instead:** Zhang, Z., Iria, J., Brewster, C., and Ciravegna, F. 2008. A Comparative Evaluation of Term Recognition Algorithms. In Proceedings of The 6th Language Resources and Evaluation Conference, May 2008, Marrakech, Morocco. 

### Support and Issues

JATE2 is at an early stage of development. Its mailling list is available on [Google Groups] (https://groups.google.com/d/forum/jate2). If you have any **questions about using JATE**, please read the Wiki and the mailling list first. Please do not use the issue tracker for personal support requests nor feature requests. Support requests and development issues should be discussed in the mailling list.

For bug report, please [create an issue](https://github.com/ziqizhang/jate/issues). 

### Pull requests

We always welcome contributions to help make JATE better. Bug fixes, improvements, and new features are welcomed. Before embarking on making significant changes, please open an issue and ask first so that you do not risk duplicating efforts or spending time working on something that may be out of scope.

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

### Contact

For anything else, you can also find how to contact the team at:

 - [Jie GAO](http://staffwww.dcs.shef.ac.uk/people/J.Gao/)
 - [Ziqi ZHANG](http://staffwww.dcs.shef.ac.uk/people/Z.Zhang/)

### Changes Overview

 -  JATE2.0 Alpha version - 04 April 2016

 -  JATE2.0 Beta version - 20 May 2016



[//]: # (These are reference links used in the body of this note and get stripped out when the markdown processor does its job.)
   [Apache Solr]: <http://lucene.apache.org/solr/>
   [here]: <http://staffwww.dcs.shef.ac.uk/people/Z.Zhang/resources/jate_1.11.tar.gz>
   [JATE1.0 wiki]: <https://code.google.com/archive/p/jatetoolkit/wikis/JATEIntro.wiki>
   [wiki on the JATE website]: <https://github.com/ziqizhang/jate/wiki>
   [Fork the project]: <https://help.github.com/articles/fork-a-repo/>
  
