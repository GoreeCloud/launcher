package com.goreecloud.launcher.ui.theme

import android.animation.ValueAnimator
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Repository-local Android presentation policy for the exact GLAZE UI V1.6 Stable source.
 *
 * This policy maps authoritative V1.6 presentation semantics into Launcher-owned Compose code.
 * It never creates privacy, security, authorization, connectivity, availability, recovery, or
 * workflow truth. Those states remain owned by their responsible GoreeCloud or Android authority.
 *
 * The mapping is Development source evidence only. It does not establish rendered, accessibility,
 * representative-device, performance, production, release, or Stable acceptance for Launcher.
 */
enum class GlazeV16MaterialRole {
    CANVAS,
    SOLID,
    RAISED,
    FUNCTIONAL_GLASS,
    CLEAR_GLASS,
    OVERLAY,
}

enum class GlazeV16PerformanceLevel {
    FULL,
    BALANCED,
    EFFICIENT,
    ESSENTIAL,
}

enum class GlazeV16MotionMode {
    STANDARD,
    REDUCED,
    MINIMAL,
}


data class GlazeV16AndroidPresentationSignals(
    val fontScale: Float,
    val animationsEnabled: Boolean,
    val touchExplorationEnabled: Boolean,
)

object GlazeV16AndroidPresentationContext {
    const val LARGE_TEXT_FONT_SCALE = 1.30f
    const val EXTRA_LARGE_TEXT_FONT_SCALE = 1.60f

    fun resolve(signals: GlazeV16AndroidPresentationSignals): GlazeV16PresentationContext =
        GlazeV16PresentationContext(
            reducedMotion = !signals.animationsEnabled,
            largeText = signals.fontScale >= LARGE_TEXT_FONT_SCALE,
            extraLargeText = signals.fontScale >= EXTRA_LARGE_TEXT_FONT_SCALE,
            touchAssistance = signals.touchExplorationEnabled,
            screenReaderOptimized = signals.touchExplorationEnabled,
        )
}

@Composable
fun rememberAndroidGlazeV16PresentationContext(): GlazeV16PresentationContext {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val accessibilityManager = remember(context) {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    }

    var touchExplorationEnabled by remember(accessibilityManager) {
        mutableStateOf(accessibilityManager?.isTouchExplorationEnabled == true)
    }
    var animationsEnabled by remember(context) {
        mutableStateOf(ValueAnimator.areAnimatorsEnabled())
    }

    DisposableEffect(accessibilityManager) {
        if (accessibilityManager == null) {
            onDispose { }
        } else {
            val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled ->
                touchExplorationEnabled = enabled
            }
            accessibilityManager.addTouchExplorationStateChangeListener(listener)
            onDispose {
                accessibilityManager.removeTouchExplorationStateChangeListener(listener)
            }
        }
    }

    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                animationsEnabled = ValueAnimator.areAnimatorsEnabled()
            }
        }
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        context.contentResolver.registerContentObserver(uri, false, observer)
        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    return GlazeV16AndroidPresentationContext.resolve(
        GlazeV16AndroidPresentationSignals(
            fontScale = configuration.fontScale,
            animationsEnabled = animationsEnabled,
            touchExplorationEnabled = touchExplorationEnabled,
        ),
    )
}

data class GlazeV16PresentationContext(
    val reducedMotion: Boolean = false,
    val reducedTransparency: Boolean = false,
    val increasedContrast: Boolean = false,
    val largeText: Boolean = false,
    val extraLargeText: Boolean = false,
    val touchAssistance: Boolean = false,
    val strongFocus: Boolean = false,
    val keyboardFirst: Boolean = false,
    val screenReaderOptimized: Boolean = false,
    val performanceLevel: GlazeV16PerformanceLevel = GlazeV16PerformanceLevel.FULL,
)

data class GlazeV16ResolvedPresentation(
    val materialRole: GlazeV16MaterialRole,
    val motionMode: GlazeV16MotionMode,
    val minimumInteractionTarget: Dp,
    val focusRingWidth: Dp,
    val focusRingOffset: Dp,
    val densityMayYieldToReflow: Boolean,
    val strongVisibleFocusRequired: Boolean,
)

object GlazeV16PresentationPolicy {
    const val stableVersion = "1.6.0"
    const val stableSourceRevision = "a7180679ea851389e0f3004515f9a25f420e716d"

    // Inherited Stable layout-state sources carried by the accepted V1.6 release source.
    val inheritedCoarseTargetFloor: Dp = 44.dp
    val inheritedPointerCompactFloor: Dp = 32.dp
    val inheritedFocusRingWidth: Dp = 2.dp
    val inheritedFocusRingOffset: Dp = 2.dp

    fun resolve(
        requestedMaterial: GlazeV16MaterialRole,
        context: GlazeV16PresentationContext,
    ): GlazeV16ResolvedPresentation {
        val resolvedMaterial = when {
            context.reducedTransparency &&
                requestedMaterial in setOf(
                    GlazeV16MaterialRole.FUNCTIONAL_GLASS,
                    GlazeV16MaterialRole.CLEAR_GLASS,
                ) -> GlazeV16MaterialRole.SOLID

            context.performanceLevel == GlazeV16PerformanceLevel.ESSENTIAL &&
                requestedMaterial !in setOf(
                    GlazeV16MaterialRole.CANVAS,
                    GlazeV16MaterialRole.SOLID,
                    GlazeV16MaterialRole.OVERLAY,
                ) -> GlazeV16MaterialRole.SOLID

            context.performanceLevel == GlazeV16PerformanceLevel.EFFICIENT &&
                requestedMaterial in setOf(
                    GlazeV16MaterialRole.FUNCTIONAL_GLASS,
                    GlazeV16MaterialRole.CLEAR_GLASS,
                ) -> GlazeV16MaterialRole.RAISED

            else -> requestedMaterial
        }

        val motionMode = when {
            context.reducedMotion ||
                context.performanceLevel == GlazeV16PerformanceLevel.ESSENTIAL ->
                GlazeV16MotionMode.MINIMAL

            context.performanceLevel == GlazeV16PerformanceLevel.EFFICIENT ->
                GlazeV16MotionMode.REDUCED

            else -> GlazeV16MotionMode.STANDARD
        }

        return GlazeV16ResolvedPresentation(
            materialRole = resolvedMaterial,
            motionMode = motionMode,
            minimumInteractionTarget = GlazeMetrics.interactionTarget(context.touchAssistance),
            focusRingWidth = inheritedFocusRingWidth,
            focusRingOffset = inheritedFocusRingOffset,
            densityMayYieldToReflow = context.largeText || context.extraLargeText,
            strongVisibleFocusRequired =
                context.strongFocus ||
                    context.keyboardFirst ||
                    context.screenReaderOptimized ||
                    context.increasedContrast,
        )
    }
}

/**
 * Caller/platform supplied presentation context. Defaults are intentionally neutral and do not
 * infer device accessibility or performance state.
 */
val LocalGlazeV16PresentationContext = staticCompositionLocalOf {
    GlazeV16PresentationContext()
}
