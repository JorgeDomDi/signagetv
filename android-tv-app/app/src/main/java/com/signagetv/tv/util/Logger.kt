package com.signagetv.tv.util

import android.util.Log

/** Centralized logger with a consistent tag. */
object Logger {
    private const val TAG = "SignageTV"

    fun d(msg: String) = Log.d(TAG, msg)
    fun i(msg: String) = Log.i(TAG, msg)
    fun w(msg: String, t: Throwable? = null) = Log.w(TAG, msg, t)
    fun e(msg: String, t: Throwable? = null) = Log.e(TAG, msg, t)
}
