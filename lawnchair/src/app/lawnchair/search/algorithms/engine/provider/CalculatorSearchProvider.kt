package app.lawnchair.search.algorithms.engine.provider

import android.content.Context
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.search.algorithms.data.Calculation
import app.lawnchair.search.algorithms.data.calculator.Expressions
import app.lawnchair.search.algorithms.engine.SearchProvider
import app.lawnchair.search.algorithms.engine.SearchResult
import java.math.BigDecimal
import java.math.MathContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object CalculatorSearchProvider : SearchProvider {
    private const val MAX_CALCULATOR_QUERY_LENGTH = 160
    // Keep the parser's existing modulo operator intact; "percent of" is a
    // separate, common calculator request such as "15% of 80".
    private val percentOfPattern = Regex(
        """^([+-]?\d+(?:\.\d+)?)\s*%\s*of\s*([+-]?\d+(?:\.\d+)?)$""",
        RegexOption.IGNORE_CASE,
    )

    override val id: String = "calculator"

    override fun search(
        context: Context,
        query: String,
    ): Flow<List<SearchResult>> = flow {
        val legacyPrefs = PreferenceManager.getInstance(context)

        if (query.isBlank() || !legacyPrefs.searchResultCalculator.get()) {
            emit(emptyList())
            return@flow
        }

        val calculation = calculateEquationFromString(query)

        if (calculation.isValid) {
            val searchResult = SearchResult.Calculation(data = calculation)
            emit(listOf(searchResult))
        } else {
            emit(emptyList())
        }
    }

    private fun calculateEquationFromString(
        query: String,
    ): Calculation {
        return try {
            // The user may type everyday calculator symbols (or paste a result
            // expression ending in "="); keep the original string for display.
            // A length bound prevents very long input from blocking app search.
            if (query.length > MAX_CALCULATOR_QUERY_LENGTH) {
                throw IllegalArgumentException("Calculator expression too long")
            }
            val trimmedQuery = query.trim()
            val expression = (if (trimmedQuery.endsWith("=") && !trimmedQuery.endsWith("==")) {
                trimmedQuery.dropLast(1).trim()
            } else {
                trimmedQuery
            })
                .replace('×', '*')
                .replace('÷', '/')
                .replace('−', '-')
                .replace('＋', '+')
                .replace('（', '(')
                .replace('）', ')')
            val evaluatedValue = percentOfPattern.matchEntire(expression)?.let { match ->
                val percent = BigDecimal(match.groupValues[1])
                val value = BigDecimal(match.groupValues[2])
                percent.multiply(value, MathContext.DECIMAL64)
                    .divide(BigDecimal("100"), MathContext.DECIMAL64)
            } ?: Expressions().eval(expression)
            val roundedValue = evaluatedValue.round(MathContext.DECIMAL64)
            val formattedValue = roundedValue.stripTrailingZeros()
            val absoluteValue = formattedValue.abs()
            val threshold = BigDecimal("9999999999999999")

            val result = if (absoluteValue > threshold) {
                formattedValue.toString()
            } else {
                formattedValue.toPlainString()
            }

            Calculation(
                equation = query,
                result = result,
                isValid = true,
            )
        } catch (_: Exception) {
            Calculation(
                equation = "",
                result = "",
                isValid = false,
            )
        }
    }
}
