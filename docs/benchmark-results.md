# JATE Benchmark Results

Generated: 2026-03-14

## Methodology

**Candidate extraction**: POS pattern matching over Universal POS tags (spaCy `en_core_web_sm`). The default pattern matches sequences of adjectives, nouns, and proper nouns: `(ADJ |NOUN |PROPN )*(NOUN |PROPN )`. Domain-specific patterns are available in `src/jate/extractors/patterns/` (e.g., `genia.txt` adds optional preposition support for "X of Y" terms). All results below use the default pattern.

**Evaluation metric**: P@K (precision at top-K ranked terms). K cutoffs: 100, 500, 1,000, 5,000, 10,000 where applicable.

**Reference corpus**: BNC (British National Corpus) frequency list, used by algorithms requiring a reference (weirdness, glossex, termex).

**Scoring time**: Wall-clock time for the algorithm's scoring step only, excluding NLP processing and feature building.

### Comparison with JATE 2.0 (Java)

JATE v3 (Python) results are **lower than JATE 2.0** (Java) on the same datasets, primarily due to differences in candidate extraction:

| Factor | JATE 2.0 (Java) | JATE v3 (Python) | Impact |
|--------|-----------------|------------------|--------|
| **NLP backend** | Apache OpenNLP + Solr analysis chain | spaCy `en_core_web_sm` | Different tokenisation of hyphens, compounds |
| **POS tagset** | Penn Treebank (NN, NNP, NNS, JJ, VBN) | Universal POS (NOUN, PROPN, ADJ) | Coarser tag distinctions |
| **Tokenisation** | Solr preserves hyphenated compounds (e.g., `NF-kappa` as one token) | spaCy splits hyphens (e.g., `NF`, `-`, `kappa` as three tokens) | ~50% of missing GENIA gold terms contain hyphens |
| **Lemmatiser** | Dragon NLP `EngLemmatiser` | spaCy lemmatiser | Different normalisation of biomedical terms |
| **GENIA candidates** | 38,805 | 36,355 | 6% fewer candidates extracted |
| **GENIA gold overlap** | ~65%+ (estimated from published P@K) | 43% | Hyphenated/compound terms account for most of the gap |

The remaining gap is dominated by **tokenisation differences** — biomedical text is heavy with hyphenated compounds, parenthetical expressions, and alphanumeric identifiers that spaCy's general-purpose tokeniser splits differently from Solr's domain-tuned analysis chain.

#### Breakdown of missing GENIA gold terms

Of the ~20,000 gold terms not found in JATE v3 candidates:

| Category | Count | % of missing |
|----------|-------|-------------|
| Contains hyphens (e.g., `NF-kappa`, `T-cell`) | 12,837 | 50% |
| Contains digits (e.g., `interleukin-2`, `CD28`) | 9,315 | 36% |
| Contains parentheses | 3,109 | 12% |
| Any special character | 19,546 | **76%** |
| Pure alphabetic only | 6,074 | 24% |

The issue is not matching — it is that **candidates are never generated** for hyphenated terms. spaCy splits `NF-kappa` into three tokens (`NF`, `-`, `kappa`) and the `-` receives a PUNCT tag, which breaks the POS pattern. Stripping punctuation from the gold standard does not help because the extraction step already missed these terms:

| Evaluation mode | GENIA gold coverage |
|----------------|-------------------|
| Raw gold vs raw candidates | 42.7% |
| Punctuation-stripped gold vs stripped candidates | 48.7% |
| Java JATE 2.0 (estimated) | ~65%+ |

Closing this gap requires tokeniser-level changes (e.g., merging hyphenated tokens before POS tagging, or using a biomedical spaCy model like `en_core_sci_sm` that handles domain compounds natively).

### Published baselines for comparison

**GENIA** (Zhang, Gao & Ciravegna, LREC 2016 — JATE 2.0 Java):
- TF-IDF P@1000 ≈ 0.85–0.95, C-Value P@1000 ≈ 0.85–0.90 (from Figure 4, POS-based extraction)
- RAKE consistently worst performer across all K values

