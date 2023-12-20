package no.nav.helse.spleis.e2e.overstyring

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.TestPerson.Companion.AKTØRID
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.TestPerson.Companion.UNG_PERSON_FNR_2018
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated

@Isolated
@EnableToggle(Toggle.AvviksvurderingFlyttet::class)
internal class SkjønnsmessigFastsettelseVersjon2Test: AbstractDslTest() {

    @Test
    fun `Saker med "avvik" går til godkjenning uten varsel`() {
        System.setProperty("AVVIKSAKER_FLYTTET", "true")
        a1 {
            val søknadId = UUID.randomUUID()
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), søknadId = søknadId)
            val innteksmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT * 2)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertIngenVarsler()
            val håndterSkjønnsmessigFastsettelseId = UUID.randomUUID()
            håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT * 3)), meldingsreferanseId = håndterSkjønnsmessigFastsettelseId)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            val tidspunkt = LocalDateTime.now()
            val utkastTilVedtak = observatør.vedtakFattetEventer[1.vedtaksperiode]?.single()?.copy(vedtakFattetTidspunkt = tidspunkt)
            assertEquals(PersonObserver.VedtakFattetEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = AKTØRID,
                organisasjonsnummer = a1,
                vedtaksperiodeId = 1.vedtaksperiode,
                periode = 1.januar til 31.januar,
                hendelseIder = setOf(søknadId, innteksmeldingId, håndterSkjønnsmessigFastsettelseId),
                skjæringstidspunkt = 1.januar,
                sykepengegrunnlag = 561804.0,
                beregningsgrunnlag = 1116000.0,
                omregnetÅrsinntektPerArbeidsgiver = mapOf(a1 to 1116000.0),
                inntekt = 93000.0,
                utbetalingId = inspektør.utbetalinger.last().id,
                sykepengegrunnlagsbegrensning = "ER_6G_BEGRENSET",
                vedtakFattetTidspunkt = tidspunkt,
                tags = emptySet(),
                sykepengegrunnlagsfakta = PersonObserver.VedtakFattetEvent.FastsattEtterSkjønn(
                    omregnetÅrsinntekt = 744000.0,
                    innrapportertÅrsinntekt = 0.0,
                    skjønnsfastsatt = 1116000.0,
                    avviksprosent = 0.0,
                    `6G` = 561804.0,
                    tags = setOf(PersonObserver.VedtakFattetEvent.Tag.`6GBegrenset`),
                    arbeidsgivere = listOf(PersonObserver.VedtakFattetEvent.FastsattEtterSkjønn.Arbeidsgiver(a1, 744000.0, 1116000.0)),
                )

            ), utkastTilVedtak)
            System.setProperty("AVVIKSAKER_FLYTTET", "false")
        }
    }
    @Test
    fun `Skal ikke avviksvurdere på nytt etter overstyring der kun refusjon har blitt endret`() {
        System.setProperty("AVVIKSAKER_FLYTTET", "true")
        a1 {
            val imInntekt = INNTEKT * 2
            val sammenligningsgrunnlag = INNTEKT
            val skjønnsinntekt = INNTEKT*3

            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = imInntekt)
            håndterVilkårsgrunnlag(1.vedtaksperiode, inntekt = sammenligningsgrunnlag)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, skjønnsinntekt)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterOverstyrArbeidsgiveropplysninger(1.januar, overstyringer = listOf(
                OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = imInntekt, forklaring = "forklaring", refusjonsopplysninger = listOf(
                    Triple(1.januar, null, imInntekt*0.8)))
            ))
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            val utkastTilVedtak = observatør.vedtakFattetEventer[1.vedtaksperiode]?.last()

            assertForventetFeil(
                forklaring = "Skal fortsatt være skjønnsmessig vurdert når vi kun har overstyrt refusjon",
                nå = {
                    assertFalse(utkastTilVedtak?.sykepengegrunnlagsfakta is PersonObserver.VedtakFattetEvent.FastsattEtterSkjønn)
                },
                ønsket = {
                    assertTrue(utkastTilVedtak?.sykepengegrunnlagsfakta is PersonObserver.VedtakFattetEvent.FastsattEtterSkjønn)
                }
            )

            System.setProperty("AVVIKSAKER_FLYTTET", "false")
        }
    }

}