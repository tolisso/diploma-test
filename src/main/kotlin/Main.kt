package io.github.tolisso

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger


const val structureTests = false

fun main() {
    if (structureTests) {
        rec(TestState(2, 0), listOf(RetRun { get() }, RetRun { get() }), mutableSetOf(), mutableSetOf(), 2, 1000)
    } else {
        runBlocking {
            for (i in 1..50) {
                val state = RealState(AtomicInteger(2), AtomicInteger(0))

                val res = (1..6).map { a ->
                    async {
                        (1..40).map { b ->
                            println("$a $b")
                            state.get()
                        }
                    }
                }.awaitAll()

                println(state.toString() + " " + res.flatten().size)

                assert(res.flatten().size == res.flatten().toSet().size)
                res.forEach {
                    for (i in 0..<it.size - 1) {
                        assert(it[i].first < it[i + 1].first || (it[i].first == it[i + 1].first && it[i].second < it[i + 1].second))
                    }
                }

            }
        }
    }

}
