package com.raysix.fitns.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

private const val WideThresholdDp = 700

val LocalFloatingNavigationClearance = staticCompositionLocalOf { 0.dp }

/**
 * True when the current screen width is large enough for two-pane layouts
 * (e.g. phones held in landscape, tablets, large foldables).
 */
@Composable
fun isWideScreen(): Boolean = LocalConfiguration.current.screenWidthDp >= WideThresholdDp

@Composable
fun isCompactScreen(): Boolean = !isWideScreen()

/**
 * Shared horizontal padding for a content column depending on screen width.
 */
@Composable
fun adaptiveHorizontalPadding() = if (isWideScreen()) 32.dp else 16.dp

/**
 * Constrains a [Column] to a readable maximum width, centers it on wide screens,
 * and makes it scroll vertically. Used by screens that stay single-column but
 * would otherwise stretch too far.
 */
@Composable
fun AdaptiveColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val bottomClearance = LocalFloatingNavigationClearance.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 860.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = adaptiveHorizontalPadding())
            .padding(bottom = bottomClearance),
        verticalArrangement = verticalArrangement,
        content = content
    )
}

/**
 * Splits the main content into two weighted columns on wide screens.
 * The [header] spans the full width above the columns. On compact screens
 * both panes are rendered in reading order so secondary functionality never
 * disappears because of the available width.
 */
@Composable
fun AdaptiveTwoColumn(
    header: (@Composable () -> Unit)? = null,
    main: @Composable ColumnScope.() -> Unit,
    side: @Composable ColumnScope.() -> Unit
) {
    if (isWideScreen()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = adaptiveHorizontalPadding())
            ) {
                header?.invoke()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = adaptiveHorizontalPadding()),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    main()
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    side()
                }
            }
        }
    } else {
        val bottomClearance = LocalFloatingNavigationClearance.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = adaptiveHorizontalPadding())
                .padding(bottom = bottomClearance),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            header?.invoke()
            main()
            side()
        }
    }
}

/**
 * Wide variant of [AdaptiveTwoColumn] that keeps the [side] content in a fixed-width
 * gutter (used for e.g. the scan options next to a longer form).
 */
@Composable
fun AdaptiveGutterLayout(
    header: (@Composable () -> Unit)? = null,
    gutterWidthFraction: Float = 0.42f,
    gutter: @Composable () -> Unit,
    main: @Composable ColumnScope.() -> Unit
) {
    if (isWideScreen()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = adaptiveHorizontalPadding())
            ) {
                header?.invoke()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = adaptiveHorizontalPadding()),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(modifier = Modifier.weight(gutterWidthFraction)) {
                    gutter()
                }
                Column(
                    modifier = Modifier.weight(1f - gutterWidthFraction),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    main()
                }
            }
        }
    } else {
        val bottomClearance = LocalFloatingNavigationClearance.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = adaptiveHorizontalPadding())
                .padding(bottom = bottomClearance),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            header?.invoke()
            gutter()
            main()
        }
    }
}

/**
 * Puts a full-width [BoxScope] content (e.g. a map, camera or hero) above adaptive
 * content. Used by screens that keep a single column but want a full-bleed element.
 */
@Composable
fun BoxScope.AdaptiveBoxContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 860.dp)
    ) {
        content()
    }
}
