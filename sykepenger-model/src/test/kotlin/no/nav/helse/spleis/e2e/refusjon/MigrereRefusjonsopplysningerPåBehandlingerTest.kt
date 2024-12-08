package no.nav.helse.spleis.e2e.refusjon

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Toggle.Companion.LagreRefusjonsopplysningerPåBehandling
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.UgyldigeSituasjonerObservatør.Companion.assertUgyldigSituasjon
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class MigrereRefusjonsopplysningerPåBehandlingerTest : AbstractDslTest() {

    private lateinit var forrigeRefusjonstidslinje: Beløpstidslinje
    private val meldingsreferanseId1 = UUID.randomUUID()
    private val mottatt1 = LocalDate.EPOCH.atStartOfDay()
    private val meldingsreferanseId2 = UUID.randomUUID()
    private val mottatt2 = mottatt1.plusYears(1)

    private fun tillatUgyldigSituasjon(block: () -> Unit) {
        if (LagreRefusjonsopplysningerPåBehandling.enabled) return block()
        assertUgyldigSituasjon("") { block() }
    }

    private fun setup1og2() {
        håndterSøknad(januar)
        tillatUgyldigSituasjon {  håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT, opphørsdato = 25.januar),
            beregnetInntekt = INNTEKT,
            id = meldingsreferanseId1,
            mottatt = mottatt1
        ) }
        tillatUgyldigSituasjon {  håndterVilkårsgrunnlag(1.vedtaksperiode) }
        tillatUgyldigSituasjon {  håndterYtelser(1.vedtaksperiode) }
    }

    @Test
    @Order(1)
    fun `Vedtaksperiode med én beregnet endring - med toggle`() = LagreRefusjonsopplysningerPåBehandling.enable {
        a1 {
            setup1og2()
            forrigeRefusjonstidslinje = inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje
        }
    }

    @Test
    @Order(2)
    @Disabled("Midertidig deaktivert ifbm. overgang til nye refusjonsopplysninger")
    fun `Vedtaksperiode med én beregnet endring - uten toggle`() = LagreRefusjonsopplysningerPåBehandling.disable {
        a1 {
            setup1og2()
            migrerRefusjonsopplysningerPåBehandlinger()
            assertEquals(forrigeRefusjonstidslinje, inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje)
        }
    }

    private fun setup3og4() {
        a1 {
            håndterSøknad(januar)
            tillatUgyldigSituasjon { håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT, opphørsdato = 25.januar),
                beregnetInntekt = INNTEKT,
                id = meldingsreferanseId1,
                mottatt = mottatt1
            ) }
            tillatUgyldigSituasjon { håndterVilkårsgrunnlag(1.vedtaksperiode) }
            tillatUgyldigSituasjon { håndterYtelser(1.vedtaksperiode) }
            tillatUgyldigSituasjon { håndterSimulering(1.vedtaksperiode) }
            tillatUgyldigSituasjon { håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                overstyringer = listOf(
                    OverstyrtArbeidsgiveropplysning(
                        orgnummer = a1,
                        inntekt = INNTEKT,
                        forklaring = "foo",
                        subsumsjon = null,
                        refusjonsopplysninger = listOf(Triple(1.januar, null, INNTEKT * 1.25))
                    )
                ),
                hendelseId = meldingsreferanseId2,
                tidsstempel = mottatt2
            ) }
            tillatUgyldigSituasjon { håndterYtelser(1.vedtaksperiode) }
        }
    }

    private val TestArbeidsgiverInspektør.refusjonstidslinjeFraFørsteBeregnedeEndring: Beløpstidslinje
        get() {
            val endringer = vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().endringer
            check(endringer.filter { it.grunnlagsdata != null }.size == 2)
            return endringer.first { it.grunnlagsdata != null }.refusjonstidslinje
        }

    @Test
    @Order(3)
    fun `Vedtaksperiode med flere beregnede endringer - med toggle`() = LagreRefusjonsopplysningerPåBehandling.enable {
        a1 {
            setup3og4()
            forrigeRefusjonstidslinje = inspektør.refusjonstidslinjeFraFørsteBeregnedeEndring
        }
    }

    @Test
    @Order(4)
    @Disabled("Midertidig deaktivert ifbm. overgang til nye refusjonsopplysninger")
    fun `Vedtaksperiode med flere beregnede endringer - uten toggle`() = LagreRefusjonsopplysningerPåBehandling.disable {
        a1 {
            setup3og4()
            migrerRefusjonsopplysningerPåBehandlinger()
            assertEquals(forrigeRefusjonstidslinje, inspektør.refusjonstidslinjeFraFørsteBeregnedeEndring)
        }
    }

    private fun setup13og14og15() {
        a1 {
            nyPeriode(januar)
            nyPeriode(februar)
        }
        a2 {
            nyPeriode(januar)
            nyPeriode(februar)
        }
        a1 {
            tillatUgyldigSituasjon {
                håndterInntektsmelding(listOf(1.januar til 16.januar), refusjon = Inntektsmelding.Refusjon(INNTEKT, 31.januar))
            }
        }
    }

    @Test
    @Order(15)
    fun `flere arbeidsgivere med bare en inntektsmelding - med toggle alltid på`() = LagreRefusjonsopplysningerPåBehandling.enable {
        a1 {
            setup13og14og15()
            forrigeRefusjonstidslinje = inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje + inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje
            forrigeRefusjonstidslinje.assertBeløpstidslinje(1.januar til 31.januar to INNTEKT)
            forrigeRefusjonstidslinje.assertBeløpstidslinje(1.februar til 28.februar to INGEN)
            migrerRefusjonsopplysningerPåBehandlinger()
            val faktiskBeløpstidslinje = inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje + inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje
            faktiskBeløpstidslinje.assertBeløpstidslinje(1.januar til 31.januar to INNTEKT)
            faktiskBeløpstidslinje.assertBeløpstidslinje(1.februar til 28.februar to INGEN)
        }
    }

    @Test
    @Order(16)
    fun `igangsatt revurdering pga refusjon - toggle alltid på`() = LagreRefusjonsopplysningerPåBehandling.enable {
        a1 {
            håndterSøknad(januar)
            tillatUgyldigSituasjon { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
            tillatUgyldigSituasjon { håndterVilkårsgrunnlag(1.vedtaksperiode) }
            tillatUgyldigSituasjon { håndterYtelser(1.vedtaksperiode) }
            tillatUgyldigSituasjon { håndterSimulering(1.vedtaksperiode) }
            tillatUgyldigSituasjon { håndterUtbetalingsgodkjenning(1.vedtaksperiode) }
            tillatUgyldigSituasjon { håndterUtbetalt() }

            tillatUgyldigSituasjon { håndterSøknad(februar) }
            tillatUgyldigSituasjon { håndterYtelser(2.vedtaksperiode) }
            tillatUgyldigSituasjon { håndterSimulering(2.vedtaksperiode) }
            tillatUgyldigSituasjon { håndterUtbetalingsgodkjenning(2.vedtaksperiode) }
            tillatUgyldigSituasjon { håndterUtbetalt() }

            tillatUgyldigSituasjon { håndterInntektsmelding(listOf(1.januar til 16.januar), refusjon = Inntektsmelding.Refusjon(INNTEKT/2, null)) }

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)

            val refusjonFørMigrering = inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje + inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje
            refusjonFørMigrering.assertBeløpstidslinje(1.januar til 28.februar to INNTEKT / 2)

            migrerRefusjonsopplysningerPåBehandlinger()

            val refusjonEtterMigrering = inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje + inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje
            refusjonEtterMigrering.assertBeløpstidslinje(1.januar til 28.februar to INNTEKT / 2)
        }
    }

    private fun Beløpstidslinje.assertBeløpstidslinje(vararg forventetBeløp: Pair<Periode, Inntekt>) {
        forventetBeløp.forEach { (periode, inntekt) ->
            val subset = subset(periode)
            assertEquals(periode, subset.perioderMedBeløp.singleOrNull()) {"Vi har ikke beløp i hele $periode"}
            assertTrue(subset.all { it.beløp == inntekt }) {"Vi forventet at inntekten skulle være ${inntekt.dagligInt} i $periode, men var: ${subset.map { it.beløp.dagligInt }}"}
        }
    }
}