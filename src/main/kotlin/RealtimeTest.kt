package io.github.tolisso

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.random.Random


val delayMs = 10L
suspend fun wait() {
    if (Random.nextInt() % 3 == 0) {
        delay(delayMs)
    }
}

data class State(val stateX: AtomicInteger, val stateY: AtomicInteger)

suspend fun State.get(): Ans {
    wait()
    val x = stateX.get()
    wait()
    val y = stateY.get()
    wait()
    return when {
        y >= 0 && x > 0 -> {
            if (y < x) {
                stateY.compareAndSet(y, x)
                wait()
                get()
            } else if (y == x) {
                stateX.compareAndSet(x, -x)
                wait()
                get()
            } else {
                get()
            }
        }

        y < 0 && abs(x) > -y -> {
            if (stateY.compareAndSet(y, y + 1)) {
                wait()
                if (x == stateX.get()) {
                    Ans(x, y)
                } else {
                    get()
                }
            } else {
                get()
            }
        }

        y > 0 && x == -y -> {
            stateY.compareAndSet(y, -y)
            get()
        }

        y < 0 && x == y -> {
            stateX.compareAndSet(x, -x + 1)
            get()
        }

        abs(y) > abs(x) -> {
            get()
        }

        y < 0 && y < x -> {
            get()
        }

        y >= 0 && x < 0 -> {
            get()
        }

        else -> {
            throw RuntimeException("x: $x, y: $y, state.x: ${stateX.get()}, state.y: ${stateY.get()}")
        }
    }
}

fun realtimeTest() = runBlocking {
    repeat(RealtimeTestGlobals.REPEATS) {
        println("realtime test: ${it.toDouble() / RealtimeTestGlobals.REPEATS * 100}%")
        val state = State(AtomicInteger(2), AtomicInteger(0))

        val res = (1..RealtimeTestGlobals.THREADS_NUM).map { a ->
            async {
                (1..RealtimeTestGlobals.DEPTH).map { b ->
                    // println("$a $b")
                    state.get()
                }
            }
        }.awaitAll()

        // println(state.toString() + " " + res.flatten().size)

        assert(res.flatten().size == res.flatten().toSet().size)
        res.forEach {
            for (i in 0..<it.size - 1) {
                assert(it[i].lowerThan(it[i + 1]))
            }
        }
    }
}