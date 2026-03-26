package com.ustadmobile.meshrabiya.testapp.appstate

data class AppUiState(
        val title: String = "",
        val fabState: FabState = FabState(),
        val topBarActionIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
        val topBarActionContentDescription: String? = null,
        val onTopBarActionClick: (() -> Unit)? = null
) {}
