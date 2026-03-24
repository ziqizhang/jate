# [3.3.0](https://github.com/ziqizhang/jate/compare/v3.2.0...v3.3.0) (2026-03-24)


### Bug Fixes

* add neural extras to pyproject.toml (transformers, torch, datasets, seqeval) ([3bd7ed0](https://github.com/ziqizhang/jate/commit/3bd7ed0d5286e08e496f555cd3520b1bf2dce79f))
* add progress logging for tagger evaluation in benchmark script ([d6d7663](https://github.com/ziqizhang/jate/commit/d6d7663bcdecdc0ef35932fa66ea0ba7b73e05c8))
* add type parameters to dict in _group_bio_spans for mypy ([c884971](https://github.com/ziqizhang/jate/commit/c88497126daf3e4f701c4994bb12d5c4eb9c6cc1))
* implement custom BIO span grouping instead of HF aggregation_strategy ([d1392c7](https://github.com/ziqizhang/jate/commit/d1392c790fffe5152eea08a2c6270e2c15a98851))
* install jate from GitHub in Colab notebook, not PyPI ([f6534c1](https://github.com/ziqizhang/jate/commit/f6534c1921c4054f8bee62fa05a0e1ae3f5970ff))
* point Colab notebook at feature branch for testing ([952ced2](https://github.com/ziqizhang/jate/commit/952ced21a7ae6fd179c7615bfcc2d837f9404c85))
* regenerate poetry.lock after adding neural extras ([80f96c1](https://github.com/ziqizhang/jate/commit/80f96c15421bd862e94944242a43ecafec9ac0fb))
* relax version pins to support numpy 2.x and latest transformers/torch ([4e45c75](https://github.com/ziqizhang/jate/commit/4e45c751731fd4ea3b379ee29582d9a30fdecda7))
* remove dead max_workers config and correct parallelism claims ([69f1818](https://github.com/ziqizhang/jate/commit/69f1818a8061e6c4cb7e6394dfbdb7e8ffb87f8e))
* remove extra trailing blank lines in Dockerfile and .dockerignore ([a4c864d](https://github.com/ziqizhang/jate/commit/a4c864d8c22e0b24a55b29b0f7f66dd9eeecf8a7))
* rename evaluation_strategy to eval_strategy for transformers 5.x ([505c32d](https://github.com/ziqizhang/jate/commit/505c32de85853f3164944709cf0b19b06d41eba4))
* rename tokenizer to processing_class for transformers 5.x Trainer ([44138b5](https://github.com/ziqizhang/jate/commit/44138b54731eb4ce3b123d453724d2bbe7386982))
* resolve CI failures — version test, python-multipart dep, mypy types ([7bb3d9f](https://github.com/ziqizhang/jate/commit/7bb3d9f90542c53c304ec4e3696909c8000da00e))
* resolve Docker Smoke CI failure (Python 3.11 + spaCy/pydantic conflict) ([41a49ef](https://github.com/ziqizhang/jate/commit/41a49efba3cd51d78f782f9bc5a47a2fe91c1dc5))
* update HuggingFace model paths to ziqizhang2026 and fix subword merging ([7cf30f1](https://github.com/ziqizhang/jate/commit/7cf30f154f6f5a6496a551981c16aff61e53f0e2))
* upgrade numpy before jate install in Colab to avoid binary incompatibility ([7f7f8a1](https://github.com/ziqizhang/jate/commit/7f7f8a1a6493a5c9ca8c6bb957a66e76e69d6fe3))
* use --no-deps install to avoid numpy conflicts in Colab ([05e934d](https://github.com/ziqizhang/jate/commit/05e934d52bc792799d9e3ea7438cf0d9dad9f34a))


### Features

* add ATERanker/ATETagger abstraction with capabilities and compatibility checks ([#89](https://github.com/ziqizhang/jate/issues/89)) ([0911240](https://github.com/ziqizhang/jate/commit/0911240e3948a5a295f6f6a0b8e96f8c46575cfa))
* add BERT tagger demo script ([#92](https://github.com/ziqizhang/jate/issues/92)) ([d8bcfbf](https://github.com/ziqizhang/jate/commit/d8bcfbf4753f0266d1c1fcd17fd95e9b46248ce5))
* add Colab notebook for training BERT ATE tagger ([#92](https://github.com/ziqizhang/jate/issues/92) P1.1) ([022be3e](https://github.com/ziqizhang/jate/commit/022be3edc437c4201c9f6094bacf63b59d943213))
* add local web UI with Extract and Corpus pages ([#66](https://github.com/ziqizhang/jate/issues/66)) ([5fcbbd4](https://github.com/ziqizhang/jate/commit/5fcbbd425ce7276a8cae49bd06c7df8549efcb74))
* add NMF ranker and web demo design ([#66](https://github.com/ziqizhang/jate/issues/66), [#92](https://github.com/ziqizhang/jate/issues/92) P2.3) ([7d41076](https://github.com/ziqizhang/jate/commit/7d41076b65225e0c117d0db7edcb55c441693626))
* add spaCy pipeline integration with nlp.add_pipe('jate') ([#65](https://github.com/ziqizhang/jate/issues/65)) ([e15aede](https://github.com/ziqizhang/jate/commit/e15aede56a445d9a1a088006f6abb8b239c1ca6f))
* add tagger to benchmarks, update docs and notebook install link ([#92](https://github.com/ziqizhang/jate/issues/92)) ([240462d](https://github.com/ziqizhang/jate/commit/240462d3c45b04e7ce7d6c6865dde5d504cf92f8))
* add training script for transformer ATE tagger ([#92](https://github.com/ziqizhang/jate/issues/92) P1.1) ([e17f22c](https://github.com/ziqizhang/jate/commit/e17f22cf56c6a7c640e32795d5cfc33722be8dda))
* add transformer token classification tagger infrastructure ([#92](https://github.com/ziqizhang/jate/issues/92) P1.1) ([0e33b54](https://github.com/ziqizhang/jate/commit/0e33b541af9f8e8e8e3b8623f9e2b80fb216f157))
* scaffold evaluation routing and pipeline contracts ([#89](https://github.com/ziqizhang/jate/issues/89)) ([3a2ac41](https://github.com/ziqizhang/jate/commit/3a2ac412548f1566043b66e148deca94f45f216f))

# [3.2.0](https://github.com/ziqizhang/jate/compare/v3.1.0...v3.2.0) (2026-03-14)


### Bug Fixes

* add configurable POS pattern presets including PROPN support ([23c1938](https://github.com/ziqizhang/jate/commit/23c19380d73c90c68acc37aa907d40a8d0282724))
* build features once for multi-algorithm runs ([d0b5841](https://github.com/ziqizhang/jate/commit/d0b5841f2488d15fee1907ad435ee73d6e1b8437))
* fix CI test failures and drop Python 3.11 from CI matrix ([058cc9c](https://github.com/ziqizhang/jate/commit/058cc9c76b7f06ae8be0ba25e79ec9efd7455669))
* optimise chi-square scoring with sparse co-occurrence lookups ([16ccf06](https://github.com/ziqizhang/jate/commit/16ccf06c557f8ea1f8764ec194b30bf2f2d1a806))
* optimise context frequency computation and fix dataset loaders ([fce1cb8](https://github.com/ziqizhang/jate/commit/fce1cb8803585af9b6d14724609fde8b2079114f))
* resolve harness audit findings (mypy, dead code, registry refactor) ([f1a7358](https://github.com/ziqizhang/jate/commit/f1a7358ca8e74882e79d3955887980581785ab37))
* skip unnecessary co-occurrence computation and add benchmark progress logging ([982ca3a](https://github.com/ziqizhang/jate/commit/982ca3a0203338868a51bab13b09e9e8ca644769))


### Features

* add benchmark dataset infrastructure for ACL RD-TEC, ACTER, CoastTerm ([#67](https://github.com/ziqizhang/jate/issues/67)) ([ef3039a](https://github.com/ziqizhang/jate/commit/ef3039a5140ddc53a9099e8c966b6bf105355be5))

# [3.1.0](https://github.com/ziqizhang/jate/compare/v3.0.0...v3.1.0) (2026-03-13)


### Bug Fixes

* resolve poetry.lock merge conflict with dev ([979aa94](https://github.com/ziqizhang/jate/commit/979aa94ffeef3bccdcbe2df241283baa61f83e0d))


### Features

* add official Docker image for JATE ([2040021](https://github.com/ziqizhang/jate/commit/204002162506966874e7c1363f8683214488efa7))

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
