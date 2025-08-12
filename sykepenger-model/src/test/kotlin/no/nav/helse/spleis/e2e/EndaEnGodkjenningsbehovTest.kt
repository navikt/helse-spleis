package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.august
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.hentFeltFraBehov
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mars
import no.nav.helse.person.PersonObserver.UtkastTilVedtakEvent.Inntektskilde
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class EndaEnGodkjenningsbehovTest : AbstractEndToEndTest() {
    private fun IdInnhenter.sisteBehandlingId(orgnr: String) = inspektør(orgnr).vedtaksperioder(this).inspektør.behandlinger.last().id

    @Test
    fun `Arbeidsgiver ber om refusjon, men det blir avslått en dag etter opphør av refusjon`() {
        håndterSøknad(Sykdom(1.januar, 30.januar, 100.prosent), Sykdom(31.januar, 31.januar, 19.prosent))
        håndterArbeidsgiveropplysninger(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT,
            refusjon = Inntektsmelding.Refusjon(INNTEKT, 30.januar)
        )
        håndterVilkårsgrunnlag()
        håndterYtelser()
        håndterSimulering()

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertGodkjenningsbehov(tags = setOf("Førstegangsbehandling", "DelvisInnvilget", "Arbeidsgiverutbetaling", "ArbeidsgiverØnskerRefusjon", "EnArbeidsgiver"))
        assertVarsel(Varselkode.RV_VV_4, 1.vedtaksperiode.filter())
    }

    @Test
    fun `Arbeidsgiver ønsker refusjon, men perioden ble avslått`() {
        håndterSøknad(Sykdom(1.januar, 31.januar, 19.prosent))
        håndterArbeidsgiveropplysninger(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, arbeidsgiverperioder = listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT, refusjon = Inntektsmelding.Refusjon(INNTEKT, null))
        håndterVilkårsgrunnlag()
        håndterYtelser()

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertGodkjenningsbehov(tags = setOf("Førstegangsbehandling", "Avslag", "IngenUtbetaling", "ArbeidsgiverØnskerRefusjon", "EnArbeidsgiver"))
        assertVarsel(Varselkode.RV_VV_4, 1.vedtaksperiode.filter())
    }

    @Test
    fun `Arbeidsgiver ønsker refusjon for én dag i perioden`() {
        tilGodkjenning(januar, refusjon = Inntektsmelding.Refusjon(INGEN, null, listOf(Inntektsmelding.Refusjon.EndringIRefusjon(1.daglig, 31.januar))), organisasjonsnummere = arrayOf(a1))
        assertGodkjenningsbehov(tags = setOf("Førstegangsbehandling", "Innvilget", "Arbeidsgiverutbetaling", "Personutbetaling", "ArbeidsgiverØnskerRefusjon", "EnArbeidsgiver"))
    }

    @Test
    fun `Arbeidsgiver ønsker ikke refusjon i forlengelsen`() {
        tilGodkjenning(januar, refusjon = Inntektsmelding.Refusjon(INNTEKT, 31.januar), organisasjonsnummere = arrayOf(a1))
        assertGodkjenningsbehov(tags = setOf("Førstegangsbehandling", "Innvilget", "Arbeidsgiverutbetaling", "ArbeidsgiverØnskerRefusjon", "EnArbeidsgiver"))

        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()
        forlengelseTilGodkjenning(februar, organisasjonsnumre = arrayOf(a1))

        assertGodkjenningsbehov(
            tags = setOf("Forlengelse", "Innvilget", "Personutbetaling", "EnArbeidsgiver"),
            periodeFom = 1.februar,
            periodeTom = 28.februar,
            vedtaksperiodeId = 2.vedtaksperiode.id(a1),
            behandlingId = 2.vedtaksperiode.sisteBehandlingId(a1),
            periodeType = "FORLENGELSE",
            førstegangsbehandling = false,
            perioderMedSammeSkjæringstidspunkt = listOf(
                mapOf("vedtaksperiodeId" to 1.vedtaksperiode.id(a1).toString(), "behandlingId" to 1.vedtaksperiode.sisteBehandlingId(a1).toString(), "fom" to 1.januar.toString(), "tom" to 31.januar.toString()),
                mapOf("vedtaksperiodeId" to 2.vedtaksperiode.id(a1).toString(), "behandlingId" to 2.vedtaksperiode.sisteBehandlingId(a1).toString(), "fom" to 1.februar.toString(), "tom" to 28.februar.toString()),
            )
        )
    }

    @Test
    fun forlengelse() {
        nyeVedtak(januar, a1, a2, inntekt = 20000.månedlig)
        forlengelseTilGodkjenning(1.februar til 10.februar, a1, a2)
        assertGodkjenningsbehov(
            vedtaksperiodeId = 2.vedtaksperiode.id(a1),
            periodeFom = 1.februar,
            periodeTom = 10.februar,
            behandlingId = 2.vedtaksperiode.sisteBehandlingId(a1),
            tags = setOf("Forlengelse", "Innvilget", "Arbeidsgiverutbetaling", "FlereArbeidsgivere", "ArbeidsgiverØnskerRefusjon"),
            periodeType = "FORLENGELSE",
            førstegangsbehandling = false,
            inntektskilde = "FLERE_ARBEIDSGIVERE",
            orgnummere = setOf(a1, a2),
            omregnedeÅrsinntekter = listOf(
                mapOf("organisasjonsnummer" to a1, "beløp" to 240000.0),
                mapOf("organisasjonsnummer" to a2, "beløp" to 240000.0)
            ),
            perioderMedSammeSkjæringstidspunkt = listOf(
                mapOf("vedtaksperiodeId" to 1.vedtaksperiode.id(a1).toString(), "behandlingId" to 1.vedtaksperiode.sisteBehandlingId(a1).toString(), "fom" to 1.januar.toString(), "tom" to 31.januar.toString()),
                mapOf("vedtaksperiodeId" to 1.vedtaksperiode.id(a2).toString(), "behandlingId" to 1.vedtaksperiode.sisteBehandlingId(a2).toString(), "fom" to 1.januar.toString(), "tom" to 31.januar.toString()),
                mapOf("vedtaksperiodeId" to 2.vedtaksperiode.id(a1).toString(), "behandlingId" to 2.vedtaksperiode.sisteBehandlingId(a1).toString(), "fom" to 1.februar.toString(), "tom" to 10.februar.toString()),
                mapOf("vedtaksperiodeId" to 2.vedtaksperiode.id(a2).toString(), "behandlingId" to 2.vedtaksperiode.sisteBehandlingId(a2).toString(), "fom" to 1.februar.toString(), "tom" to 10.februar.toString()),
            ),
            sykepengegrunnlagsfakta = mapOf(
                "omregnetÅrsinntektTotalt" to 480_000.0,
                "sykepengegrunnlag" to 480_000.0,
                "6G" to 561_804.0,
                "fastsatt" to "EtterHovedregel",
                "arbeidsgivere" to listOf(
                    mapOf(
                        "arbeidsgiver" to a1,
                        "omregnetÅrsinntekt" to 240000.0,
                        "inntektskilde" to Inntektskilde.Arbeidsgiver
                    ), mapOf(
                    "arbeidsgiver" to a2,
                    "omregnetÅrsinntekt" to 240000.0,
                    "inntektskilde" to Inntektskilde.Arbeidsgiver
                )
                )
            )
        )
    }

    @Test
    fun arbeidsgiverutbetaling() {
        tilGodkjenning(januar, a1)
        assertGodkjenningsbehov(
            tags = setOf("Førstegangsbehandling", "Innvilget", "Arbeidsgiverutbetaling", "EnArbeidsgiver", "ArbeidsgiverØnskerRefusjon"),
            omregnedeÅrsinntekter = listOf(mapOf("organisasjonsnummer" to a1, "beløp" to INNTEKT.årlig)),
            sykepengegrunnlagsfakta = mapOf(
                "omregnetÅrsinntektTotalt" to INNTEKT.årlig,
                "sykepengegrunnlag" to INNTEKT.årlig,
                "6G" to 561_804.0,
                "fastsatt" to "EtterHovedregel",
                "arbeidsgivere" to listOf(
                    mapOf(
                        "arbeidsgiver" to a1,
                        "omregnetÅrsinntekt" to INNTEKT.årlig,
                        "inntektskilde" to Inntektskilde.Arbeidsgiver
                    )
                )
            )
        )
    }

    @Test
    fun ferie() {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Søknad.Søknadsperiode.Ferie(31.januar, 31.januar))
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag()
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertGodkjenningsbehov(tags = setOf("Førstegangsbehandling", "Innvilget", "Arbeidsgiverutbetaling", "Ferie", "EnArbeidsgiver", "ArbeidsgiverØnskerRefusjon"))
        val utkastTilvedtak = observatør.utkastTilVedtakEventer.last()
        assertTrue(utkastTilvedtak.tags.contains("Ferie"))
    }

    @Test
    fun `6G-begrenset`() {
        tilGodkjenning(januar, a1, beregnetInntekt = 100000.månedlig)
        assertGodkjenningsbehov(
            tags = setOf("Førstegangsbehandling", "Innvilget", "Arbeidsgiverutbetaling", "6GBegrenset", "EnArbeidsgiver", "ArbeidsgiverØnskerRefusjon"),
            omregnedeÅrsinntekter = listOf(mapOf("organisasjonsnummer" to a1, "beløp" to 1_200_000.0)),
            sykepengegrunnlagsfakta = mapOf(
                "omregnetÅrsinntektTotalt" to 1_200_000.0,
                "sykepengegrunnlag" to 561_804.0,
                "6G" to 561_804.0,
                "fastsatt" to "EtterHovedregel",
                "arbeidsgivere" to listOf(
                    mapOf(
                        "arbeidsgiver" to a1,
                        "omregnetÅrsinntekt" to 1200000.0,
                        "inntektskilde" to Inntektskilde.Arbeidsgiver
                    )
                )
            )
        )
    }

    @Test
    fun `ingen ny arbeidsgiverperiode og sykepengegrunnlag under 2g`() {
        nyttVedtak(januar, orgnummer = a1)
        tilGodkjenning(10.februar til 20.februar, a1, beregnetInntekt = 10000.månedlig, vedtaksperiodeIdInnhenter = 2.vedtaksperiode, arbeidsgiverperiode = emptyList())
        assertGodkjenningsbehov(
            skjæringstidspunkt = 10.februar,
            periodeFom = 10.februar,
            periodeTom = 20.februar,
            vedtaksperiodeId = 2.vedtaksperiode.id(a1),
            omregnedeÅrsinntekter = listOf(mapOf("organisasjonsnummer" to a1, "beløp" to 10000.månedlig.årlig)),
            behandlingId = inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.behandlinger.last().id,
            tags = setOf("Førstegangsbehandling", "IngenNyArbeidsgiverperiode", "Innvilget", "Arbeidsgiverutbetaling", "SykepengegrunnlagUnder2G", "EnArbeidsgiver", "ArbeidsgiverØnskerRefusjon"),
            perioderMedSammeSkjæringstidspunkt = listOf(
                mapOf(
                    "vedtaksperiodeId" to 2.vedtaksperiode.id(a1).toString(),
                    "behandlingId" to 2.vedtaksperiode.sisteBehandlingId(a1).toString(),
                    "fom" to 10.februar.toString(),
                    "tom" to 20.februar.toString()
                ),
            ),
            sykepengegrunnlagsfakta = mapOf(
                "omregnetÅrsinntektTotalt" to 10000.månedlig.årlig,
                "6G" to 561_804.0,
                "sykepengegrunnlag" to 10000.månedlig.årlig,
                "fastsatt" to "EtterHovedregel",
                "arbeidsgivere" to listOf(
                    mapOf(
                        "arbeidsgiver" to a1,
                        "omregnetÅrsinntekt" to 120000.0,
                        "inntektskilde" to Inntektskilde.Arbeidsgiver
                    )
                )
            )

        )
    }

    @Test
    fun `Sender med tag IngenNyArbeidsgiverperiode når det ikke er ny AGP pga AIG-dager`() {
        nyttVedtak(juni)
        håndterSøknad(august)
        håndterInntektsmelding(
            listOf(1.juni til 16.juni),
            førsteFraværsdag = 1.august,
            begrunnelseForReduksjonEllerIkkeUtbetalt = "FerieEllerAvspasering"
        )
        assertVarsler(listOf(Varselkode.RV_IM_3, Varselkode.RV_IM_25), 2.vedtaksperiode.filter())
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje((juli).map { ManuellOverskrivingDag(it, Dagtype.ArbeidIkkeGjenopptattDag) })
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        assertTags(setOf("Førstegangsbehandling", "IngenNyArbeidsgiverperiode", "Innvilget", "Arbeidsgiverutbetaling", "ArbeidsgiverØnskerRefusjon", "EnArbeidsgiver"), 2.vedtaksperiode.id(a1))
    }

    @Test
    fun `Sender med tag IngenNyArbeidsgiverperiode når det ikke er ny AGP pga mindre enn 16 dagers gap selv om AGP er betalt av nav`() {
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INGEN, null, emptyList()),
            begrunnelseForReduksjonEllerIkkeUtbetalt = "ArbeidOpphoert",
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        assertVarsel(Varselkode.RV_IM_8, 1.vedtaksperiode.filter())
        tilGodkjenning(16.februar til 28.februar, a1, vedtaksperiodeIdInnhenter = 2.vedtaksperiode, arbeidsgiverperiode = emptyList())
        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS???? ??????? ????SHH SSSSSHH SSS", inspektør.sykdomstidslinje.toShortString())
        assertEquals(listOf(1.januar til 16.januar), inspektør.dagerNavOvertarAnsvar(1.vedtaksperiode))
        assertTags(setOf("Førstegangsbehandling", "IngenNyArbeidsgiverperiode", "Innvilget", "Arbeidsgiverutbetaling", "ArbeidsgiverØnskerRefusjon", "EnArbeidsgiver"), 2.vedtaksperiode.id(a1))
    }

    @Test
    fun `Sender ikke med tag IngenNyArbeidsgiverperiode når det ikke er ny AGP pga Infotrygforlengelse`() {
        createOvergangFraInfotrygdPerson()
        forlengTilGodkjenning(mars)
        assertIngenTag("IngenNyArbeidsgiverperiode", 2.vedtaksperiode.id(a1))
        assertSykepengegrunnlagsfakta(
            sykepengegrunnlagsfakta = mapOf(
                "omregnetÅrsinntektTotalt" to INNTEKT.årlig,
                "fastsatt" to "IInfotrygd"
            ),
            vedtaksperiodeId = 2.vedtaksperiode.id(a1)
        )
    }

    @Test
    fun `Sender ikke med tag IngenNyArbeidsgiverperiode når det ikke er ny AGP pga Infotrygovergang - revurdering`() {
        createOvergangFraInfotrygdPerson()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.februar, Dagtype.Sykedag, 50)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertVarsler(listOf(Varselkode.RV_UT_23, Varselkode.RV_IT_14), 1.vedtaksperiode.filter())
        assertIngenTag("IngenNyArbeidsgiverperiode")
        assertTags(setOf("Førstegangsbehandling", "Innvilget", "Revurdering", "NegativArbeidsgiverutbetaling", "ArbeidsgiverØnskerRefusjon", "InngangsvilkårFraInfotrygd", "EnArbeidsgiver"))
    }

    @Test
    fun `Sender ikke med tag IngenNyArbeidsgiverperiode når det ikke er ny AGP pga forlengelse`() {
        tilGodkjenning(januar, a1)
        assertIngenTag("IngenNyArbeidsgiverperiode", 1.vedtaksperiode.id(a1))
        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()
        forlengTilGodkjenning(februar)
        assertIngenTag("IngenNyArbeidsgiverperiode", 2.vedtaksperiode.id(a1))
    }

    @Test
    fun `Sender ikke med tag IngenNyArbeidsgiverperiode når det er ny AGP`() {
        tilGodkjenning(januar, a1)
        assertIngenTag("IngenNyArbeidsgiverperiode", 1.vedtaksperiode.id(a1))
        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()
        tilGodkjenning(mars, a1, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        assertIngenTag("IngenNyArbeidsgiverperiode", 2.vedtaksperiode.id(a1))
    }

    @Test
    fun `Periode med utbetaling etter kort gap etter kort auu tagges ikke med IngenNyArbeidsgiverperiode`()  {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar))
        håndterSøknad(1.januar til 10.januar)
        tilGodkjenning(15.januar til 31.januar, a1, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)

        assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
        assertIngenTag("IngenNyArbeidsgiverperiode", 2.vedtaksperiode.id(a1))
    }

    @Test
    fun `Forlengelse av auu skal ikke tagges med IngenNyArbeidsgiverperiode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar))
        håndterSøknad(1.januar til 16.januar)
        val inntektsmeldingId = UUID.randomUUID()
        tilGodkjenning(17.januar til 31.januar, a1, arbeidsgiverperiode = listOf(1.januar til 16.januar), inntektsmeldingId = inntektsmeldingId, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        assertIngenTag("IngenNyArbeidsgiverperiode", 2.vedtaksperiode.id(a1))
    }

    @Test
    fun personutbetaling() {
        nyPeriode(januar)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(beløp = INGEN, opphørsdato = null),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertGodkjenningsbehov(tags = setOf("Førstegangsbehandling", "Innvilget", "Personutbetaling", "EnArbeidsgiver"))
    }

    @Test
    fun `delvis refusjon`() {
        nyPeriode(januar)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT / 2, opphørsdato = null),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertGodkjenningsbehov(tags = setOf("Førstegangsbehandling", "Innvilget", "Arbeidsgiverutbetaling", "Personutbetaling", "EnArbeidsgiver", "ArbeidsgiverØnskerRefusjon"))
    }

    @Test
    fun `ingen utbetaling`() {
        nyttVedtak(januar)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Søknad.Søknadsperiode.Ferie(1.februar, 28.februar))
        håndterYtelser(2.vedtaksperiode)
        assertGodkjenningsbehov(
            tags = setOf("Forlengelse", "Avslag", "IngenUtbetaling", "Ferie", "EnArbeidsgiver", "ArbeidsgiverØnskerRefusjon"),
            vedtaksperiodeId = 2.vedtaksperiode.id(a1),
            periodeFom = 1.februar,
            periodeTom = 28.februar,
            periodeType = "FORLENGELSE",
            førstegangsbehandling = false,
            behandlingId = inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.behandlinger.last().id,
            perioderMedSammeSkjæringstidspunkt = listOf(
                mapOf("vedtaksperiodeId" to 1.vedtaksperiode.id(a1).toString(), "behandlingId" to 1.vedtaksperiode.sisteBehandlingId(a1).toString(), "fom" to 1.januar.toString(), "tom" to 31.januar.toString()),
                mapOf("vedtaksperiodeId" to 2.vedtaksperiode.id(a1).toString(), "behandlingId" to 2.vedtaksperiode.sisteBehandlingId(a1).toString(), "fom" to 1.februar.toString(), "tom" to 28.februar.toString())
            ),
            sykepengegrunnlagsfakta = mapOf(
                "omregnetÅrsinntektTotalt" to INNTEKT.årlig,
                "sykepengegrunnlag" to INNTEKT.årlig,
                "6G" to 561_804.0,
                "fastsatt" to "EtterHovedregel",
                "arbeidsgivere" to listOf(
                    mapOf(
                        "arbeidsgiver" to a1,
                        "omregnetÅrsinntekt" to INNTEKT.årlig,
                        "inntektskilde" to Inntektskilde.Arbeidsgiver,
                    )
                )
            )
        )
    }

    @Test
    fun `saksbehandlerkorrigert inntekt`() {
        tilGodkjenning(januar, a1)
        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning("a1", INNTEKT/2)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertGodkjenningsbehov(
            tags = setOf("Førstegangsbehandling", "Innvilget", "Arbeidsgiverutbetaling", "SykepengegrunnlagUnder2G", "EnArbeidsgiver", "ArbeidsgiverØnskerRefusjon"),
            vedtaksperiodeId = 1.vedtaksperiode.id(a1),
            periodeFom = 1.januar,
            periodeTom = 31.januar,
            periodeType = "FØRSTEGANGSBEHANDLING",
            førstegangsbehandling = true,
            behandlingId = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.last().id,
            perioderMedSammeSkjæringstidspunkt = listOf(
                mapOf("vedtaksperiodeId" to 1.vedtaksperiode.id(a1).toString(), "behandlingId" to 1.vedtaksperiode.sisteBehandlingId(a1).toString(), "fom" to 1.januar.toString(), "tom" to 31.januar.toString()),
            ),
            omregnedeÅrsinntekter = listOf(
                mapOf("organisasjonsnummer" to a1, "beløp" to (INNTEKT/2).årlig)
            ),
            sykepengegrunnlagsfakta = mapOf(
                "omregnetÅrsinntektTotalt" to (INNTEKT/2).årlig,
                "sykepengegrunnlag" to (INNTEKT/2).årlig,
                "6G" to 561_804.0,
                "fastsatt" to "EtterHovedregel",
                "arbeidsgivere" to listOf(
                    mapOf(
                        "arbeidsgiver" to a1,
                        "omregnetÅrsinntekt" to (INNTEKT/2).årlig,
                        "inntektskilde" to Inntektskilde.Saksbehandler,
                    )
                )
            )
        )
    }

    @Test
    fun `trekker tilbake penger fra arbeidsgiver og flytter til bruker`() {
        nyttVedtak(januar)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(beløp = INGEN, opphørsdato = null)
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertGodkjenningsbehov(tags = setOf("Førstegangsbehandling", "Innvilget", "Revurdering", "NegativArbeidsgiverutbetaling", "Personutbetaling", "EnArbeidsgiver"), kanAvvises = false, utbetalingstype = "REVURDERING")
    }

    @Test
    fun `trekker tilbake penger fra person og flytter til arbeidsgiver`() {
        nyttVedtak(januar, refusjon = Inntektsmelding.Refusjon(INGEN, null))
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT, opphørsdato = null)
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertVarsler(listOf(Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
        assertGodkjenningsbehov(tags = setOf("Førstegangsbehandling", "Innvilget", "Revurdering", "Arbeidsgiverutbetaling", "NegativPersonutbetaling", "EnArbeidsgiver", "ArbeidsgiverØnskerRefusjon"), kanAvvises = false, utbetalingstype = "REVURDERING")
    }

    @Test
    fun `flere arbeidsgivere`() {
        tilGodkjenning(januar, a1, a2)
        assertGodkjenningsbehov(
            tags = setOf("Førstegangsbehandling", "Innvilget", "Arbeidsgiverutbetaling", "6GBegrenset", "FlereArbeidsgivere", "ArbeidsgiverØnskerRefusjon"),
            inntektskilde = "FLERE_ARBEIDSGIVERE",
            orgnummere = setOf(a1, a2),
            omregnedeÅrsinntekter = listOf(
                mapOf("organisasjonsnummer" to a1, "beløp" to INNTEKT.årlig),
                mapOf("organisasjonsnummer" to a2, "beløp" to INNTEKT.årlig)
            ),
            perioderMedSammeSkjæringstidspunkt = listOf(
                mapOf("vedtaksperiodeId" to 1.vedtaksperiode.id(a1).toString(), "behandlingId" to 1.vedtaksperiode.sisteBehandlingId(a1).toString(), "fom" to 1.januar.toString(), "tom" to 31.januar.toString()),
                mapOf("vedtaksperiodeId" to 1.vedtaksperiode.id(a2).toString(), "behandlingId" to 1.vedtaksperiode.sisteBehandlingId(a2).toString(), "fom" to 1.januar.toString(), "tom" to 31.januar.toString())
            ),
            sykepengegrunnlagsfakta = mapOf(
                "omregnetÅrsinntektTotalt" to 744000.årlig.årlig,
                "sykepengegrunnlag" to 561_804.0,
                "6G" to 561_804.0,
                "fastsatt" to "EtterHovedregel",
                "arbeidsgivere" to listOf(
                    mapOf(
                        "arbeidsgiver" to a1,
                        "omregnetÅrsinntekt" to INNTEKT.årlig,
                        "inntektskilde" to Inntektskilde.Arbeidsgiver,
                    ),
                    mapOf(
                        "arbeidsgiver" to a2,
                        "omregnetÅrsinntekt" to INNTEKT.årlig,
                        "inntektskilde" to Inntektskilde.Arbeidsgiver,
                    )
                )
            )
        )
    }

    @Test
    fun `Periode med minst én navdag får Innvilget-tag`() {
        tilGodkjenning(januar, a1, beregnetInntekt = INNTEKT)
        assertGodkjenningsbehov(tags = setOf("Førstegangsbehandling", "Innvilget", "Arbeidsgiverutbetaling", "EnArbeidsgiver", "ArbeidsgiverØnskerRefusjon"))
    }

    @Test
    fun `Periode med minst én navdag og minst én avslagsdag får DelvisInnvilget-tag`() {
        createTestPerson(Personidentifikator("18.01.1948"), 18.januar(1948))
        tilGodkjenning(januar, a1, beregnetInntekt = INNTEKT)
        assertGodkjenningsbehov(tags = setOf("Førstegangsbehandling", "DelvisInnvilget", "Arbeidsgiverutbetaling", "EnArbeidsgiver", "ArbeidsgiverØnskerRefusjon"))
    }

    @Test
    fun `Periode med arbeidsgiverperiodedager, ingen navdager og minst én avslagsdag får Avslag-tag`() {
        createTestPerson(Personidentifikator("16.01.1948"), 16.januar(1948))
        nyPeriode(januar, a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertGodkjenningsbehov(tags = setOf("Førstegangsbehandling", "Avslag", "IngenUtbetaling", "EnArbeidsgiver", "ArbeidsgiverØnskerRefusjon"))
    }

    @Test
    fun `Periode med kun avslagsdager får Avslag-tag`() {
        createTestPerson(Personidentifikator("16.01.1946"), 16.januar(1946))
        nyPeriode(januar, a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertGodkjenningsbehov(tags = setOf("Førstegangsbehandling", "Avslag", "IngenUtbetaling", "EnArbeidsgiver", "ArbeidsgiverØnskerRefusjon"))
    }

    @Test
    fun `Periode uten navdager og avslagsdager får Avslag-tag`() {
        nyttVedtak(januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Søknad.Søknadsperiode.Ferie(1.februar, 28.februar))
        håndterYtelser(2.vedtaksperiode)
        assertGodkjenningsbehov(
            tags = setOf("Forlengelse", "Avslag", "IngenUtbetaling", "Ferie", "EnArbeidsgiver", "ArbeidsgiverØnskerRefusjon"),
            vedtaksperiodeId = 2.vedtaksperiode.id(a1),
            periodeType = "FORLENGELSE",
            periodeFom = 1.februar,
            periodeTom = 28.februar,
            førstegangsbehandling = false,
            behandlingId = inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.behandlinger.last().id,
            perioderMedSammeSkjæringstidspunkt = listOf(
                mapOf("vedtaksperiodeId" to 1.vedtaksperiode.id(a1).toString(), "behandlingId" to 1.vedtaksperiode.sisteBehandlingId(a1).toString(), "fom" to 1.januar.toString(), "tom" to 31.januar.toString()),
                mapOf("vedtaksperiodeId" to 2.vedtaksperiode.id(a1).toString(), "behandlingId" to 2.vedtaksperiode.sisteBehandlingId(a1).toString(), "fom" to 1.februar.toString(), "tom" to 28.februar.toString())
            )
        )
    }

    @Test
    fun `legger til førstegangsbehandling eller forlengelse som tag`() {
        tilGodkjenning(januar, a1, beregnetInntekt = INNTEKT)
        assertGodkjenningsbehov(tags = setOf("Førstegangsbehandling", "Innvilget", "Arbeidsgiverutbetaling", "EnArbeidsgiver", "ArbeidsgiverØnskerRefusjon"))
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        forlengTilGodkjenning(februar)
        assertGodkjenningsbehov(
            tags = setOf("Forlengelse", "Innvilget", "Arbeidsgiverutbetaling", "EnArbeidsgiver", "ArbeidsgiverØnskerRefusjon"),
            periodeFom = 1.februar,
            periodeTom = 28.februar,
            vedtaksperiodeId = 2.vedtaksperiode.id(a1),
            behandlingId = inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.behandlinger.last().id,
            periodeType = "FORLENGELSE",
            førstegangsbehandling = false,
            perioderMedSammeSkjæringstidspunkt = listOf(
                mapOf("vedtaksperiodeId" to 1.vedtaksperiode.id(a1).toString(), "behandlingId" to 1.vedtaksperiode.sisteBehandlingId(a1).toString(), "fom" to 1.januar.toString(), "tom" to 31.januar.toString()),
                mapOf("vedtaksperiodeId" to 2.vedtaksperiode.id(a1).toString(), "behandlingId" to 2.vedtaksperiode.sisteBehandlingId(a1).toString(), "fom" to 1.februar.toString(), "tom" to 28.februar.toString()),
            )
        )
    }

    @Test
    fun `legger til hendelses ID'er og dokumenttype på godkjenningsbehovet`() {
        håndterSykmelding(januar)
        val søknadId = håndterSøknad(januar)
        val inntektsmeldingId = håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertGodkjenningsbehov(
            hendelser = setOf(søknadId, inntektsmeldingId),
            tags = setOf("Førstegangsbehandling", "Innvilget", "Arbeidsgiverutbetaling", "EnArbeidsgiver", "ArbeidsgiverØnskerRefusjon")
        )
    }

    @Test
    fun `markerer sykepengegrunnlagsfakta som skjønnsfastsatt dersom én arbeidsgiver har fått skjønnmessig fastsatt sykepengegrunnlaget`() {
        tilGodkjenning(januar, a1, a2, beregnetInntekt = 20000.månedlig)
        håndterSkjønnsmessigFastsettelse(
            1.januar, arbeidsgiveropplysninger = listOf(
            OverstyrtArbeidsgiveropplysning(a1, 41000.månedlig),
            OverstyrtArbeidsgiveropplysning(a2, 30000.månedlig)
        )
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertSykepengegrunnlagsfakta(
            vedtaksperiodeId = 1.vedtaksperiode.id(a1),
            sykepengegrunnlagsfakta = mapOf(
                "omregnetÅrsinntektTotalt" to 480000.0,
                "6G" to 561804.0,
                "fastsatt" to "EtterSkjønn",
                "skjønnsfastsatt" to 852000.0,
                "sykepengegrunnlag" to 561804.0,
                "arbeidsgivere" to listOf(
                    mapOf(
                        "arbeidsgiver" to a1,
                        "omregnetÅrsinntekt" to 240000.0,
                        "inntektskilde" to Inntektskilde.Saksbehandler,
                        "skjønnsfastsatt" to 492000.0
                    ),
                    mapOf(
                        "arbeidsgiver" to a2,
                        "omregnetÅrsinntekt" to 240000.0,
                        "inntektskilde" to Inntektskilde.Saksbehandler,
                        "skjønnsfastsatt" to 360000.0
                    )
                )
            )
        )
    }

    private fun assertTags(tags: Set<String>, vedtaksperiodeId: UUID = 1.vedtaksperiode.id(a1)) {
        val actualtags = hentFelt<Set<String>>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "tags") ?: emptySet()
        assertEquals(tags, actualtags)
        val utkastTilVedtak = observatør.utkastTilVedtakEventer.last()
        assertEquals(actualtags, utkastTilVedtak.tags)
    }

    private fun assertIngenTag(tag: String, vedtaksperiodeId: UUID = 1.vedtaksperiode.id(a1)) {
        val actualtags = hentFelt<Set<String>>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "tags") ?: emptySet()
        assertFalse(actualtags.contains(tag))
        val utkastTilVedtak = observatør.utkastTilVedtakEventer.last()
        assertFalse(utkastTilVedtak.tags.contains(tag))
    }

    private fun assertSykepengegrunnlagsfakta(
        vedtaksperiodeId: UUID = 1.vedtaksperiode.id(a1),
        sykepengegrunnlagsfakta: Map<String, Any>
    ) {
        val actualSykepengegrunnlagsfakta = hentFelt<Any>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "sykepengegrunnlagsfakta")!!
        assertEquals(sykepengegrunnlagsfakta, actualSykepengegrunnlagsfakta)
    }

    private fun assertHendelser(hendelser: Set<UUID>, vedtaksperiodeId: UUID) {
        val actualHendelser = hentFelt<Set<UUID>>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "hendelser")!!
        assertEquals(hendelser, actualHendelser)
    }

    private fun assertGodkjenningsbehov(
        tags: Set<String>,
        skjæringstidspunkt: LocalDate = 1.januar,
        periodeFom: LocalDate = 1.januar,
        periodeTom: LocalDate = 31.januar,
        vedtaksperiodeId: UUID = 1.vedtaksperiode.id(a1),
        hendelser: Set<UUID>? = null,
        orgnummere: Set<String> = setOf(a1),
        kanAvvises: Boolean = true,
        periodeType: String = "FØRSTEGANGSBEHANDLING",
        førstegangsbehandling: Boolean = true,
        utbetalingstype: String = "UTBETALING",
        inntektskilde: String = "EN_ARBEIDSGIVER",
        omregnedeÅrsinntekter: List<Map<String, Any>> = listOf(mapOf("organisasjonsnummer" to a1, "beløp" to INNTEKT.årlig)),
        behandlingId: UUID = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.last().id,
        perioderMedSammeSkjæringstidspunkt: List<Map<String, String>> = listOf(
            mapOf("vedtaksperiodeId" to 1.vedtaksperiode.id(a1).toString(), "behandlingId" to 1.vedtaksperiode.sisteBehandlingId(a1).toString(), "fom" to 1.januar.toString(), "tom" to 31.januar.toString()),
        ),
        sykepengegrunnlagsfakta: Map<String, Any> = mapOf(
            "omregnetÅrsinntektTotalt" to INNTEKT.årlig,
            "sykepengegrunnlag" to 372_000.0,
            "6G" to 561_804.0,
            "fastsatt" to "EtterHovedregel",
            "arbeidsgivere" to listOf(
                mapOf(
                    "arbeidsgiver" to a1,
                    "omregnetÅrsinntekt" to INNTEKT.årlig,
                    "inntektskilde" to Inntektskilde.Arbeidsgiver
                )
            )
        )
    ) {
        val actualtags = hentFelt<Set<String>>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "tags") ?: emptySet()
        val actualSkjæringstidspunkt = hentFelt<String>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "skjæringstidspunkt")!!
        val actualInntektskilde = hentFelt<String>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "inntektskilde")!!
        val actualPeriodetype = hentFelt<String>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "periodetype")!!
        val actualPeriodeFom = hentFelt<String>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "periodeFom")!!
        val actualPeriodeTom = hentFelt<String>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "periodeTom")!!
        val actualFørstegangsbehandling = hentFelt<Boolean>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "førstegangsbehandling")!!
        val actualUtbetalingtype = hentFelt<String>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "utbetalingtype")!!
        val actualOrgnummereMedRelevanteArbeidsforhold = hentFelt<Set<String>>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "orgnummereMedRelevanteArbeidsforhold")!!
        val actualKanAvises = hentFelt<Boolean>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "kanAvvises")!!
        val actualOmregnedeÅrsinntekter = hentFelt<List<Map<String, String>>>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "omregnedeÅrsinntekter")!!
        val actualBehandlingId = hentFelt<String>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "behandlingId")!!
        val actualPerioderMedSammeSkjæringstidspunkt = hentFelt<Any>(vedtaksperiodeId = vedtaksperiodeId, feltNavn = "perioderMedSammeSkjæringstidspunkt")!!

        hendelser?.let { assertHendelser(it, vedtaksperiodeId) }
        assertSykepengegrunnlagsfakta(vedtaksperiodeId, sykepengegrunnlagsfakta)
        assertEquals(tags, actualtags)
        assertEquals(skjæringstidspunkt.toString(), actualSkjæringstidspunkt)
        assertEquals(inntektskilde, actualInntektskilde)
        assertEquals(periodeType, actualPeriodetype)
        assertEquals(periodeFom.toString(), actualPeriodeFom)
        assertEquals(periodeTom.toString(), actualPeriodeTom)
        assertEquals(førstegangsbehandling, actualFørstegangsbehandling)
        assertEquals(utbetalingstype, actualUtbetalingtype)
        assertEquals(orgnummere, actualOrgnummereMedRelevanteArbeidsforhold)
        assertEquals(kanAvvises, actualKanAvises)
        assertEquals(omregnedeÅrsinntekter, actualOmregnedeÅrsinntekter)
        assertEquals(behandlingId.toString(), actualBehandlingId)
        assertEquals(perioderMedSammeSkjæringstidspunkt, actualPerioderMedSammeSkjæringstidspunkt)

        val utkastTilVedtak = observatør.utkastTilVedtakEventer.last()
        assertEquals(actualtags, utkastTilVedtak.tags)
        assertEquals(actualBehandlingId, utkastTilVedtak.behandlingId.toString())
        assertEquals(vedtaksperiodeId, utkastTilVedtak.vedtaksperiodeId)
    }

    private inline fun <reified T> hentFelt(vedtaksperiodeId: UUID = 1.vedtaksperiode.id(a1), feltNavn: String) =
        hendelselogg.hentFeltFraBehov<T>(
            vedtaksperiodeId = vedtaksperiodeId,
            behov = Aktivitet.Behov.Behovtype.Godkjenning,
            felt = feltNavn
        )
}
