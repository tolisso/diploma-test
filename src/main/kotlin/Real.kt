package io.github.tolisso

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.random.Random


val delayMs = 20L
suspend fun wait() {
    if (Random.nextInt() % 3 == 0) {
        delay(delayMs)
    }
}

data class RealState(val stateX: AtomicInteger, val stateY: AtomicInteger)

suspend fun RealState.get(): Pair<Int, Int> {
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
                    x to y
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