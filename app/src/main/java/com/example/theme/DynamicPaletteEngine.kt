package com.example.theme

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.LruCache
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.example.data.model.AppSettings
import com.example.data.model.Song
import com.example.data.util.AlbumArtUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class AppThemeColors(
    val primary: Color,
    val primaryLight: Color,
    val primaryDim: Color,
    val background: Color,
    val backgroundDark: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val card: Color,
    val textPrimary: Color = Color(0xFFF8FAFC),
    val textSecondary: Color = Color(0xFFCBD5E1),
    val textTertiary: Color = Color(0xFF64748B),
    val divider: Color,
    val onPrimary: Color = Color(0xFF090D16),
    val onPrimaryContainer: Color = Color(0xFFF8FAFC),
    val toneProfileLabel: String = "Balanced Vibrant",
    val contrastRatio: Float = 7.5f
)

data class VisualizerSpectrumPalette(
    val bandColors: List<Color>,
    val bandBottomColors: List<Color>
)

/**
 * Classification of extreme & standard album cover tonal distributions.
 */
enum class ExtremeToneRegime(val displayName: String) {
    PURE_WHITE_ALABASTER("Pure White / Alabaster High-Contrast"),
    PURE_OBSIDIAN_NOIR("Pure Obsidian & Platinum Noir"),
    CYBER_NOIR_MICRO_ACCENT("Obsidian Cyber-Noir (Neon Accent Boost)"),
    DEEP_OBSIDIAN_BLACK("Deep Black / Obsidian High-Contrast"),
    DARK_FOREST_EMERALD("Dark Forest / Emerald Chroma Boost"),
    DEEP_CHROMATIC_SHADOW("Deep Shadow Chromatic Boost"),
    PASTEL_HIGH_KEY("High-Key Pastel Contrast"),
    VIBRANT_BALANCED("Material You Tonal Harmony")
}

object DynamicPaletteEngine {
    private const val TAG = "DynamicPaletteEngine"

    // LRU cache keyed by "${songId}_${darkThemeMode}" for instant zero-jank theme switching
    private val themeCache = object : LruCache<String, AppThemeColors>(64) {}

