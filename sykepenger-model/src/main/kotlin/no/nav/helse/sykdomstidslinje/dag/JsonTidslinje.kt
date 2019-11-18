package no.nav.helse.sykdomstidslinje.dag

import com.fasterxml.jackson.databind.JsonNode

internal data class JsonTidslinje(
    val dager: List<JsonDag>,
    val hendelser: List<JsonNode>

)
