package no.nav.helse.serde

import com.fasterxml.jackson.databind.JsonNode

class Metrikk(val path: List<String>, private val node: List<JsonNode>) {
    fun størrelse() = node.sumOf { it.toString().length }

    companion object {
        fun metrikkerAv(
            json: String,
            vararg paths: List<String>
        ): List<Metrikk> {
            return paths.map { path ->
                metrikkFor(serdeObjectMapper.readTree(json), path)
            }
        }


        private fun metrikkFor(node: JsonNode, path: List<String>): Metrikk {
            return Metrikk(path, node.atPath(path))
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
