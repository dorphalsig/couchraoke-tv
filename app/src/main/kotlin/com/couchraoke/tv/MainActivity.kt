package com.couchraoke.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.couchraoke.tv.presentation.songlist.SongListScreen
import com.couchraoke.tv.ui.theme.CouchraokeTheme

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CouchraokeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    // T040 builds JoinViewModel and T041 the overlay, both later in this
                    // slice. The Join action stays inert until that composition root exists.
                    SongListScreen(onJoinClick = {})
                }
            }
        }
    }
}
