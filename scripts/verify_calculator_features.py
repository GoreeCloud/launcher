#!/usr/bin/env python3
"""Source-level guard for local-only accessible math input in launcher search.

Android execution and user-visible results still require device verification.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
calculator = (
    root / "lawnchair/src/app/lawnchair/search/algorithms/engine/provider/CalculatorSearchProvider.kt"
).read_text(encoding="utf-8")

required = [
    'private const val MAX_CALCULATOR_QUERY_LENGTH = 160',
    'query.length > MAX_CALCULATOR_QUERY_LENGTH',
    'trimmedQuery.endsWith("=")',
    "trimmedQuery.dropLast(1).trim()",
    ".replace('×', '*')",
    ".replace('÷', '/')",
    ".replace('−', '-')",
    ".replace('＋', '+')",
    ".replace('（', '(')",
    ".replace('）', ')')",
    "Expressions().eval(expression)",
    "percentOfPattern.matchEntire(expression)",
    "percent.multiply(value, MathContext.DECIMAL64)",
    '.divide(BigDecimal("100"), MathContext.DECIMAL64)',
    "RegexOption.IGNORE_CASE",
    "equation = query,",
    "legacyPrefs.searchResultCalculator.get()",
]
for invariant in required:
    assert invariant in calculator, f"Calculator invariant missing: {invariant}"

print("PASS: local calculator supports common math glyphs, optional equals and query limits")
