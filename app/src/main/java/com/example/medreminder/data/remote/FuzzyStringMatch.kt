package com.example.medreminder.data.remote

object FuzzyStringMatch {
    private const val MIN_ACCEPT_SCORE = 0.42

    fun isAcceptable(query: String, candidate: String): Boolean =
        score(query, candidate) >= MIN_ACCEPT_SCORE

    fun score(query: String, candidate: String): Double {
        val q = normalize(query)
        val c = normalize(candidate)
        if (q.isEmpty() || c.isEmpty()) return 0.0
        if (q == c) return 1.0
        if (c.contains(q) || q.contains(c)) {
            val ratio = minOf(q.length, c.length).toDouble() / maxOf(q.length, c.length)
            return 0.75 + ratio * 0.2
        }
        val qTokens = q.split(' ').filter { it.length >= 2 }
        val cTokens = c.split(' ').filter { it.length >= 2 }
        var tokenBest = 0.0
        for (qt in qTokens) {
            for (ct in cTokens) {
                tokenBest = maxOf(tokenBest, tokenScore(qt, ct))
            }
        }
        val fullDist = levenshtein(q, c)
        val fullScore = 1.0 - fullDist.toDouble() / maxOf(q.length, c.length)
        return maxOf(tokenBest, fullScore)
    }

    fun bestScore(query: String, candidates: List<String>): Double =
        candidates.maxOfOrNull { score(query, it) } ?: 0.0

    private fun tokenScore(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.length >= 3 && b.contains(a)) return 0.88
        if (b.length >= 3 && a.contains(b)) return 0.88
        val dist = levenshtein(a, b)
        return 1.0 - dist.toDouble() / maxOf(a.length, b.length)
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace('ё', 'е')
            .replace(Regex("[^\\p{L}\\p{N}\\s-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val costs = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = i - 1
            costs[0] = i
            for (j in 1..b.length) {
                val temp = costs[j]
                val cost = if (a[i - 1] == b[j - 1]) prev else prev + 1
                prev = costs[j]
                costs[j] = minOf(
                    costs[j] + 1,
                    costs[j - 1] + 1,
                    cost,
                )
            }
        }
        return costs[b.length]
    }
}
