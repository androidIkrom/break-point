package com.example.poststudy.presentation.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import poststudy.composeapp.generated.resources.Res
import poststudy.composeapp.generated.resources.public_sans_bold
import poststudy.composeapp.generated.resources.public_sans_medium
import poststudy.composeapp.generated.resources.public_sans_regular
import poststudy.composeapp.generated.resources.public_sans_semibold

/**
 * "Signal" design tokens: dark command-center surfaces for intro, role, network and result
 * screens, a light workspace for admin and test screens, teal primary with cyan and amber
 * accents. Every color used by the UI comes from here.
 */
object Signal {
    // Dark surfaces
    val Dark = Color(0xFF0B1220)
    val Dark2 = Color(0xFF0E2F3A)
    val TitleBar = Color(0xFF07101A)
    val OnDark = Color.White
    val OnDarkMuted = Color(0xFFCBD5E1)
    val OnDarkFaint = Color(0xFF94A3B8)
    val GlassFill = Color.White.copy(alpha = 0.07f)
    val GlassFillHover = Color.White.copy(alpha = 0.11f)
    val GlassBorder = Color.White.copy(alpha = 0.14f)
    val GlassBorderStrong = Color.White.copy(alpha = 0.2f)
    val DarkScrim = Color(0xFF0B1220).copy(alpha = 0.5f)

    // Brand
    val Primary = Color(0xFF0F766E)
    val PrimaryHover = Color(0xFF115E59)
    val OnPrimary = Color.White
    val Cyan = Color(0xFF22D3EE)
    val CyanSoft = Color(0xFF5EEAD4)
    val Amber = Color(0xFFFBBF24)
    val Container = Color(0xFFCCFBF1)
    val OnContainer = Color(0xFF134E4A)

    // Light workspace
    val Surface = Color(0xFFF3F7F8)
    val Card = Color.White
    val SurfaceContainer = Color(0xFFE6EDEF)
    val Outline = Color(0xFFD7E1E3)
    val OutlineStrong = Color(0xFFC7D0DB)
    val Ink = Color(0xFF0F172A)
    val Muted = Color(0xFF5B6673)
    val Faint = Color(0xFF9AA5B1)

    // Status (always icon + label, never color alone)
    val Success = Color(0xFF15803D)
    val SuccessContainer = Color(0xFFDCF3E4)
    val OnSuccessContainer = Color(0xFF14532D)
    val Warning = Color(0xFFB45309)
    val WarningContainer = Color(0xFFFEF3C7)
    val OnWarningContainer = Color(0xFF78350F)
    val WarningBorder = Color(0xFFFDE68A)
    val Danger = Color(0xFFB42318)
    val DangerContainer = Color(0xFFFDE8E6)
    val OnDangerContainer = Color(0xFF7A1C12)
    val DangerBorder = Color(0xFFF3B9B3)
    val DangerOnDark = Color(0xFFF87171)
    val SuccessOnDark = Color(0xFF4ADE80)

    val GradientPrimary: Brush get() = Brush.linearGradient(listOf(Primary, Cyan))
    val GradientCyanAmber: Brush get() = Brush.linearGradient(listOf(Cyan, Amber))
    val GradientAmberPrimary: Brush get() = Brush.linearGradient(listOf(Amber, Primary))
    val GradientHero: Brush get() = Brush.linearGradient(listOf(Dark, Dark2))
    val GradientHeroTeal: Brush get() = Brush.linearGradient(listOf(Primary, Dark2))
    val GradientRail: Brush get() = Brush.verticalGradient(listOf(Dark2, Dark))
    /** Window title bar: night blue on the left warming to deep teal on the right. */
    val GradientTitleBar: Brush get() = Brush.horizontalGradient(listOf(TitleBar, Dark2, Color(0xFF0F5F5E)))
}

/** Spacing scale on a 4dp grid. */
object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
    val page = 32.dp
    val contentMaxWidth = 1200.dp
}

/** Corner radii. */
object Radius {
    val s = 10.dp
    val m = 12.dp
    val l = 18.dp
    val xl = 20.dp
}

/** Shapes and the old names still used by screens that are not migrated yet. */
object AppDesign {
    val CardShape = RoundedCornerShape(Radius.l)
    val ComponentShape = RoundedCornerShape(Radius.m)
    val SmallShape = RoundedCornerShape(Radius.s)
    val HeroShape = RoundedCornerShape(Radius.xl)
    val PillShape = CircleShape

