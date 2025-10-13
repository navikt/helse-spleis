package no.nav.helse

internal fun tournament(times: Int, vararg block: () -> Any) {
    block.toList().map { timer(times, it) }.also { results ->
        println("Results after $times invocations:")
        results.forEachIndexed { key, (sum, avg) ->
            println("Alternative ${key + 1}: $sum ms total, $avg ms avg")
        }
    }.also { results ->
        val best = results.maxOf { it.second }
        val worst = results.minOf { it.second }
        println("Best avg at $best ms is ${(best / worst * 10).toInt() / 10.0} times better than the worst avg of $worst ms\n")
    }
}

private fun timer(times: Int, block: () -> Any): Pair<Long, Double> {
    var sum = 0L
    repeat(times) { sum += timed(block) }
    return sum to sum / times.toDouble()
}

private fun timed(block: () -> Any): Long {
    val start = System.currentTimeMillis()
    block()
    return System.currentTimeMillis() - start
}
