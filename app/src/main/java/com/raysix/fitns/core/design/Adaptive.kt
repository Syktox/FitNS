package com.raysix.fitns.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

private const val WideThresholdDp = 700
private const val CompactHeightThresholdDp = 500
private val ReadableContentMaxWidth = 1040.dp
private val TwoPaneContentMaxWidth = 1560.dp

val LocalFloatingNavigationClearance = staticCompositionLocalOf { 0.dp }

/**
 * True when the current screen width is large enough for two-pane layouts
 * (e.g. phones held in landscape, tablets, large foldables).
 */
@Composable
fun isWideScreen(): Boolean = LocalConfiguration.current.screenWidthDp >= WideThresholdDp

@Composable
fun isCompactScreen(): Boolean = !isWideScreen()

/** True for short windows such as a phone in landscape or a split-screen pane. */
@Composable
fun isCompactHeight(): Boolean = LocalConfiguration.current.screenHeightDp < CompactHeightThresholdDp

/**
 * Shared horizontal padding for a content column depending on screen width.
 */
@Composable
fun adaptiveHorizontalPadding() = if (isWideScreen()) 24.dp else 16.dp

/**
 * Constrains a [Column] to a readable maximum width, centers it on wide screens,
 * and makes it scroll vertically. Used by screens that stay single-column but
 * would otherwise stretch too far.
 */
@Composable
fun AdaptiveColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val bottomClearance = LocalFloatingNavigationClearance.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = ReadableContentMaxWidth)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = adaptiveHorizontalPadding())
                .padding(bottom = bottomClearance + 20.dp),
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
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
        val bottomClearance = LocalFloatingNavigationClearance.current
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = TwoPaneContentMaxWidth)
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = adaptiveHorizontalPadding())
                    .padding(bottom = bottomClearance + 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                header?.invoke()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        main()
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        side()
                    }
                }
            }
        }
    } else {
        val bottomClearance = LocalFloatingNavigationClearance.current
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = ReadableContentMaxWidth)
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = adaptiveHorizontalPadding())
                    .padding(bottom = bottomClearance + 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                header?.invoke()
                main()
                side()
            }
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
        val bottomClearance = LocalFloatingNavigationClearance.current
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = TwoPaneContentMaxWidth)
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = adaptiveHorizontalPadding())
                    .padding(bottom = bottomClearance + 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                header?.invoke()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(gutterWidthFraction)) {
                        gutter()
                    }
                    Column(
                        modifier = Modifier.weight(1f - gutterWidthFraction),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        main()
                    }
                }
            }
        }
    } else {
        val bottomClearance = LocalFloatingNavigationClearance.current
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = ReadableContentMaxWidth)
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = adaptiveHorizontalPadding())
                    .padding(bottom = bottomClearance + 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                header?.invoke()
                gutter()
                main()
            }
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
            .align(Alignment.TopCenter)
            .widthIn(max = ReadableContentMaxWidth)
            .fillMaxWidth()
    ) {
        content()
    }
}
