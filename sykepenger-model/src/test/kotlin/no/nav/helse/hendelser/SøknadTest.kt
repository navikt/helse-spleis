package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Søknad.Inntektskilde
import no.nav.helse.hendelser.Søknad.Merknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Egenmelding
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Papirsykmelding
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Permisjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Utdanning
import no.nav.helse.hentErrors
import no.nav.helse.hentWarnings
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.november
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.sykdomstidslinje.Dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.Dag.FriskHelgedag
import no.nav.helse.sykdomstidslinje.Dag.ProblemDag
import no.nav.helse.sykdomstidslinje.Dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class SøknadTest {

    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12029240045"
        private val EN_PERIODE = Periode(1.januar, 31.januar)
        val FYLLER_18_ÅR_2_NOVEMBER = "02110075045"
    }

    private lateinit var søknad: Søknad

    @Test
    fun `søknad med bare sykdom`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent))
        assertFalse(søknad.valider(EN_PERIODE, MaskinellJurist()).hasErrorsOrWorse())
        assertEquals(10, søknad.sykdomstidslinje().count())
    }

    @Test
    fun `warning ved ANDRE_ARBEIDSFORHOLD hvor bruker er sykmeldt, men vi kun kjenner til sykmelding for en arbeidsgiver`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), andreInntektskilder = listOf(Inntektskilde(true, "ANDRE_ARBEIDSFORHOLD")))
        assertFalse(søknad.valider(EN_PERIODE, MaskinellJurist()).hasErrorsOrWorse())
        assertTrue(søknad.validerIkkeOppgittFlereArbeidsforholdMedSykmelding().hasWarningsOrWorse())
    }

    @Test
    fun `søknad med ferie`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), Ferie(2.januar, 4.januar))
        assertFalse(søknad.valider(EN_PERIODE, MaskinellJurist()).hasErrorsOrWorse())
        assertEquals(10, søknad.sykdomstidslinje().count())
    }

    @Test
    fun `17 år på søknadstidspunkt gir error`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), fnr = FYLLER_18_ÅR_2_NOVEMBER, sendtTilNAVEllerArbeidsgiver = 1.november.atStartOfDay())
        assertTrue(søknad.valider(EN_PERIODE, MaskinellJurist()).hasErrorsOrWorse())
    }

    @Test
    fun `18 år på søknadstidspunkt gir ikke error`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), fnr = FYLLER_18_ÅR_2_NOVEMBER, sendtTilNAVEllerArbeidsgiver = 2.november.atStartOfDay())
        assertFalse(søknad.valider(EN_PERIODE, MaskinellJurist()).hasErrorsOrWorse())
    }

    @Test
    fun `søknad med utdanning`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), Utdanning(5.januar, 10.januar))
        assertTrue(søknad.valider(EN_PERIODE, MaskinellJurist()).hasWarningsOrWorse())
        assertEquals(10, søknad.sykdomstidslinje().count())
    }

    @Test
    fun `søknad med utdanning før perioden`() {
        søknad(Sykdom(5.januar, 10.januar, 100.prosent), Utdanning(1.januar, 10.januar))
        assertTrue(søknad.valider(EN_PERIODE, MaskinellJurist()).hasWarningsOrWorse())
        assertEquals(5.januar til 10.januar, søknad.periode())
        assertEquals(6, søknad.sykdomstidslinje().count())
    }

    @Test
    fun `søknad med permisjon`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), Permisjon(5.januar, 10.januar))
        assertFalse(søknad.valider(EN_PERIODE, MaskinellJurist()).hasErrorsOrWorse())
        assertEquals(10, søknad.sykdomstidslinje().count())
    }

    @Test
    fun `søknad med permisjon før perioden`() {
        søknad(Sykdom(5.januar, 10.januar, 100.prosent), Permisjon(1.januar, 10.januar))
        assertTrue(søknad.valider(EN_PERIODE, MaskinellJurist()).hasWarningsOrWorse())
        assertEquals(5.januar til 10.januar, søknad.periode())
        assertEquals(6, søknad.sykdomstidslinje().count())
    }

    @Test
    fun `søknad med papirsykmelding utenfor søknadsperioden`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), Papirsykmelding(11.januar, 16.januar))
        assertTrue(søknad.valider(EN_PERIODE, MaskinellJurist()).hasErrorsOrWorse())
        assertEquals(10, søknad.sykdomstidslinje().count())
        assertEquals(1.januar til 10.januar, søknad.periode())
        assertEquals(0, søknad.sykdomstidslinje().filterIsInstance<ProblemDag>().size)
    }

    @Test
    fun `søknad med papirsykmelding`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), Papirsykmelding(1.januar, 10.januar))
        assertTrue(søknad.valider(EN_PERIODE, MaskinellJurist()).hasErrorsOrWorse())
        assertEquals(10, søknad.sykdomstidslinje().count())
        assertEquals(10, søknad.sykdomstidslinje().filterIsInstance<ProblemDag>().size)
    }

    @Test
    fun `sykdomsgrad under 100 støttes`() {
        søknad(Sykdom(1.januar, 10.januar, 50.prosent))
        assertFalse(søknad.valider(EN_PERIODE, MaskinellJurist()).hasErrorsOrWorse())
    }

    @Test
    fun `sykdom faktiskgrad under 100 støttes`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent, 50.prosent))
        assertFalse(søknad.valider(EN_PERIODE, MaskinellJurist()).hasErrorsOrWorse())
    }

    @Test
    fun `ferie foran sykdomsvindu`() {
        søknad(Sykdom(1.februar, 10.februar, 100.prosent), Ferie(20.januar, 31.januar))
        assertFalse(søknad.valider(EN_PERIODE, MaskinellJurist()).hasWarningsOrWorse())
        assertEquals(1.februar, søknad.sykdomstidslinje().førsteDag())
    }

    @Test
    fun `ulik ferieinformasjon`() {
        søknad(Sykdom(1.februar, 10.februar, 100.prosent), Ferie(20.januar, 31.januar))
        assertFalse(søknad.harUlikFerieinformasjon(Sykdomstidslinje.Companion.feriedager(20.januar, 31.januar, SykdomstidslinjeHendelse.Hendelseskilde.INGEN)))
        assertFalse(søknad.harUlikFerieinformasjon(Sykdomstidslinje.Companion.feriedager(21.januar, 31.januar, SykdomstidslinjeHendelse.Hendelseskilde.INGEN)))
        assertFalse(søknad.harUlikFerieinformasjon(Sykdomstidslinje.Companion.feriedager(20.januar, 30.januar, SykdomstidslinjeHendelse.Hendelseskilde.INGEN)))
        assertTrue(søknad.harUlikFerieinformasjon(Sykdomstidslinje.Companion.sykedager(20.januar, 10.februar, 100.prosent, SykdomstidslinjeHendelse.Hendelseskilde.INGEN)))
    }

    @Test
    fun `ferie etter sykdomsvindu - ikke et realistisk scenario`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), Ferie(2.januar, 16.januar))
        assertFalse(søknad.valider(EN_PERIODE, MaskinellJurist()).hasWarningsOrWorse())
        assertEquals(1.januar til 10.januar, søknad.sykdomstidslinje().periode())
    }

    @Test
    fun `utdanning ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), Utdanning(16.januar, 17.januar))
        assertTrue(søknad.valider(EN_PERIODE, MaskinellJurist()).hasWarningsOrWorse())
    }

    @Test
    fun `permisjon ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), Permisjon(2.januar, 16.januar))
        assertTrue(søknad.valider(EN_PERIODE, MaskinellJurist()).hasWarningsOrWorse())
    }

    @Test
    fun `arbeidag ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), Arbeid(2.januar, 16.januar))
        assertTrue(søknad.valider(EN_PERIODE, MaskinellJurist()).hasWarningsOrWorse())
    }

    @Test
    fun `egenmelding ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(5.januar, 12.januar, 100.prosent), Egenmelding(2.januar, 3.januar))
        assertFalse(søknad.valider(EN_PERIODE, MaskinellJurist()).hasErrorsOrWorse())
        assertEquals(8, søknad.sykdomstidslinje().count())
        assertEquals(6, søknad.sykdomstidslinje().filterIsInstance<Sykedag>().size)
        assertEquals(2, søknad.sykdomstidslinje().filterIsInstance<SykHelgedag>().size)
        assertEquals(5.januar til 12.januar, søknad.sykdomstidslinje().periode())
    }

    @Test
    fun `egenmelding ligger etter sykdomsvindu`() {
        søknad(Sykdom(5.januar, 12.januar, 100.prosent), Egenmelding(13.januar, 17.januar))
        assertFalse(søknad.valider(EN_PERIODE, MaskinellJurist()).hasErrorsOrWorse())
        assertEquals(8, søknad.sykdomstidslinje().count())
        assertEquals(6, søknad.sykdomstidslinje().filterIsInstance<Sykedag>().size)
        assertEquals(2, søknad.sykdomstidslinje().filterIsInstance<SykHelgedag>().size)
        assertEquals(5.januar til 12.januar, søknad.sykdomstidslinje().periode())
    }

    @Test
    fun `egenmelding ligger langt utenfor sykdomsvindu`() {
        søknad(Sykdom(5.januar, 12.januar, 100.prosent), Egenmelding(19.desember(2017), 20.desember(2017)))
        assertFalse(søknad.valider(EN_PERIODE, MaskinellJurist()).hasErrorsOrWorse())
        assertEquals(8, søknad.sykdomstidslinje().count())
    }

    @Test
    fun `søknad uten andre inntektskilder`() {
        søknad(Sykdom(5.januar, 12.januar, 100.prosent), andreInntektskilder = emptyList())
        assertFalse(søknad.valider(EN_PERIODE, MaskinellJurist()).hasErrorsOrWorse())
    }

    @Test
    fun `må ha perioder`() {
        assertThrows<Aktivitetslogg.AktivitetException> { søknad() }
    }

    @Test
    fun `må ha sykdomsperioder`() {
        assertThrows<Aktivitetslogg.AktivitetException> { søknad(Ferie(2.januar, 16.januar)) }
    }

    @Test
    fun `angitt arbeidsgrad kan ikke føre til sykegrad høyere enn graden fra sykmelding`() {
        søknad(Sykdom(1.januar, 31.januar, 20.prosent, 79.prosent))
        søknad.valider(EN_PERIODE, MaskinellJurist())
        assertTrue(søknad.hasErrorsOrWorse())
    }

    @Test
    fun `angitt arbeidsgrad kan føre til lavere sykegrad enn graden fra sykmelding`() {
        søknad(Sykdom(1.januar, 31.januar, 20.prosent, 81.prosent))
        søknad.valider(EN_PERIODE, MaskinellJurist())
        assertFalse(søknad.hasErrorsOrWorse())
    }

    @Test
    fun `angitt arbeidsgrad kan føre til lik sykegrad som graden fra sykmelding`() {
        søknad(Sykdom(1.januar, 31.januar, 20.prosent, 80.prosent))
        søknad.valider(EN_PERIODE, MaskinellJurist())
        assertFalse(søknad.hasErrorsOrWorse())
    }

    @Test
    fun `søknad uten permittering får ikke warning`() {
        søknad(Sykdom(1.januar, 31.januar, 20.prosent, 80.prosent))
        søknad.valider(EN_PERIODE, MaskinellJurist())
        assertFalse(søknad.hasWarningsOrWorse())
    }

    @Test
    fun `søknad med permittering får warning`() {
        søknad(Sykdom(1.januar, 31.januar, 20.prosent, 80.prosent), permittert = true)
        søknad.valider(EN_PERIODE, MaskinellJurist())
        assertTrue(søknad.hasWarningsOrWorse())
    }

    @Test
    fun `søknad uten tilbakedateringmerknad får ikke warning`() {
        søknad(Sykdom(1.januar, 31.januar, 20.prosent, 80.prosent))
        søknad.valider(EN_PERIODE, MaskinellJurist())
        assertFalse(søknad.hasWarningsOrWorse())
    }

    @Test
    fun `søknad med tilbakedateringmerknad får warning`() {
        søknad(Sykdom(1.januar, 31.januar, 20.prosent, 80.prosent), merknaderFraSykmelding = listOf(Merknad("UGYLDIG_TILBAKEDATERING", null)))
        søknad.valider(EN_PERIODE, MaskinellJurist())
        assertTrue(søknad.hasWarningsOrWorse())
    }

    @Test
    fun `søknadsturnering for nye dagtyper`() {
        søknad(Arbeid(15.januar, 31.januar), Sykdom(1.januar, 31.januar, 100.prosent))

        assertEquals(10, søknad.sykdomstidslinje().filterIsInstance<Sykedag>().size)
        assertEquals(4, søknad.sykdomstidslinje().filterIsInstance<SykHelgedag>().size)
        assertEquals(13, søknad.sykdomstidslinje().filterIsInstance<Arbeidsdag>().size)
        assertEquals(4, søknad.sykdomstidslinje().filterIsInstance<FriskHelgedag>().size)
    }

    @Test
    fun `turnering mellom arbeidsgiverdager og sykedager`() {
        søknad(Sykdom(1.januar, 31.januar, 100.prosent), Egenmelding(15.januar, 31.januar))

        assertEquals(23, søknad.sykdomstidslinje().filterIsInstance<Sykedag>().size)
        assertEquals(8, søknad.sykdomstidslinje().filterIsInstance<SykHelgedag>().size)
    }

    @Test
    fun `legger på warning om søknad inneholder foreldete dager`() {
        søknad(Sykdom(1.januar, 1.mai, 100.prosent))
        søknad.valider(EN_PERIODE, MaskinellJurist())
        assertEquals(1, søknad.kontekster().size)
        assertTrue(søknad.hasWarningsOrWorse())
        assertFalse(søknad.hasErrorsOrWorse())
    }

    @Test
    fun `inntektskilde med type ANNET skal gi warning istedenfor error`() {
        søknad(Sykdom(1.januar, 31.januar, 100.prosent), andreInntektskilder = listOf(Inntektskilde(true, "ANNET")))
        søknad.valider(EN_PERIODE, MaskinellJurist())
        assertTrue(søknad.hentWarnings().contains("Det er oppgitt annen inntektskilde i søknaden. Vurder inntekt."))
        assertTrue(søknad.hentErrors().isEmpty())
    }

    @Test
    fun `inntektskilde med type FRILANSER skal gi error`() {
        søknad(Sykdom(1.januar, 31.januar, 100.prosent), andreInntektskilder = listOf(Inntektskilde(true, "FRILANSER")))
        søknad.valider(EN_PERIODE, MaskinellJurist())
        assertTrue(søknad.hentErrors().contains("Søknaden inneholder andre inntektskilder enn ANDRE_ARBEIDSFORHOLD"))
    }

    private fun søknad(vararg perioder: Søknadsperiode, andreInntektskilder: List<Inntektskilde> = emptyList(), permittert: Boolean = false, merknaderFraSykmelding: List<Merknad> = emptyList(), fnr: String = UNG_PERSON_FNR_2018, sendtTilNAVEllerArbeidsgiver: LocalDateTime? = null) {
        søknad = Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = fnr,
            aktørId = "12345",
            orgnummer = "987654321",
            perioder = listOf(*perioder),
            andreInntektskilder = andreInntektskilder,
            sendtTilNAVEllerArbeidsgiver = sendtTilNAVEllerArbeidsgiver ?: Søknadsperiode.søknadsperiode(perioder.toList())?.endInclusive?.atStartOfDay() ?: LocalDateTime.now(),
            permittert = permittert,
            merknaderFraSykmelding = merknaderFraSykmelding,
            sykmeldingSkrevet = LocalDateTime.now()
        )
    }
}