**GENIA** (Fedorenko, Astrakhantsev & Turdakov, 2013 — supervised voting):
- Voting (best features) AvP@1000 = 0.88, AvP@5000 = 0.79

**ACTER** (TermEval 2020 shared task — heart failure domain, F1):
- TALN-LS2N (BERT-based): F1 = 46.7%, NYU Termolator (classical statistical): F1 = 30.6%

**CoastTerm** (Delaunay et al., TSD 2024):
- No published classical ATE baselines. Only transformer-based sequence labelling (~80% F1). These results represent the first classical statistical benchmarks on CoastTerm.

---

## acl_rdtec_mini

Small test fixture — 3 documents from the ACL Anthology. Useful for quick smoke tests, not for comparing algorithms.

| | |
|-|-|
| **Documents** | 3 |
| **Gold terms** | 435 |
| **Candidates** | 1,577 |
| **NLP time** | 3.5s |

| Algorithm | P@100 | P@500 | P@1,000 | Score time |
|-----------|-------|-------|---------|------------|
| attf | 0.3700 | 0.1940 | 0.1700 | 0.01s |
| tfidf | 0.3400 | 0.1920 | 0.1630 | 0.01s |
| ttf | 0.3400 | 0.2000 | 0.1690 | 0.01s |
| chi_square | 0.3000 | 0.1900 | 0.1460 | 0.02s |
| basic | 0.3000 | 0.2300 | 0.1720 | 0.01s |
| combobasic | 0.3000 | 0.2180 | 0.1710 | 0.01s |
| ridf | 0.2900 | 0.2000 | 0.1690 | 0.01s |
| cvalue | 0.2300 | 0.1440 | 0.1440 | 0.01s |
| ncvalue | 0.2200 | 0.1280 | 0.1430 | 0.02s |
| rake | 0.0500 | 0.1500 | 0.1520 | 0.03s |
| termex | 0.0200 | 0.1520 | 0.1710 | 0.06s |
| weirdness | 0.0200 | 0.1020 | 0.1620 | 0.05s |
| glossex | 0.0200 | 0.0920 | 0.1460 | 0.05s |

---

## genia

2,000 biomedical abstracts from PubMed (GENIA 3.02 corpus). 35,298 gold terms from the GENIA ontology.

| | |
|-|-|
| **Documents** | 2,000 |
| **Gold terms** | 35,298 |
| **Candidates** | 36,355 |
| **NLP time** | 118.0s |

| Algorithm | P@100 | P@500 | P@1,000 | P@5,000 | P@10,000 | Score time |
|-----------|-------|-------|---------|---------|----------|------------|
| attf | 0.7900 | 0.7400 | 0.7350 | 0.6730 | 0.5755 | 0.17s |
| ridf | 0.7700 | 0.7600 | 0.7660 | 0.6730 | 0.6015 | 0.22s |
| basic | 0.7700 | 0.7520 | 0.7360 | 0.6276 | 0.5875 | 0.71s |
| combobasic | 0.7700 | 0.7500 | 0.7370 | 0.6274 | 0.5875 | 0.67s |
| cvalue | 0.7200 | 0.7220 | 0.7090 | 0.6164 | 0.5092 | 0.68s |
| ncvalue | 0.7200 | 0.7220 | 0.7090 | 0.6172 | 0.5161 | 1.17s |
| tfidf | 0.6100 | 0.6720 | 0.6560 | 0.6434 | 0.6015 | 0.51s |
| ttf | 0.5900 | 0.6620 | 0.6490 | 0.6424 | 0.5908 | 0.59s |
| chi_square | 0.5700 | 0.5540 | 0.5550 | 0.5306 | 0.5097 | 0.89s |
| rake | 0.4300 | 0.4760 | 0.4800 | 0.4460 | 0.4437 | 37.12s |
| weirdness | 0.3800 | 0.4360 | 0.4720 | 0.5244 | 0.5163 | 0.34s |
| termex | 0.2400 | 0.3640 | 0.4160 | 0.5216 | 0.5207 | 0.96s |
| glossex | 0.0000 | 0.0000 | 0.0860 | 0.4374 | 0.4794 | 0.36s |

