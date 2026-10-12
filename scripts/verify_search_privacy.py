#!/usr/bin/env python3
"""Static safeguards for GoreeCloud Launcher search opt-in and endpoint isolation.

These checks do not replace device or network traffic tests.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
errors = []


def text(path):
    return (root / path).read_text()


def require(test, label):
    if not test:
        errors.append(label)


prefs = text("lawnchair/src/app/lawnchair/preferences/PreferenceManager.kt")
require('searchResultStartPageSuggestion = BoolPref("pref_searchResultStartPageSuggestion", false, recreate)' in prefs,
        "Online suggestions must default OFF for new installs")
require('searchResultCalculator = BoolPref("pref_searchResultCalculator", true)' in prefs,
        "Local calculator is enabled by default")
require("defaultValue = LawnchairSearchAlgorithm.LOCAL_SEARCH" in text("lawnchair/src/app/lawnchair/preferences2/PreferenceManager2.kt"),
        "New-install search algorithm remains on-device")
require('searchResultPeople = BoolPref("pref_searchResultPeople", false' in prefs,
        "Contacts search is opt-in")
require('searchResultFilesToggle = BoolPref("pref_searchResultFiles", false' in prefs,
        "File search is opt-in")
web = text("lawnchair/src/app/lawnchair/search/algorithms/engine/provider/web/WebSuggestionProvider.kt")
require("!prefs.searchResultStartPageSuggestion.get()" in web,
        "Web suggestions require opt-in")
require('Log.w(TAG, "Web suggestions unavailable")' in web and
        "emit(emptyList())" in web.split(".catch { cause ->", 1)[1].split(".map", 1)[0],
        "Network and provider exceptions must fail closed to empty suggestions")
require('!webProvider.getSearchUrl(query).startsWith("https://", ignoreCase = true)' in web,
        "Web suggestions fail closed with unusable search destination")
provider = text("lawnchair/src/app/lawnchair/search/algorithms/engine/provider/web/WebSearchProvider.kt")
require("else -> CustomWebSearchProvider" in provider, "Unknown provider IDs must not default to Google")
custom = text("lawnchair/src/app/lawnchair/search/algorithms/engine/provider/web/CustomWebSearchProvider.kt")
require("!isValidHttpsTemplate(suggestionsUrlTemplate)" in custom, "Validate custom suggestion URL")
require("!isValidHttpsTemplate(searchUrlTemplate)" in custom, "Validate custom search URL")
require("followRedirects(false)" in custom and "followSslRedirects(false)" in custom,
        "Custom suggestions must not silently redirect query terms")
require('Log.e(TAG, "Error during suggestion retrieval", e)' not in custom and
        'Log.e(TAG, "Failed to parse custom provider response", e)' not in custom,
        "Custom search failures must not log exception details carrying user queries")
require("GoogleWebSearchProvider.getSearchUrl(query)" not in custom, "No fallback to Google")
require('return ""' in custom and 'template.contains("%s")' in custom,
        "Invalid custom URLs fail closed")
require("URI(template.replace" in custom and "uri.rawAuthority?.contains(TEMPLATE_QUERY_MARKER) == false" in custom and
        "catch (_: URISyntaxException)" in custom and "uri.rawUserInfo == null" in custom and
        "uri.rawFragment == null" in custom and "uri.rawQuery?.contains(TEMPLATE_QUERY_MARKER)" in custom and
        "uri.rawPath?.contains(TEMPLATE_QUERY_MARKER)" in custom,
        "Custom query placeholder must be in HTTPS path/query, never hostname, credentials or fragment")
require("response.peekBody(MAX_SUGGESTION_BODY_BYTES + 1)" in custom and
        "boundedBody.contentLength() > MAX_SUGGESTION_BODY_BYTES" in custom,
        "Custom suggestions must not read unbounded HTTP response bodies")
history = text("lawnchair/src/app/lawnchair/search/algorithms/engine/provider/HistorySearchProvider.kt")
require('Log.w("HistorySearchProvider", "Recent search history unavailable")' in history and
        'Log.e("HistorySearchProvider", "Error during recent keyword retrieval", e)' not in history,
        "History failures must not log exception payloads containing search queries")
require("MAX_BUFFERED_HISTORY_ROWS = 512" in history and
        "val recentKeywords = ArrayDeque<RecentKeyword>()" in history and
        "recentKeywords.removeFirst()" in history and
        "recentKeywords.addLast(RecentKeyword(recentKeywordData))" in history and
        "recentKeywords.toList().asReversed()" in history and
        "if (maxResults <= 0) return@withContext emptyList<SearchResult.History>()" in history,
        "Recent search rows must be bounded before result assembly")
require('.filter { !it.getValueByKey("display1").isNullOrBlank() }' in history and
        '.distinctBy { it.getValueByKey("display1")!!.trim().lowercase() }' in history and
        '.take(maxResults.coerceIn(0, 20))' in history,
        "Recent searches must omit empty/duplicate queries and bound results")
search = text("lawnchair/src/app/lawnchair/search/algorithms/LawnchairLocalSearchAlgorithm.kt")
require('searchUrl.startsWith("https://", ignoreCase = true)' in search,
        "Do not create actions for invalid external destinations")
require("SearchResult.Action.MarketSearch" not in search,
        "No automatic app-market placement among local search results")
popup = text("lawnchair/src/app/lawnchair/ui/preferences/components/search/WebSearchProvider.kt")
require("CustomWebSearchProvider.isValidHttpsTemplate(it)" in popup,
        "Custom URL editor validates HTTPS")
require("enabled = !isErrorCheck(value.text)" in popup,
        "Invalid custom URLs cannot be saved")
drawer = text("lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt")
require("goreecloud_web_suggestions_privacy_description" in drawer,
        "Explicit search-transmission disclosure")
strings = ET.parse(root / "lawnchair/res/values/strings.xml").getroot()
require(any(x.get("name") == "goreecloud_web_suggestions_privacy_description" for x in strings),
        "Search disclosure string must exist")
require(any(x.get("name") == "goreecloud_calculator_copy_result_a11y" for x in strings),
        "Calculator copy accessibility string must exist")
calculator_row = text("lawnchair/src/app/lawnchair/allapps/views/SearchResultIconRow.kt")
require("contentDescription = null" in calculator_row and "goreecloud_calculator_copy_result_a11y" in calculator_row,
        "Calculator copy result is screen-reader labeled and cleared when recycled")
require('target.extras.getString("result").orEmpty()' in calculator_row,
        "Calculator clipboard action cannot copy null placeholder")
start = text("lawnchair/src/app/lawnchair/search/algorithms/engine/provider/web/BuiltInWebSearchProviders.kt")
require("startpage.lawnchair" not in start and '@Query("partner")' not in start,
        "No inherited partner identifiers in suggestions")
builtins = text("lawnchair/src/app/lawnchair/search/algorithms/engine/provider/web/BuiltInWebSearchProviders.kt")
require("followRedirects(false)" in builtins and "followSslRedirects(false)" in builtins and
        builtins.count(".client(nonRedirectingSuggestionClient)") == 5,
        "All built-in web suggestion services must reject automatic redirects")
require(builtins.count("service.getSuggestions(query = query)") == 5 and
        "service.getSuggestions(query = encodedQuery)" not in builtins,
        "All Retrofit @Query parameters must be encoded exactly once")
require("Log.e(" not in builtins, "Built-in suggestions must not log query-bearing exceptions")
require("MAX_BUILTIN_SUGGESTION_BODY_BYTES = 64 * 1024" in builtins and
        "val bytes = ByteArray(MAX_BUILTIN_SUGGESTION_BODY_BYTES + 1)" in builtins and
        "response.body()?.readBoundedSuggestionBody()" in builtins and
        "value.readBoundedSuggestionBody()" in builtins,
        "All built-in suggestion responses must be bounded, including streaming payloads")
require(".take(maxResults.coerceIn(0, 10))" in web,
        "Corrupted suggestion-count settings must not throw or flood search results")
for name in ("Startpage.kt", "StartpageEU.kt"):
    data = text("lawnchair/src/app/lawnchair/qsb/providers/" + name)
    require("segment=startpage.lawnchair" not in data, "No partner identifier in " + name)

if errors:
    for issue in errors:
        print("FAIL:", issue)
    raise SystemExit(1)
print("PASS: search queries local-first; online opt-in and safe endpoints enforced in source")
print("Android device and network observation are still required")
