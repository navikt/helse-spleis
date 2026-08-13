package no.nav.helse.opptjening.domain

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.fredag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.lørdag
import no.nav.helse.mai
import no.nav.helse.mandag
import no.nav.helse.mars
import no.nav.helse.oktober
import no.nav.helse.tirsdag
import no.nav.helse.torsdag
import no.nav.helse.søndag
import no.nav.helse.opptjening.domain.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT
import no.nav.helse.opptjening.domain.Kodeverkkode.IKKE_OPPTJENING_ARBEID_ELLER_YTELSE
import no.nav.helse.opptjening.domain.Kodeverkkode.OPPTJENING_MINST_4_UKER
import no.nav.helse.opptjening.domain.Opptjening.AutomatiskVurdering
import no.nav.helse.opptjening.domain.Opptjening.AutomatiskVurdering.OpptjeningsgrunnlagForAutomatiskVurdering
import no.nav.helse.opptjening.domain.Opptjening.AutomatiskVurdering.OpptjeningsgrunnlagForAutomatiskVurdering.ForArbeidstaker
import no.nav.helse.opptjening.domain.Opptjening.AutomatiskVurdering.OpptjeningsgrunnlagForAutomatiskVurdering.ForSelvstendigNæringsdrivende
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

internal class OpptjeningAutomatiskVurderingTest {

    // 4.januar til 31.januar er nøyaktig 28 dager fram til dagen før skjæringstidspunktet
    @Test
    fun `nøyaktig 28 opptjeningsdager er oppfylt`() {
        val kodeverkkode = vurderArbeidstaker(1.februar, arbeidsforhold(4.januar til 31.januar))
        assertEquals(OPPTJENING_MINST_4_UKER, kodeverkkode)
    }

    @Test
    fun `27 opptjeningsdager er ikke oppfylt`() {
        val kodeverkkode = vurderArbeidstaker(1.februar, arbeidsforhold(5.januar til 31.januar))
        assertEquals(IKKE_OPPTJENING_ARBEID_ELLER_YTELSE, kodeverkkode)
    }

    @Test
    fun `flere enn 28 opptjeningsdager er oppfylt`() {
        val kodeverkkode = vurderArbeidstaker(1.februar, arbeidsforhold(1.januar til 31.januar))
        assertEquals(OPPTJENING_MINST_4_UKER, kodeverkkode)
    }

    @Test
    fun `uten arbeidsforhold er opptjening ikke oppfylt`() {
        val kodeverkkode = vurderArbeidstaker(1.februar)
        assertEquals(IKKE_OPPTJENING_ARBEID_ELLER_YTELSE, kodeverkkode)
    }

    // Arbeidsforholdet må være løpende dagen før skjæringstidspunktet for å telle
    @Test
    fun `arbeidsforhold avsluttet før dagen før skjæringstidspunktet teller ikke`() {
        val kodeverkkode = vurderArbeidstaker(1.februar, arbeidsforhold(1.januar til 30.januar))
        assertEquals(IKKE_OPPTJENING_ARBEID_ELLER_YTELSE, kodeverkkode)
    }

    @Test
    fun `arbeidsforhold som starter på skjæringstidspunktet teller ikke`() {
        val kodeverkkode = vurderArbeidstaker(1.februar, arbeidsforhold(1.februar til 28.februar))
        assertEquals(IKKE_OPPTJENING_ARBEID_ELLER_YTELSE, kodeverkkode)
    }

    // Løpende arbeidsforhold har ansettelseperiode til LocalDate.MAX,
    // og må avkortes ved skjæringstidspunktet før dagene telles
    @Test
    fun `løpende arbeidsforhold avkortes ved skjæringstidspunktet`() {
        val løpende = Arbeidsforhold(orgnummer = ORGNUMMER, ansattFom = 1.januar, ansattTom = null, type = ORDINÆRT)
        assertEquals(OPPTJENING_MINST_4_UKER, vurderArbeidstaker(1.februar, løpende))
        // 1.januar til 3.januar er bare 3 dager, selv om arbeidsforholdet er løpende
        assertEquals(IKKE_OPPTJENING_ARBEID_ELLER_YTELSE, vurderArbeidstaker(4.januar, løpende))
    }

