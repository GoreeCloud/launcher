package com.goreecloud.launcher.core.launcher

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale

/**
 * Privacy-bounded local quick answers for Launcher Universal Search.
 *
 * This provider evaluates a deliberately small arithmetic grammar and a fixed allowlist of common
 * unit conversions in-process. It does not execute arbitrary code, retain queries, request Android
 * permissions, or use the network.
 */
class LauncherQuickAnswersSearchProvider : LauncherSearchProvider {
    override val id: String = PROVIDER_ID

    override fun search(rawQuery: String): List<LauncherSearchResult> {
        val answer = LauncherQuickAnswerEngine.answer(rawQuery) ?: return emptyList()
        return listOf(
            LauncherSearchResult(
                providerId = id,
                resultId = answer.resultId,
                title = answer.displayValue,
                subtitle = answer.detail,
                category = LauncherSearchCategory.ACTION,
                score = QUICK_ANSWER_SCORE,
                action = LauncherCopyTextSearchAction(answer.copyValue),
            ),
        )
    }

    companion object {
        const val PROVIDER_ID = "launcher.quick-answers"
        private const val QUICK_ANSWER_SCORE = 1_500
    }
}

data class LauncherCopyTextSearchAction(
    val text: String,
) : LauncherSearchAction

internal data class LauncherQuickAnswer(
    val resultId: String,
    val displayValue: String,
    val detail: String,
    val copyValue: String,
)

internal object LauncherQuickAnswerEngine {
    private const val MAX_QUERY_LENGTH = 160
    private const val MAX_NESTING = 32
    private val calculationContext = MathContext.DECIMAL64
    private val displayContext = MathContext(12, RoundingMode.HALF_UP)

    private val conversionPattern = Regex(
        """^\s*([-+]?(?:\d+(?:\.\d*)?|\.\d+))\s*([A-Za-z°]+)\s+(?:to|in)\s+([A-Za-z°]+)\s*$""",
        RegexOption.IGNORE_CASE,
    )

    fun answer(rawQuery: String): LauncherQuickAnswer? {
        val query = rawQuery.trim()
        if (query.isEmpty() || query.length > MAX_QUERY_LENGTH) return null

        return convert(query) ?: calculate(query)
    }

    private fun calculate(query: String): LauncherQuickAnswer? {
        val expression = query
            .replace('×', '*')
            .replace('÷', '/')
            .replace('−', '-')
        if (!expression.all { it.isDigit() || it.isWhitespace() || it in ".+-*/()" }) return null
        if (!Regex("""(?:\d|\))\s*[+\-*/]""").containsMatchIn(expression)) return null

        val value = runCatching { ExpressionParser(expression).parse() }.getOrNull() ?: return null
        val display = formatNumber(value)
        return LauncherQuickAnswer(
            resultId = "calculator",
            displayValue = display,
            detail = "Calculator · $query",
            copyValue = display,
        )
    }

    private fun convert(query: String): LauncherQuickAnswer? {
        val match = conversionPattern.matchEntire(query) ?: return null
        val value = match.groupValues[1].toBigDecimalOrNull() ?: return null
        val sourceToken = normalizeUnit(match.groupValues[2])
        val targetToken = normalizeUnit(match.groupValues[3])

        val sourceTemperature = temperatureUnits[sourceToken]
        val targetTemperature = temperatureUnits[targetToken]
        if (sourceTemperature != null || targetTemperature != null) {
            if (sourceTemperature == null || targetTemperature == null) return null
            val converted = convertTemperature(value, sourceTemperature, targetTemperature)
                ?: return null
            val rendered = formatNumber(converted) + " " + targetTemperature.symbol
            return LauncherQuickAnswer(
                resultId = "conversion:${sourceTemperature.symbol}->${targetTemperature.symbol}",
                displayValue = rendered,
                detail = "Unit conversion · ${formatNumber(value)} ${sourceTemperature.symbol} → ${targetTemperature.symbol}",
                copyValue = rendered,
            )
        }

        val source = linearUnitsByAlias[sourceToken] ?: return null
        val target = linearUnitsByAlias[targetToken] ?: return null
        if (source.dimension != target.dimension) return null

        val baseValue = value.multiply(source.factorToBase, calculationContext)
        val converted = runCatching {
            baseValue.divide(target.factorToBase, calculationContext)
        }.getOrNull() ?: return null
        val rendered = formatNumber(converted) + " " + target.symbol
        return LauncherQuickAnswer(
            resultId = "conversion:${source.symbol}->${target.symbol}",
            displayValue = rendered,
            detail = "Unit conversion · ${formatNumber(value)} ${source.symbol} → ${target.symbol}",
            copyValue = rendered,
        )
    }

