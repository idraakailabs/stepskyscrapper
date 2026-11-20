package com.idraak.stepskyscraper.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.idraak.stepskyscraper.core.designsystem.theme.SsRadius
import com.idraak.stepskyscraper.core.designsystem.theme.SsTheme

/**
 * Modal sheet: 24dp top corners, 32 × 4 drag handle, scrim #24211D at 32%. Material's sheet motion is the
 * closest native match to "300ms emphasized-decelerate; drag down to dismiss".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SsModalSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    skipPartiallyExpanded: Boolean = true,
    state: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = state,
        shape = SsRadius.shapeSheet,
        containerColor = SsTheme.colors.surface,
        contentColor = SsTheme.colors.onSurface,
        scrimColor = SsTheme.colors.scrim,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) { SheetHandle() }
        },
        content = content,
    )
}
