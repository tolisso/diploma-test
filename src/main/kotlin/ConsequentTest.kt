package io.github.tolisso

import kotlin.math.abs
import kotlin.random.Random

private data class TestState(
    private var x: Int,
    private var y: Int,
    val stack: MutableList<Pair<Int, Int>> = mutableListOf(x to y)
) {

    fun readX() = x
    fun readY() = y
    fun csX(oldX: Int, newX: Int) = if (x == oldX) {
        x = newX
        stack += x to y
        true
    } else {
        false
    }

    fun csY(oldY: Int, newY: Int) = if (y == oldY) {
        y = newY
        stack += x to y
        true
    } else {
        false
    }
}

private sealed class StateRun

private class RetRun(val run: TestState.() -> StateRun) : StateRun()

private class RetInt(val res: Ans) : StateRun()

private fun TestState.get(): StateRun {
    val x = readX()
    return RetRun {
        val y = readY()
        when {
            y >= 0 && x > 0 -> {
                if (y < x) {
                    RetRun {
                        csY(y, x)
                        get()
                    }
                } else if (y == x) {
                    RetRun {
                        csX(x, -x)
                        get()
                    }
                } else {
                    get()
                }
            }

            y < 0 && abs(x) > -y -> {
                RetRun {
                    if (csY(y, y + 1)) {
                        RetRun {
                            if (x == readX()) {
                                RetInt(Ans(x, y))
                            } else {
                                get()
                            }
                        }
                    } else {
                        get()
                    }
                }
            }

            y > 0 && x == -y -> {
                RetRun {
                    csY(y, -y)
                    get()
                }
            }

            y < 0 && x == y -> {
                RetRun {
                    csX(x, -x + 1)
                    get()
                }
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
                throw RuntimeException("x: $x, y: $y, state.x: ${readX()}, state.y: ${readY()}")
            }
        }
    }
}

private fun assertTrue(check: Boolean, msg: String, state: TestState, res: Map<Int, MutableList<Ans>>) {
    if (!check) {
        val lines = listOf(msg, "stack: ${state.stack.joinToString()}", "res: ") +
                res.map { (key, value) -> "thread $key: " + value.joinToString() }
        throw AssertionError(lines.joinToString(System.lineSeparator()))
    }
}

private fun testThreadsOrder(
    threadsOrder: List<Int>
) {
    val res = (0..<ConsequentTestGlobals.THREADS_NUM).associateWith {
        mutableListOf<Ans>()
    }
    val resSet = mutableSetOf<Ans>()

    val state = TestState(2, 0)
    val getInvokes = (1..ConsequentTestGlobals.THREADS_NUM).map {
        state.get()
    }.toMutableList()

    for (curThread in threadsOrder) {
        val invoke = getInvokes[curThread]
        when (invoke) {
            is RetRun -> {
                getInvokes[curThread] = invoke.run(state)
            }

            is RetInt -> {
                assertTrue(!resSet.contains(invoke.res), "${invoke.res} already was answered", state, res)
                res[curThread]!!.lastOrNull()?.also { last ->
                    assertTrue(last.lowerThan(invoke.res), "Thread received lower value after bigger $last ${invoke.res}", state, res)
                }

                res[curThread]!! += invoke.res
                resSet += invoke.res
                getInvokes[curThread] = state.get()
            }
        }
    }
}

fun consequentTest() {
    val rand = Random(1)
    repeat(ConsequentTestGlobals.REPEATS) {
        if (it % (ConsequentTestGlobals.REPEATS / 20) == 0) {
            println("consequent test: ${it.toDouble() / ConsequentTestGlobals.REPEATS * 100}%")
        }
        val threadsOrder = (1..ConsequentTestGlobals.DEPTH).map {
            rand.nextInt(0, ConsequentTestGlobals.THREADS_NUM)
        }
        testThreadsOrder(threadsOrder)
    }
}