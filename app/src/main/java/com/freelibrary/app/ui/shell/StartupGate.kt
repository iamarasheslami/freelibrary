package com.freelibrary.app.ui.shell

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tells the start-up screen when the first page has real content. Home marks it ready when its
 * first lists arrive, and the activity then reveals the page. Once ready it stays ready for the rest
 * of the process, so a screen that is recreated (a theme change, a rotation) skips the start-up screen.
 */
@Singleton
class StartupGate
    @Inject
    constructor() {
        private val readyState = MutableStateFlow(false)

        /** True from the moment the first page has content. */
        val ready: StateFlow<Boolean> = readyState.asStateFlow()

        fun markReady() {
            readyState.value = true
        }
    }
