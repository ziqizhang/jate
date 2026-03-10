# Export Formats

`TermExtractionResult` provides several ways to export extracted terms.

## pandas DataFrame

```python
df = result.to_dataframe()
print(df.head())
```

| Column | Type | Description |
|--------|------|-------------|
| `term` | str | Normalised (lemmatised) term |
| `score` | float | Algorithm-assigned score |
| `frequency` | int | Total corpus frequency |
| `rank` | int | Rank (1-based, after sorting) |
| `surface_forms` | str | Comma-separated surface variants |

From the DataFrame, you can use any pandas operation:

```python
# Filter and sort
top_multiword = df[df["term"].str.contains(" ")].head(20)

# Save to file
df.to_csv("terms.csv", index=False)
df.to_excel("terms.xlsx", index=False)
df.to_parquet("terms.parquet")
```

## CSV

```python
csv_str = result.to_csv()
print(csv_str)

# Write to file
with open("terms.csv", "w") as f:
    f.write(result.to_csv())
```

Fields: `term`, `score`, `frequency`, `rank`.

## JSON

```python
json_str = result.to_json()
print(json_str)

# Write to file
with open("terms.json", "w") as f:
    f.write(result.to_json())
```

Each term is an object with `term`, `score`, `frequency`, `rank`, `surface_forms` (as a list), and `metadata`.

## Iteration

```python
# Direct iteration
for term in result:
    print(term.string, term.score)

# Indexing
first = result[0]

# Slicing via list conversion
top_10 = list(result)[:10]

# Length
print(f"{len(result)} terms extracted")
```

## Filtering before export

```python
# Chain filters
filtered = (
    result
    .filter_by_score(min_score=1.0)
    .filter_by_frequency(min_frequency=3)
    .filter_by_length(min_words=2, max_words=5)
)

df = filtered.to_dataframe()
```
