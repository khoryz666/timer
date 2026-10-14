package io.github.khoryz666.timer.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.khoryz666.timer.core.AppClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface UpdatePhase {
    data object Idle : UpdatePhase
    data object Checking : UpdatePhase
    /** Waiting for the user to allow "Install unknown apps" for this app. */
    data object NeedsPermission : UpdatePhase
    data class Downloading(val progress: Float) : UpdatePhase
    /** Handed to the system installer; its confirmation screen takes over. */
    data object Installing : UpdatePhase
}

data class UpdateState(
    val enabled: Boolean,
    val installedVersion: String,
    val phase: UpdatePhase = UpdatePhase.Idle,
    /** A newer release, once one has been found. */
    val available: Release? = null,
    val dialogOpen: Boolean = false,
    /** The result of the last manual check or install attempt, for the user to read. */
    val message: String? = null,
)

class UpdateViewModel(
    private val releases: GitHubReleases,
    private val installer: ApkInstaller,
    private val store: UpdateCheckStore,
    private val clock: AppClock,
    installedVersion: String,
    enabled: Boolean,
) : ViewModel() {

    private val _state = MutableStateFlow(UpdateState(enabled, installedVersion))
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        viewModelScope.launch {
            installer.failures.filterNotNull().collect { failure ->
                installer.clearFailure()
                _state.update { it.copy(phase = UpdatePhase.Idle, message = failure, dialogOpen = it.available != null) }
            }
        }
    }

    /** On launch: checks at most once a day, and stays silent unless an update is found. */
    fun autoCheck() {
        if (!state.value.enabled) return
        job = viewModelScope.launch {
            val now = clock.now()
            if (!isAutoCheckDue(store.lastAutoCheck(), now)) return@launch
            store.setLastAutoCheck(now)
            val release = runCatching { releases.latest() }.getOrNull() ?: return@launch
            if (release.isNewerThan(state.value.installedVersion)) {
                _state.update { it.copy(available = release, dialogOpen = true) }
            }
        }
    }

    /** From the Settings button: always checks, and always says what happened. */
    fun checkNow() {
        if (!state.value.enabled || state.value.phase != UpdatePhase.Idle) return
        job = viewModelScope.launch {
            _state.update { it.copy(phase = UpdatePhase.Checking, message = null) }
            try {
                val release = releases.latest()
                _state.update {
                    if (release.isNewerThan(it.installedVersion)) {
                        it.copy(phase = UpdatePhase.Idle, available = release, dialogOpen = true)
                    } else {
                        it.copy(phase = UpdatePhase.Idle, available = null, message = "You're on the latest version.")
                    }
                }
            } catch (e: UpdateException) {
                _state.update { it.copy(phase = UpdatePhase.Idle, message = e.message) }
            }
        }
    }

    fun openDialog() = _state.update { it.copy(dialogOpen = it.available != null, message = null) }

    fun dismiss() {
        job?.cancel()
        _state.update { it.copy(phase = UpdatePhase.Idle, dialogOpen = false) }
    }

    /** Downloads and installs [UpdateState.available], first asking for the install permission if needed. */
    fun install() {
        val release = state.value.available ?: return
        if (!installer.canInstall()) {
            _state.update { it.copy(phase = UpdatePhase.NeedsPermission) }
            return
        }
        job = viewModelScope.launch {
            _state.update { it.copy(phase = UpdatePhase.Downloading(0f), message = null) }
            try {
                val apk = installer.apkFile
                releases.download(release, apk) { progress ->
                    _state.update { it.copy(phase = UpdatePhase.Downloading(progress)) }
                }
                installer.install(apk)
                _state.update { it.copy(phase = UpdatePhase.Installing) }
            } catch (e: IOException) {
                val message = if (e is UpdateException) e.message else "The update couldn't be installed: ${e.message}"
                _state.update { it.copy(phase = UpdatePhase.Idle, message = message) }
            }
        }
    }

    /** Back from the permission screen: carry on if it was granted. */
    fun onResume() {
        if (state.value.phase == UpdatePhase.NeedsPermission && installer.canInstall()) install()
    }

    fun permissionSettingsIntent() = installer.permissionSettingsIntent()
}