**Observations**: RIDF and ATTF lead at both small and large K. Basic/combobasic are strong at all cutoffs. Weirdness, termex, and glossex perform poorly at small K but improve at large K — these reference-based methods benefit from corpus-level statistics that only emerge with larger candidate sets. Glossex at P@100=0.00 suggests a ranking calibration issue at the top of the list.

---

## acl_rdtec

Full ACL RD-TEC 2.0 corpus — 1,758 computational linguistics abstracts with 5,031 annotated terms.

| | |
|-|-|
| **Documents** | 1,758 |
| **Gold terms** | 5,031 |
| **Candidates** | 6,135 |
| **NLP time** | 34.1s |

| Algorithm | P@100 | P@500 | P@1,000 | P@5,000 | Score time |
|-----------|-------|-------|---------|---------|------------|
| ttf | 0.7300 | 0.5360 | 0.4590 | 0.2370 | 0.03s |
| tfidf | 0.7200 | 0.5200 | 0.4590 | 0.2370 | 0.03s |
| ridf | 0.6600 | 0.4880 | 0.4530 | 0.2836 | 0.04s |
| chi_square | 0.4900 | 0.4060 | 0.3860 | 0.2708 | 0.09s |
| termex | 0.4700 | 0.1740 | 0.0980 | 0.2502 | 0.11s |
| weirdness | 0.4700 | 0.1580 | 0.0850 | 0.2532 | 0.12s |
| attf | 0.4400 | 0.4320 | 0.3940 | 0.2836 | 0.20s |
| ncvalue | 0.4400 | 0.3860 | 0.3440 | 0.2420 | 0.13s |
| cvalue | 0.4300 | 0.3860 | 0.3700 | 0.2428 | 0.06s |
| basic | 0.3700 | 0.3480 | 0.3240 | 0.2726 | 0.03s |
| combobasic | 0.3600 | 0.3240 | 0.3200 | 0.2726 | 0.04s |
| rake | 0.0700 | 0.1260 | 0.1940 | 0.2838 | 0.48s |
| glossex | 0.0100 | 0.1620 | 0.0880 | 0.2408 | 0.13s |

**Observations**: TTF and TF-IDF dominate at small K — simple frequency is a strong signal in computational linguistics text. Reference-based methods (weirdness, termex) show erratic behaviour, likely because the BNC reference corpus is not well-suited to computational linguistics terminology. RAKE performs poorly at small K but catches up at P@5000.

---

## acter

ACTER v1.5 — 241 documents across 4 English domains (corruption, dressage, heart failure, wind energy). 5,329 gold terms (Specific_Term + Common_Term categories, excluding OOD_Term).

| | |
|-|-|
| **Documents** | 241 |
| **Gold terms** | 5,329 |
| **Candidates** | 16,497 |
| **NLP time** | 53.9s |

| Algorithm | P@100 | P@500 | P@1,000 | P@5,000 | P@10,000 | Score time |
|-----------|-------|-------|---------|---------|----------|------------|
| basic | 0.6100 | 0.4900 | 0.4120 | 0.2814 | 0.1866 | 0.07s |
| combobasic | 0.6100 | 0.4780 | 0.4060 | 0.2810 | 0.1891 | 0.35s |
| cvalue | 0.5300 | 0.4180 | 0.3700 | 0.1808 | 0.1326 | 0.10s |
| ncvalue | 0.5300 | 0.4240 | 0.3700 | 0.1842 | 0.1395 | 0.56s |
| tfidf | 0.4700 | 0.4120 | 0.4030 | 0.2774 | 0.1802 | 0.08s |
| ttf | 0.4600 | 0.3920 | 0.3940 | 0.2860 | 0.1802 | 0.06s |
| ridf | 0.4200 | 0.4640 | 0.4070 | 0.2774 | 0.1802 | 0.25s |
| attf | 0.4100 | 0.4220 | 0.3970 | 0.2632 | 0.1764 | 0.07s |
| chi_square | 0.2800 | 0.2860 | 0.2970 | 0.2368 | 0.1890 | 0.33s |
| weirdness | 0.2200 | 0.2820 | 0.2850 | 0.2452 | 0.2008 | 0.36s |
| termex | 0.1600 | 0.2820 | 0.3100 | 0.2800 | 0.2131 | 0.44s |
| rake | 0.0300 | 0.0500 | 0.0720 | 0.1138 | 0.1280 | 1.42s |
| glossex | 0.0000 | 0.0880 | 0.1850 | 0.2318 | 0.2117 | 0.17s |