    @Test
    fun `sammenhengende arbeidsforhold hos to arbeidsgivere slås sammen`() {
        val førsteJobb = arbeidsforhold(1.januar til 15.januar, orgnummer = "111111111")
        val andreJobb = arbeidsforhold(16.januar til 31.januar, orgnummer = "222222222")

        // hver for seg er ingen av dem lange nok
        assertEquals(IKKE_OPPTJENING_ARBEID_ELLER_YTELSE, vurderArbeidstaker(1.februar, førsteJobb))
        assertEquals(IKKE_OPPTJENING_ARBEID_ELLER_YTELSE, vurderArbeidstaker(1.februar, andreJobb))
        // til sammen utgjør de 31 sammenhengende dager
        assertEquals(OPPTJENING_MINST_4_UKER, vurderArbeidstaker(1.februar, førsteJobb, andreJobb))
    }

    // Sammenhengen brytes ikke av en helg: fredag regnes som rett før påfølgende mandag
    @Test
    fun `arbeidsforhold som henger sammen over helg slås sammen`() {
        val sisteDagFørJobbskifte = 4.fredag // 26.januar 2018
        val førsteDagEtterJobbskifte = 5.mandag // 29.januar 2018
        val gammelJobb = arbeidsforhold(1.januar til sisteDagFørJobbskifte, orgnummer = "111111111")
        val nyJobb = arbeidsforhold(førsteDagEtterJobbskifte til 31.januar, orgnummer = "222222222")

        val kodeverkkode = vurderArbeidstaker(1.februar, gammelJobb, nyJobb)
        assertEquals(OPPTJENING_MINST_4_UKER, kodeverkkode)
    }

    // Bare den sammenhengende perioden fram til skjæringstidspunktet teller;
    // dager fra et tidligere, avbrutt arbeidsforhold legges ikke til
    @Test
    fun `opphold i arbeidsforhold nullstiller opptjeningen`() {
        val gammelJobb = arbeidsforhold(1.januar til 31.januar, orgnummer = "111111111")
        val nyJobb = arbeidsforhold(10.februar til 28.februar, orgnummer = "222222222")

        val kodeverkkode = vurderArbeidstaker(1.mars, gammelJobb, nyJobb)
        assertEquals(IKKE_OPPTJENING_ARBEID_ELLER_YTELSE, kodeverkkode)
    }

    // To samtidige arbeidsforhold gir ikke dobbelt opptjening
    @Test
    fun `overlappende arbeidsforhold teller ikke dobbelt`() {
        val enJobb = arbeidsforhold(15.januar til 31.januar, orgnummer = "111111111")
        val annenJobb = arbeidsforhold(20.januar til 31.januar, orgnummer = "222222222")

        // 17 + 12 dager ville vært nok hvis dagene ble summert, men periodene overlapper
        val kodeverkkode = vurderArbeidstaker(1.februar, enJobb, annenJobb)
        assertEquals(IKKE_OPPTJENING_ARBEID_ELLER_YTELSE, kodeverkkode)
    }

    @Test
    fun `selvstendig næringsdrivende har alltid oppfylt opptjening`() {
        val kodeverkkode = nyAutomatiskVurdering(1.februar, ForSelvstendigNæringsdrivende).kodeverkkode
        assertEquals(OPPTJENING_MINST_4_UKER, kodeverkkode)
    }

    @Test
    fun `nyAutomatiskVurdering beholder opplysningene den ble opprettet med`() {
        val grunnlag = ForArbeidstaker(listOf(arbeidsforhold(1.januar til 31.januar)))

        val vurdering = AutomatiskVurdering.nyAutomatiskVurdering(
            fødselsnummer = FØDSELSNUMMER,
            skjæringstidspunkt = 1.februar,
            versjonAvKildekode = VERSJON_AV_KILDEKODE,
        ).also { it.fullfør(grunnlag) }

        assertEquals(FØDSELSNUMMER, vurdering.fødselsnummer)
        assertEquals(1.februar, vurdering.skjæringstidspunkt)
        assertEquals(VERSJON_AV_KILDEKODE, vurdering.versjonAvKildekode)
        assertSame(grunnlag, vurdering.grunnlagForAutomatiskVurdering)
    }

