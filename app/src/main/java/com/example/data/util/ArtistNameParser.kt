package com.example.data.util

import com.example.data.model.Song

/**
 * Intelligent Artist Metadata Parser & Multi-Artist Resolver.
 *
 * Solves common music library metadata issues with fast O(N) token & word-set matching:
 * 1. Comma-in-name artists (e.g., "Tyler, The Creator", "Earth, Wind & Fire", "Crosby, Stills, Nash & Young")
 *    are recognized as single cohesive artist entities instead of being split at the comma.
 * 2. Collaborations and features (e.g., "21 Savage, Tyler, The Creator", "21 Savage, Kendrick Lamar",
 *    "21 Savage feat. Drake", "Metro Boomin & 21 Savage (with Travis Scott)") are parsed into individual
 *    participating artists so that EVERY artist on a song has that song credited under their single artist profile.
 * 3. Known-Artist Database Cross-Matching: Any artist that appears in the library is matched against
 *    song artist and title strings (including "(feat. X)" in titles) in O(N) time without expensive regex loops.
 */
object ArtistNameParser {

    private data class CommaArtistRule(
        val canonical: String,
        val aliasesLower: List<String>,
        val aliasRegexes: List<Regex>
    )

    // Pre-compiled canonical multi-word artists whose names legitimately contain commas
    private val KNOWN_COMMA_ARTISTS = listOf(
        "Tyler, The Creator" to listOf("tyler, the creator", "tyler,the creator", "tyler the creator"),
        "Earth, Wind & Fire" to listOf("earth, wind & fire", "earth,wind & fire", "earth, wind and fire"),
        "Crosby, Stills, Nash & Young" to listOf("crosby, stills, nash & young", "crosby, stills & nash"),
        "Blood, Sweat & Tears" to listOf("blood, sweat & tears", "blood,sweat & tears"),
        "Emerson, Lake & Palmer" to listOf("emerson, lake & palmer"),
        "Peter, Paul and Mary" to listOf("peter, paul and mary", "peter, paul & mary"),
        "Panic! At The Disco" to listOf("panic! at the disco", "panic at the disco"),
        "Portugal. The Man" to listOf("portugal. the man")
    ).map { (canonical, aliases) ->
        CommaArtistRule(
            canonical = canonical,
            aliasesLower = aliases,
            aliasRegexes = aliases.map { Regex(Regex.escape(it), RegexOption.IGNORE_CASE) }
        )
    }

    // Pre-compiled regexes (allocated once for the lifetime of the app)
    private val FEATURE_DELIMITER_REGEX = Regex(
        pattern = """(?i)\s+(?:feat\.?|ft\.?|featuring|with|x|vs\.?|versus|presents|prod\.?\s+by)\s+|\s*[;/|•]\s*|\s+&\s+"""
    )

    private val TITLE_FEATURE_REGEX = Regex(
        pattern = """(?i)[\(\[\-]\s*(?:feat\.?|ft\.?|featuring|with)\s+([^\)\]]+)[\)\]]?"""
    )

    private val PAREN_FEATURE_REGEX = Regex(
        pattern = """(?i)[\(\[]\s*(?:feat\.?|ft\.?|featuring|with)\s+([^\)\]]+)[\)\]]"""
    )

    private val DYNAMIC_THE_PATTERN = Regex(
        pattern = """(?i)\b([A-Za-z0-9]+)\s*,\s*(The\s+[A-Za-z0-9]+)\b"""
    )

    private val TRAILING_NOISE_REGEX = Regex(
        pattern = """(?i)\s*[\(\[](?:official|audio|video|lyric|lyrics|remix|live|prod[^\)\]]*|hd|hq|explicit|clean|version|edit|remastered[^\)\]]*)[\)\]]\s*$"""
    )

    private val NON_ALPHANUM_REGEX = Regex("""[^a-z0-9\s]""")
    private val MULTI_SPACE_REGEX = Regex("""\s+""")

    private fun normalizeKey(name: String): String {
        return name.lowercase()
            .replace(NON_ALPHANUM_REGEX, " ")
            .replace(MULTI_SPACE_REGEX, " ")
            .trim()
    }

    /**
     * Cleans up a raw single artist token.
     */
    private fun cleanArtistToken(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.contains('(') || cleaned.contains('[')) {
            cleaned = TRAILING_NOISE_REGEX.replace(cleaned, "").trim()
        }
        cleaned = cleaned.trim(',', ';', '/', '-', '(', ')', '[', ']', ' ')
        if (cleaned.isEmpty()) return ""