**Observations**: Basic and combobasic lead at P@100 (0.61). The multi-domain nature of ACTER makes it challenging — reference-based methods underperform because the BNC doesn't cover all four specialist domains well. For comparison, the best classical system at TermEval 2020 (NYU Termolator) achieved F1=30.6% on the heart failure held-out test set.

---

## coastterm

CoastTerm — 2,004 sentences from coastal science abstracts. 4,316 gold terms. First published classical ATE benchmarks on this dataset.

| | |
|-|-|
| **Documents** | 2,004 |
| **Gold terms** | 4,316 |
| **Candidates** | 7,819 |
| **NLP time** | 11.8s |

| Algorithm | P@100 | P@500 | P@1,000 | P@5,000 | Score time |
|-----------|-------|-------|---------|---------|------------|
| combobasic | 0.5900 | 0.4440 | 0.4340 | 0.2732 | 0.05s |
| basic | 0.5700 | 0.4660 | 0.4330 | 0.2966 | 0.03s |
| cvalue | 0.4600 | 0.3260 | 0.2830 | 0.2164 | 0.05s |
| ncvalue | 0.4700 | 0.3400 | 0.2860 | 0.2178 | 0.10s |
| termex | 0.4500 | 0.4880 | 0.4730 | 0.3116 | 0.26s |
| weirdness | 0.4300 | 0.4860 | 0.4420 | 0.3050 | 0.13s |
| tfidf | 0.4200 | 0.4900 | 0.4390 | 0.2920 | 0.04s |
| ttf | 0.4200 | 0.4940 | 0.4460 | 0.2920 | 0.03s |
| ridf | 0.4000 | 0.4320 | 0.4390 | 0.2920 | 0.20s |
| glossex | 0.3800 | 0.4400 | 0.4160 | 0.3120 | 0.12s |
| attf | 0.2900 | 0.3740 | 0.2890 | 0.2796 | 0.04s |
| chi_square | 0.2700 | 0.2420 | 0.2910 | 0.2868 | 0.29s |
| rake | 0.0300 | 0.0640 | 0.0970 | 0.2280 | 0.36s |

**Observations**: Basic/combobasic lead at P@100. Termex, weirdness, and glossex perform well at P@500+ — the BNC reference is a reasonable match for coastal science vocabulary. TF-IDF and TTF are strong at mid-range K. No prior classical ATE baselines exist for CoastTerm; the original paper only evaluated transformer-based sequence labelling (RoBERTa F1 ≈ 82%).

---

## Cross-dataset summary

Best algorithm at each P@K cutoff per dataset:

| Dataset | P@100 | P@500 | P@1,000 |
|---------|-------|-------|---------|
| acl_rdtec_mini | attf (0.37) | basic (0.23) | basic (0.17) |
| genia | attf (0.79) | ridf (0.76) | ridf (0.77) |
| acl_rdtec | ttf (0.73) | ttf (0.54) | tfidf/ttf (0.46) |
| acter | basic (0.61) | basic (0.49) | basic (0.41) |
| coastterm | combobasic (0.59) | ttf (0.49) | termex (0.47) |

**General findings:**
- **basic/combobasic** are the most consistent performers across domains and K values.
- **ridf** excels on large biomedical corpora (GENIA).
- **tfidf/ttf** dominate on computational linguistics text (ACL RD-TEC) where simple frequency is a strong signal.
- **termex/weirdness** improve with larger K values and are competitive on coastterm, where the BNC reference is a good domain contrast.
- **rake** consistently underperforms at small K — it is designed for document-level keyword extraction, not corpus-level term extraction.
- **glossex** shows poor precision at small K across all datasets, suggesting a calibration issue in the top-ranked terms.
