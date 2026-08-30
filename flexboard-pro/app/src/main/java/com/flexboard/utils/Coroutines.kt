package com.flexboard.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
fun runOnIO(block: suspend () -> Unit) { ioScope.launch { block() } }
