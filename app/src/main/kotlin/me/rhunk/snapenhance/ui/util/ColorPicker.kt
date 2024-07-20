package me.rhunk.snapenhance.ui.util

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.AlphaTile

@Composable
fun CircularAlphaTile(
    selectedColor: Color?,
) {
    AlphaTile(
        modifier = Modifier
            .size(30.dp)
            .border(2.dp, Color.White, shape = RoundedCornerShape(15.dp))
            .clip(RoundedCornerShape(15.dp)),
        selectedColor = selectedColor ?: Color.Transparent,
        tileEvenColor = selectedColor?.let { Color(0xFFCBCBCB) } ?: Color.Transparent,
        tileOddColor = selectedColor?.let { Color.White } ?: Color.Transparent,
        tileSize = 8.dp,
    )
}