    // nyAutomatiskVurdering utleder kodeverkkoden selv, mens fraLagring gjenbruker
    // den lagrede koden uten å vurdere på nytt
    @Test
    fun `nyAutomatiskVurdering utleder kodeverkkode, fraLagring gjenbruker den lagrede`() {
        val grunnlag = ForArbeidstaker(listOf(arbeidsforhold(1.januar til 31.januar)))

        val nyVurdering = nyAutomatiskVurdering(1.februar, grunnlag)
        assertEquals(OPPTJENING_MINST_4_UKER, nyVurdering.kodeverkkode)

        val lagretVurdering = AutomatiskVurdering.fraLagring(
            id = ID,
            fødselsnummer = FØDSELSNUMMER,
            skjæringstidspunkt = 1.februar,
            versjonAvKildekode = VERSJON_AV_KILDEKODE,
            grunnlagForAutomatiskVurdering = grunnlag,
            kodeverkkode = IKKE_OPPTJENING_ARBEID_ELLER_YTELSE,
            erKomplett = true
        )
        assertEquals(IKKE_OPPTJENING_ARBEID_ELLER_YTELSE, lagretVurdering.kodeverkkode)
        assertNotEquals(nyVurdering.kodeverkkode, lagretVurdering.kodeverkkode)
    }

    // ---------------------------------------------------------------------
    // Scenarioer overført fra OpptjeningTest i sykepenger-model
    // ---------------------------------------------------------------------

    // Fra `Én dags opptjening oppfyller ikke krav til opptjening`
    @Test
    fun `én dags opptjening er ikke oppfylt`() {
        val løpende = Arbeidsforhold(orgnummer = ORGNUMMER, ansattFom = 1.januar, ansattTom = null, type = ORDINÆRT)
        val kodeverkkode = vurderArbeidstaker(2.januar, løpende)
        assertEquals(IKKE_OPPTJENING_ARBEID_ELLER_YTELSE, kodeverkkode)
    }

    // Fra `to tilstøtende arbeidsforhold` – to perioder hos samme arbeidsgiver
    // som ligger kant i kant skal telle som sammenhengende opptjening
    @Test
    fun `to tilstøtende arbeidsforhold hos samme arbeidsgiver slås sammen`() {
        val første = arbeidsforhold(1.januar til 10.januar)
        val andre = Arbeidsforhold(orgnummer = ORGNUMMER, ansattFom = 11.januar, ansattTom = null, type = ORDINÆRT)

        val kodeverkkode = vurderArbeidstaker(29.januar, første, andre)
        assertEquals(OPPTJENING_MINST_4_UKER, kodeverkkode)
    }

    // Fra `Opptjening kobler sammen gap selvom rekkefølgen ikke er kronologisk`
    @Test
    fun `arbeidsforhold slås sammen selv om rekkefølgen ikke er kronologisk`() {
        val først = arbeidsforhold(1.januar til 10.januar)
        val sist = Arbeidsforhold(orgnummer = ORGNUMMER, ansattFom = 15.januar, ansattTom = null, type = ORDINÆRT)
        val imellom = arbeidsforhold(11.januar til 14.januar)

        val kodeverkkode = vurderArbeidstaker(29.januar, først, sist, imellom)
        assertEquals(OPPTJENING_MINST_4_UKER, kodeverkkode)
    }

    // Fra `slutter på lørdag, starter på mandag` – lørdag regnes som rett før påfølgende mandag
    @Test
    fun `arbeidsforhold som slutter lørdag og starter mandag slås sammen`() {
        val gammelJobb = arbeidsforhold(
            (søndag den 1.oktober(2017)) til (lørdag den 30.april(2022)),
            orgnummer = "111111111"
        )
        val nyJobb = Arbeidsforhold(
            orgnummer = "222222222",
            ansattFom = mandag den 2.mai(2022),
            ansattTom = null,
            type = ORDINÆRT
        )

        val kodeverkkode = vurderArbeidstaker(2.mai(2022), gammelJobb, nyJobb)
        assertEquals(OPPTJENING_MINST_4_UKER, kodeverkkode)
    }

