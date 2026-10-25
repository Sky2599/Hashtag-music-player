package com.example.player

object AutoEqDatabase {
    data class AutoEqProfile(
        val name: String,
        val targetCurve: String,
        val description: String,
        val bands: List<ParametricBand>
    )

    val PROFILES = listOf(
        AutoEqProfile(
            name = "Harman Target (In-Ear)",
            targetCurve = "Harman 2019v2",
            description = "Industry standard research curve with punchy sub-bass lift and warm natural vocals",
            bands = listOf(
                ParametricBand(0, 32f, 4.5f, 0.7f, FilterType.LOW_SHELF),
                ParametricBand(1, 64f, 3.2f, 1.2f, FilterType.PEAK),
                ParametricBand(2, 125f, 1.0f, 1.4f, FilterType.PEAK),
                ParametricBand(3, 250f, -0.5f, 1.5f, FilterType.PEAK),
                ParametricBand(4, 500f, 0.0f, 1.4f, FilterType.PEAK),
                ParametricBand(5, 1000f, 0.5f, 1.2f, FilterType.PEAK),
                ParametricBand(6, 2500f, 2.8f, 1.8f, FilterType.PEAK),
                ParametricBand(7, 4000f, 1.5f, 2.0f, FilterType.PEAK),
                ParametricBand(8, 8000f, -1.2f, 2.5f, FilterType.PEAK),
                ParametricBand(9, 16000f, 2.0f, 0.9f, FilterType.HIGH_SHELF)
            )
        ),
        AutoEqProfile(
            name = "Sennheiser HD 600 / 650",
            targetCurve = "Diffuse Field Neutral",
            description = "Corrects sub-bass roll-off and slight 3.5kHz veil for legendary studio monitors",
            bands = listOf(
                ParametricBand(0, 30f, 5.0f, 0.8f, FilterType.LOW_SHELF),
                ParametricBand(1, 60f, 3.5f, 1.0f, FilterType.PEAK),
                ParametricBand(2, 150f, -1.0f, 1.4f, FilterType.PEAK),
                ParametricBand(3, 300f, 0.0f, 1.2f, FilterType.PEAK),
                ParametricBand(4, 1000f, 0.0f, 1.0f, FilterType.PEAK),
                ParametricBand(5, 2200f, -1.5f, 1.8f, FilterType.PEAK),
                ParametricBand(6, 3500f, 2.0f, 2.2f, FilterType.PEAK),
                ParametricBand(7, 6000f, -1.0f, 2.5f, FilterType.PEAK),
                ParametricBand(8, 9000f, 1.5f, 2.0f, FilterType.PEAK),
                ParametricBand(9, 14000f, 2.5f, 0.8f, FilterType.HIGH_SHELF)
            )
        ),
        AutoEqProfile(
            name = "Sony WH-1000XM4 / XM5",
            targetCurve = "Clean Bass Clarifier",
            description = "Tames boomy mid-bass hump and opens up transparent mids and sparkling highs",
            bands = listOf(
                ParametricBand(0, 35f, 1.5f, 0.9f, FilterType.LOW_SHELF),
                ParametricBand(1, 100f, -3.5f, 1.4f, FilterType.PEAK),
                ParametricBand(2, 220f, -2.8f, 1.6f, FilterType.PEAK),
                ParametricBand(3, 500f, 0.5f, 1.2f, FilterType.PEAK),
                ParametricBand(4, 1200f, 1.2f, 1.4f, FilterType.PEAK),
                ParametricBand(5, 2500f, 2.0f, 1.5f, FilterType.PEAK),
                ParametricBand(6, 4000f, 1.0f, 2.0f, FilterType.PEAK),
                ParametricBand(7, 6500f, 2.5f, 2.2f, FilterType.PEAK),
                ParametricBand(8, 9500f, -1.0f, 3.0f, FilterType.PEAK),
                ParametricBand(9, 15000f, 1.5f, 1.0f, FilterType.HIGH_SHELF)
            )
        ),
        AutoEqProfile(
            name = "Apple AirPods Pro 2",
            targetCurve = "Studio Reference",
            description = "Fine-tunes the neutral frequency response with extended sub-bass and crisp acoustic air",
            bands = listOf(
                ParametricBand(0, 25f, 2.5f, 0.8f, FilterType.LOW_SHELF),
                ParametricBand(1, 60f, 1.0f, 1.2f, FilterType.PEAK),
                ParametricBand(2, 180f, -0.8f, 1.4f, FilterType.PEAK),
                ParametricBand(3, 500f, 0.0f, 1.0f, FilterType.PEAK),
                ParametricBand(4, 1500f, 0.5f, 1.5f, FilterType.PEAK),
                ParametricBand(5, 3000f, -1.2f, 2.0f, FilterType.PEAK),
                ParametricBand(6, 4800f, 1.8f, 2.2f, FilterType.PEAK),
                ParametricBand(7, 7500f, -1.5f, 2.5f, FilterType.PEAK),
                ParametricBand(8, 10500f, 2.0f, 2.0f, FilterType.PEAK),
                ParametricBand(9, 16000f, 3.0f, 0.8f, FilterType.HIGH_SHELF)
            )
        ),
        AutoEqProfile(
            name = "Etymotic ER4XR Target",
            targetCurve = "Diffuse Field Neutral",
            description = "Ultimate analytical transparency, vocal clarity, and surgical acoustic accuracy",
            bands = listOf(
                ParametricBand(0, 30f, 3.0f, 0.9f, FilterType.LOW_SHELF),
                ParametricBand(1, 80f, 1.5f, 1.2f, FilterType.PEAK),
                ParametricBand(2, 200f, 0.0f, 1.0f, FilterType.PEAK),
                ParametricBand(3, 600f, 0.0f, 1.0f, FilterType.PEAK),
                ParametricBand(4, 1500f, 0.0f, 1.0f, FilterType.PEAK),
                ParametricBand(5, 2700f, -2.0f, 2.0f, FilterType.PEAK),
                ParametricBand(6, 4500f, 1.0f, 2.0f, FilterType.PEAK),
                ParametricBand(7, 7000f, 0.0f, 1.5f, FilterType.PEAK),
                ParametricBand(8, 11000f, 2.5f, 2.2f, FilterType.PEAK),
                ParametricBand(9, 15000f, 2.0f, 1.0f, FilterType.HIGH_SHELF)
            )
        )
    )
}
