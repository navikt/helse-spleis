package no.nav.helse.testhelpers

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import java.time.LocalDateTime
import java.util.*

internal sealed class TestEvent(opprettet: LocalDateTime = LocalDateTime.now()) : SykdomstidslinjeHendelse(UUID.randomUUID(), opprettet) {
    private val UNG_PERSON_FNR_2018 = "12020052345"
    private val AKTØRID = "42"
    private val ORGNUMMER = "987654321"

    companion object {
        val søknad = Søknad().kilde
        val inntektsmelding = Inntektsmelding().kilde
        val sykmelding = Sykmelding().kilde
        val aareg = Aareg().kilde
        val testkilde = TestHendelse().kilde
    }

    // Objects impersonating real-life sources of sickness timeline days
    class Inntektsmelding(opprettet: LocalDateTime = LocalDateTime.now()) : TestEvent(opprettet)
    class Sykmelding(opprettet: LocalDateTime = LocalDateTime.now()) : TestEvent(opprettet)
    class Søknad(opprettet: LocalDateTime = LocalDateTime.now()) : TestEvent(opprettet)
    class Aareg(opprettet: LocalDateTime = LocalDateTime.now()) : TestEvent(opprettet) // Dette er ren spekulasjon omkring AAreg som kilde
    class TestHendelse(opprettet: LocalDateTime = LocalDateTime.now()) : TestEvent(opprettet)

    override fun sykdomstidslinje() = Sykdomstidslinje()
    override fun valider(periode: Periode) = Aktivitetslogg()
    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = Unit
    override fun aktørId() = AKTØRID
    override fun fødselsnummer() = UNG_PERSON_FNR_2018
    override fun organisasjonsnummer() = ORGNUMMER
}
