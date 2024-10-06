package io.github.tolisso

import kotlin.math.abs
import kotlin.random.Random

const val test = false

data class TestState(
    private var x: Int,
    private var y: Int,
    val stack: MutableList<Pair<Int, Int>> = mutableListOf(x to y)
) {

    fun copy(): TestState = if (test) {
        TestState(x, y, ArrayList(stack))
    } else {
        TestState(x, y, mutableListOf())
    }

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

sealed class StateRun

class RetRun(val run: TestState.() -> StateRun) : StateRun()

class RetInt(val res: Pair<Int, Int>) : StateRun()

fun TestState.get(): RetRun {
    val x = readX()
    return RetRun {
        val y = readY()
        when {
            y >= 0 && x > 0 -> {
                if (y < x) {
                    RetRun {
                        csY(y, x)
                        RetRun { get() }
                    }
                } else if (y == x) {
                    RetRun {
                        csX(x, -x)
                        RetRun { get() }
                    }
                } else {
                    RetRun { get() }
                }
            }

            y < 0 && abs(x) > -y -> {
                RetRun {
                    if (csY(y, y + 1)) {
                        RetRun {
                            if (x == readX()) {
                                RetInt(x to y)
                            } else {
                                RetRun { get() }
                            }
                        }
                    } else {
                        RetRun { get() }
                    }
                }
            }

            y > 0 && x == -y -> {
                RetRun {
                    csY(y, -y)
                    RetRun { get() }
                }
            }

            y < 0 && x == y -> {
                RetRun {
                    csX(x, -x + 1)
                    RetRun { get() }
                }
            }

            abs(y) > abs(x) -> {
                RetRun { get() }
            }

            y < 0 && y < x -> {
                RetRun { get() }
            }

            y >= 0 && x < 0 -> {
                RetRun { get() }
            }

            else -> {
                throw RuntimeException("x: $x, y: $y, state.x: ${readX()}, state.y: ${readY()}")
            }
        }
    }
}

fun rec(
    state: TestState,
    threads: List<RetRun>,
    ans: MutableSet<Pair<Int, Pair<Int, Int>>>,
    s: MutableSet<Pair<Int, Int>>,
    w: Int,
    kMax: Int,
    k: Int = 0,
    prc: Double = 0.0,
    prcStep: Double = 1.0
) {
    if (Random.nextInt() % 10000000 == 0) {
        println("$prc%")
    }
    if (s.size < ans.size) {
        println(state.stack)
        println(ans)
        throw RuntimeException()
    }
    if (k == kMax) {
        return
        println(state.stack)
        println(ans)
        throw RuntimeException()
    }
    for (i in 0..<threads.size) {
        if (ans.filter { it.first == i }.size >= w) {
            continue
        }
        val newPrc = prc + prcStep / threads.size * i
        val stateCopy = state.copy()
        val res = try {
            threads[i].run.invoke(stateCopy)
        } catch (e: Exception) {
            throw RuntimeException("${e.message}, stack=[${state.stack.joinToString(", ")}]")
        }
        when {
            (res is RetInt) -> {
                if (ans.contains(i to res.res)) {
                    println(state.stack)
                    println(ans)
                    throw RuntimeException()
                }
                if (s.contains(res.res)) {
                    println(i to res.res)
                    println(state.stack)
                    println(ans)
                    throw RuntimeException()
                }
                ans += i to res.res
                s += res.res
                rec(
                    stateCopy,
                    threads.subList(0, i) + listOf(RetRun { get() }) + threads.subList(i + 1, threads.size),
                    ans,
                    s,
                    w,
                    kMax,
                    k + 1,
                    newPrc,
                    prcStep / threads.size
                )
                ans.remove(i to res.res)
                s.remove(res.res)
            }

            res is RetRun -> {
                rec(
                    stateCopy,
                    threads.subList(0, i) + listOf(res) + threads.subList(i + 1, threads.size),
                    ans,
                    s,
                    w,
                    kMax,
                    k + 1,
                    newPrc,
                    prcStep / threads.size
                )
            }
        }
    }
}