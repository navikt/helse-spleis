package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

enum class Avsender {
    SYKMELDT, ARBEIDSGIVER, SAKSBEHANDLER, SYSTEM
}

interface Hendelse : IAktivitetslogg {
    fun meldingsreferanseId(): UUID
    fun innsendt(): LocalDateTime
    fun registrert(): LocalDateTime
    fun avsender(): Avsender
    fun navn(): String
}