        val lower = cleaned.lowercase().replace(MULTI_SPACE_REGEX, " ")
        for (rule in KNOWN_COMMA_ARTISTS) {
            if (rule.aliasesLower.any { it == lower }) {
                return rule.canonical
            }
        }
        return cleaned
    }

    /**
     * Splits a raw artist string (and optional song title with "(feat. ...)") into individual artist names,
     * while protecting comma-containing artist names like "Tyler, The Creator".
     */
    fun extractRawArtistsFromSong(rawArtist: String, rawTitle: String = ""): List<String> {
        if (rawArtist.isBlank() || rawArtist.equals("<unknown>", ignoreCase = true)) {
            return listOf("Unknown Artist")
        }

        val combinedSources = ArrayList<String>(2)
        combinedSources.add(rawArtist)
        if (rawTitle.contains("feat", ignoreCase = true) ||
            rawTitle.contains("ft.", ignoreCase = true) ||
            rawTitle.contains("with", ignoreCase = true)
        ) {
            TITLE_FEATURE_REGEX.findAll(rawTitle).forEach { match ->
                val featPart = match.groupValues.getOrNull(1)?.trim()
                if (!featPart.isNullOrEmpty()) {
                    combinedSources.add(featPart)
                }
            }
        }

        val extracted = ArrayList<String>(4)

        for (source in combinedSources) {
            var working = source.trim()
            val placeholderMap = HashMap<String, String>(2)

            if (working.contains(',')) {
                // 1. Temporarily replace known comma artists with placeholders
                val workingLower = working.lowercase()
                KNOWN_COMMA_ARTISTS.forEachIndexed { idx, rule ->
                    if (rule.aliasesLower.any { workingLower.contains(it) }) {
                        val token = "__COMMA_ARTIST_${idx}__"
                        for (regex in rule.aliasRegexes) {
                            if (regex.containsMatchIn(working)) {
                                working = regex.replace(working, token)
                                placeholderMap[token] = rule.canonical
                            }
                        }
                    }
                }

                // Also detect dynamic "<Name>, The <Noun>" pattern (e.g., "Tyler, The Creator" or "Chance, The Rapper")
                if (workingLower.contains(", the ")) {
                    var dynIdx = 0
                    working = DYNAMIC_THE_PATTERN.replace(working) { m ->
                        val token = "__DYN_THE_${dynIdx++}__"
                        val restored = "${m.groupValues[1].trim()}, ${m.groupValues[2].trim()}"
                        placeholderMap[token] = restored
                        token
                    }
                }
            }

            // 2. Replace parenthetical "(feat. X)" or "[with Y]" inside artist field with delimiter
            if (working.contains('(') || working.contains('[')) {
                working = PAREN_FEATURE_REGEX.replace(working, " , $1 ")
            }

            // 3. Split by standard feature/collaboration delimiters first
            val primarySegments = working.split(FEATURE_DELIMITER_REGEX)

            for (segment in primarySegments) {
                val commaParts = if (segment.contains(',')) segment.split(',') else listOf(segment)
                for (part in commaParts) {
                    var restored = part.trim()
                    if (placeholderMap.isNotEmpty() && restored.contains("__")) {
                        for ((token, canonical) in placeholderMap) {
                            if (restored.contains(token)) {
                                restored = restored.replace(token, canonical)
                            }
                        }
                    }
                    val cleaned = cleanArtistToken(restored)
                    if (cleaned.length >= 2 && !cleaned.equals("Unknown Artist", ignoreCase = true)) {
                        extracted.add(cleaned)
                    }
                }
            }
        }

        if (extracted.isEmpty()) {
            return listOf(rawArtist.trim().ifEmpty { "Unknown Artist" })
        }

        val seenKeys = HashSet<String>(extracted.size)
        val unique = ArrayList<String>(extracted.size)
        for (artist in extracted) {
            val key = normalizeKey(artist)
            if (key.isNotEmpty() && seenKeys.add(key)) {
                unique.add(artist)
            }
        }
        return unique
    }

    /**
     * Groups the entire song library into clean, deduplicated Artist entries in fast O(N) time:
     * - Builds a canonical Known-Artists dictionary from all parsed artists in the library.
     * - Scans each song's artist and title tokens against the Known-Artists hash table (1 to 4 word n-grams)
     *   so any known artist in the database is credited even if followed by commas, features, or extra text.
     */
    fun groupSongsBySmartArtist(allSongs: List<Song>): Map<String, List<Song>> {
        if (allSongs.isEmpty()) return emptyMap()

        // Step 1: First-pass extraction and canonical display name election
        val canonicalNameCount = HashMap<String, HashMap<String, Int>>()
        val songInitialKeys = HashMap<Long, MutableSet<String>>(allSongs.size)

        for (song in allSongs) {
            val parsedArtists = extractRawArtistsFromSong(song.artist, song.title)
            val keysForSong = songInitialKeys.getOrPut(song.id) { LinkedHashSet() }
            for (artistName in parsedArtists) {
                val normKey = normalizeKey(artistName)
                if (normKey.isNotEmpty()) {
                    keysForSong.add(normKey)
                    val counts = canonicalNameCount.getOrPut(normKey) { HashMap() }
                    counts[artistName] = (counts[artistName] ?: 0) + 1
                }
            }
        }

        // Choose the cleanest, most frequent display name for each normalized artist key
        val canonicalDisplayNames = HashMap<String, String>(canonicalNameCount.size)
        for ((normKey, nameVariants) in canonicalNameCount) {
            var bestName = ""
            var bestCount = -1
            for ((variant, count) in nameVariants) {
                val isBetter = if (count != bestCount) {
                    count > bestCount
                } else {
                    val vUpper = variant.firstOrNull()?.isUpperCase() == true
                    val bUpper = bestName.firstOrNull()?.isUpperCase() == true
                    if (vUpper != bUpper) vUpper else variant.length < bestName.length
                }
                if (isBetter) {
                    bestName = variant
                    bestCount = count
                }
            }
            if (bestName.isNotEmpty()) {
                canonicalDisplayNames[normKey] = bestName
            }
        }

        // Step 2: Fast O(1) N-gram Hash Lookup for Known Artists inside song artist/title strings
        val knownArtistKeySet = canonicalDisplayNames.keys
            .filterTo(HashSet()) { it.length >= 3 && it != "unknown artist" }

        val groupedByNormKey = HashMap<String, ArrayList<Song>>(canonicalDisplayNames.size)

        fun scanTextForKnownArtists(normalizedText: String, targetSet: MutableSet<String>) {
            if (normalizedText.isEmpty()) return
            val words = normalizedText.split(' ').filter { it.isNotEmpty() }
            val n = words.size
            for (i in 0 until n) {
                var phrase = words[i]
                if (phrase in knownArtistKeySet) targetSet.add(phrase)
                val maxWindow = minOf(i + 4, n)
                for (j in (i + 1) until maxWindow) {
                    phrase = "$phrase ${words[j]}"
                    if (phrase in knownArtistKeySet) {
                        targetSet.add(phrase)
                    }
                }
            }
        }

        for (song in allSongs) {
            val matchedKeys = songInitialKeys[song.id] ?: LinkedHashSet()
            scanTextForKnownArtists(normalizeKey(song.artist), matchedKeys)

            if (song.title.contains("feat", ignoreCase = true) ||
                song.title.contains("ft.", ignoreCase = true) ||
                song.title.contains("with", ignoreCase = true)
            ) {
                scanTextForKnownArtists(normalizeKey(song.title), matchedKeys)
            }

            if (matchedKeys.isEmpty()) {
                val fallbackKey = "unknown artist"
                canonicalDisplayNames.putIfAbsent(fallbackKey, "Unknown Artist")
                matchedKeys.add(fallbackKey)
            }

            for (key in matchedKeys) {
                groupedByNormKey.getOrPut(key) { ArrayList() }.add(song)
            }
        }

        // Step 3: Build final sorted Map<DisplayName, List<Song>>
        val sortedEntries = groupedByNormKey.entries
            .mapNotNull { (normKey, songsForArtist) ->
                val displayName = canonicalDisplayNames[normKey] ?: return@mapNotNull null
                displayName to songsForArtist.distinctBy { it.id }
            }
            .sortedBy { it.first.lowercase() }

        return LinkedHashMap<String, List<Song>>(sortedEntries.size).apply {
            for ((k, v) in sortedEntries) {
                put(k, v)
            }
        }
    }
}

