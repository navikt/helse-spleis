package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID

enum class Avsender {
    SYKMELDT, SAKSBEHANDLER, ARBEIDSGIVER, SYSTEM
}

interface Hendelseinfo {
    fun meldingsreferanseId(): UUID
    fun innsendt(): LocalDateTime
    fun avsender(): Avsender
}