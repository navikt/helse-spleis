package no.nav.helse.spleis.e2e

import java.util.UUID
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.EventSubscription.UtkastTilVedtakEvent.FastsattEtterHovedregel
import no.nav.helse.person.EventSubscription.UtkastTilVedtakEvent.FastsattEtterSkjønn
import no.nav.helse.person.EventSubscription.UtkastTilVedtakEvent.Inntektskilde
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.somOrganisasjonsnummer
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AvsluttetMedVedtakTest : AbstractDslTest() {

    @Test
    fun `sender ikke vedtak fattet for perioder innenfor arbeidsgiverperioden`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar))
            håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertEquals(0, inspektør.antallUtbetalinger)
            assertEquals(0, observatør.utbetalingUtenUtbetalingEventer.size)
            assertEquals(0, observatør.utbetalingMedUtbetalingEventer.size)
            1.vedtaksperiode.assertIngenVedtakFattet()
            assertEquals(1.januar til 10.januar, 1.vedtaksperiode.avsluttetUtenVedtakEventer.single().periode)
        }
    }

    @Test
    fun `sender vedtak fattet for perioder utenfor arbeidsgiverperioden`() {
        a1 {

            nyttVedtak(januar, 100.prosent)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertEquals(1, inspektør.antallUtbetalinger)
            assertEquals(0, observatør.utbetalingUtenUtbetalingEventer.size)
            assertEquals(1, observatør.utbetalingMedUtbetalingEventer.size)
            assertEquals(1, observatør.avsluttetMedVedtakEvent.size)
            val event = observatør.avsluttetMedVedtakEvent.getValue(1.vedtaksperiode)
            assertEquals(inspektør.utbetaling(0).utbetalingId, event.utbetalingId)
            assertEquals(Utbetalingstatus.UTBETALT, inspektør.utbetaling(0).tilstand)
            val forventetSykepengegrunnlagsfakta = FastsattEtterHovedregel(
                omregnetÅrsinntekt = 372_000.0,
                sykepengegrunnlag = 372_000.0,
                `6G` = 561_804.0,
                arbeidsgivere = listOf(FastsattEtterHovedregel.Arbeidsgiver(a1, 372_000.0, Inntektskilde.Arbeidsgiver))
            )
            assertEquals(forventetSykepengegrunnlagsfakta, event.sykepengegrunnlagsfakta)
        }
    }

    @Test
    fun `sender vedtak fattet for perioder utenfor arbeidsgiverperioden med bare ferie`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar))
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), Ferie(17.januar, 20.januar))
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertEquals(0, inspektør.antallUtbetalinger)
            assertEquals(0, observatør.utbetalingUtenUtbetalingEventer.size)
            assertEquals(0, observatør.utbetalingMedUtbetalingEventer.size)
            assertEquals(0, observatør.avsluttetUtenVedtakEventer.size)
            1.vedtaksperiode.assertIngenVedtakFattet()
        }
    }

    @Test
    fun `sender vedtak fattet for forlengelseperioder utenfor arbeidsgiverperioden med bare ferie`() {
        a1 {

            nyttVedtak(1.januar til 20.januar, 100.prosent)
            håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar))
            håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent), Ferie(21.januar, 31.januar))
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            assertEquals(2, inspektør.antallUtbetalinger)
            assertEquals(1, observatør.utbetalingUtenUtbetalingEventer.size)
            assertEquals(1, observatør.utbetalingMedUtbetalingEventer.size)
            assertEquals(2, observatør.avsluttetMedVedtakEvent.size)
            val event = observatør.avsluttetMedVedtakEvent.getValue(2.vedtaksperiode)
            assertEquals(inspektør.utbetaling(1).utbetalingId, event.utbetalingId)
            assertEquals(Utbetalingstatus.GODKJENT_UTEN_UTBETALING, inspektør.utbetaling(1).tilstand)
            val forventetSykepengegrunnlagsfakta = FastsattEtterHovedregel(
                omregnetÅrsinntekt = 372_000.0,
                `6G` = 561804.0,
                sykepengegrunnlag = 372_000.0,
                arbeidsgivere = listOf(FastsattEtterHovedregel.Arbeidsgiver(a1, 372000.0, Inntektskilde.Arbeidsgiver))
            )
            assertEquals(forventetSykepengegrunnlagsfakta, event.sykepengegrunnlagsfakta)
        }
    }

    @Test
    fun `sender vedtak fattet ved fastsettelse etter hovedregel med flere arbeidsgivere`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020)))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020)))
        }
        a1 {
            håndterSøknad(1.januar(2020) til 31.januar(2020))
        }
        a2 {
            håndterSøknad(1.januar(2020) til 31.januar(2020))
        }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar(2020) til 16.januar(2020)),
                beregnetInntekt = INNTEKT,
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar(2020) til 16.januar(2020)),
                beregnetInntekt = INNTEKT,
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertEquals(2, observatør.avsluttetMedVedtakEvent.size)
        }

        val a1Sykepengegrunnlagsfakta = observatør.avsluttetMedVedtakEvent.values.first { it.yrkesaktivitetssporing.somOrganisasjonsnummer == a1 }.sykepengegrunnlagsfakta
        val a2Sykepengegrunnlagsfakta = observatør.avsluttetMedVedtakEvent.values.first { it.yrkesaktivitetssporing.somOrganisasjonsnummer == a2 }.sykepengegrunnlagsfakta
        assertEquals(a1Sykepengegrunnlagsfakta, a2Sykepengegrunnlagsfakta)

        val forventetSykepengegrunnlagsfakta = FastsattEtterHovedregel(
            omregnetÅrsinntekt = 744_000.0,
            `6G` = 599_148.0,
            sykepengegrunnlag = 599_148.0,
            arbeidsgivere = listOf(
                FastsattEtterHovedregel.Arbeidsgiver(a1, 372_000.0, Inntektskilde.Arbeidsgiver),
                FastsattEtterHovedregel.Arbeidsgiver(a2, 372_000.0, Inntektskilde.Arbeidsgiver),
            )
        )
        assertEquals(forventetSykepengegrunnlagsfakta, a1Sykepengegrunnlagsfakta)
    }

    @Test
    fun `sender vedtak fattet etter skjønnsmessig fastsettelse med flere arbeidsgivere`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020)))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar(2020), 31.januar(2020)))
        }
        a1 {
            håndterSøknad(1.januar(2020) til 31.januar(2020))
        }
        a2 {
            håndterSøknad(1.januar(2020) til 31.januar(2020))
        }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar(2020) til 16.januar(2020)),
                beregnetInntekt = 45000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar(2020) til 16.januar(2020)),
                beregnetInntekt = 44000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            håndterSkjønnsmessigFastsettelse(
                1.januar(2020),
                listOf(
                    OverstyrtArbeidsgiveropplysning(a1, 46000.månedlig),
                    OverstyrtArbeidsgiveropplysning(a2, 45000.månedlig)
                )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertEquals(2, observatør.avsluttetMedVedtakEvent.size)
        }
        val a1Sykepengegrunnlagsfakta = observatør.avsluttetMedVedtakEvent.values.first { it.yrkesaktivitetssporing.somOrganisasjonsnummer == a1 }.sykepengegrunnlagsfakta
        val a2Sykepengegrunnlagsfakta = observatør.avsluttetMedVedtakEvent.values.first { it.yrkesaktivitetssporing.somOrganisasjonsnummer == a2 }.sykepengegrunnlagsfakta
        assertEquals(a1Sykepengegrunnlagsfakta, a2Sykepengegrunnlagsfakta)

        val forventetSykepengegrunnlagsfakta = FastsattEtterSkjønn(
            omregnetÅrsinntekt = 1_068_000.0,
            `6G` = 599_148.0,
            sykepengegrunnlag = 599_148.0,
            arbeidsgivere = listOf(
                FastsattEtterSkjønn.Arbeidsgiver(a1, 540_000.0, 552_000.0, Inntektskilde.Saksbehandler),
                FastsattEtterSkjønn.Arbeidsgiver(a2, 528_000.0, 540_000.0, Inntektskilde.Saksbehandler),
            )
        )
        assertEquals(forventetSykepengegrunnlagsfakta, a1Sykepengegrunnlagsfakta)
    }

    @Test
    fun `sender ikke avsluttet uten vedtak fordi saksbehandler ikke overstyrer perioden inn i AvsluttetUtenUtbetaling`() {
        a1 {
            val søknadId = UUID.randomUUID()
            håndterSøknad(1.januar til 16.januar, søknadId = søknadId)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            val inntektsmeldingId = håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT, null, emptyList()),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "noe"
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            val liste = (1..16).map {
                ManuellOverskrivingDag(it.januar, Dagtype.Feriedag)
            }
            håndterOverstyrTidslinje(liste)
            assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            val utbetaling = inspektør.utbetaling(0)
            assertEquals(Utbetalingstatus.FORKASTET, utbetaling.tilstand)
            1.vedtaksperiode.assertIngenVedtakFattet()
            assertEquals(2, 1.vedtaksperiode.avsluttetUtenVedtakEventer.size)
            assertEquals(setOf(søknadId, inntektsmeldingId), 1.vedtaksperiode.avsluttetUtenVedtakEventer.last().hendelseIder)
        }
    }

    @Test
    fun `Periode med kun ferie etter kort gap etter kort auu tagges ikke med IngenNyArbeidsgiverperiode`() {
        a1 {

            håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar))
            håndterSøknad(1.januar til 10.januar)
            håndterSykmelding(Sykmeldingsperiode(15.januar, 31.januar))
            håndterSøknad(Sykdom(15.januar, 31.januar, 100.prosent), Ferie(15.januar, 31.januar))
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

            assertEquals(1, 1.vedtaksperiode.avsluttetUtenVedtakEventer.size)
            assertEquals(1, 2.vedtaksperiode.avsluttetUtenVedtakEventer.size)
            1.vedtaksperiode.assertIngenVedtakFattet()
            2.vedtaksperiode.assertIngenVedtakFattet()
        }
    }

    @Test
    fun `sender med tidligere dokumenter etter revurdering`() {
        a1 {

            nyttVedtak(januar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertEquals(1, observatør.avsluttetMedVedtakEventer.getValue(1.vedtaksperiode).size)
            val tidligereVedtak = observatør.avsluttetMedVedtakEvent.getValue(1.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(2, observatør.avsluttetMedVedtakEventer.getValue(1.vedtaksperiode).size)
            val nyttVedtak = observatør.avsluttetMedVedtakEvent.getValue(1.vedtaksperiode)
            assertEquals(tidligereVedtak.hendelseIder, nyttVedtak.hendelseIder)
        }
    }

    private val UUID.avsluttetUtenVedtakEventer get() = observatør.avsluttetUtenVedtakEventer.getValue(this)
    private fun UUID.assertIngenVedtakFattet() = assertEquals(emptyList<EventSubscription.AvsluttetMedVedtakEvent>(), observatør.avsluttetMedVedtakEventer[this] ?: emptyList<EventSubscription.AvsluttetMedVedtakEvent>())
}
