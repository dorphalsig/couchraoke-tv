package com.couchraoke.tv.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Typography
import com.couchraoke.tv.R

val AldoTheApache = FontFamily(Font(R.font.aldo_the_apache))

val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_variable, weight = FontWeight.Light),
    Font(R.font.space_grotesk_variable, weight = FontWeight.Normal),
    Font(R.font.space_grotesk_variable, weight = FontWeight.Medium),
    Font(R.font.space_grotesk_variable, weight = FontWeight.SemiBold),
    Font(R.font.space_grotesk_variable, weight = FontWeight.Bold),
)

val DisplayHeroNumber = TextStyle(fontFamily = AldoTheApache, fontWeight = FontWeight.Bold, fontSize = 160.sp)
val DisplayHeroTitle = TextStyle(fontFamily = AldoTheApache, fontWeight = FontWeight.Bold, fontSize = 56.sp)
val DisplayAccentTitle = TextStyle(fontFamily = AldoTheApache, fontWeight = FontWeight.Bold, fontSize = 44.sp)
val ScreenTitle = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 40.sp)
val SectionTitle = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 32.sp)
val PanelTitle = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 28.sp)
val SongCardTitle = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 24.sp)
val SongCardCompactTitleTextSize = 18.sp
val SongCardCompactTitle = SongCardTitle.copy(fontSize = SongCardCompactTitleTextSize)
val SongCardArtistFocused = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 18.sp)
val PreviewTitle = TextStyle(fontFamily = AldoTheApache, fontWeight = FontWeight.Bold, fontSize = 22.sp)
val PreviewArtist = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 15.sp)
val TagChipLabel = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 16.sp)
val BodyPrimary = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 24.sp)
val BodySecondary = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 20.sp)
val ButtonLabel = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
val FieldLabel = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 20.sp)
val Caption = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 18.sp)
val LyricsCurrent = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 40.sp)
val LyricsNext = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 32.sp)
val LiveScore = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 56.sp)
val SentenceRating = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 28.sp)
val TopMetadataMinimal = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 20.sp)
val SingerBadge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 22.sp)
val Timer = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 24.sp)

@OptIn(ExperimentalTvMaterial3Api::class)
val Typography = Typography(
    bodyLarge = BodyPrimary,
)
