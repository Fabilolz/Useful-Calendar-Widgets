package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Renders an exact 1:1 preview of a home screen widget RemoteViews inside Jetpack Compose.
 */
@Composable
fun RemoteViewsPreview(
    views: RemoteViews,
    widthDp: Dp,
    heightDp: Dp,
    hostScale: Float = 1f,
    cornerRadius: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    BoxWithConstraints(modifier = modifier) {
        val fittedScale = minOf(hostScale, maxWidth / widthDp, maxHeight / heightDp)
        AndroidView(
            modifier = Modifier.size(widthDp * fittedScale, heightDp * fittedScale)
                .clip(RoundedCornerShape(cornerRadius)),
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { frame ->
                frame.removeAllViews()
                runCatching {
                    val inflated = views.apply(context.applicationContext, frame)
                    val density = ctxDensity(context)
                    inflated.pivotX = 0f
                    inflated.pivotY = 0f
                    inflated.scaleX = fittedScale
                    inflated.scaleY = fittedScale
                    frame.addView(inflated, FrameLayout.LayoutParams(
                        (widthDp.value * density).toInt(),
                        (heightDp.value * density).toInt(),
                        android.view.Gravity.TOP or android.view.Gravity.START
                    ))
                }
            }
        )
    }
}

private fun ctxDensity(context: android.content.Context): Float = context.resources.displayMetrics.density
