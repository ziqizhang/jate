# NEWS
**02 Apr 2018:** JATE 2.0 Beta.9 released. The main change is migration to Solr 6.6.0 (thanks to [MysterionRise]) - JATE is now based on Solr 6.6.0. **WARNING**: the index files created by this version of Solr is not compatible with the previous versions. Please consider this before upgrading! 


  * [Introduction](#intro)
  * [Targeted Users](#users)
  * [Contributing](#contrib)
  * [Support](#support)
  * [License](#license)
  * [Contact](#contact)

# <a name="intro"> Introduction
JATE (Java Automatic Term Extraction) is an open source library for Automatic Term Extraction (or Recognition) from text corpora. It is implemented within the [Apache Solr] framework (currently Solr 6.6), currently supporting more than 10 ATE algorithms, and almost any kinds of term candidate patterns. The integration with Solr gives JATE potential to be easily customised and adapted to different documents (including binary), domains, and languages. 

JATE is **not just a library for ATE**. It also implements several text processing utilities that can be easily used for other general-purpose indexing, such as tokenisation, advanced phrase and n-gram extraction. See [Targeted Users](#users)

# <a name="users"> Targeted Users

### Term Extraction

For details of how to use JATE2, please follow the [wiki on the JATE website].

General introduction of JATE is also available in [JATE1.0 wiki].


# APPEAL FOR SUPPORT: PLEASE SHARE YOUR USE CASE OF JATE WITH US
```diff
- you can reach us at https://github.com/ziqizhang/jate/issues/42 or by email (link at the end of this message)
```
Dear visitors and users of JATE,

Over the last few years we are absolutely thrilled and grateful by your continued interest and support for JATE such as increasing downloads, citations, bug reports/fixing, and further development/customisation activities on GitHub. Thanks to you, we are seeing JATE become one of the most popular tools for Automatic Term Extraction and Text Mining in general. 

However, as you all know, JATE is far from perfect. Over the years, we have received numerous valuable suggestions on how to improve JATE and take it further. Some of these have been implemented, but **most have not**. As much enthusiastic and willing as you are to make these happen, we developers are a very small team and have to prioritise other jobs that we are employed for - **JATE is maintained free but we struggle finding spare time to provide quality maintenance of the tool, for which we apologise.**

**Therefore, we seek your support:** 
* **If you are using JATE for your research, business, or simply doing anything that you were not able to without it, please share your use case with us.** This will help us make a compelling case to apply for fundings from various institutions to support the development and maintenance of JATE.
* **If you have an innovative idea that can benefit from the usage or further development of JATE, we are keen to discuss it with you.** This will help us apply together for fundings to support activities that capitalise on innovation (due to the nature of these funding sources, innovation is the key).

Therefore, please support us by sharing your stories and ideas on this [issue] or by [email]. Your continued support is the only power to drive JATE further!

Thank you for reading this message.


# JATE
JATE (Java Automatic Term Extraction) is an open source library for automatic domain specific term extraction from text corpora. It is implemented within the [Apache Solr] framework (currently Solr 5.3), currently supporting more than 10 ATE algorithms, and almost any kinds of term candidate patterns. The seamless integration with Solr gives JATE potential to be easily customised and adapted to different documents (including binary), domains, and languages. 

For details of how to use JATE2, please follow the [wiki on the JATE website].

General introduction of JATE is also available in [JATE1.0 wiki].

**To cite JATE:**

Zhang, Z., Gao, J., Ciravegna, F. 2016. JATE 2.0: Java Automatic Term Extraction with Apache Solr. In _The Proceedings of the 10th Language Resources and Evaluation Conference_, May 2016, Portorož, Slovenia

**If you are after the old JATE 1.11 previously hosted on Google Code Project, you can download it from [here]. However the old version is no longer supported. 
To cite the old version, please use instead:** Zhang, Z., Iria, J., Brewster, C., and Ciravegna, F. 2008. A Comparative Evaluation of Term Recognition Algorithms. In Proceedings of The 6th Language Resources and Evaluation Conference, May 2008, Marrakech, Morocco. 


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
 - [Ziqi ZHANG] (https://ziqizhang.github.com/)

### Changes Overview

 -  JATE2.0 Alpha version - 04 April 2016

 -  JATE2.0 Beta version - 20 May 2016



[//]: # (These are reference links used in the body of this note and get stripped out when the markdown processor does its job.)
   [Apache Solr]: <http://lucene.apache.org/solr/>
   [here]: <https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/jatetoolkit/jate_1.11.zip>
   [JATE1.0 wiki]: <https://code.google.com/archive/p/jatetoolkit/wikis/JATEIntro.wiki>
   [wiki on the JATE website]: <https://github.com/ziqizhang/jate/wiki>
   [Fork the project]: <https://help.github.com/articles/fork-a-repo/>
   [issue]: <https://github.com/ziqizhang/jate/issues/42>
   [email]: <mailto:ziqi.zhang@sheffield.ac.uk>
   [MysterionRise]: <https://github.com/MysterionRise>
