package com.senghol.knotx.ui.dukaan

import androidx.compose.ui.graphics.Color

// Moved here from the bill file so the bill screen AND the "My business" screen
// use exactly the same colors. In the bill file: DELETE the private BillTheme,
// DarkBillTheme, LightBillTheme and Accent, then add
//   import com.senghol.knotx.ui.dukaan.BillTheme
//   import com.senghol.knotx.ui.dukaan.DarkBillTheme
//   import com.senghol.knotx.ui.dukaan.LightBillTheme
//   import com.senghol.knotx.ui.dukaan.Accent


data class BillTheme(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceMid: Color,
    val onSurfaceDim: Color,
    val divider: Color,
    val fieldBorder: Color,
    val fieldContainer: Color,
)

val DarkBillTheme = BillTheme(
    background     = Color(0xFF121212),
    surface        = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF252525),
    onSurface      = Color(0xFFEEEEEE),
    onSurfaceMid   = Color(0xFFAAAAAA),
    onSurfaceDim   = Color(0xFF666666),
    divider        = Color(0xFF2A2A2A),
    fieldBorder    = Color(0xFF3A3A3A),
    fieldContainer = Color(0xFF1E1E1E),
)

val LightBillTheme = BillTheme(
    background     = Color(0xFFF5F5F5),
    surface        = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurface      = Color(0xFF111111),
    onSurfaceMid   = Color(0xFF555555),
    onSurfaceDim   = Color(0xFF999999),
    divider        = Color(0xFFE0E0E0),
    fieldBorder    = Color(0xFFDDDDDD),
    fieldContainer = Color(0xFFFFFFFF),
)



val Accent = Color(0xFFEB5B00)

/** Error / delete color (new - the bill theme had none). */
val BillDanger = Color(0xFFE5484D)