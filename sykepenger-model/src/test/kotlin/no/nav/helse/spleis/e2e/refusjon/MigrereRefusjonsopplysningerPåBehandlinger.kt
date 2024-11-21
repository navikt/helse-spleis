package no.nav.helse.spleis.e2e.refusjon

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.TestPerson
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.januar
import no.nav.helse.person.beløp.Beløpstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class MigrereRefusjonsopplysningerPåBehandlinger : AbstractDslTest() {

    private lateinit var forrigeRefusjonstidslinje: Beløpstidslinje
    private val meldingsreferanseId1 = UUID.randomUUID()
    private val mottatt1 = LocalDate.EPOCH.atStartOfDay()
    private val meldingsreferanseId2 = UUID.randomUUID()
    private val mottatt2 = mottatt1.plusYears(1)

    private fun setup1og2() {
        håndterSøknad(januar)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(beløp = TestPerson.INNTEKT, opphørsdato = 25.januar),
            beregnetInntekt = TestPerson.INNTEKT,
            id = meldingsreferanseId1,
            mottatt = mottatt1
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
    }

    @Test
    @Order(1)
    fun `Vedtaksperiode med én beregnet endring - med toggle`() = Toggle.LagreRefusjonsopplysningerPåBehandling.enable {
        a1 {
            setup1og2()
            forrigeRefusjonstidslinje = inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje
        }
    }

    @Test
    @Order(2)
    fun `Vedtaksperiode med én beregnet endring - uten toggle`() = Toggle.LagreRefusjonsopplysningerPåBehandling.disable {
        a1 {
            setup1og2()
            migrerRefusjonsopplysningerPåBehandlinger()
            assertEquals(forrigeRefusjonstidslinje, inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje)
        }
    }

    private fun setup3og4() {
        a1 {
            håndterSøknad(januar)
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT, opphørsdato = 25.januar),
                beregnetInntekt = TestPerson.INNTEKT,
                id = meldingsreferanseId1,
                mottatt = mottatt1
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterOverstyrArbeidsgiveropplysninger(
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
            )
            håndterYtelser(1.vedtaksperiode)
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
    fun `Vedtaksperiode med flere beregnede endringer - med toggle`() = Toggle.LagreRefusjonsopplysningerPåBehandling.enable {
        a1 {
            setup3og4()
            forrigeRefusjonstidslinje = inspektør.refusjonstidslinjeFraFørsteBeregnedeEndring
        }
    }

    @Test
    @Order(4)
    fun `Vedtaksperiode med flere beregnede endringer - uten toggle`() = Toggle.LagreRefusjonsopplysningerPåBehandling.disable {
        a1 {
            setup3og4()
            migrerRefusjonsopplysningerPåBehandlinger()
            assertForventetFeil(
                forklaring = "Har kun migrert inn siste endring",
                nå = { assertEquals(Beløpstidslinje(), inspektør.refusjonstidslinjeFraFørsteBeregnedeEndring) },
                ønsket = { assertEquals(forrigeRefusjonstidslinje, inspektør.refusjonstidslinjeFraFørsteBeregnedeEndring) }
            )
        }
    }

}