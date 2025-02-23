package com.shapun.clickomater.util

object MathUtil {
    fun clamp(value: Float, minimum: Float, maximum: Float): Float {
        return Math.min(maximum, Math.max(minimum, value))
    }

    fun clamp(value: Long, minimum: Long, maximum: Long): Long {
        return Math.min(maximum, Math.max(minimum, value))
    }

    fun map(n: Float, start1: Float, stop1: Float, start2: Float, stop2: Float): Float {
        return (n - start1) / (stop1 - start1) * (stop2 - start2) + start2
    }
}