    private fun convertTemperature(
        value: BigDecimal,
        source: TemperatureUnit,
        target: TemperatureUnit,
    ): BigDecimal? {
        val kelvin = when (source.kind) {
            TemperatureKind.CELSIUS -> value.add(BigDecimal("273.15"), calculationContext)
            TemperatureKind.FAHRENHEIT -> value
                .subtract(BigDecimal("32"), calculationContext)
                .multiply(BigDecimal("5"), calculationContext)
                .divide(BigDecimal("9"), calculationContext)
                .add(BigDecimal("273.15"), calculationContext)
            TemperatureKind.KELVIN -> value
        }
        if (kelvin < BigDecimal.ZERO) return null

        return when (target.kind) {
            TemperatureKind.CELSIUS -> kelvin.subtract(BigDecimal("273.15"), calculationContext)
            TemperatureKind.FAHRENHEIT -> kelvin
                .subtract(BigDecimal("273.15"), calculationContext)
                .multiply(BigDecimal("9"), calculationContext)
                .divide(BigDecimal("5"), calculationContext)
                .add(BigDecimal("32"), calculationContext)
            TemperatureKind.KELVIN -> kelvin
        }
    }

    private fun normalizeUnit(value: String): String =
        value.trim().lowercase(Locale.ROOT)

    private fun formatNumber(value: BigDecimal): String {
        val rounded = value.round(displayContext).stripTrailingZeros()
        return if (rounded.compareTo(BigDecimal.ZERO) == 0) "0" else rounded.toPlainString()
    }

    private enum class UnitDimension { LENGTH, MASS, TIME }

    private data class LinearUnit(
        val dimension: UnitDimension,
        val symbol: String,
        val factorToBase: BigDecimal,
        val aliases: Set<String>,
    )

    private fun unit(
        dimension: UnitDimension,
        symbol: String,
        factorToBase: String,
        vararg aliases: String,
    ): LinearUnit = LinearUnit(
        dimension = dimension,
        symbol = symbol,
        factorToBase = BigDecimal(factorToBase),
        aliases = aliases.mapTo(linkedSetOf(), ::normalizeUnit),
    )

    private val linearUnits = listOf(
        unit(UnitDimension.LENGTH, "mm", "0.001", "mm", "millimeter", "millimeters", "millimetre", "millimetres"),
        unit(UnitDimension.LENGTH, "cm", "0.01", "cm", "centimeter", "centimeters", "centimetre", "centimetres"),
        unit(UnitDimension.LENGTH, "m", "1", "m", "meter", "meters", "metre", "metres"),
        unit(UnitDimension.LENGTH, "km", "1000", "km", "kilometer", "kilometers", "kilometre", "kilometres"),
        unit(UnitDimension.LENGTH, "in", "0.0254", "in", "inch", "inches"),
        unit(UnitDimension.LENGTH, "ft", "0.3048", "ft", "foot", "feet"),
        unit(UnitDimension.LENGTH, "yd", "0.9144", "yd", "yard", "yards"),
        unit(UnitDimension.LENGTH, "mi", "1609.344", "mi", "mile", "miles"),
        unit(UnitDimension.MASS, "mg", "0.000001", "mg", "milligram", "milligrams"),
        unit(UnitDimension.MASS, "g", "0.001", "g", "gram", "grams"),
        unit(UnitDimension.MASS, "kg", "1", "kg", "kilogram", "kilograms"),
        unit(UnitDimension.MASS, "oz", "0.028349523125", "oz", "ounce", "ounces"),
        unit(UnitDimension.MASS, "lb", "0.45359237", "lb", "lbs", "pound", "pounds"),
        unit(UnitDimension.TIME, "ms", "0.001", "ms", "millisecond", "milliseconds"),
        unit(UnitDimension.TIME, "s", "1", "s", "sec", "secs", "second", "seconds"),
        unit(UnitDimension.TIME, "min", "60", "min", "mins", "minute", "minutes"),
        unit(UnitDimension.TIME, "h", "3600", "h", "hr", "hrs", "hour", "hours"),
        unit(UnitDimension.TIME, "day", "86400", "d", "day", "days"),
    )

