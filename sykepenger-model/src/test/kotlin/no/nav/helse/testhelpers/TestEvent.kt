package no.nav.helse.testhelpers

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.NySykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import java.time.LocalDate
import java.util.*

internal sealed class TestEvent : SykdomstidslinjeHendelse(UUID.randomUUID()) {
    private val UNG_PERSON_FNR_2018 = "12020052345"
    private val AKTØRID = "42"
    private val ORGNUMMER = "987654321"

    companion object {
        val søknad = Søknad.kilde
        val inntektsmelding = Inntektsmelding.kilde
        val sykmelding = Sykmelding.kilde
        val aareg = Aareg.kilde
        val testkilde = TestHendelse.kilde
    }

    // Objects impersonating real-life sources of sickness timeline days
    object Inntektsmelding : TestEvent()

    object Sykmelding : TestEvent()
    object Søknad : TestEvent()
    object Aareg : TestEvent() // Dette er ren spekulasjon omkring AAreg som kilde
    object TestHendelse : TestEvent()

    override fun nySykdomstidslinje() = NySykdomstidslinje()
    override fun nySykdomstidslinje(tom: LocalDate) = NySykdomstidslinje()
    override fun valider(periode: Periode) = Aktivitetslogg()
    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = Unit
    override fun aktørId() = AKTØRID
    override fun fødselsnummer() = UNG_PERSON_FNR_2018
    override fun organisasjonsnummer() = ORGNUMMER
}
