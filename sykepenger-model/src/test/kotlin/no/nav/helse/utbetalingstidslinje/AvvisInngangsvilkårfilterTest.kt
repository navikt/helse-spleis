package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.etterlevelse.BehandlingSubsumsjonslogg
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.februar
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.ArbeidstakerFaktaavklartInntekt
import no.nav.helse.person.inntekt.Arbeidstakerinntektskilde
import no.nav.helse.person.inntekt.Inntektsdata
import no.nav.helse.person.inntekt.Inntektsgrunnlag
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Minsteinntektsvurdering.Companion.lagMinsteinntektsvurdering
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.periode
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AvvisInngangsvilkårfilterTest {
    private val subsumsjonslogg = BehandlingSubsumsjonslogg(EmptyLog, "fnr", "orgnr", UUID.randomUUID(), UUID.randomUUID())

    private companion object {
        private val ALDER = 12.februar(1992).alder
    }

    @Test
    fun `avviser ikke dager dersom vurdert ok`() {
        val tidslinjer = listOf(tidslinjeOf(1.NAV))
        assertEquals(0, avvisteDager(tidslinjer).size)
    }

    @Test
    fun `avviser dager uten opptjening`() {
        val tidslinjer = listOf(tidslinjeOf(1.NAV))
        val avvisteDager = avvisteDager(tidslinjer, manglerOpptjening = true)
        assertEquals(1, avvisteDager.size)
        assertEquals(listOf(Begrunnelse.ManglerOpptjening), avvisteDager.single().begrunnelser)
    }

    @Test
    fun `avviser dager uten tilstrekkelig inntekt over 67 år`() {
        val tidslinjer = listOf(tidslinjeOf(2.NAV, startDato = ALDER.redusertYtelseAlder))
        val avvisteDager = avvisteDager(tidslinjer, manglerTilstrekkeligInntekt = true)
        assertEquals(2, avvisteDager.size)
        assertEquals(listOf(Begrunnelse.MinimumInntekt), avvisteDager.first().begrunnelser)
        assertEquals(listOf(Begrunnelse.MinimumInntektOver67), avvisteDager.last().begrunnelser)
    }

    @Test
    fun `avviser med flere begrunnelser`() {
        val tidslinjer = listOf(tidslinjeOf(1.NAV))
        val avvisteDager = avvisteDager(tidslinjer, manglerOpptjening = true, manglerTilstrekkeligInntekt = true)
        assertEquals(1, avvisteDager.size)
        assertEquals(listOf(Begrunnelse.MinimumInntekt, Begrunnelse.ManglerOpptjening), avvisteDager.single().begrunnelser)
    }

    private fun avvisteDager(
        tidslinjer: List<Utbetalingstidslinje>,
        manglerOpptjening: Boolean = false,
        manglerMedlemskap: Boolean = false,
        manglerTilstrekkeligInntekt: Boolean = false
    ): List<Utbetalingsdag.AvvistDag> {
        val skjæringstidspunkt = 1.januar
        val inntektsgrunnlag = Inntektsgrunnlag(
            arbeidsgiverInntektsopplysninger = listOf(
                ArbeidsgiverInntektsopplysning(
                    orgnummer = "a1",
                    faktaavklartInntekt = ArbeidstakerFaktaavklartInntekt(
                        id = UUID.randomUUID(),
                        inntektsdata = Inntektsdata(
                            hendelseId = MeldingsreferanseId(UUID.randomUUID()),
                            dato = skjæringstidspunkt,
                            beløp = if (manglerTilstrekkeligInntekt) 1000.månedlig else 31_000.månedlig,
                            tidsstempel = LocalDateTime.now()
                        ),
                        inntektsopplysningskilde = Arbeidstakerinntektskilde.Arbeidsgiver
                    ),
                    korrigertInntekt = null,
                    skjønnsmessigFastsatt = null
                )
            ),
            selvstendigInntektsopplysning = null,
            deaktiverteArbeidsforhold = emptyList(),
            skjæringstidspunkt = skjæringstidspunkt,
            subsumsjonslogg = subsumsjonslogg,
            vurdertInfotrygd = false
        )
        val medlemskapstatus = if (manglerMedlemskap) Medlemskapsvurdering.Medlemskapstatus.Nei else Medlemskapsvurdering.Medlemskapstatus.Ja
        val redusertYtelseAlder: LocalDate = ALDER.redusertYtelseAlder
        val minsteinntektsvurdering = lagMinsteinntektsvurdering(skjæringstidspunkt, periode(tidslinjer)!!, inntektsgrunnlag.sykepengegrunnlag, redusertYtelseAlder)

        val filter = AvvisInngangsvilkårfilter(
            periodeTilFylte67UnderMinsteinntekt = minsteinntektsvurdering.periodeTilFylte67UnderMinsteinntekt,
            periodeEtterFylte67UnderMinsteinntekt = minsteinntektsvurdering.periodeEtterFylte67UnderMinsteinntekt,
            medlemskapstatus = medlemskapstatus,
            harOpptjening = !manglerOpptjening
        )
        val arbeidsgivere = tidslinjer.mapIndexed { index, it ->
            Arbeidsgiverberegning(
                yrkesaktivitet = Behandlingsporing.Yrkesaktivitet.Arbeidstaker("a${index+1}"),
                vedtaksperioder = listOf(
                    Vedtaksperiodeberegning(
                        vedtaksperiodeId = UUID.randomUUID(),
                        utbetalingstidslinje = it,
                        inntekterForBeregning = emptyMap()
                    )
                ),
                ghostOgAndreInntektskilder = emptyList()
            )
        }
        val avviste = filter.filter(arbeidsgivere, periode(tidslinjer)!!)
        return avviste.flatMap {
            it.vedtaksperioder.single().utbetalingstidslinje.inspektør.avvistedager
        }
    }
}