    // 1. Hashtag Theme (Deep Purple #1A002C + Warm Orange #FFB700)
    val defaultDarkContrastTheme = AppThemeColors(
        primary = Color(0xFFFFB700), // Warm Orange
        primaryLight = Color(0xFFFFD54F),
        primaryDim = Color(0xFF5C3D00),
        background = Color(0xFF1A002C), // Deep Purple
        backgroundDark = Color(0xFF12001F),
        surface = Color(0xFF26053E),
        surfaceVariant = Color(0xFF350B54),
        card = Color(0xFF2D0848),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFE2D6EE),
        textTertiary = Color(0xFFA592B8),
        divider = Color(0xFF4B1973),
        onPrimary = Color(0xFF1F1200),
        onPrimaryContainer = Color(0xFFFFECB3),
        toneProfileLabel = "Hashtag Theme",
        contrastRatio = 10.2f
    )

    // 2. Citiview Theme (Metropolis Midnight #0B0F19 + Electric Skyline Cyan #00E5FF)
    val citiviewTheme = AppThemeColors(
        primary = Color(0xFF00E5FF), // Electric Skyline Cyan
        primaryLight = Color(0xFF67E8F9),
        primaryDim = Color(0xFF083344),
        background = Color(0xFF0B0F19), // Deep Metropolis Midnight
        backgroundDark = Color(0xFF060910),
        surface = Color(0xFF111827),
        surfaceVariant = Color(0xFF1E293B),
        card = Color(0xFF161F30),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        textTertiary = Color(0xFF64748B),
        divider = Color(0xFF1E293B),
        onPrimary = Color(0xFF021B24),
        onPrimaryContainer = Color(0xFFE0F7FA),
        toneProfileLabel = "Citiview Theme",
        contrastRatio = 11.5f
    )

    // Sleek Onyx / Slate theme fallback
    val defaultTheme = defaultDarkContrastTheme

    /**
     * Extracts dynamic system wallpaper accent colors using Android's Material You (Monet) engine,
     * refined with WCAG high-contrast surface/foreground enforcement so extreme system wallpapers
     * (pure white, pitch black, or very dark green wallpapers) remain crisp and legible.
     */
    fun extractMaterialYouTheme(context: Context, fallbackSong: Song? = null): AppThemeColors {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val m3 = dynamicDarkColorScheme(context)
                val primaryHsl = FloatArray(3)
                ColorUtils.colorToHSL(m3.primary.toArgb(), primaryHsl)

                // Ensure Material You primary has at least 6.5:1 contrast against the dark surface
                val bgInt = ColorUtils.blendARGB(m3.background.toArgb(), 0xFF000000.toInt(), 0.22f)
                val bgDarkInt = ColorUtils.blendARGB(bgInt, 0xFF000000.toInt(), 0.42f)
                val surfaceInt = ColorUtils.blendARGB(m3.surface.toArgb(), m3.primary.toArgb(), 0.06f)
                val cardInt = ColorUtils.blendARGB(m3.surfaceVariant.toArgb(), 0xFF000000.toInt(), 0.25f)
                val surfaceVarInt = ColorUtils.blendARGB(m3.surfaceVariant.toArgb(), m3.primary.toArgb(), 0.12f)

                val highContrastPrimaryInt = enforceMinimumContrast(
                    foregroundRgb = m3.primary.toArgb(),
                    backgroundRgb = surfaceInt,
                    minContrast = 6.2
                )
                val primaryLightInt = enforceMinimumContrast(
                    foregroundRgb = m3.primaryContainer.toArgb(),
                    backgroundRgb = surfaceInt,
                    minContrast = 7.5
                )
                val primaryDimInt = ColorUtils.HSLToColor(
                    floatArrayOf(primaryHsl[0], (primaryHsl[1] * 0.75f).coerceIn(0.25f, 0.85f), 0.24f)
                )
                val dividerInt = enforceMinimumContrast(
                    foregroundRgb = m3.outlineVariant.toArgb(),
                    backgroundRgb = bgInt,
                    minContrast = 2.1
                )

                val onPrimaryInt = computeOptimalOnColor(highContrastPrimaryInt)
                val ratio = ColorUtils.calculateContrast(highContrastPrimaryInt, bgInt).toFloat()

                AppThemeColors(
                    primary = Color(highContrastPrimaryInt),
                    primaryLight = Color(primaryLightInt),
                    primaryDim = Color(primaryDimInt),
                    background = Color(bgInt),
                    backgroundDark = Color(bgDarkInt),
                    surface = Color(surfaceInt),
                    surfaceVariant = Color(surfaceVarInt),
                    card = Color(cardInt),
                    textPrimary = Color(0xFFF8FAFC),
                    textSecondary = Color(
                        enforceMinimumContrast(m3.onSurfaceVariant.toArgb(), surfaceInt, 5.5)
                    ),
                    textTertiary = Color(
                        enforceMinimumContrast(m3.outline.toArgb(), surfaceInt, 3.6)
                    ),
                    divider = Color(dividerInt),
                    onPrimary = Color(onPrimaryInt),
                    onPrimaryContainer = Color(0xFFF8FAFC),
                    toneProfileLabel = "Material You (High-Contrast Monet)",
                    contrastRatio = ratio
                )
            } else {
                // Pre-Android 12 Material You Tonal Seed synthesis
                buildHighContrastMaterialYouTonalPalette(
                    seedHue = 198f,
                    seedSat = 0.86f,
                    sourceLightness = 0.52f,
                    regime = ExtremeToneRegime.VIBRANT_BALANCED
                )
            }
        } catch (e: Exception) {
            defaultDarkContrastTheme
        }
    }

    /**
     * Resolves the app theme according to the user's selected Theme Switching Option:
     * 1. Material You (Dynamic System Accent / Monet + High-Contrast Refinement)
     * 2. Default Dark Contrast (Deep Purple #1A002C + Warm Orange #FFB700)
     * 3. Dynamic Album Art (Custom Extreme-Tone & High-Contrast Material You Palette Engine)
     */
    suspend fun resolveTheme(
        context: Context,
        themeEngine: String,
        song: Song?,
        darkThemeMode: String = "Onyx Dark"
    ): AppThemeColors {
        val baseTheme = when (themeEngine) {
            AppSettings.THEME_MATERIAL_YOU -> {
                extractMaterialYouTheme(context, song)
            }
            AppSettings.THEME_DARK_CONTRAST -> {
                defaultDarkContrastTheme
            }
            AppSettings.THEME_CITIVIEW -> {
                citiviewTheme
            }
            AppSettings.THEME_ALBUM_ART -> {
                extractThemeColors(context, song)
            }
            else -> {
                extractThemeColors(context, song)
            }
        }
        return if (darkThemeMode.equals("Pure AMOLED", ignoreCase = true)) {
            val amoledBg = 0xFF000000.toInt()
            val primaryInt = enforceMinimumContrast(baseTheme.primary.toArgb(), amoledBg, 7.0)
            val primaryLightInt = enforceMinimumContrast(baseTheme.primaryLight.toArgb(), amoledBg, 8.5)
            val tintedSurface = ColorUtils.blendARGB(0xFF09090D.toInt(), primaryInt, 0.07f)
            val tintedCard = ColorUtils.blendARGB(0xFF111118.toInt(), primaryInt, 0.10f)
            val tintedVariant = ColorUtils.blendARGB(0xFF181822.toInt(), primaryInt, 0.14f)
            val tintedDivider = ColorUtils.blendARGB(0xFF242432.toInt(), primaryInt, 0.22f)
            baseTheme.copy(
                primary = Color(primaryInt),
                primaryLight = Color(primaryLightInt),
                background = Color(amoledBg),
                backgroundDark = Color(amoledBg),
                surface = Color(tintedSurface),
                card = Color(tintedCard),
                surfaceVariant = Color(tintedVariant),
                divider = Color(tintedDivider),
                onPrimary = Color(computeOptimalOnColor(primaryInt)),
                contrastRatio = ColorUtils.calculateContrast(primaryInt, amoledBg).toFloat()
            )
        } else {
            baseTheme
        }
    }

    /**
     * Custom High-Contrast Material You Palette Extractor for Album Art.
     *
     * Specifically engineered to handle extreme image tones where standard Android `Palette` fails:
     * 1. **Pure White / Alabaster / High-Key Silver Covers**:
     *    Extracts micro-chromatic undertones (or a crisp Pearl/Alabaster monochrome seed) and builds
     *    a high-contrast obsidian-pearl Material You scheme (Contrast >= 11:1) with luminous pearl
     *    accents and dark slate/charcoal tonal containers.
     * 2. **Deep Blacks / Obsidian / Ultra-Dark Noir Covers**:
     *    Detects shadow-locked chroma down to 1.5% lightness via custom unclipped `Palette.Filter` +
     *    36-bin circular hue histogram, boosting buried accents into high-contrast foreground tokens
     *    or synthesizing a Titanium-Silver on Pitch-Obsidian theme.
     * 3. **Dark Greens (Forest, Pine, Deep Emerald, Moss, Olive)**:
     *    Applies human visual luminance compensation and green-band chroma expansion (`70°..185°`) so
     *    dark green album covers yield lush, unmistakable deep forest surfaces and vivid emerald/jade
     *    Material You accents with high contrast.
     */
    suspend fun extractThemeColors(context: Context, song: Song?): AppThemeColors = withContext(Dispatchers.IO) {
        if (song == null) {
            return@withContext defaultDarkContrastTheme
        }

        val cacheKey = "${song.id}_${song.customArtworkUri ?: ""}"
        themeCache.get(cacheKey)?.let { return@withContext it }

        try {
            val bitmap: Bitmap? = AlbumArtUtil.getArtworkBitmap(
                context = context,
                songPath = song.path,
                songUri = song.uri,
                albumId = song.albumId
            )

            if (bitmap != null) {
                val extracted = extractHighContrastPaletteFromBitmap(bitmap)
                themeCache.put(cacheKey, extracted)
                return@withContext extracted
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Palette extraction failed: ${e.message}")
        }

        // Deterministic Material You tonal fallback if track has no embedded/MediaStore cover art
        val hashHue = (abs((song.title + song.artist + song.album).hashCode()) % 360).toFloat()
        val fallback = buildHighContrastMaterialYouTonalPalette(
            seedHue = hashHue,
            seedSat = 0.76f,
            sourceLightness = 0.36f,
            regime = ExtremeToneRegime.VIBRANT_BALANCED
        )
        themeCache.put(cacheKey, fallback)
        fallback
    }

    /**
     * Core bitmap analysis & custom Palette generation pipeline.
     * Exposed as internal/public helper so unit tests or preview tools can pass a `Bitmap` directly.
     */
    fun extractHighContrastPaletteFromBitmap(bitmap: Bitmap): AppThemeColors {
        // 1. Run high-resolution 36-bin circular HSL + perceptual luminance distribution pass
        val stats = analyzeBitmapColorDistribution(bitmap)

        // 2. Run Android Palette with 64 quantization buckets and a custom non-discarding filter
        //    (Standard Palette discards L < 0.05 and L > 0.95, which wipes out pure white, deep black, and dark green covers!)
        val palette = Palette.from(bitmap)
            .maximumColorCount(64)
            .clearFilters()
            .addFilter { _, hsl ->
                // Keep all pixels except completely transparent/corrupt artifacts; allow extreme whites & deep blacks
                hsl[2] in 0.005f..0.995f
            }
            .generate()

        val allSwatches = palette.swatches
        val totalPopulation = allSwatches.sumOf { it.population }.coerceAtLeast(1)

        // 3. Classify the image's Extreme Tone Regime first
        val regime = classifyExtremeToneRegime(stats)

        // 4. Handle Pure White / Alabaster / High-Key Minimalist Covers
        if (regime == ExtremeToneRegime.PURE_WHITE_ALABASTER) {
            return createPureWhiteHighContrastTheme(stats, allSwatches, totalPopulation)
        }

        // 5. Handle Pure Obsidian / Pitch Black / Monochrome Noir Covers (e.g. Donda, Metallica Black Album, AC/DC Back in Black)
        if (regime == ExtremeToneRegime.PURE_OBSIDIAN_NOIR) {
            return createPureObsidianNoirTheme(stats)
        }

        // 6. Handle Cyber-Noir covers with subtle/small focal micro-accents on deep black (e.g. Dark Side of the Moon prism, red DAMN title)
        if (regime == ExtremeToneRegime.CYBER_NOIR_MICRO_ACCENT) {
            return createCyberNoirMicroAccentTheme(
                accentHue = stats.microAccentHue,
                accentSat = stats.microAccentSat,
                accentLight = stats.microAccentLight,
                stats = stats
            )
        }

        // 7. Multi-criteria contrast-aware swatch scoring
        //    Prioritizes swatches that carry true chromatic identity AND can achieve high contrast
        val hsl = FloatArray(3)
        val scoredCandidates = allSwatches.mapNotNull { swatch ->
            ColorUtils.colorToHSL(swatch.rgb, hsl)
            val hue = hsl[0]
            val sat = hsl[1]
            val light = hsl[2]
            val popRatio = swatch.population.toFloat() / totalPopulation.toFloat()

            // Allow micro-saturation down to 0.035f in deep shadows so ultra-dark forest green / midnight blue covers are captured
            val minSatThreshold = if (light < 0.22f) 0.035f else 0.055f
            if (sat < minSatThreshold || light !in 0.01f..0.98f) {
                return@mapNotNull null
            }

            // Green/Emerald/Forest band detection (68°..185°)
            val isGreenHue = hue in 68f..185f
            val greenMultiplier = when {
                isGreenHue && light < 0.30f -> 1.85f // Strong priority for dark forest / pine / emerald greens
                isGreenHue -> 1.45f
                else -> 1.0f
            }

            // Deep shadow chromatic boost so dark covers don't lose to tiny gray noise swatches
            val shadowChromaMultiplier = if (light in 0.015f..0.24f && sat >= 0.05f) 1.55f else 1.0f

            // High-contrast potential score against a dark Material You surface
            val simulatedFgRgb = ColorUtils.HSLToColor(
                floatArrayOf(hue, sat.coerceIn(0.45f, 0.92f), light.coerceIn(0.58f, 0.78f))
            )
            val simulatedBgRgb = ColorUtils.HSLToColor(
                floatArrayOf(hue, (sat * 0.45f).coerceIn(0.15f, 0.45f), 0.06f)
            )
            val contrastPotential = (ColorUtils.calculateContrast(simulatedFgRgb, simulatedBgRgb).toFloat() / 7.0f)
                .coerceIn(0.75f, 1.5f)

            // Population curve: sqrt(popRatio) prevents a 90% black background from drowning out a 10% vivid emblem,
            // while still respecting dominant dark green/crimson fields.
            val popWeight = sqrt(popRatio.toDouble()).toFloat() * 0.65f + popRatio * 0.35f
            val chromaWeight = 0.25f + sat.pow(0.75f) * 0.75f

            val totalScore = popWeight * chromaWeight * greenMultiplier * shadowChromaMultiplier * contrastPotential
            SwatchCandidate(
                rgb = swatch.rgb,
                hue = hue,
                sat = sat,
                light = light,
                populationRatio = popRatio,
                score = totalScore
            )
        }.sortedByDescending { it.score }

        // 8. Route to specialized Dark Green / Deep Black / High-Contrast Material You generators
        if (regime == ExtremeToneRegime.DARK_FOREST_EMERALD ||
            (scoredCandidates.isNotEmpty() && scoredCandidates.first().hue in 68f..185f && stats.avgLightness < 0.34f)
        ) {
            val topGreen = scoredCandidates.firstOrNull { it.hue in 68f..185f } ?: scoredCandidates.firstOrNull()
            val greenHue = topGreen?.hue ?: stats.dominantChromaticHue
            val greenSat = max(topGreen?.sat ?: 0f, stats.dominantChromaticSat).coerceAtLeast(0.18f)
            val greenLight = topGreen?.light ?: stats.avgLightness
            return createDarkGreenHighContrastTheme(greenHue, greenSat, greenLight, stats)
        }

        if (regime == ExtremeToneRegime.DEEP_OBSIDIAN_BLACK) {
            val bestShadow = scoredCandidates.firstOrNull()
            if (bestShadow == null || (!stats.hasMicroAccent && stats.dominantChromaticSat < 0.05f)) {
                return createPureObsidianNoirTheme(stats)
            }
            return createDeepBlackHighContrastTheme(bestShadow, stats)
        }

        if (scoredCandidates.isNotEmpty()) {
            val primaryCandidate = scoredCandidates.first()
            // Find a secondary harmonic or high-contrast companion swatch if available
            val secondaryCandidate = scoredCandidates.drop(1).firstOrNull {
                abs(hueDistance(it.hue, primaryCandidate.hue)) >= 18f || abs(it.light - primaryCandidate.light) >= 0.18f
            }
            return buildHighContrastMaterialYouTonalPalette(
                seedHue = primaryCandidate.hue,
                seedSat = primaryCandidate.sat,
                sourceLightness = primaryCandidate.light,
                regime = regime,
                secondaryHue = secondaryCandidate?.hue
            )
        }

        // 9. Ultra-low-saturation cover fallback using pixel histogram
        return if (stats.avgLightness >= 0.60f) {
            createPureWhiteHighContrastTheme(stats, allSwatches, totalPopulation)
        } else if (stats.pitchBlackRatio >= 0.45f || stats.deepBlackRatio >= 0.65f || stats.dominantChromaticSat < 0.035f) {
            createPureObsidianNoirTheme(stats)
        } else if (stats.dominantChromaticSat >= 0.035f) {
            buildHighContrastMaterialYouTonalPalette(
                seedHue = stats.dominantChromaticHue,
                seedSat = stats.dominantChromaticSat,
                sourceLightness = stats.avgLightness,
                regime = ExtremeToneRegime.DEEP_CHROMATIC_SHADOW
            )
        } else {
            createPureObsidianNoirTheme(stats)
        }
    }

    private data class SwatchCandidate(
        val rgb: Int,
        val hue: Float,
        val sat: Float,
        val light: Float,
        val populationRatio: Float,
        val score: Float
    )

    private data class BitmapColorStats(
        val avgLightness: Float,
        val avgSaturation: Float,
        val avgLuminance: Float,
        val whiteRatio: Float,
        val nearWhiteRatio: Float,
        val pitchBlackRatio: Float,
        val deepBlackRatio: Float,
        val darkRatio: Float,
        val greenPixelRatio: Float,
        val dominantChromaticHue: Float,
        val dominantChromaticSat: Float,
        val secondaryChromaticHue: Float,
        val maxPixelSaturation: Float,
        val hasMicroAccent: Boolean = false,
        val microAccentHue: Float = 0f,
        val microAccentSat: Float = 0f,
        val microAccentLight: Float = 0f,
        val isMonochromeNoir: Boolean = false
    )

    private fun classifyExtremeToneRegime(stats: BitmapColorStats): ExtremeToneRegime {
        // 1. Pure White / Alabaster / High-Key Minimalist cover
        if (stats.whiteRatio >= 0.36f ||
            stats.nearWhiteRatio >= 0.52f ||
            (stats.avgLightness >= 0.74f && stats.avgSaturation <= 0.24f)
        ) {
            return ExtremeToneRegime.PURE_WHITE_ALABASTER
        }

        // 2. Pure Obsidian & Platinum Noir (Monochrome / Achromatic pitch-black cover)
        // E.g. Kanye West 'Donda', Metallica 'The Black Album', AC/DC 'Back in Black', Arctic Monkeys 'AM'
        if ((stats.pitchBlackRatio >= 0.45f || stats.deepBlackRatio >= 0.70f || (stats.darkRatio >= 0.82f && stats.avgLightness <= 0.14f))
            && (!stats.hasMicroAccent && (stats.isMonochromeNoir || stats.dominantChromaticSat < 0.08f))
        ) {
            return ExtremeToneRegime.PURE_OBSIDIAN_NOIR
        }

        // 3. Cyber-Noir with Micro-Accent (e.g. Pink Floyd's 'Dark Side of the Moon' prism, red logo/title on black)
        if ((stats.deepBlackRatio >= 0.38f || stats.darkRatio >= 0.60f) && stats.hasMicroAccent) {
            return ExtremeToneRegime.CYBER_NOIR_MICRO_ACCENT
        }

        // 4. Dark Green / Forest / Deep Emerald / Moss / Pine cover
        val isDominantGreen = stats.dominantChromaticHue in 68f..185f
        if ((stats.greenPixelRatio >= 0.14f && stats.avgLightness <= 0.42f) ||
            (isDominantGreen && stats.darkRatio >= 0.35f && stats.dominantChromaticSat >= 0.04f)
        ) {
            return ExtremeToneRegime.DARK_FOREST_EMERALD
        }

        // 5. Deep Black / Obsidian with moderate shadow chromatic undertones
        if (stats.deepBlackRatio >= 0.45f ||
            (stats.darkRatio >= 0.70f && stats.avgLightness <= 0.18f && stats.maxPixelSaturation < 0.35f)
        ) {
            return ExtremeToneRegime.DEEP_OBSIDIAN_BLACK
        }

        // 6. Deep Chromatic Shadow cover (e.g. dark burgundy, midnight navy, dark violet, deep mahogany)
        if (stats.avgLightness <= 0.24f || stats.darkRatio >= 0.58f) {
            return ExtremeToneRegime.DEEP_CHROMATIC_SHADOW
        }

        // 7. High-Key Pastel cover
        if (stats.avgLightness >= 0.65f) {
            return ExtremeToneRegime.PASTEL_HIGH_KEY
        }

        return ExtremeToneRegime.VIBRANT_BALANCED
    }

    /**
     * High-precision 48x48 grid sampling with circular hue accumulation, shadow-chroma amplification,
     * and a micro-accent harvester that captures small focal highlights (e.g. 0.2% - 2% colored badges or text) on dark covers.
     */
    private fun analyzeBitmapColorDistribution(bitmap: Bitmap): BitmapColorStats {
        val w = bitmap.width.coerceAtLeast(1)
        val h = bitmap.height.coerceAtLeast(1)
        val stepX = (w / 48).coerceAtLeast(1)
        val stepY = (h / 48).coerceAtLeast(1)

        val hsl = FloatArray(3)
        val hueBins = FloatArray(36) // 10-degree buckets
        val hueSatMax = FloatArray(36)

        // Dedicated micro-accent harvester bins for dark covers
        val microHueBins = FloatArray(36)
        val microSatMax = FloatArray(36)
        val microLightSum = FloatArray(36)
        val microCount = IntArray(36)

        var totalLight = 0f
        var totalSat = 0f
        var totalLum = 0f
        var whiteCount = 0
        var nearWhiteCount = 0
        var pitchBlackCount = 0
        var deepBlackCount = 0
        var darkCount = 0
        var greenCount = 0
        var maxSat = 0f
        var samples = 0

        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                ColorUtils.colorToHSL(pixel, hsl)
                val hue = hsl[0]
                val sat = hsl[1]
                val light = hsl[2]
                val lum = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f

                totalLight += light
                totalSat += sat
                totalLum += lum
                if (sat > maxSat) maxSat = sat

                // Pure white / alabaster detection
                if (light >= 0.88f && sat <= 0.18f) whiteCount++
                if (light >= 0.76f && sat <= 0.26f) nearWhiteCount++

                // Pitch black & deep black detection
                if (light <= 0.04f || (r <= 12 && g <= 12 && b <= 12)) pitchBlackCount++
                if (light <= 0.09f || (r <= 24 && g <= 24 && b <= 24)) deepBlackCount++
                if (light <= 0.25f) darkCount++

                // Detect dark forest green / emerald pixels even at very low RGB values
                val isGreenDominantRgb = g > r + 3 && g >= b && hue in 68f..185f && sat >= 0.04f
                if (isGreenDominantRgb) {
                    greenCount++
                }

                // Micro-accent harvester: look for focal saturated pixels on dark covers
                if (sat >= 0.18f && light in 0.08f..0.88f) {
                    val mBin = ((hue / 10f).toInt()).coerceIn(0, 35)
                    val weight = sat * (1.0f - abs(light - 0.52f))
                    microHueBins[mBin] += weight
                    if (sat > microSatMax[mBin]) {
                        microSatMax[mBin] = sat
                    }
                    microLightSum[mBin] += light
                    microCount[mBin]++
                }

                // Accumulate chromatic hue histogram across full lightness range [0.01 .. 0.98]
                if (sat >= 0.03f && light in 0.01f..0.98f) {
                    val bin = ((hue / 10f).toInt()).coerceIn(0, 35)
                    val greenBoost = if (hue in 68f..185f) 1.55f else 1.0f
                    val shadowBoost = if (light in 0.015f..0.26f) 1.45f else 1.0f
                    val weight = (0.15f + sat * 0.85f) * greenBoost * shadowBoost
                    hueBins[bin] += weight
                    if (sat > hueSatMax[bin]) {
                        hueSatMax[bin] = sat
                    }
                }

                samples++
                x += stepX
            }
            y += stepY
        }

        val count = samples.coerceAtLeast(1).toFloat()
        val darkRatio = darkCount / count
        val deepBlackRatio = deepBlackCount / count
        val pitchBlackRatio = pitchBlackCount / count

        // Evaluate micro-accent peak for predominantly dark covers
        var hasMicroAccent = false
        var microAccentHue = 0f
        var microAccentSat = 0f
        var microAccentLight = 0.5f

        if (darkRatio >= 0.58f || deepBlackRatio >= 0.36f) {
            var bestMBin = 0
            var bestMWeight = -1f
            for (i in microHueBins.indices) {
                if (microHueBins[i] > bestMWeight) {
                    bestMWeight = microHueBins[i]
                    bestMBin = i
                }
            }
            if (bestMWeight > 0.8f && microCount[bestMBin] >= 3 && microSatMax[bestMBin] >= 0.22f) {
                hasMicroAccent = true
                microAccentHue = (bestMBin * 10f + 5f) % 360f
                microAccentSat = microSatMax[bestMBin]
                microAccentLight = (microLightSum[bestMBin] / microCount[bestMBin].coerceAtLeast(1)).coerceIn(0.20f, 0.80f)
            }
        }

        // Smooth circular hue bins (3-bin moving window) so boundary hues reinforce each other
        val smoothedBins = FloatArray(36) { i ->
            val prev = hueBins[(i + 35) % 36]
            val curr = hueBins[i]
            val next = hueBins[(i + 1) % 36]
            prev * 0.22f + curr * 0.56f + next * 0.22f
        }

        var bestBin = 0
        var bestWeight = -1f
        for (i in smoothedBins.indices) {
            if (smoothedBins[i] > bestWeight) {
                bestWeight = smoothedBins[i]
                bestBin = i
            }
        }

        var secondBin = (bestBin + 18) % 36
        var secondWeight = -1f
        for (i in smoothedBins.indices) {
            val binDist = min(abs(i - bestBin), 36 - abs(i - bestBin))
            if (binDist >= 3 && smoothedBins[i] > secondWeight) {
                secondWeight = smoothedBins[i]
                secondBin = i
            }
        }

        val isMonochromeNoir = maxSat < 0.08f ||
            (deepBlackRatio >= 0.65f && bestWeight <= 0.5f && !hasMicroAccent)

        return BitmapColorStats(
            avgLightness = totalLight / count,
            avgSaturation = totalSat / count,
            avgLuminance = totalLum / count,
            whiteRatio = whiteCount / count,
            nearWhiteRatio = nearWhiteCount / count,
            pitchBlackRatio = pitchBlackRatio,
            deepBlackRatio = deepBlackRatio,
            darkRatio = darkRatio,
            greenPixelRatio = greenCount / count,
            dominantChromaticHue = (bestBin * 10f + 5f) % 360f,
            dominantChromaticSat = if (bestWeight > 0f) hueSatMax[bestBin] else 0f,
            secondaryChromaticHue = (secondBin * 10f + 5f) % 360f,
            maxPixelSaturation = maxSat,
            hasMicroAccent = hasMicroAccent,
            microAccentHue = microAccentHue,
            microAccentSat = microAccentSat,
            microAccentLight = microAccentLight,
            isMonochromeNoir = isMonochromeNoir
        )
    }

    /**
     * 1. PURE WHITE / ALABASTER / SILVER HIGH-CONTRAST MATERIAL YOU GENERATOR
     *
     * When an album cover is predominantly pure white, cream, alabaster, or light silver:
     * - If the cover has a subtle colored graphic/text accent (e.g. a small red/emerald/cobalt logo on a white cover),
     *   we weave that subtle hue into the dark obsidian-pearl surfaces while keeping the primary accent
     *   crisp, luminous pearl-white (`L = 0.95`) with > 12:1 contrast ratio.
     * - Prevents washed-out gray-on-gray or fallback to default purple/orange.
     */
    private fun createPureWhiteHighContrastTheme(
        stats: BitmapColorStats,
        swatches: List<Palette.Swatch>,
        totalPopulation: Int
    ): AppThemeColors {
        // Check if there is a small vibrant focal element on the white cover (e.g. 2%..20% colored artwork on white background)
        val hsl = FloatArray(3)
        val focalSwatch = swatches
            .mapNotNull { s ->
                ColorUtils.colorToHSL(s.rgb, hsl)
                val pop = s.population.toFloat() / totalPopulation.toFloat()
                if (hsl[1] >= 0.25f && hsl[2] in 0.12f..0.78f && pop >= 0.008f) {
                    Pair(hsl.clone(), pop * hsl[1])
                } else null
            }
            .maxByOrNull { it.second }

        val undertoneHue = focalSwatch?.first?.get(0) ?: stats.dominantChromaticHue
        val hasFocalHue = focalSwatch != null || stats.dominantChromaticSat >= 0.12f
        val surfaceTintSat = if (hasFocalHue) 0.16f else 0.05f

        // High-contrast crisp Alabaster/Pearl primary + subtle focal tint in secondary/containers
        val primaryRgb = enforceMinimumContrast(
            foregroundRgb = ColorUtils.HSLToColor(
                floatArrayOf(undertoneHue, if (hasFocalHue) 0.18f else 0.04f, 0.95f)
            ),
            backgroundRgb = ColorUtils.HSLToColor(floatArrayOf(undertoneHue, surfaceTintSat, 0.07f)),
            minContrast = 11.5
        )
        val primaryLightRgb = ColorUtils.HSLToColor(floatArrayOf(undertoneHue, 0.08f, 0.99f))
        val primaryDimRgb = if (hasFocalHue) {
            ColorUtils.HSLToColor(floatArrayOf(undertoneHue, 0.42f, 0.32f))
        } else {
            ColorUtils.HSLToColor(floatArrayOf(215f, 0.14f, 0.30f))
        }

        // Deep architectural slate-obsidian background so pure white artwork & pearl accents pop dramatically
        val bgRgb = ColorUtils.HSLToColor(floatArrayOf(undertoneHue, surfaceTintSat, 0.065f))
        val bgDarkRgb = ColorUtils.HSLToColor(floatArrayOf(undertoneHue, surfaceTintSat, 0.035f))
        val surfaceRgb = ColorUtils.HSLToColor(floatArrayOf(undertoneHue, surfaceTintSat, 0.115f))
        val cardRgb = ColorUtils.HSLToColor(floatArrayOf(undertoneHue, surfaceTintSat, 0.155f))
        val surfaceVarRgb = ColorUtils.HSLToColor(floatArrayOf(undertoneHue, surfaceTintSat, 0.21f))
        val dividerRgb = enforceMinimumContrast(
            foregroundRgb = ColorUtils.HSLToColor(floatArrayOf(undertoneHue, surfaceTintSat, 0.32f)),
            backgroundRgb = bgRgb,
            minContrast = 2.4
        )

        val ratio = ColorUtils.calculateContrast(primaryRgb, bgRgb).toFloat()

        return AppThemeColors(
            primary = Color(primaryRgb),
            primaryLight = Color(primaryLightRgb),
            primaryDim = Color(primaryDimRgb),
            background = Color(bgRgb),
            backgroundDark = Color(bgDarkRgb),
            surface = Color(surfaceRgb),
            surfaceVariant = Color(surfaceVarRgb),
            card = Color(cardRgb),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFE2E8F0),
            textTertiary = Color(0xFF94A3B8),
            divider = Color(dividerRgb),
            onPrimary = Color(computeOptimalOnColor(primaryRgb)),
            onPrimaryContainer = Color(0xFFFFFFFF),
            toneProfileLabel = ExtremeToneRegime.PURE_WHITE_ALABASTER.displayName,
            contrastRatio = ratio
        )
    }

    /**
     * 2. DARK GREEN (FOREST / PINE / DEEP EMERALD / MOSS) HIGH-CONTRAST GENERATOR
     *
     * Dark greens on album covers (`L = 0.04..0.28`, `H = 68°..185°`) often get crushed into muddy
     * gray-black by standard color quantization. This generator:
     * - Expands chroma into a luminous Emerald/Jade/Sage-Mint primary (`L = 0.62..0.70`, `S = 0.62..0.88`)
     *   that achieves >= 7.2:1 contrast against the deep forest background.
     * - Builds rich, unmistakable deep pine/forest tonal surfaces (`L = 0.05..0.18`) with high saturation
     *   so the entire Material You UI feels immersed in deep botanical green.
     */
    private fun createDarkGreenHighContrastTheme(
        rawHue: Float,
        rawSat: Float,
        sourceLightness: Float,
        stats: BitmapColorStats
    ): AppThemeColors {
        // Keep hue anchored in the natural green spectrum (72°..178°)
        val greenHue = rawHue.coerceIn(72f, 178f)

        // Boost chroma so even dark olive/pine/forest covers produce a vivid, high-contrast emerald/mint accent
        val boostedPrimarySat = (rawSat * 1.55f + 0.24f).coerceIn(0.56f, 0.88f)
        val targetPrimaryLight = when {
            sourceLightness < 0.12f -> 0.64f // Deep forest shadow -> luminous emerald
            sourceLightness < 0.25f -> 0.67f
            else -> 0.70f
        }

        // Rich deep forest background & layered botanical surfaces
        val bgSat = (boostedPrimarySat * 0.68f).coerceIn(0.36f, 0.62f)
        val bgRgb = ColorUtils.HSLToColor(floatArrayOf(greenHue, bgSat, 0.052f))
        val bgDarkRgb = ColorUtils.HSLToColor(floatArrayOf(greenHue, (bgSat * 1.1f).coerceAtMost(0.70f), 0.028f))
        val surfaceRgb = ColorUtils.HSLToColor(floatArrayOf(greenHue, bgSat * 0.95f, 0.095f))
        val cardRgb = ColorUtils.HSLToColor(floatArrayOf(greenHue, bgSat * 0.90f, 0.135f))
        val surfaceVarRgb = ColorUtils.HSLToColor(floatArrayOf(greenHue, bgSat * 0.85f, 0.185f))

        val candidatePrimaryRgb = ColorUtils.HSLToColor(
            floatArrayOf(greenHue, boostedPrimarySat, targetPrimaryLight)
        )
        val primaryRgb = enforceMinimumContrast(
            foregroundRgb = candidatePrimaryRgb,
            backgroundRgb = surfaceRgb,
            minContrast = 6.8
        )

        val primaryLightRgb = enforceMinimumContrast(
            foregroundRgb = ColorUtils.HSLToColor(
                floatArrayOf((greenHue + 6f) % 360f, (boostedPrimarySat * 0.90f).coerceIn(0.48f, 0.92f), 0.80f)
            ),
            backgroundRgb = surfaceRgb,
            minContrast = 9.0
        )

        // Material You primaryContainer (deep lush emerald container)
        val primaryDimRgb = ColorUtils.HSLToColor(
            floatArrayOf(greenHue, (boostedPrimarySat * 0.82f).coerceIn(0.42f, 0.80f), 0.24f)
        )

        val dividerRgb = enforceMinimumContrast(
            foregroundRgb = ColorUtils.HSLToColor(floatArrayOf(greenHue, bgSat * 0.80f, 0.27f)),
            backgroundRgb = bgRgb,
            minContrast = 2.3
        )

        val textSecRgb = enforceMinimumContrast(
            foregroundRgb = ColorUtils.HSLToColor(floatArrayOf(greenHue, 0.24f, 0.86f)),
            backgroundRgb = surfaceRgb,
            minContrast = 7.0
        )
        val textTertRgb = enforceMinimumContrast(
            foregroundRgb = ColorUtils.HSLToColor(floatArrayOf(greenHue, 0.18f, 0.64f)),
            backgroundRgb = surfaceRgb,
            minContrast = 4.2
        )

        val ratio = ColorUtils.calculateContrast(primaryRgb, bgRgb).toFloat()

        return AppThemeColors(
            primary = Color(primaryRgb),
            primaryLight = Color(primaryLightRgb),
            primaryDim = Color(primaryDimRgb),
            background = Color(bgRgb),
            backgroundDark = Color(bgDarkRgb),
            surface = Color(surfaceRgb),
            surfaceVariant = Color(surfaceVarRgb),
            card = Color(cardRgb),
            textPrimary = Color(0xFFF4FBF7),
            textSecondary = Color(textSecRgb),
            textTertiary = Color(textTertRgb),
            divider = Color(dividerRgb),
            onPrimary = Color(computeOptimalOnColor(primaryRgb)),
            onPrimaryContainer = Color(0xFFE6F9EE),
            toneProfileLabel = ExtremeToneRegime.DARK_FOREST_EMERALD.displayName,
            contrastRatio = ratio
        )
    }

    /**
     * 3. PURE OBSIDIAN & PLATINUM NOIR GENERATOR
     *
     * Specifically designed for 100% black, pitch-black, dark monochrome, and minimalist noir
     * album covers (e.g., Kanye West 'Donda', Metallica 'The Black Album', AC/DC 'Back in Black',
     * Arctic Monkeys 'AM', Jay-Z 'The Black Album', Cigarettes After Sex).
     *
     * Eliminates muddy brownish-gray tint by locking the background to true deep-space obsidian (#000000)
     * with crisp, multi-layered carbon and titanium surfaces, luminous diamond-platinum primary
     * accents (contrast >= 14.5:1), and ice-white text.
     */
    private fun createPureObsidianNoirTheme(stats: BitmapColorStats): AppThemeColors {
        val bgRgb = 0xFF000000.toInt() // True pitch obsidian black
        val bgDarkRgb = 0xFF000000.toInt()
        val surfaceRgb = 0xFF0B0C10.toInt() // Deep cold-titanium carbon
        val cardRgb = 0xFF13151D.toInt() // Elevated carbon layer
        val surfaceVarRgb = 0xFF1C1E2A.toInt() // Subtle slate carbon

        // Luminous Diamond Platinum primary (Contrast > 14:1 against surface)
        val primaryRgb = 0xFFF1F5F9.toInt() // Crisp Slate-50 Diamond White
        val primaryLightRgb = 0xFFFFFFFF.toInt() // Pure Glacier White
        val primaryDimRgb = 0xFF334155.toInt() // Brushed Slate-700 Graphite Titanium
        val dividerRgb = 0xFF282B38.toInt() // Refined Steel Slate

        val onPrimaryRgb = 0xFF07080C.toInt() // Deep obsidian ink for FABs/active pills
        val ratio = ColorUtils.calculateContrast(primaryRgb, bgRgb).toFloat()

        return AppThemeColors(
            primary = Color(primaryRgb),
            primaryLight = Color(primaryLightRgb),
            primaryDim = Color(primaryDimRgb),
            background = Color(bgRgb),
            backgroundDark = Color(bgDarkRgb),
            surface = Color(surfaceRgb),
            surfaceVariant = Color(surfaceVarRgb),
            card = Color(cardRgb),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFCBD5E1), // Crisp Silver
            textTertiary = Color(0xFF94A3B8), // Refined Slate
            divider = Color(dividerRgb),
            onPrimary = Color(onPrimaryRgb),
            onPrimaryContainer = Color(0xFFFFFFFF),
            toneProfileLabel = ExtremeToneRegime.PURE_OBSIDIAN_NOIR.displayName,
            contrastRatio = ratio
        )
    }

    /**
     * 4. CYBER-NOIR MICRO-ACCENT GENERATOR
     *
     * Handles dark/black album covers that feature a subtle, small, or hidden chromatic spark
     * (e.g. Pink Floyd's 'Dark Side of the Moon' prism, Kendrick Lamar's 'DAMN.' red title,
     * Joy Division's colored badges, electric neon lines, or a golden seal on a pitch black jacket).
     *
     * Amplifies the micro-chromatic accent to maximum vibrancy (S = 0.88..1.0f, L = 0.65..0.72)
     * against deep obsidian noir surfaces, achieving breathtaking contrast (> 8.5:1).
     */
    private fun createCyberNoirMicroAccentTheme(
        accentHue: Float,
        accentSat: Float,
        accentLight: Float,
        stats: BitmapColorStats
    ): AppThemeColors {
        val boostedSat = (accentSat * 1.55f + 0.25f).coerceIn(0.85f, 1.0f)
        val targetLight = when {
            accentHue in 205f..285f -> 0.72f // Blue / violet human eye compensation
            accentHue in 45f..70f -> 0.64f  // Yellow / gold compensation
            else -> 0.68f
        }

        // Obsidian surfaces with 1.8% subtle tint of the neon accent
        val bgRgb = ColorUtils.HSLToColor(floatArrayOf(accentHue, 0.12f, 0.025f))
        val bgDarkRgb = 0xFF000000.toInt()
        val surfaceRgb = ColorUtils.HSLToColor(floatArrayOf(accentHue, 0.15f, 0.075f))
        val cardRgb = ColorUtils.HSLToColor(floatArrayOf(accentHue, 0.18f, 0.115f))
        val surfaceVarRgb = ColorUtils.HSLToColor(floatArrayOf(accentHue, 0.20f, 0.165f))

        val candidatePrimary = ColorUtils.HSLToColor(floatArrayOf(accentHue, boostedSat, targetLight))
        val primaryRgb = enforceMinimumContrast(candidatePrimary, surfaceRgb, 7.8)

        val lightHue = (accentHue + 15f) % 360f
        val primaryLightRgb = enforceMinimumContrast(
            ColorUtils.HSLToColor(floatArrayOf(lightHue, (boostedSat * 0.9f).coerceIn(0.6f, 1f), 0.84f)),
            surfaceRgb,
            9.2
        )

        val primaryDimRgb = ColorUtils.HSLToColor(
            floatArrayOf(accentHue, (boostedSat * 0.8f).coerceIn(0.5f, 0.9f), 0.24f)
        )

        val dividerRgb = enforceMinimumContrast(
            ColorUtils.HSLToColor(floatArrayOf(accentHue, 0.22f, 0.24f)),
            bgRgb,
            2.3
        )

        val textSecRgb = enforceMinimumContrast(
            ColorUtils.HSLToColor(floatArrayOf(accentHue, 0.15f, 0.88f)),
            surfaceRgb,
            7.2
        )
        val textTertRgb = enforceMinimumContrast(
            ColorUtils.HSLToColor(floatArrayOf(accentHue, 0.12f, 0.65f)),
            surfaceRgb,
            4.2
        )

        val ratio = ColorUtils.calculateContrast(primaryRgb, bgRgb).toFloat()

        return AppThemeColors(
            primary = Color(primaryRgb),
            primaryLight = Color(primaryLightRgb),
            primaryDim = Color(primaryDimRgb),
            background = Color(bgRgb),
            backgroundDark = Color(bgDarkRgb),
            surface = Color(surfaceRgb),
            surfaceVariant = Color(surfaceVarRgb),
            card = Color(cardRgb),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(textSecRgb),
            textTertiary = Color(textTertRgb),
            divider = Color(dividerRgb),
            onPrimary = Color(computeOptimalOnColor(primaryRgb)),
            onPrimaryContainer = Color(0xFFFFFFFF),
            toneProfileLabel = ExtremeToneRegime.CYBER_NOIR_MICRO_ACCENT.displayName,
            contrastRatio = ratio
        )
    }

    /**
     * 5. DEEP BLACK / OBSIDIAN / NOIR HIGH-CONTRAST GENERATOR
     *
     * Handles near-black, dark metal, charcoal, and pitch-black album covers:
     * - If the cover is pitch black or achromatic, delegates directly to pure obsidian noir.
     * - If even a faint chromatic trace (sat >= 0.035) exists in the artwork's shadows,
     *   amplifies that trace into a high-contrast luminous accent over deep obsidian surfaces.
     */
    private fun createDeepBlackHighContrastTheme(
        shadowCandidate: SwatchCandidate?,
        stats: BitmapColorStats
    ): AppThemeColors {
        val hasHiddenChroma = (shadowCandidate != null && shadowCandidate.sat >= 0.04f) ||
            stats.dominantChromaticSat >= 0.04f

        if (!hasHiddenChroma || stats.isMonochromeNoir || stats.pitchBlackRatio >= 0.40f) {
            return createPureObsidianNoirTheme(stats)
        }

        val seedHue = shadowCandidate?.hue ?: stats.dominantChromaticHue
        val rawSat = shadowCandidate?.sat ?: stats.dominantChromaticSat

        val primarySat = (rawSat * 1.85f + 0.22f).coerceIn(0.55f, 0.90f)
        val surfaceSat = (primarySat * 0.38f).coerceIn(0.14f, 0.36f)

        val bgRgb = ColorUtils.HSLToColor(floatArrayOf(seedHue, surfaceSat, 0.035f))
        val bgDarkRgb = ColorUtils.HSLToColor(floatArrayOf(seedHue, surfaceSat, 0.015f))
        val surfaceRgb = ColorUtils.HSLToColor(floatArrayOf(seedHue, surfaceSat, 0.080f))
        val cardRgb = ColorUtils.HSLToColor(floatArrayOf(seedHue, surfaceSat, 0.120f))
        val surfaceVarRgb = ColorUtils.HSLToColor(floatArrayOf(seedHue, surfaceSat, 0.170f))

        val targetPrimaryLight = when {
            seedHue in 205f..285f -> 0.72f // Blue/violet compensation
            else -> 0.68f
        }
        val primaryRgb = enforceMinimumContrast(
            foregroundRgb = ColorUtils.HSLToColor(floatArrayOf(seedHue, primarySat, targetPrimaryLight)),
            backgroundRgb = surfaceRgb,
            minContrast = 7.5
        )
        val primaryLightRgb = enforceMinimumContrast(
            foregroundRgb = ColorUtils.HSLToColor(
                floatArrayOf((seedHue + 12f) % 360f, (primarySat * 0.88f).coerceIn(0.40f, 0.95f), 0.84f)
            ),
            backgroundRgb = surfaceRgb,
            minContrast = 9.5
        )
        val primaryDimRgb = ColorUtils.HSLToColor(
            floatArrayOf(seedHue, (primarySat * 0.75f).coerceIn(0.20f, 0.75f), 0.24f)
        )
        val dividerRgb = enforceMinimumContrast(
            foregroundRgb = ColorUtils.HSLToColor(floatArrayOf(seedHue, surfaceSat, 0.26f)),
            backgroundRgb = bgRgb,
            minContrast = 2.3
        )

        val ratio = ColorUtils.calculateContrast(primaryRgb, bgRgb).toFloat()

        return AppThemeColors(
            primary = Color(primaryRgb),
            primaryLight = Color(primaryLightRgb),
            primaryDim = Color(primaryDimRgb),
            background = Color(bgRgb),
            backgroundDark = Color(bgDarkRgb),
            surface = Color(surfaceRgb),
            surfaceVariant = Color(surfaceVarRgb),
            card = Color(cardRgb),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFFE2E8F0),
            textTertiary = Color(0xFF94A3B8),
            divider = Color(dividerRgb),
            onPrimary = Color(computeOptimalOnColor(primaryRgb)),
            onPrimaryContainer = Color(0xFFF8FAFC),
            toneProfileLabel = ExtremeToneRegime.DEEP_OBSIDIAN_BLACK.displayName,
            contrastRatio = ratio
        )
    }

    /**
     * 6. GENERAL HIGH-CONTRAST MATERIAL YOU TONAL PALETTE BUILDER
     *
     * Synthesizes a complete Material 3 / Material You tonal hierarchy from the extracted seed color,
     * with comprehensive perceptual tuning for every sector of the color wheel:
     * - Crimson & Blood Scarlet (345°..16°)
     * - Sunset Orange & Amber (16°..45°)
     * - Electric Gold & Honey (45°..65°)
     * - Cyber Lime & Chartreuse (65°..85°)
     * - Emerald & Forest Green (85°..165°)
     * - Oceanic Teal & Seafoam (165°..195°)
     * - Royal Cobalt & Deep Sapphire (195°..235°)
     * - Amethyst & Royal Violet (235°..285°)
     * - Neon Magenta & Cyber Fuchsia (285°..345°)
     * - Muted Earth Tones & Lo-Fi Pastels
     */
    private fun buildHighContrastMaterialYouTonalPalette(
        seedHue: Float,
        seedSat: Float,
        sourceLightness: Float,
        regime: ExtremeToneRegime,
        secondaryHue: Float? = null
    ): AppThemeColors {
        // Classify the specific color wheel sector
        val isCrimsonScarlet = seedHue in 345f..360f || seedHue in 0f..16f
        val isAmberSunset = seedHue in 16f..45f
        val isElectricGold = seedHue in 45f..65f
        val isAcidLime = seedHue in 65f..85f
        val isForestEmerald = seedHue in 85f..165f
        val isOceanicTeal = seedHue in 165f..195f
        val isRoyalCobalt = seedHue in 195f..235f
        val isAmethystViolet = seedHue in 235f..285f
        val isNeonMagenta = seedHue in 285f..345f
        val isDeepShadow = regime == ExtremeToneRegime.DEEP_CHROMATIC_SHADOW || sourceLightness < 0.26f

        // Boost chroma according to the perceptual requirements of each hue family
        val primarySat = when {
            isRoyalCobalt -> (seedSat * 1.35f + 0.18f).coerceIn(0.82f, 0.98f)
            isAmethystViolet -> (seedSat * 1.30f + 0.16f).coerceIn(0.78f, 0.96f)
            isCrimsonScarlet -> (seedSat * 1.30f + 0.15f).coerceIn(0.85f, 0.98f)
            isAmberSunset -> (seedSat * 1.25f + 0.12f).coerceIn(0.85f, 0.98f)
            isElectricGold -> (seedSat * 1.20f + 0.12f).coerceIn(0.88f, 1.0f)
            isAcidLime -> (seedSat * 1.25f + 0.14f).coerceIn(0.85f, 1.0f)
            isForestEmerald -> (seedSat * 1.25f + 0.12f).coerceIn(0.65f, 0.92f)
            isOceanicTeal -> (seedSat * 1.25f + 0.12f).coerceIn(0.75f, 0.96f)
            isNeonMagenta -> (seedSat * 1.30f + 0.15f).coerceIn(0.82f, 0.98f)
            regime == ExtremeToneRegime.PASTEL_HIGH_KEY -> (seedSat * 1.45f + 0.15f).coerceIn(0.52f, 0.88f)
            isDeepShadow -> (seedSat * 1.40f + 0.16f).coerceIn(0.55f, 0.90f)
            else -> (seedSat * 1.20f + 0.10f).coerceIn(0.55f, 0.95f)
        }

        // Perceptual lightness targets: Blue and Violet have low eye sensitivity and require higher HSL lightness.
        // Red is kept in the scarlet range without becoming pink.
        val basePrimaryLightness = when {
            isRoyalCobalt -> 0.72f
            isAmethystViolet -> 0.70f
            isCrimsonScarlet -> 0.65f
            isElectricGold -> 0.64f
            isAcidLime -> 0.68f
            isOceanicTeal -> 0.67f
            isNeonMagenta -> 0.66f
            isForestEmerald -> 0.66f
            isAmberSunset -> 0.64f
            regime == ExtremeToneRegime.PASTEL_HIGH_KEY -> 0.76f
            isDeepShadow -> 0.67f
            else -> 0.66f
        }

        // Atmospheric tonal surfaces tailored to the specific hue family
        val bgSat = when {
            isForestEmerald -> (primarySat * 0.58f).coerceIn(0.30f, 0.55f)
            isRoyalCobalt -> (primarySat * 0.42f).coerceIn(0.24f, 0.46f)
            isAmethystViolet -> (primarySat * 0.44f).coerceIn(0.24f, 0.48f)
            isCrimsonScarlet -> (primarySat * 0.46f).coerceIn(0.28f, 0.52f)
            isAmberSunset -> (primarySat * 0.42f).coerceIn(0.25f, 0.46f)
            isDeepShadow -> (primarySat * 0.48f).coerceIn(0.24f, 0.46f)
            else -> (primarySat * 0.40f).coerceIn(0.20f, 0.42f)
        }

        val bgBaseLight = when {
            isDeepShadow -> 0.045f
            isCrimsonScarlet -> 0.048f
            isRoyalCobalt -> 0.046f
            else -> 0.054f
        }

        val backgroundRgb = ColorUtils.HSLToColor(floatArrayOf(seedHue, bgSat, bgBaseLight))
        val backgroundDarkRgb = ColorUtils.HSLToColor(
            floatArrayOf(seedHue, (bgSat * 1.1f).coerceAtMost(0.65f), bgBaseLight * 0.55f)
        )
        val surfaceRgb = ColorUtils.HSLToColor(floatArrayOf(seedHue, bgSat * 0.95f, bgBaseLight + 0.044f))
        val cardRgb = ColorUtils.HSLToColor(floatArrayOf(seedHue, bgSat * 0.90f, bgBaseLight + 0.078f))
        val surfaceVariantRgb = ColorUtils.HSLToColor(floatArrayOf(seedHue, bgSat * 0.85f, bgBaseLight + 0.122f))

        // Enforce strict contrast on Primary, PrimaryLight (Secondary), and Divider
        val rawPrimaryRgb = ColorUtils.HSLToColor(floatArrayOf(seedHue, primarySat, basePrimaryLightness))
        val primaryRgb = enforceMinimumContrast(
            foregroundRgb = rawPrimaryRgb,
            backgroundRgb = surfaceRgb,
            minContrast = 6.6
        )

        val companionHue = secondaryHue ?: ((seedHue + 14f) % 360f)
        val rawPrimaryLightRgb = ColorUtils.HSLToColor(
            floatArrayOf(companionHue, (primarySat * 0.92f).coerceIn(0.40f, 0.95f), (basePrimaryLightness + 0.15f).coerceAtMost(0.88f))
        )
        val primaryLightRgb = enforceMinimumContrast(
            foregroundRgb = rawPrimaryLightRgb,
            backgroundRgb = surfaceRgb,
            minContrast = 8.2
        )

        // Material You PrimaryContainer (Tone 24..28) so white/light icons inside containers have >= 7:1 contrast
        val primaryDimRgb = ColorUtils.HSLToColor(
            floatArrayOf(seedHue, (primarySat * 0.80f).coerceIn(0.35f, 0.82f), 0.25f)
        )

        val dividerRgb = enforceMinimumContrast(
            foregroundRgb = ColorUtils.HSLToColor(floatArrayOf(seedHue, bgSat * 0.82f, bgBaseLight + 0.18f)),
            backgroundRgb = backgroundRgb,
            minContrast = 2.2
        )

        val textSecRgb = enforceMinimumContrast(
            foregroundRgb = ColorUtils.HSLToColor(floatArrayOf(seedHue, (bgSat * 0.45f).coerceIn(0.10f, 0.28f), 0.86f)),
            backgroundRgb = surfaceRgb,
            minContrast = 7.0
        )
        val textTertRgb = enforceMinimumContrast(
            foregroundRgb = ColorUtils.HSLToColor(floatArrayOf(seedHue, (bgSat * 0.35f).coerceIn(0.08f, 0.22f), 0.63f)),
            backgroundRgb = surfaceRgb,
            minContrast = 4.0
        )

        val ratio = ColorUtils.calculateContrast(primaryRgb, backgroundRgb).toFloat()

        return AppThemeColors(
            primary = Color(primaryRgb),
            primaryLight = Color(primaryLightRgb),
            primaryDim = Color(primaryDimRgb),
            background = Color(backgroundRgb),
            backgroundDark = Color(backgroundDarkRgb),
            surface = Color(surfaceRgb),
            surfaceVariant = Color(surfaceVariantRgb),
            card = Color(cardRgb),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(textSecRgb),
            textTertiary = Color(textTertRgb),
            divider = Color(dividerRgb),
            onPrimary = Color(computeOptimalOnColor(primaryRgb)),
            onPrimaryContainer = Color(0xFFF8FAFC),
            toneProfileLabel = regime.displayName,
            contrastRatio = ratio
        )
    }

    /**
     * Iteratively lifts (or lowers) the foreground color's HSL lightness while preserving its
     * chromatic hue until it meets or exceeds [minContrast] against [backgroundRgb].
     */
    fun enforceMinimumContrast(
        foregroundRgb: Int,
        backgroundRgb: Int,
        minContrast: Double
    ): Int {
        var currentContrast = ColorUtils.calculateContrast(foregroundRgb, backgroundRgb)
        if (currentContrast >= minContrast) return foregroundRgb

        val fgHsl = FloatArray(3)
        val bgHsl = FloatArray(3)
        ColorUtils.colorToHSL(foregroundRgb, fgHsl)
        ColorUtils.colorToHSL(backgroundRgb, bgHsl)

        val lightenForeground = bgHsl[2] < 0.50f
        var candidateRgb = foregroundRgb

        for (step in 1..28) {
            if (lightenForeground) {
                fgHsl[2] = (fgHsl[2] + 0.025f).coerceAtMost(0.97f)
                // Gently preserve saturation until very high lightness
                if (fgHsl[2] < 0.85f && fgHsl[1] > 0.10f) {
                    fgHsl[1] = (fgHsl[1] * 1.01f).coerceAtMost(0.95f)
                }
            } else {
                fgHsl[2] = (fgHsl[2] - 0.025f).coerceAtLeast(0.04f)
            }
            candidateRgb = ColorUtils.HSLToColor(fgHsl)
            currentContrast = ColorUtils.calculateContrast(candidateRgb, backgroundRgb)
            if (currentContrast >= minContrast) {
                return candidateRgb
            }
        }
        return candidateRgb
    }

    /**
     * Computes a high-contrast `onPrimary` token (deep tonal ink for bright/luminous primaries,
     * crisp white for deeper primaries) so icons and labels inside active pills/FABs are always razor-sharp.
     */
    fun computeOptimalOnColor(primaryRgb: Int): Int {
        val contrastWithBlack = ColorUtils.calculateContrast(0xFF090D14.toInt(), primaryRgb)
        val contrastWithWhite = ColorUtils.calculateContrast(0xFFFFFFFF.toInt(), primaryRgb)
        return if (contrastWithBlack >= contrastWithWhite * 0.85) {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(primaryRgb, hsl)
            ColorUtils.HSLToColor(floatArrayOf(hsl[0], (hsl[1] * 0.55f).coerceIn(0.10f, 0.50f), 0.07f))
        } else {
            0xFFFFFFFF.toInt()
        }
    }

    private fun hueDistance(h1: Float, h2: Float): Float {
        val diff = abs(h1 - h2) % 360f
        return if (diff > 180f) 360f - diff else diff
    }

    /**
     * Real-time reactive FFT visualizer album-adaptive gradient tinting:
     * Links the spectrum visualizer custom painter directly to the active track's extracted
     * high-contrast palette colors.
     *
     * Provides dedicated visualizer palettes for:
     * - Pure Obsidian / Platinum Noir: High-end luxury audio gradient from Deep Titanium to Glacier White.
     * - Cyber-Noir: Neon micro-accent energy radiating over obsidian black.
     * - Monochrome Pearl White: Luminous silver and frosted alabaster.
     * - Full Color Spectrum: Harmonic 7-band transitions matching the album's exact hue family.
     */
    fun generateSpectrumColors(themeColors: AppThemeColors): VisualizerSpectrumPalette {
        val primaryInt = themeColors.primary.toArgb()
        val secondaryInt = themeColors.primaryLight.toArgb()
        val hsl = FloatArray(3)
        val secHsl = FloatArray(3)
        ColorUtils.colorToHSL(primaryInt, hsl)
        ColorUtils.colorToHSL(secondaryInt, secHsl)

        val baseHue = hsl[0]
        val sat = hsl[1]
        val light = hsl[2]

        val isPureObsidianNoir = themeColors.toneProfileLabel.contains("Noir", ignoreCase = true) ||
            themeColors.toneProfileLabel.contains("Obsidian", ignoreCase = true) ||
            (sat < 0.12f && light > 0.82f && ColorUtils.calculateContrast(primaryInt, 0xFF000000.toInt()) >= 13.0)

        val isCyberNoir = themeColors.toneProfileLabel.contains("Cyber-Noir", ignoreCase = true)
        val isMonochromePearl = sat < 0.16f && light > 0.82f && !isPureObsidianNoir

        // 1. Pure Obsidian & Platinum Noir: Ultra-sleek audiophile titanium-to-glacier-white spectrum
        if (isPureObsidianNoir) {
            val noirTops = listOf(
                Color(0xFF475569), // Sub-bass: Deep Titanium Slate
                Color(0xFF64748B), // Bass: Cool Carbon Steel
                Color(0xFF94A3B8), // Low-mid: Refined Chrome
                Color(0xFF67E8F9), // Mid: Electric Ice Cyan
                Color(0xFFA5F3FC), // High-mid: Radiant Diamond Sky
                Color(0xFFE2E8F0), // Presence: Luminous White Frost
                Color(0xFFFFFFFF)  // Brilliance: Pure Glacier White
            )
            val noirBottoms = listOf(
                Color(0xFF0F172A).copy(alpha = 0.35f),
                Color(0xFF0F172A).copy(alpha = 0.40f),
                Color(0xFF1E293B).copy(alpha = 0.45f),
                Color(0xFF0E7490).copy(alpha = 0.40f),
                Color(0xFF0891B2).copy(alpha = 0.45f),
                Color(0xFF334155).copy(alpha = 0.50f),
                Color(0xFF475569).copy(alpha = 0.55f)
            )
            return VisualizerSpectrumPalette(
                bandColors = noirTops,
                bandBottomColors = noirBottoms
            )
        }

        // 2. Cyber-Noir Micro-Accent: Neon energy radiating over black
        if (isCyberNoir) {
            val tops = mutableListOf<Color>()
            val bottoms = mutableListOf<Color>()
            val lightSteps = listOf(0.55f, 0.60f, 0.66f, 0.72f, 0.78f, 0.85f, 0.94f)
            for (i in 0 until 7) {
                val bandHue = (baseHue + (i * 4f)) % 360f
                val topRgb = ColorUtils.HSLToColor(floatArrayOf(bandHue, sat.coerceAtLeast(0.85f), lightSteps[i]))
                val bottomRgb = ColorUtils.HSLToColor(floatArrayOf(bandHue, 0.50f, 0.12f))
                tops.add(Color(topRgb))
                bottoms.add(Color(bottomRgb).copy(alpha = 0.35f))
            }
            return VisualizerSpectrumPalette(
                bandColors = tops,
                bandBottomColors = bottoms
            )
        }

        // 3. Full color wheel / standard Material You tonal harmonic spectrum
        val baseSat = if (isMonochromePearl) 0.14f else sat.coerceIn(0.65f, 0.96f)

        val hueOffsets = if (isMonochromePearl) {
            listOf(-12f, -6f, 0f, 6f, 12f, 18f, 24f)
        } else {
            listOf(-20f, -6f, 12f, 32f, 58f, 92f, 130f)
        }
        val lightnesses = if (isMonochromePearl) {
            listOf(0.72f, 0.77f, 0.82f, 0.86f, 0.90f, 0.94f, 0.97f)
        } else {
            listOf(0.58f, 0.62f, 0.66f, 0.70f, 0.74f, 0.78f, 0.84f)
        }

        val topColors = mutableListOf<Color>()
        val bottomColors = mutableListOf<Color>()

        for (i in 0 until 7) {
            val bandHue = (baseHue + hueOffsets[i] + 360f) % 360f
            val bandLight = lightnesses[i]

            val topRgb = ColorUtils.HSLToColor(floatArrayOf(bandHue, baseSat, bandLight))
            val bottomRgb = ColorUtils.HSLToColor(
                floatArrayOf(bandHue, (baseSat * 0.85f).coerceIn(0.12f, 1f), 0.16f)
            )

            topColors.add(Color(topRgb))
            bottomColors.add(Color(bottomRgb))
        }

        return VisualizerSpectrumPalette(
            bandColors = topColors,
            bandBottomColors = bottomColors
        )
    }
}
