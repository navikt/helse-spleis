package no.nav.helse.serde

import com.fasterxml.jackson.databind.JsonNode

class Metrikk(val path: List<String>, private val node: List<JsonNode>, internal val totalStørrelse: Int) {
    fun prosentdel() = node.sumOf { it.toString().length.toDouble() } / totalStørrelse.toDouble()

    companion object {
        fun metrikkerAv(
            json: String,
            vararg paths: List<String>
        ): List<Metrikk> {
            val node = serdeObjectMapper.readTree(json)
            val totalStørrelse = node.toString().length
            return paths.map { path ->
                metrikkFor(node, path, totalStørrelse)
            }
        }


        private fun metrikkFor(node: JsonNode, path: List<String>, totalStørrelse: Int): Metrikk {
            return Metrikk(path, node.atPath(path), totalStørrelse)
        }

        private fun JsonNode.atPath(path: List<String>) = path.fold(listOf(this), ::subObjects)

        private fun subObjects(
            nodes: List<JsonNode>,
            path: String
        ) = nodes.flatMap { current ->
            val nextNode = current[path]
            if (nextNode.isArray) nextNode.toList() else listOf(nextNode)
        }
    }
}
