package no.nav.helse.sak

import no.nav.helse.hendelser.Hendelsetype
import java.time.LocalDateTime

interface ArbeidstakerHendelse {
    fun hendelsetype(): Hendelsetype
    fun opprettet(): LocalDateTime

    fun aktørId(): String
    fun fødselsnummer(): String
    fun organisasjonsnummer(): String
    fun kanBehandles() = true

    fun toJson(): String
}
