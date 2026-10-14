package io.github.khoryz666.timer.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.khoryz666.timer.AppGraph
import io.github.khoryz666.timer.appGraph

/** A ViewModel built from the [AppGraph] with an explicit factory - no reflection, so R8-safe. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppGraph) -> VM): VM {
    val graph = LocalContext.current.appGraph
    return viewModel(factory = viewModelFactory { initializer { create(graph) } })
}
