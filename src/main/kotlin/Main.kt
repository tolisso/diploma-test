package io.github.tolisso

object ConsequentTestGlobals {
    const val THREADS_NUM = 3
    const val REPEATS = 20000
    const val DEPTH = 5000
}

object RealtimeTestGlobals {
    const val THREADS_NUM = 100
    const val REPEATS = 10
    const val DEPTH = 10
}

data class Ans(val x: Int, val y: Int) {
    fun lowerThan(other: Ans): Boolean {
        if (x < other.x) return true
        if (x == other.x && y < other.y) return true
        return false
    }
}

fun main() {
    consequentTest()
    realtimeTest()
}
