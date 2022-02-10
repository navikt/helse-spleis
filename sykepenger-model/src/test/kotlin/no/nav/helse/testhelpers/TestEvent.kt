package no.nav.helse.testhelpers

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import java.time.LocalDateTime
import java.util.*

private const val UNG_PERSON_FNR_2018 = "12029240045"
private const val AKTØRID = "42"
private const val ORGNUMMER = "987654321"
internal sealed class TestEvent(opprettet: LocalDateTime) : SykdomstidslinjeHendelse(UUID.randomUUID(), UNG_PERSON_FNR_2018, AKTØRID, ORGNUMMER, opprettet) {
    companion object {
        val søknad = Søknad(LocalDateTime.now()).kilde
        val inntektsmelding = Inntektsmelding(LocalDateTime.now()).kilde
        val sykmelding = Sykmelding(LocalDateTime.now()).kilde
        val aareg = Aareg(LocalDateTime.now()).kilde
        val testkilde = TestHendelse(LocalDateTime.now()).kilde
    }

    // Objects impersonating real-life sources of sickness timeline days
    class Inntektsmelding(opprettet: LocalDateTime) : TestEvent(opprettet)
    class Sykmelding(opprettet: LocalDateTime) : TestEvent(opprettet)
    class Søknad(opprettet: LocalDateTime) : TestEvent(opprettet)
    class Aareg(opprettet: LocalDateTime) : TestEvent(opprettet) // Dette er ren spekulasjon omkring AAreg som kildo
    class TestHendelse(opprettet: LocalDateTime) : TestEvent(opprettet)

    override fun sykdomstidslinje() = Sykdomstidslinje()
    override fun valider(periode: Periode, subsumsjonObserver: SubsumsjonObserver) = Aktivitetslogg()
    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = Unit
}
