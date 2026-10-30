package com.example.medreminder.data.remote

object DrugSearchQueryExpander {
    fun expand(raw: String): List<String> {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return emptyList()
        val variants = linkedSetOf<String>()
        variants += trimmed
        val normalized = trimmed
            .replace('ё', 'е')
            .replace('Ё', 'Е')
            .replace(Regex("[^\\p{L}\\p{N}\\s-+]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (normalized.isNotBlank()) variants += normalized
        val words = normalized.split(' ').filter { it.length >= 3 }
        if (words.isNotEmpty()) {
            variants += words.first()
            if (words.size >= 2) {
                variants += words.take(2).joinToString(" ")
            }
        }
        val compact = normalized.replace(" ", "")
        if (compact.length >= 4) {
            variants += compact
        }
        if (normalized.length >= 5) {
            variants += normalized.dropLast(1)
        }
        if (normalized.length >= 6) {
            variants += normalized.take(normalized.length - 2)
        }
        return variants
            .map { it.trim() }
            .filter { it.length >= 3 }
            .distinct()
    }
}