    // Fra `slutter på fredag, starter på mandag` – fredag regnes som rett før påfølgende mandag,
    // akkurat som lørdag; lang historikk hos gammel arbeidsgiver teller med
    @Test
    fun `arbeidsforhold som slutter fredag og starter mandag slås sammen`() {
        val gammelJobb = arbeidsforhold(
            (søndag den 1.oktober(2017)) til (fredag den 29.april(2022)),
            orgnummer = "111111111"
        )
        val nyJobb = Arbeidsforhold(
            orgnummer = "222222222",
            ansattFom = mandag den 2.mai(2022),
            ansattTom = null,
            type = ORDINÆRT
        )

        val kodeverkkode = vurderArbeidstaker(tirsdag den 3.mai(2022), gammelJobb, nyJobb)
        assertEquals(OPPTJENING_MINST_4_UKER, kodeverkkode)
    }

    // Fra `slutter på torsdag, starter på mandag` – torsdag er ikke rett før mandag,
    // så oppholdet bryter opptjeningen selv om det bare er snakk om noen få dager
    @Test
    fun `arbeidsforhold som slutter torsdag og starter mandag slås ikke sammen`() {
        val gammelJobb = arbeidsforhold(
            (søndag den 1.oktober(2017)) til (torsdag den 28.april(2022)),
            orgnummer = "111111111"
        )
        val nyJobb = Arbeidsforhold(
            orgnummer = "222222222",
            ansattFom = mandag den 2.mai(2022),
            ansattTom = null,
            type = ORDINÆRT
        )

        val kodeverkkode = vurderArbeidstaker(tirsdag den 3.mai(2022), gammelJobb, nyJobb)
        assertEquals(IKKE_OPPTJENING_ARBEID_ELLER_YTELSE, kodeverkkode)
    }

    // Fra subsumsjonstestene for § 8-2 ledd 1 – et arbeidsforhold som fortsatt løper
    // etter skjæringstidspunktet skal avkortes, slik at bare dagene før teller
    @Test
    fun `arbeidsforhold som varer forbi skjæringstidspunktet avkortes`() {
        // 4.desember til 31.desember er 28 dager
        val nøyaktigNok = arbeidsforhold(4.desember(2017) til 31.januar)
        assertEquals(OPPTJENING_MINST_4_UKER, vurderArbeidstaker(1.januar, nøyaktigNok))

        // 5.desember til 31.desember er 27 dager
        val énForLite = arbeidsforhold(5.desember(2017) til 31.januar)
        assertEquals(IKKE_OPPTJENING_ARBEID_ELLER_YTELSE, vurderArbeidstaker(1.januar, énForLite))
    }

    private companion object {
        const val FØDSELSNUMMER = "12029240045"
        const val VERSJON_AV_KILDEKODE = "en-versjon-av-kildekode"
        const val ORGNUMMER = "987654321"
        val ID = UUID.randomUUID()

        fun arbeidsforhold(ansettelseperiode: Periode, orgnummer: String = ORGNUMMER) =
            Arbeidsforhold(orgnummer = orgnummer, ansettelseperiode = ansettelseperiode, type = ORDINÆRT)

        fun nyAutomatiskVurdering(
            skjæringstidspunkt: LocalDate,
            grunnlag: OpptjeningsgrunnlagForAutomatiskVurdering
        ) = AutomatiskVurdering.nyAutomatiskVurdering(
            fødselsnummer = FØDSELSNUMMER,
            skjæringstidspunkt = skjæringstidspunkt,
            versjonAvKildekode = VERSJON_AV_KILDEKODE,
        ).also { it.fullfør(grunnlag) }

        fun vurderArbeidstaker(skjæringstidspunkt: LocalDate, vararg arbeidsforhold: Arbeidsforhold) =
            nyAutomatiskVurdering(skjæringstidspunkt, ForArbeidstaker(arbeidsforhold.toList())).kodeverkkode
    }
}
