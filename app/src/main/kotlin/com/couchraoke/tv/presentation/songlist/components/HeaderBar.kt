package com.couchraoke.tv.presentation.songlist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Text

@Suppress("FunctionNaming", "LongParameterList")
@Composable
fun HeaderBar(
    joinToken: String,
    onJoinPressed: () -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    searchFocusRequester: FocusRequester,
    joinFocusRequester: FocusRequester,
    settingsFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Code: $joinToken",
            modifier = Modifier
                .widthIn(min = 140.dp)
                .padding(vertical = 12.dp)
        )

        SearchField(
            query = searchQuery,
            onQueryChanged = onSearchQueryChanged,
            modifier = Modifier
                .focusRequester(searchFocusRequester)
                .weight(1f)
                .focusProperties {
                    left = joinFocusRequester
                    right = settingsFocusRequester
                }
        )

        Button(
            onClick = onJoinPressed,
            modifier = Modifier
                .focusRequester(joinFocusRequester)
                .focusProperties { right = searchFocusRequester }
        ) {
            Text("JOIN")
        }

        Button(
            onClick = { },
            modifier = Modifier
                .focusRequester(settingsFocusRequester)
                .focusProperties { left = searchFocusRequester }
        ) {
            Text("⚙")
        }
    }
}
