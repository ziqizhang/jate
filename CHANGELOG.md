# 3.0.0 (2026-03-10)


### Bug Fixes

* add external reference frequency file support and fix +1 denominator ([217065e](https://github.com/ziqizhang/jate/commit/217065e4a46159ea5c6215270e65b8f639760e67))
* add package-lock.json for npm ci in semantic release workflow ([88e6cce](https://github.com/ziqizhang/jate/commit/88e6cce072eaffb74074e7cce75d5437fd52f459))
* forward containment to NCValue, defer containment build to when needed ([b331dfd](https://github.com/ziqizhang/jate/commit/b331dfd68de26bd0c01a5889e3a5227e0463f428))
* resolve all lint and mypy errors for CI compliance ([76f8187](https://github.com/ziqizhang/jate/commit/76f8187d3061eed702cde0051d2547d34aa0c079))
* split_range bug, add tests, fix import ordering ([5aee83a](https://github.com/ziqizhang/jate/commit/5aee83afed5108f1921891c041bd176a86ead5d7))
* TermEx DC uses precomputed per-doc totals, remove unused ContextFrequency ([40e66c5](https://github.com/ziqizhang/jate/commit/40e66c53aeb830f811dc902407322f7adf609b5f))
* use RELEASE_TOKEN for semantic-release to bypass branch protection ([4ad7e8a](https://github.com/ziqizhang/jate/commit/4ad7e8aaee1aa638e0d3d7da7ec8140035c66f41))


### Features

* add context_index parameter to Algorithm.score() and all implementations ([a7f4a98](https://github.com/ziqizhang/jate/commit/a7f4a985c44f7a4adbc4d9e22c6f9ed5941426ba))
* add ContextIndex class for sentence-level context statistics ([451c80b](https://github.com/ziqizhang/jate/commit/451c80b89f35c65657ce9e81a3e0ce5f6683c914))
* add ContextWindow + ContextFrequency (mirrors Java FrequencyCtxBased) ([17944b2](https://github.com/ziqizhang/jate/commit/17944b20313ea16cff167c3be24becf9fd8dde71))
* add Cooccurrence + ChiSquareFrequentTerms features ([376bcfa](https://github.com/ziqizhang/jate/commit/376bcfa1e941153c568031287af7dfffdefb68f6))
* add evaluation module, dataset loaders, and benchmark CLI ([39f03c4](https://github.com/ziqizhang/jate/commit/39f03c4c8477e66a15759eb1541cf91d5210fecd))
* add JATEConfig and parallel_map helper ([cd75450](https://github.com/ziqizhang/jate/commit/cd754504791f232a724a0c50f2161539ccfd09f9))
* add minTTF/minTCF prefiltering to Cooccurrence.build() ([8c75e8f](https://github.com/ziqizhang/jate/commit/8c75e8f738cbc78078fc9b5b62c9cf8cb41a13fc))
* add PyPI publishing workflow and restore install instructions ([871f498](https://github.com/ziqizhang/jate/commit/871f4989ef87679478608e51ed4c20d0f2e524ee))
* add README, Apache 2.0 license, issue templates, bump to Python 3.11 ([f97d510](https://github.com/ziqizhang/jate/commit/f97d510a86604777bdfe380a07d8d1ec0685a6f8))
* add ReferenceFrequency feature (mirrors Java FrequencyTermBased ref) ([29e26a8](https://github.com/ziqizhang/jate/commit/29e26a83eaeb4638507c353a47d7d361a9058ebe))
* add SpacyBackend.process_batch() using nlp.pipe() ([4090217](https://github.com/ziqizhang/jate/commit/4090217f6a69fb9a4f87ff7af612c32dadb14ee8))
* add TermComponentIndex + Containment with regex word boundaries ([147675c](https://github.com/ziqizhang/jate/commit/147675c9d156f0b40f059fc5dce2440ae0365526))
* add TermFrequency feature (mirrors Java FrequencyTermBased type=0) ([957214f](https://github.com/ziqizhang/jate/commit/957214f1f8eca53aa893ae19f715086e10bdcdd1))
* add Voting ensemble algorithm (reciprocal rank fusion) ([64d508f](https://github.com/ziqizhang/jate/commit/64d508f407a05819c0d69c78978ccfd5bf6c0a02))
* add WordFrequency feature (mirrors Java FrequencyTermBased type=1) ([ca2ec1b](https://github.com/ziqizhang/jate/commit/ca2ec1b8f32f90fa61672446a489882f38c6ebf9))
* algorithms accept pre-built containment index ([fa3e225](https://github.com/ziqizhang/jate/commit/fa3e22550cf68ccc717a16cc95d8e0dc39aa21b8))
* align Python JATE with Java implementation (fixes 1-5) ([062f7fd](https://github.com/ziqizhang/jate/commit/062f7fd6f10e6790c9e7d2f54743721ddfd80638))
* extend Candidate.add_position() with sentence_idx parameter ([4a236c8](https://github.com/ziqizhang/jate/commit/4a236c89b6dde909ee79d4e71c8f83becc2204eb))
* extractors use batch NLP processing via spaCy Docs ([b63a0ef](https://github.com/ziqizhang/jate/commit/b63a0ef255865bde7631679a8c18b785275018f4))
* implement Tier 1 — full Python ATE library ([3eb03a5](https://github.com/ziqizhang/jate/commit/3eb03a58737cae82e807ac340e9328e15275c16a))
* initialize Python rewrite of JATE ([9c912d9](https://github.com/ziqizhang/jate/commit/9c912d92fcf1f5e3d80c91b39032f89f0b9cd388))
* parallel co-occurrence computation in MemoryCorpusStore ([c823862](https://github.com/ziqizhang/jate/commit/c823862d11d4f4d0cfada9fbd5446fa174d3dafa))
* shared containment index builder with parallel support ([2d78df5](https://github.com/ziqizhang/jate/commit/2d78df53188b42b55917b13622d8a780973f0625))
* TermEx supports multiple reference corpora (per-word best selection) ([446aff9](https://github.com/ziqizhang/jate/commit/446aff969bc0cb5e4eeaba85faca87520ee114f0))
* update extractors to split by sentence and pass sentence_idx ([2474df1](https://github.com/ziqizhang/jate/commit/2474df10c6182fc92c5df93928e9ff7bab20d83c))
* wire JATEConfig through API with parallel containment and co-occurrence ([3972eb6](https://github.com/ziqizhang/jate/commit/3972eb6b56b02421c6636e3a96e0fa4da7e4f854))
