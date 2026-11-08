package com.example.widget

/**
 * Standard tokens for Express Cal widgets following Material 3 Expressive guidelines.
 * All dimension tokens are specified in base DP (scale = 1.0) on a 4dp grid.
 */
object WidgetTokens {
    // Spacing & Grid (4dp multiples)
    const val PAD_BASE_DP = 16f
    const val PAD_MIN_DP = 8f
    const val GAP_BASE_DP = 8f
    const val GAP_MIN_DP = 4f

    // Radii
    const val WIDGET_RADIUS_DP = 28f
    const val INNER_CARD_RADIUS_DP = 14f
    const val PILL_RADIUS_DP = 12f
    const val ACCENT_BAR_WIDTH_DP = 3f
    const val ACCENT_BAR_RADIUS_DP = 2f

    // Typography (sp/dp base at scale 1.0)
    const val TEXT_DISPLAY_AGENDA = 40f
    const val TEXT_DISPLAY_GLANCE = 20f
    const val TEXT_TITLE = 14f
    const val TEXT_BODY = 12f
    const val TEXT_LABEL = 11f
    const val TEXT_MICRO = 10f // Minimum accessible size, never below 10dp
}