    private val linearUnitsByAlias: Map<String, LinearUnit> = buildMap {
        linearUnits.forEach { definition ->
            definition.aliases.forEach { alias -> put(alias, definition) }
        }
    }

    private enum class TemperatureKind { CELSIUS, FAHRENHEIT, KELVIN }

    private data class TemperatureUnit(
        val kind: TemperatureKind,
        val symbol: String,
    )

    private val temperatureUnits: Map<String, TemperatureUnit> = buildMap {
        fun register(unit: TemperatureUnit, vararg aliases: String) {
            aliases.forEach { alias -> put(normalizeUnit(alias), unit) }
        }
        register(TemperatureUnit(TemperatureKind.CELSIUS, "°C"), "c", "°c", "celsius")
        register(TemperatureUnit(TemperatureKind.FAHRENHEIT, "°F"), "f", "°f", "fahrenheit")
        register(TemperatureUnit(TemperatureKind.KELVIN, "K"), "k", "kelvin")
    }

    private class ExpressionParser(
        private val source: String,
    ) {
        private var index = 0

        fun parse(): BigDecimal? {
            val value = parseExpression(depth = 0) ?: return null
            skipWhitespace()
            return value.takeIf { index == source.length }
        }

        private fun parseExpression(depth: Int): BigDecimal? {
            var value = parseTerm(depth) ?: return null
            while (true) {
                skipWhitespace()
                value = when {
                    match('+') -> value.add(parseTerm(depth) ?: return null, calculationContext)
                    match('-') -> value.subtract(parseTerm(depth) ?: return null, calculationContext)
                    else -> return value
                }
            }
        }

        private fun parseTerm(depth: Int): BigDecimal? {
            var value = parseFactor(depth) ?: return null
            while (true) {
                skipWhitespace()
                value = when {
                    match('*') -> value.multiply(parseFactor(depth) ?: return null, calculationContext)
                    match('/') -> {
                        val divisor = parseFactor(depth) ?: return null
                        if (divisor.compareTo(BigDecimal.ZERO) == 0) return null
                        value.divide(divisor, calculationContext)
                    }
                    else -> return value
                }
            }
        }

        private fun parseFactor(depth: Int): BigDecimal? {
            skipWhitespace()
            if (depth > MAX_NESTING) return null
            if (match('+')) return parseFactor(depth)
            if (match('-')) return parseFactor(depth)?.negate(calculationContext)
            if (match('(')) {
                val value = parseExpression(depth + 1) ?: return null
                skipWhitespace()
                if (!match(')')) return null
                return value
            }
            return parseNumber()
        }

        private fun parseNumber(): BigDecimal? {
            skipWhitespace()
            val start = index
            var sawDigit = false
            var sawDot = false
            while (index < source.length) {
                val char = source[index]
                when {
                    char.isDigit() -> {
                        sawDigit = true
                        index += 1
                    }
                    char == '.' && !sawDot -> {
                        sawDot = true
                        index += 1
                    }
                    else -> break
                }
            }
            if (!sawDigit) return null
            return source.substring(start, index).toBigDecimalOrNull()
        }

        private fun match(expected: Char): Boolean {
            skipWhitespace()
            if (index >= source.length || source[index] != expected) return false
            index += 1
            return true
        }

        private fun skipWhitespace() {
            while (index < source.length && source[index].isWhitespace()) index += 1
        }
    }
}
