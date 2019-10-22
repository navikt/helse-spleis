package no.nav.helse.sykdomstidslinje.dag

internal data class JsonTidslinje(
    val dager: List<JsonDag>,
    val hendelser: List<JsonHendelse>

)