    /** Flat workspace background; kept as a brush so old call sites keep compiling. */
    val BackgroundGradient: Brush get() = Brush.verticalGradient(listOf(Signal.Surface, Signal.Surface))

    // Legacy accent names mapped onto the Signal palette
    val Indigo = Signal.Primary
    val Emerald = Signal.Success
    val Amber = Signal.Warning
    val Rose = Signal.Danger
    val Sky = Signal.Cyan
    val Violet = Signal.Primary
    val Orange = Signal.Amber
    val Teal = Signal.Primary

    val RainbowPalette = listOf(Signal.Primary, Signal.Cyan, Signal.Amber, Signal.Success)
}

/** Durations and springs. Everything collapses to an instant snap when motion is reduced. */
object AppMotion {
    const val fast = 120
    const val normal = 220
    const val screen = 260
    const val emphasized = 320
    const val slow = 1000

    val standardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val decelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val accelerateEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    @Composable
    fun <T> tweenOrSnap(millis: Int, easing: androidx.compose.animation.core.Easing = standardEasing): FiniteAnimationSpec<T> =
        if (LocalReduceMotion.current) snap() else tween(millis, easing = easing)

    @Composable
    fun <T> springOrSnap(stiffness: Float = Spring.StiffnessMedium, damping: Float = Spring.DampingRatioLowBouncy): FiniteAnimationSpec<T> =
        if (LocalReduceMotion.current) snap() else spring(dampingRatio = damping, stiffness = stiffness)
}

/** True when the OS asks for reduced motion (Windows "Animation effects" switched off). */
val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
fun publicSans(): FontFamily = FontFamily(
    Font(Res.font.public_sans_regular, FontWeight.Normal),
    Font(Res.font.public_sans_medium, FontWeight.Medium),
    Font(Res.font.public_sans_semibold, FontWeight.SemiBold),
    Font(Res.font.public_sans_bold, FontWeight.Bold)
)

@Composable
fun signalTypography(): Typography {
    val family = publicSans()
    return Typography(
        displayLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 52.sp),
        displayMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
        displaySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
        headlineLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
        headlineMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
        headlineSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
        titleLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 26.sp),
        titleMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
        titleSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
        labelSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
    )
}

private val SignalColorScheme = lightColorScheme(
    primary = Signal.Primary,
    onPrimary = Signal.OnPrimary,
    primaryContainer = Signal.Container,
    onPrimaryContainer = Signal.OnContainer,
    secondary = Signal.Cyan,
    onSecondary = Signal.Dark,
    secondaryContainer = Signal.Container,
    onSecondaryContainer = Signal.OnContainer,
    tertiary = Signal.Amber,
    onTertiary = Signal.Dark,
    tertiaryContainer = Signal.WarningContainer,
    onTertiaryContainer = Signal.OnWarningContainer,
    error = Signal.Danger,
    onError = Color.White,
    errorContainer = Signal.DangerContainer,
    onErrorContainer = Signal.OnDangerContainer,
    background = Signal.Surface,
    onBackground = Signal.Ink,
    surface = Signal.Card,
    onSurface = Signal.Ink,
    surfaceVariant = Signal.SurfaceContainer,
    onSurfaceVariant = Signal.Muted,
    surfaceContainerLowest = Signal.Card,
    surfaceContainerLow = Signal.Surface,
    surfaceContainer = Signal.SurfaceContainer,
    surfaceContainerHigh = Signal.SurfaceContainer,
    surfaceContainerHighest = Signal.Outline,
    outline = Signal.OutlineStrong,
    outlineVariant = Signal.Outline,
    inverseSurface = Signal.Dark,
    inverseOnSurface = Signal.OnDark,
    inversePrimary = Signal.Cyan,
    scrim = Signal.DarkScrim
)

private val SignalShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(Radius.s),
    medium = RoundedCornerShape(Radius.m),
    large = RoundedCornerShape(Radius.l),
    extraLarge = RoundedCornerShape(Radius.xl)
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PostStudyTheme(content: @Composable () -> Unit) {
    val reduceMotion = MotionPrefs.reduceMotion
    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        MaterialTheme(
            colorScheme = SignalColorScheme,
            motionScheme = MotionScheme.standard(),
            shapes = SignalShapes,
            typography = signalTypography(),
            content = content
        )
    }
}
