package com.egan.core.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

fun backgroundScope() = CoroutineScope(Dispatchers.Default)