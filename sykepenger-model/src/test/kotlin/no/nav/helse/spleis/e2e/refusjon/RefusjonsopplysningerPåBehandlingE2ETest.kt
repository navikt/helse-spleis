package no.nav.helse.spleis.e2e.refusjon

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittArbeidgiverperiode
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittInntekt
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittRefusjon
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittRefusjon.Refusjonsendring
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.Avsender.SAKSBEHANDLER
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.onsdag
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.arbeidsgiver
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.saksbehandler
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.serde.tilPersonData
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.tirsdag
import no.nav.helse.torsdag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RefusjonsopplysningerPåBehandlingE2ETest : AbstractDslTest() {

    @Test
    fun `arbeidsgiver opplyser om refusjonsopplysnigner frem i tid, så skal saksbehandler overstyre dem før søknaden kommer - ingen ubrukte etter overstyring`() {
        a1 {
            håndterSøknad(januar)
            val arbeidsgiverId = håndterArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittArbeidgiverperiode(listOf(1.januar til 16.januar)),
                OppgittInntekt(INNTEKT),
                OppgittRefusjon(INNTEKT, listOf(Refusjonsendring(1.februar, INNTEKT * 0.8), Refusjonsendring(1.mars, INNTEKT * 0.6)))
            )
            assertBeløpstidslinje(Beløpstidslinje.fra(januar, INNTEKT, arbeidsgiverId.arbeidsgiver), inspektør.refusjon(1.vedtaksperiode))
            val fremITid = Beløpstidslinje.fra(februar, INNTEKT * 0.8, arbeidsgiverId.arbeidsgiver) + Beløpstidslinje.fra(1.mars.somPeriode(), INNTEKT * 0.6, arbeidsgiverId.arbeidsgiver)
            assertBeløpstidslinje(fremITid, inspektør.ubrukteRefusjonsopplysninger.refusjonstidslinjer.values.single())

            val saksbehandlerId = håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                overstyringer = listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT, refusjonsopplysninger = listOf(Triple(1.januar, null, INNTEKT))))
            ).metadata.meldingsreferanseId

            assertBeløpstidslinje(Beløpstidslinje.fra(1.februar til 1.mars, INNTEKT, saksbehandlerId.id.saksbehandler), inspektør.ubrukteRefusjonsopplysninger.refusjonstidslinjer.values.single())
        }
    }

    @Test
    fun `arbeidsgiver opplyser om refusjonsopplysnigner frem i tid, så skal saksbehandler overstyre dem før søknaden kommer - nye ubrukte etter overstyring`() {
        a1 {
            håndterSøknad(januar)
            val id = håndterArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittArbeidgiverperiode(listOf(1.januar til 16.januar)),
                OppgittInntekt(INNTEKT),
                OppgittRefusjon(INNTEKT, listOf(Refusjonsendring(1.februar, INNTEKT * 0.8), Refusjonsendring(1.mars, INNTEKT * 0.6)))
            )
            assertBeløpstidslinje(Beløpstidslinje.fra(januar, INNTEKT, id.arbeidsgiver), inspektør.refusjon(1.vedtaksperiode))
            val fremITid = Beløpstidslinje.fra(februar, INNTEKT * 0.8, id.arbeidsgiver) + Beløpstidslinje.fra(1.mars.somPeriode(), INNTEKT * 0.6, id.arbeidsgiver)
            assertBeløpstidslinje(fremITid, inspektør.ubrukteRefusjonsopplysninger.refusjonstidslinjer.values.single())

            val saksbehandlerId = håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                overstyringer = listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT, refusjonsopplysninger = listOf(Triple(1.januar, 31.januar, INNTEKT), Triple(1.februar, null, INGEN))))
            ).metadata.meldingsreferanseId

            assertBeløpstidslinje(Beløpstidslinje.fra(1.februar til 1.mars, INGEN, saksbehandlerId.id.saksbehandler), inspektør.ubrukteRefusjonsopplysninger.refusjonstidslinjer.values.single())
        }
    }

    @Test
    fun `og noen ganger sendes det endringer i refusjon på samme dato`() {
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode,
                refusjon = Refusjon(
                    INNTEKT, null, endringerIRefusjon = listOf(
                    Refusjon.EndringIRefusjon(INNTEKT * 0.7, 20.januar),
                    Refusjon.EndringIRefusjon(INNTEKT * 0.8, 20.januar),
                    Refusjon.EndringIRefusjon(INNTEKT * 0.9, 20.januar)
                )
                )
            )
            val forventet = ARBEIDSGIVER.beløpstidslinje(1.januar til 19.januar, INNTEKT) + ARBEIDSGIVER.beløpstidslinje(20.januar til 31.januar, INNTEKT * 0.7)
            assertBeløpstidslinje(forventet, inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje, ignoreMeldingsreferanseId = true)
        }
    }

    @Test
    fun `håndterer refusjonsopplysninger ved out of order`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(17.februar til 28.februar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertEquals(Beløpstidslinje(), inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje)
            håndterSøknad(1.februar til 16.februar)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_HISTORIKK)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertTrue(inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje.isNotEmpty())
        }
    }

    @Test
    fun `En forespurt portalinntektsmelding som setter arbeidsgiverperioden _etter_ søknaden - virkedag mellom siste søknadsdag og første agp-dag`() {
        a1 {
            håndterSøknad(1.januar til 16.januar)
            håndterSøknad(17.januar til 21.januar)
            håndterArbeidsgiveropplysninger(
                listOf(tirsdag(23.januar) til 7.februar),
                vedtaksperiodeId = 2.vedtaksperiode
            )
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
            assertVarsler(emptyList(), 2.vedtaksperiode.filter()) // Ingen har jo håndtert disse dagene, så ikke noe varsel
        }
    }

    @Test
    fun `overstyring av ubrukte refusjonsopplysninger`() {
        nyttVedtak(januar)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT,
            refusjon = Refusjon(
                INNTEKT, null,
                endringerIRefusjon = listOf(Refusjon.EndringIRefusjon(INNTEKT / 2, 20.februar))
            )
        )
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

        val ubrukteRefusjonsopplysninger = inspektør.ubrukteRefusjonsopplysninger
        assertBeløpstidslinje(
            ARBEIDSGIVER.beløpstidslinje(1.februar til 19.februar, INNTEKT) + ARBEIDSGIVER.beløpstidslinje(20.februar.somPeriode(), INNTEKT / 2),
            ubrukteRefusjonsopplysninger.refusjonstidslinjer.getValue(1.januar),
            ignoreMeldingsreferanseId = true
        )
        håndterOverstyrArbeidsgiveropplysninger(
            skjæringstidspunkt = 1.januar,
            arbeidsgiveropplysninger = listOf(OverstyrtArbeidsgiveropplysning(
                orgnummer = a1,
                inntekt = INNTEKT,
                refusjonsopplysninger = listOf(Triple(1.januar, 31.januar, INNTEKT), Triple(1.februar, null, INGEN))
            ))
        )

        val ubrukteRefusjonsopplysninger2 = inspektør.ubrukteRefusjonsopplysninger
        assertBeløpstidslinje(
            SAKSBEHANDLER.beløpstidslinje(1.februar til 20.februar, INGEN),
            ubrukteRefusjonsopplysninger2.refusjonstidslinjer.getValue(1.januar),
            ignoreMeldingsreferanseId = true
        )
    }

    @Test
    fun `En situasjon med gjenbruk hvor refusjonsopplysningene på vilkårsgrunnlaget blir feil, men vil løse seg med refusjonsopplysniger på behandlingene`() {
        a1 {
            nyttVedtak(2.januar til 31.januar)
            assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(2.januar til 31.januar, INNTEKT), inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje, ignoreMeldingsreferanseId = true)

            håndterInntektsmelding(listOf(2.januar til 5.januar, 8.januar til 12.januar, 15.januar til 19.januar, 22.januar til 23.januar))
            assertVarsel(Varselkode.RV_IM_24, 1.vedtaksperiode.filter())
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(2.januar til 31.januar, INNTEKT), inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje, ignoreMeldingsreferanseId = true)

            håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 22.januar,
                overstyringer = listOf(
                    OverstyrtArbeidsgiveropplysning(
                        orgnummer = a1,
                        inntekt = INNTEKT,
                        refusjonsopplysninger = listOf(
                            Triple(2.januar, 25.januar, INGEN), Triple(26.januar, null, INNTEKT)
                        )
                    )
                )
            )
            assertBeløpstidslinje(SAKSBEHANDLER.beløpstidslinje(2.januar til 25.januar, INGEN) + ARBEIDSGIVER.beløpstidslinje(26.januar til 31.januar, INNTEKT), inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje, ignoreMeldingsreferanseId = true)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            håndterOverstyrTidslinje((2.januar til 23.januar).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, 100) })
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertBeløpstidslinje(SAKSBEHANDLER.beløpstidslinje(2.januar til 25.januar, INGEN) + ARBEIDSGIVER.beløpstidslinje(26.januar til 31.januar, INNTEKT), inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje, ignoreMeldingsreferanseId = true)
        }
    }

    @Test
    fun `Perioder i avsluttet uten utbetling må alltid håndtere refusjonsopplysninger`() {
        a1 {
            håndterSøknad(10.januar til 20.januar)
            håndterSøknad(21.januar til 25.januar)
            nullstillTilstandsendringer()
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE)
            assertBeløpstidslinje(inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje, 1.januar til 20.januar, INNTEKT)
            assertBeløpstidslinje(inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje, 21.januar til 25.januar, INNTEKT)
        }
    }

    @Test
    fun `Refusjonsopplysningene strekker seg sammen med strekking av vedtaksperioden`() {
        a1 {
            tilGodkjenning(10.januar til 31.januar)
            assertBeløpstidslinje(inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje, 10.januar til 31.januar, INNTEKT)

            håndterOverstyrTidslinje((1.januar til 9.januar).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, 100) })
            assertVarsel(Varselkode.RV_IV_7, 1.vedtaksperiode.filter())
            håndterVilkårsgrunnlag()
            håndterYtelser(1.vedtaksperiode)

            assertBeløpstidslinje(inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje, januar, INNTEKT)
        }
    }

    @Test
    fun `En overstyring som er kommet etter IM skal vinne på forlengelsen`() {
        a1 {
            nyttVedtak(januar)
            val overstyringId = UUID.randomUUID()
            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT, listOf(Triple(1.januar, null, INNTEKT / 2)))), hendelseId = overstyringId)
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            håndterSøknad(februar)
            assertBeløpstidslinje(inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje, februar, INNTEKT / 2, overstyringId)
        }
    }

    @Test
    fun `Feil refusjon på forlengelse som ikke har kommet`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                overstyringer = listOf(
                    OverstyrtArbeidsgiveropplysning(
                        orgnummer = a1,
                        inntekt = INNTEKT,
                        refusjonsopplysninger = listOf(
                            Triple(1.januar, 28.februar, INGEN),
                            Triple(1.mars, null, INNTEKT)
                        )
                    )
                )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(mars)
            håndterYtelser(3.vedtaksperiode)

            assertBeløpstidslinje(inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje, januar, INGEN)
            assertBeløpstidslinje(inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje, februar, INGEN)

            assertBeløpstidslinje(inspektør.vedtaksperioder(3.vedtaksperiode).refusjonstidslinje, mars, INNTEKT)
        }
    }

    @Test
    fun `arbeidsgiver opplyser om refusjonsopplysninger frem i tid`() {
        a1 {
            håndterSøknad(januar)
            val mottatt = LocalDateTime.now()
            val im = MeldingsreferanseId(håndterInntektsmelding(listOf(1.januar til 16.januar), refusjon = Refusjon(INNTEKT, opphørsdato = 28.februar), mottatt = mottatt))

            val forventetUbruktEtterJanuarSøknad =
                Beløpstidslinje.fra(1.februar til 28.februar, INNTEKT, Kilde(im, ARBEIDSGIVER, mottatt)) +
                    Beløpstidslinje.fra(1.mars.somPeriode(), INGEN, Kilde(im, ARBEIDSGIVER, mottatt))

            assertEquals(setOf(1.januar), inspektør.ubrukteRefusjonsopplysninger.refusjonstidslinjer.keys)
            assertEquals(forventetUbruktEtterJanuarSøknad, inspektør.ubrukteRefusjonsopplysninger.refusjonstidslinjer.getValue(1.januar))
            assertEquals(forventetUbruktEtterJanuarSøknad, gjenopprettBeløpstislinjeFor(1.januar))

            håndterSøknad(februar)

            val forventetUbruktEtterFebruarSøknad = Beløpstidslinje.fra(1.mars.somPeriode(), INGEN, Kilde(im, ARBEIDSGIVER, mottatt))
            val forventetBruktForFebruar = Beløpstidslinje.fra(februar, INNTEKT, Kilde(im, ARBEIDSGIVER, mottatt))

            assertEquals(setOf(1.januar), inspektør.ubrukteRefusjonsopplysninger.refusjonstidslinjer.keys)
            assertEquals(forventetUbruktEtterFebruarSøknad, inspektør.ubrukteRefusjonsopplysninger.refusjonstidslinjer.getValue(1.januar))
            assertEquals(forventetUbruktEtterFebruarSøknad, gjenopprettBeløpstislinjeFor(1.januar))
            assertEquals(forventetBruktForFebruar, inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje)

            val forventetBruktForMars = Beløpstidslinje.fra(mars, INGEN, Kilde(im, ARBEIDSGIVER, mottatt))
            håndterSøknad(mars)
            assertEquals(emptySet<LocalDate>(), inspektør.ubrukteRefusjonsopplysninger.refusjonstidslinjer.keys)
            assertEquals(forventetBruktForMars, inspektør.vedtaksperioder(3.vedtaksperiode).refusjonstidslinje)
        }
    }

    @Test
    fun `saksbehandler opplyser om endring i refusjon frem i tid`() {
        a1 {
            nyttVedtak(januar)
            assertEquals(emptySet<LocalDate>(), inspektør.ubrukteRefusjonsopplysninger.refusjonstidslinjer.keys)

            val overstyringId = MeldingsreferanseId(UUID.randomUUID())
            val overstyringTidspunkt = LocalDateTime.now()
            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT, listOf(Triple(1.februar, null, INGEN)))), hendelseId = overstyringId.id, tidsstempel = overstyringTidspunkt)
            assertEquals(setOf(1.januar), inspektør.ubrukteRefusjonsopplysninger.refusjonstidslinjer.keys)

            val forventetUbrukt =
                Beløpstidslinje.fra(1.februar.somPeriode(), INGEN, Kilde(overstyringId, SAKSBEHANDLER, overstyringTidspunkt))

            assertEquals(forventetUbrukt, inspektør.ubrukteRefusjonsopplysninger.refusjonstidslinjer.getValue(1.januar))
            assertEquals(forventetUbrukt, gjenopprettBeløpstislinjeFor(1.januar))

            val forventetBrukt =
                Beløpstidslinje.fra(februar, INGEN, Kilde(overstyringId, SAKSBEHANDLER, overstyringTidspunkt))

            håndterSøknad(februar)
            assertEquals(emptySet<LocalDate>(), inspektør.ubrukteRefusjonsopplysninger.refusjonstidslinjer.keys)
            assertEquals(forventetBrukt, inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje)
        }
    }

    @Test
    fun `periode etter ferie mangler _ikke_ refusjonsopplysninger`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(1.februar, 28.februar))
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            forlengVedtak(mars)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)

            assertBeløpstidslinje(inspektør.vedtaksperioder(3.vedtaksperiode).refusjonstidslinje, mars, INNTEKT)
        }
    }

    @Test
    fun `periode etter ferie _legger_ til grunn de nye refusjonopplysningene`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(1.februar, 28.februar))
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            forlengVedtak(mars)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                førsteFraværsdag = 1.januar,
                refusjon = Refusjon(
                    beløp = INNTEKT / 2,
                    opphørsdato = null
                )
            )
            assertBeløpstidslinje(inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje, januar, INNTEKT / 2)
            assertBeløpstidslinje(inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje, februar, INNTEKT / 2)
            assertBeløpstidslinje(inspektør.vedtaksperioder(3.vedtaksperiode).refusjonstidslinje, mars, INNTEKT / 2)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `periode etter ferie _legger_ til grunn de nye refusjonopplysningene og har rester frem i tid med opphør`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(1.februar, 28.februar))
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            forlengVedtak(mars)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                førsteFraværsdag = 1.januar,
                refusjon = Refusjon(
                    beløp = INNTEKT / 2,
                    opphørsdato = 30.april
                )
            )
            assertBeløpstidslinje(inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje, januar, INNTEKT / 2)
            assertBeløpstidslinje(inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje, februar, INNTEKT / 2)

            assertBeløpstidslinje(inspektør.vedtaksperioder(3.vedtaksperiode).refusjonstidslinje, mars, INNTEKT / 2)
            assertInfo("Refusjonsservitøren har rester for 01-01-2018 etter servering: 01-04-2018 til 01-05-2018", AktivitetsloggFilter.arbeidsgiver(a1))

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `periode som er strukket kant i kant med annen med AIG`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(mars)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterOverstyrTidslinje(februar.map { ManuellOverskrivingDag(it, Dagtype.ArbeidIkkeGjenopptattDag) })
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.mars, beregnetInntekt = INNTEKT * 1.1)
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            assertBeløpstidslinje(inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje, januar, INNTEKT)
            assertBeløpstidslinje(inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje, 1.februar til 31.mars, INNTEKT * 1.1)

            håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, refusjon = Refusjon(INGEN, null))
            assertBeløpstidslinje(inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje, januar, INGEN)
            assertBeløpstidslinje(inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje, 1.februar til 31.mars, INNTEKT * 1.1)
        }
    }

    @Test
    fun `periode med arbeidsdag i halen`() {
        a1 {
            nyttVedtak(januar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(onsdag(31.januar), Dagtype.Arbeidsdag)))
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            håndterSøknad(februar)
            assertTrue(inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje.isEmpty())
        }
    }

    @Test
    fun `periode med arbeidsdag i snute`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(torsdag(1.februar), Dagtype.Arbeidsdag)))
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter())
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, refusjon = Refusjon(INGEN, null))
            assertBeløpstidslinje(inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje, januar, INGEN)
            assertBeløpstidslinje(inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje, februar, INNTEKT)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `ny vedtaksperiode`() {
        håndterSøknad(januar)

        assertEquals(Beløpstidslinje(), inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje)
    }

    @Test
    fun `IM før vedtaksperiode`() {
        val tidsstempel = LocalDateTime.now()
        val im = MeldingsreferanseId(håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, mottatt = tidsstempel))
        håndterSøknad(januar)

        val kilde = Kilde(im, ARBEIDSGIVER, tidsstempel)
        assertEquals(Beløpstidslinje.fra(januar, INNTEKT, kilde), inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje)
    }

    @Test
    fun `korrigerte refusjonsopplysninger i AvventerVilkårsprøving`() {
        håndterSøknad(januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT)
        nullstillTilstandsendringer()

        val tidsstempelNy = LocalDateTime.now()
        val imNy = MeldingsreferanseId(håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, refusjon = Refusjon(500.daglig, 27.januar), mottatt = tidsstempelNy))
        val kildeNy = Kilde(imNy, ARBEIDSGIVER, tidsstempelNy)

        val forventetTidslinje = Beløpstidslinje.fra(1.januar til 27.januar, 500.daglig, kildeNy) + Beløpstidslinje.fra(28.januar til 31.januar, INGEN, kildeNy)
        assertEquals(forventetTidslinje, inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje)
        assertTilstander(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `korrigerte refusjonsopplysninger i AvventerHistorikk`() {
        håndterSøknad(januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT)
        håndterVilkårsgrunnlag(1.vedtaksperiode)

        nullstillTilstandsendringer()

        val tidsstempelNy = LocalDateTime.now()
        val imNy = MeldingsreferanseId(håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, refusjon = Refusjon(500.daglig, 27.januar), mottatt = tidsstempelNy))
        val kildeNy = Kilde(imNy, ARBEIDSGIVER, tidsstempelNy)

        val forventetTidslinje = Beløpstidslinje.fra(1.januar til 27.januar, 500.daglig, kildeNy) + Beløpstidslinje.fra(28.januar til 31.januar, INGEN, kildeNy)
        assertEquals(forventetTidslinje, inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje)
        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `korrigerte refusjonsopplysninger i AvventerSimulering`() {
        håndterSøknad(januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)

        nullstillTilstandsendringer()

        val tidsstempelNy = LocalDateTime.now()
        val imNy = MeldingsreferanseId(håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, refusjon = Refusjon(500.daglig, 27.januar), mottatt = tidsstempelNy))
        val kildeNy = Kilde(imNy, ARBEIDSGIVER, tidsstempelNy)

        val forventetTidslinje = Beløpstidslinje.fra(1.januar til 27.januar, 500.daglig, kildeNy) + Beløpstidslinje.fra(28.januar til 31.januar, INGEN, kildeNy)
        assertEquals(forventetTidslinje, inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje)
        assertTilstander(1.vedtaksperiode, AVVENTER_SIMULERING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `korrigerte refusjonsopplysninger i AvventerGodkjenning`() {
        håndterSøknad(januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val tidsstempelNy = LocalDateTime.now()
        val imNy = MeldingsreferanseId(håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, refusjon = Refusjon(500.daglig, 27.januar), mottatt = tidsstempelNy))
        val kildeNy = Kilde(imNy, ARBEIDSGIVER, tidsstempelNy)

        val forventetTidslinje = Beløpstidslinje.fra(1.januar til 27.januar, 500.daglig, kildeNy) + Beløpstidslinje.fra(28.januar til 31.januar, INGEN, kildeNy)
        assertEquals(forventetTidslinje, inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `korrigerte refusjonsopplysninger i TilUtbetaling`() {
        håndterSøknad(januar)
        val tidsstempelGammel = LocalDateTime.now()
        val imGammel = MeldingsreferanseId(håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, mottatt = tidsstempelGammel))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        nullstillTilstandsendringer()

        val tidsstempelNy = LocalDateTime.now()
        val imNy = MeldingsreferanseId(håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, refusjon = Refusjon(500.daglig, 27.januar), mottatt = tidsstempelNy))
        val kildeGammel = Kilde(imGammel, ARBEIDSGIVER, tidsstempelGammel)
        val kildeNy = Kilde(imNy, ARBEIDSGIVER, tidsstempelNy)

        val inspektør = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør
        assertEquals(2, inspektør.behandlinger.size)

        inspektør.behandlinger[0].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.januar til 31.januar, INNTEKT, kildeGammel)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        inspektør.behandlinger[1].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.januar til 27.januar, 500.daglig, kildeNy) + Beløpstidslinje.fra(28.januar til 31.januar, INGEN, kildeNy)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }

        assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING)
    }

    @Test
    fun `korrigerte refusjonsopplysninger i Avsluttet`() {
        val tidsstempelGammel = LocalDateTime.now()
        val imGammel = nyttVedtak(januar, tidsstempelGammel)
        val kildeGammel = Kilde(imGammel, ARBEIDSGIVER, tidsstempelGammel)
        nullstillTilstandsendringer()

        val tidsstempelNy = LocalDateTime.now()
        val imNy = MeldingsreferanseId(håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, refusjon = Refusjon(500.daglig, 27.januar), mottatt = tidsstempelNy))
        val kildeNy = Kilde(imNy, ARBEIDSGIVER, tidsstempelNy)

        val inspektør = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør
        assertEquals(2, inspektør.behandlinger.size)

        inspektør.behandlinger[0].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.januar til 31.januar, INNTEKT, kildeGammel)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        inspektør.behandlinger[1].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.januar til 27.januar, 500.daglig, kildeNy) + Beløpstidslinje.fra(28.januar til 31.januar, INGEN, kildeNy)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `uendret refusjonsopplysninger i Avsluttet - arbeidsgiverperiode utført tidligere`() {
        val tidsstempelEldst = LocalDateTime.now().minusDays(10)
        val eldstId = nyttVedtak(januar, tidsstempelEldst)
        val kildeEldst = Kilde(eldstId, ARBEIDSGIVER, tidsstempelEldst)

        val tidsstempelGammel = LocalDateTime.now().minusDays(1)
        val gammelId = nyttVedtak(10.februar til 28.februar, tidsstempelGammel, vedtaksperiode = 2, arbeidsgiverperiode = listOf(1.januar til 16.januar))
        val kildeGammel = Kilde(gammelId, ARBEIDSGIVER, tidsstempelGammel)

        nullstillTilstandsendringer()

        inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.also { inspektør ->
            assertEquals(1, inspektør.behandlinger.size)
            inspektør.behandlinger[0].also {
                val forventetTidslinje = Beløpstidslinje.fra(januar, INNTEKT, kildeEldst)
                assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
            }
        }
        inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.also { inspektør ->
            assertEquals(1, inspektør.behandlinger.size)

            inspektør.behandlinger[0].also {
                val forventetTidslinje = Beløpstidslinje.fra(10.februar til 28.februar, INNTEKT, kildeGammel)
                assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
            }
        }
        assertTilstander(2.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `korrigerte refusjonsopplysninger i AvventerHistorikkRevurdering`() {
        val tidsstempelGammel = LocalDateTime.now()
        val imGammel = nyttVedtak(januar, tidsstempelGammel)
        val kildeGammel = Kilde(imGammel, ARBEIDSGIVER, tidsstempelGammel)
        // Trigg en revurdering
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, mottatt = LocalDateTime.now())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        nullstillTilstandsendringer()

        val tidsstempelNy = LocalDateTime.now()
        val imNy = MeldingsreferanseId(håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, refusjon = Refusjon(500.daglig, 27.januar), mottatt = tidsstempelNy))
        val kildeNy = Kilde(imNy, ARBEIDSGIVER, tidsstempelNy)

        val inspektør = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør
        assertEquals(2, inspektør.behandlinger.size)

        inspektør.behandlinger[0].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.januar til 31.januar, INNTEKT, kildeGammel)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        inspektør.behandlinger[1].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.januar til 27.januar, 500.daglig, kildeNy) + Beløpstidslinje.fra(28.januar til 31.januar, INGEN, kildeNy)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `korrigerte refusjonsopplysninger i AvventerSimuleringRevurdering`() {
        val tidsstempelGammel = LocalDateTime.now()
        val imGammel = nyttVedtak(januar, tidsstempelGammel)
        val kildeGammel = Kilde(imGammel, ARBEIDSGIVER, tidsstempelGammel)
        // Trigg en revurdering
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT * 1.1, mottatt = LocalDateTime.now())
        assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        håndterYtelser(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
        nullstillTilstandsendringer()

        val tidsstempelNy = LocalDateTime.now()
        val imNy = MeldingsreferanseId(håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, refusjon = Refusjon(500.daglig, 27.januar), mottatt = tidsstempelNy))
        val kildeNy = Kilde(imNy, ARBEIDSGIVER, tidsstempelNy)

        val inspektør = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør
        assertEquals(2, inspektør.behandlinger.size)

        inspektør.behandlinger[0].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.januar til 31.januar, INNTEKT, kildeGammel)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        inspektør.behandlinger[1].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.januar til 27.januar, 500.daglig, kildeNy) + Beløpstidslinje.fra(28.januar til 31.januar, INGEN, kildeNy)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        assertTilstander(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `korrigerte refusjonsopplysninger i AvventerGodkjenningRevurdering`() {
        val tidsstempelGammel = LocalDateTime.now()
        val imGammel = nyttVedtak(januar, tidsstempelGammel)
        val kildeGammel = Kilde(imGammel, ARBEIDSGIVER, tidsstempelGammel)
        // Trigg en revurdering
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, mottatt = LocalDateTime.now())
        håndterYtelser(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        nullstillTilstandsendringer()

        val tidsstempelNy = LocalDateTime.now()
        val imNy = MeldingsreferanseId(håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, refusjon = Refusjon(500.daglig, 27.januar), mottatt = tidsstempelNy))
        val kildeNy = Kilde(imNy, ARBEIDSGIVER, tidsstempelNy)

        val inspektør = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør
        assertEquals(2, inspektør.behandlinger.size)

        inspektør.behandlinger[0].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.januar til 31.januar, INNTEKT, kildeGammel)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        inspektør.behandlinger[1].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.januar til 27.januar, 500.daglig, kildeNy) + Beløpstidslinje.fra(28.januar til 31.januar, INGEN, kildeNy)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `korrigerte refusjonsopplysninger i AvventerRevurdering`() {
        nyttVedtak(januar, tidsstempel = LocalDateTime.now())

        val tidsstempelGammel = LocalDateTime.now()
        val imGammel = nyttVedtak(mars, tidsstempel = tidsstempelGammel, vedtaksperiode = 2)
        val kildeGammel = Kilde(imGammel, ARBEIDSGIVER, tidsstempelGammel)

        // Trigger en revurdering
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, mottatt = LocalDateTime.now())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        nullstillTilstandsendringer()

        val tidsstempelNy = LocalDateTime.now()
        val imNy = MeldingsreferanseId(håndterInntektsmelding(listOf(1.mars til 16.mars), INNTEKT, refusjon = Refusjon(500.daglig, 27.mars), mottatt = tidsstempelNy))
        val kildeNy = Kilde(imNy, ARBEIDSGIVER, tidsstempelNy)

        val inspektør = inspektør.vedtaksperioder(2.vedtaksperiode).inspektør
        assertEquals(2, inspektør.behandlinger.size)

        inspektør.behandlinger[0].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.mars til 31.mars, INNTEKT, kildeGammel)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        inspektør.behandlinger[1].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.mars til 27.mars, 500.daglig, kildeNy) + Beløpstidslinje.fra(28.mars til 31.mars, INGEN, kildeNy)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
    }

    @Test
    fun `Forlengelser bruker refusjonsopplysninger fra perioden før`() {
        a1 {
            val tidsstempel = LocalDateTime.now()
            val im = nyttVedtak(januar, tidsstempel)
            val kilde = Kilde(im, ARBEIDSGIVER, tidsstempel)
            forlengVedtak(februar)

            val refusjonstidslinjeVedtaksperiode1 = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.single().endringer.last().refusjonstidslinje
            val refusjonstidslinjeVedtaksperiode2 = inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.behandlinger.single().endringer.last().refusjonstidslinje

            assertEquals(Beløpstidslinje.fra(januar, INNTEKT, kilde), refusjonstidslinjeVedtaksperiode1)
            assertEquals(Beløpstidslinje.fra(februar, INNTEKT, kilde), refusjonstidslinjeVedtaksperiode2)
        }
    }

    @Test
    fun `Må kunne videreføre refusjonsopplysninger når det kommer søknad som bridger gap'et som før var der`() {
        a1 {
            nyttVedtak(mars)
            nyPeriode(januar)
            nyPeriode(februar)
            assertVarsel(Varselkode.RV_IV_7, 1.vedtaksperiode.filter())

            assertTrue(inspektør.vedtaksperioder(3.vedtaksperiode).refusjonstidslinje.isNotEmpty())
            assertTrue(inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje.isNotEmpty())
        }
    }

    @Test
    fun `Må kunne videreføre refusjonsopplysninger når flere perioder bridger gap'et som før var der`() {
        a1 {
            nyttVedtak(april)
            nyPeriode(januar)
            nyPeriode(februar)
            nyPeriode(mars)

            val vedtaksperiodeJanuar = 2.vedtaksperiode
            val vedtaksperiodeFebruar = 3.vedtaksperiode

            // Vi har egentlig refusjonsopplysninger for alle periodene, men for out-of-order-perioder får de ikke
            // refusjonsopplysninger før det er dens tur til å gå videre
            assertTrue(inspektør.vedtaksperioder(januar).refusjonstidslinje.isNotEmpty())
            assertTrue(inspektør.vedtaksperioder(februar).refusjonstidslinje.isEmpty())
            assertTrue(inspektør.vedtaksperioder(mars).refusjonstidslinje.isNotEmpty())
            assertTrue(inspektør.vedtaksperioder(april).refusjonstidslinje.isNotEmpty())

            håndterVilkårsgrunnlag(vedtaksperiodeJanuar)
            håndterYtelser(vedtaksperiodeJanuar)
            håndterSimulering(vedtaksperiodeJanuar)
            håndterUtbetalingsgodkjenning(vedtaksperiodeJanuar)
            håndterUtbetalt()

            assertVarsel(Varselkode.RV_IV_7, 1.vedtaksperiode.filter())

            assertSisteTilstand(vedtaksperiodeFebruar, AVVENTER_HISTORIKK)
            assertTrue(inspektør.vedtaksperioder(vedtaksperiodeFebruar).refusjonstidslinje.isNotEmpty())

        }
    }

    @Test
    fun `Må kunne tilbakeføre refusjonsopplysninger når perioden etter deg har refusjonsopplysninger`() {
        a1 {
            nyttVedtak(februar)
            nyPeriode(januar)
            assertVarsel(Varselkode.RV_IV_7, 1.vedtaksperiode.filter())
            assertTrue(inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje.isNotEmpty())
        }
    }

    @Test
    fun `Refusjonsopplysninger på en periode som kiler seg midt mellom to strekker opplysningene fra perioden før seg`() {
        a1 {
            nyttVedtak(januar, beregnetInntekt = INNTEKT)
            nyttVedtak(mars, beregnetInntekt = INNTEKT * 1.1)
            nyPeriode(februar)

            assertTrue(inspektør.vedtaksperioder(3.vedtaksperiode).refusjonstidslinje.all { it.beløp == INNTEKT })
        }
    }

    @Test
    fun `Forlengelser bruker refusjonsopplysninger fra perioden før, men må hensynta eventuell opphør av refusjon i refusjonshistorikken`() {
        a1 {
            val tidsstempel = LocalDateTime.now()
            val im = nyttVedtak(januar, tidsstempel, opphørAvRefusjon = 31.januar)
            val kilde = Kilde(im, ARBEIDSGIVER, tidsstempel)
            forlengVedtak(februar)

            val refusjonstidslinjeVedtaksperiode1 = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.single().endringer.last().refusjonstidslinje
            val refusjonstidslinjeVedtaksperiode2 = inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.behandlinger.single().endringer.last().refusjonstidslinje

            assertEquals(Beløpstidslinje.fra(januar, INNTEKT, kilde), refusjonstidslinjeVedtaksperiode1)
            assertEquals(Beløpstidslinje.fra(februar, INGEN, kilde), refusjonstidslinjeVedtaksperiode2)

        }
    }

    @Test
    fun `Forlengelser bruker refusjonsopplysninger fra perioden før, men må hensynta eventuelle endringer i refusjonshistorikken`() {
        a1 {
            val tidsstempel = LocalDateTime.now()
            val im = nyttVedtak(
                januar, tidsstempel, endringerIRefusjon = listOf(
                Refusjon.EndringIRefusjon(INNTEKT * 0.8, 1.februar),
                Refusjon.EndringIRefusjon(INNTEKT * 0.5, 20.februar)
            )
            )
            val kilde = Kilde(im, ARBEIDSGIVER, tidsstempel)
            forlengVedtak(februar)

            val refusjonstidslinjeVedtaksperiode1 = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.single().endringer.last().refusjonstidslinje
            val refusjonstidslinjeVedtaksperiode2 = inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.behandlinger.single().endringer.last().refusjonstidslinje

            assertEquals(Beløpstidslinje.fra(januar, INNTEKT, kilde), refusjonstidslinjeVedtaksperiode1)
            assertEquals(Beløpstidslinje.fra(1.februar til 19.februar, INNTEKT * 0.8, kilde) + Beløpstidslinje.fra(20.februar til 28.februar, INNTEKT * 0.5, kilde), refusjonstidslinjeVedtaksperiode2)
        }
    }

    @Test
    fun `Saksbehandler overstyrer refusjon`() {
        a1 {
            val tidsstempel = LocalDateTime.now()
            val im = nyttVedtak(januar, tidsstempel)

            val refusjonstidslinje = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.single().endringer.last().refusjonstidslinje
            val kilde = Kilde(im, ARBEIDSGIVER, tidsstempel)

            assertEquals(Beløpstidslinje.fra(januar, INNTEKT, kilde), refusjonstidslinje)

            val tidsstempel2 = LocalDateTime.now()
            val saksbehandlerOverstyring = håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                overstyringer = listOf(
                    OverstyrtArbeidsgiveropplysning(
                        orgnummer = a1,
                        inntekt = INNTEKT,
                        refusjonsopplysninger = listOf(Triple(1.januar, null, INGEN))
                    )
                ),
                tidsstempel = tidsstempel2
            )

            val refusjonstidslinje2 = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.last().endringer.last().refusjonstidslinje
            val kildeSaksbehandler = Kilde(saksbehandlerOverstyring.metadata.meldingsreferanseId, SAKSBEHANDLER, tidsstempel2)

            assertEquals(Beløpstidslinje.fra(januar, INGEN, kildeSaksbehandler), refusjonstidslinje2)
        }
    }

    @Test
    fun `Saksbehandler overstyrer refusjon på tidligere skjæringstidspunkt`() {
        a1 {
            nyttVedtak(januar)
            val tidsstempel = LocalDateTime.now()
            val im = nyttVedtak(mars, tidsstempel, vedtaksperiode = 2)
            val kildeIm = Kilde(im, ARBEIDSGIVER, tidsstempel)
            val tidsstempel2 = LocalDateTime.now()
            val saksbehandlerOverstyring = håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                overstyringer = listOf(
                    OverstyrtArbeidsgiveropplysning(
                        orgnummer = a1,
                        inntekt = INNTEKT,
                        refusjonsopplysninger = listOf(Triple(1.januar, null, INGEN))
                    )
                ),
                tidsstempel = tidsstempel2
            )
            val kildeSaksbehandler = Kilde(saksbehandlerOverstyring.metadata.meldingsreferanseId, SAKSBEHANDLER, tidsstempel2)
            val refusjonstidslinje1 = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.last().endringer.last().refusjonstidslinje
            val refusjonstidslinje2 = inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.behandlinger.last().endringer.last().refusjonstidslinje
            assertEquals(Beløpstidslinje.fra(januar, INGEN, kildeSaksbehandler), refusjonstidslinje1)
            assertEquals(Beløpstidslinje.fra(mars, INNTEKT, kildeIm), refusjonstidslinje2)
        }
    }

    @Test
    fun `saksbehandler overstyrer litt ulik refusjon`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            val tidsstempel = LocalDateTime.now()
            val overstyring = håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                overstyringer = listOf(
                    OverstyrtArbeidsgiveropplysning(
                        orgnummer = a1,
                        inntekt = INNTEKT,
                        refusjonsopplysninger = listOf(Triple(1.januar, 31.januar, INGEN), Triple(1.februar, null, INNTEKT / 2))
                    )
                ),
                tidsstempel = tidsstempel
            )
            val kildeSaksbehandler = Kilde(overstyring.metadata.meldingsreferanseId, SAKSBEHANDLER, tidsstempel)
            val refusjonstidslinjeJanuar = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.last().endringer.last().refusjonstidslinje
            val refusjonstidslinjeFebruar = inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.behandlinger.last().endringer.last().refusjonstidslinje

            assertEquals(Beløpstidslinje.fra(januar, INGEN, kildeSaksbehandler), refusjonstidslinjeJanuar)
            assertEquals(Beløpstidslinje.fra(februar, INNTEKT / 2, kildeSaksbehandler), refusjonstidslinjeFebruar)
        }
    }

    @Test
    fun `saksbehandler overstyrer litt ulik refusjon inn i en enkelt vedtaksperiode`() {
        a1 {
            nyttVedtak(januar)
            val tidsstempel = LocalDateTime.now()
            val overstyring = håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                overstyringer = listOf(
                    OverstyrtArbeidsgiveropplysning(
                        orgnummer = a1,
                        inntekt = INNTEKT,
                        refusjonsopplysninger = listOf(Triple(1.januar, 15.januar, INGEN), Triple(16.januar, null, INNTEKT / 2))
                    )
                ),
                tidsstempel = tidsstempel
            )
            val kildeSaksbehandler = Kilde(overstyring.metadata.meldingsreferanseId, SAKSBEHANDLER, tidsstempel)
            val refusjonstidslinjeJanuar = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.last().endringer.last().refusjonstidslinje
            val expected = Beløpstidslinje.fra(1.januar til 15.januar, INGEN, kildeSaksbehandler) + Beløpstidslinje.fra(16.januar til 31.januar, INNTEKT / 2, kildeSaksbehandler)
            assertEquals(expected, refusjonstidslinjeJanuar)
        }
    }

    @Test
    fun `saksbehandler overstyrer refusjon på flere arbeidsgivere`() {
        (a1 og a2).nyeVedtak(januar)
        (a1 og a2).forlengVedtak(februar)
        val tidsstempel = LocalDateTime.now()
        val meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID())
        val kildeSaksbehandler = Kilde(meldingsreferanseId, SAKSBEHANDLER, tidsstempel)
        a1 {
            håndterOverstyrArbeidsgiveropplysninger(
                hendelseId = meldingsreferanseId.id,
                skjæringstidspunkt = 1.januar,
                overstyringer = listOf(
                    OverstyrtArbeidsgiveropplysning(
                        orgnummer = a1,
                        inntekt = INNTEKT,
                        refusjonsopplysninger = listOf(
                            Triple(1.januar, 31.januar, INGEN), Triple(1.februar, null, INNTEKT / 2),
                        )
                    ),
                    OverstyrtArbeidsgiveropplysning(
                        orgnummer = a2,
                        inntekt = INNTEKT,
                        refusjonsopplysninger = listOf(Triple(1.januar, 31.januar, INGEN), Triple(1.februar, null, INNTEKT / 2))
                    )
                ),
                tidsstempel = tidsstempel
            )
            val refusjonstidslinjeJanuar = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.last().endringer.last().refusjonstidslinje
            val refusjonstidslinjeFebruar = inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.behandlinger.last().endringer.last().refusjonstidslinje

            assertEquals(Beløpstidslinje.fra(1.januar til 31.januar, INGEN, kildeSaksbehandler), refusjonstidslinjeJanuar)
            assertEquals(Beløpstidslinje.fra(1.februar til 28.februar, INNTEKT / 2, kildeSaksbehandler), refusjonstidslinjeFebruar)
        }
        a2 {
            val refusjonstidslinjeJanuar = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.last().endringer.last().refusjonstidslinje
            val refusjonstidslinjeFebruar = inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.behandlinger.last().endringer.last().refusjonstidslinje

            assertEquals(Beløpstidslinje.fra(1.januar til 31.januar, INGEN, kildeSaksbehandler), refusjonstidslinjeJanuar)
            assertEquals(Beløpstidslinje.fra(1.februar til 28.februar, INNTEKT / 2, kildeSaksbehandler), refusjonstidslinjeFebruar)
        }
    }

    @Test
    fun `Saksbehandler endrer refusjon for førstegangsbehandlingen, og vi feilaktig bruker disse når forlengelsen kommer`() {
        a1 {
            nyttVedtak(januar)
            håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                overstyringer = listOf(
                    OverstyrtArbeidsgiveropplysning(
                        orgnummer = a1,
                        inntekt = INNTEKT,
                        refusjonsopplysninger = listOf(
                            Triple(1.januar, 31.januar, INNTEKT / 2),
                            Triple(1.februar, null, INNTEKT)
                        )
                    )
                )
            )
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterSøknad(februar)

            assertInfo("Refusjonsservitøren har rester for 01-01-2018 etter servering: 01-02-2018 til 01-02-2018", AktivitetsloggFilter.arbeidsgiver(a1))

            assertEquals(
                INNTEKT,
                inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje[1.februar].beløp
            )
        }
    }

    @Test
    fun `Inntektsmelding kommer før søknad`() {
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar), refusjon = Refusjon(INNTEKT, 31.mars, emptyList()))
            assertInfo("Refusjonsservitøren har rester for 01-01-2018 etter servering: 01-01-2018 til 01-04-2018", AktivitetsloggFilter.arbeidsgiver(a1))
        }
    }

    @Test
    fun `Endrer refusjon i en lukket periode`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                overstyringer = listOf(
                    OverstyrtArbeidsgiveropplysning(
                        orgnummer = a1,
                        inntekt = INNTEKT,
                        refusjonsopplysninger = listOf(Triple(1.januar, 31.januar, INNTEKT / 2))
                    )
                )
            )
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            assertEquals(INNTEKT, inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje[1.februar].beløp)
        }
    }

    @Test
    fun `Endrer refusjon midt i en periode`() {
        a1 {
            nyttVedtak(januar)
            assertEquals(INNTEKT, inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje[19.januar].beløp)
            assertEquals(INNTEKT, inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje[20.januar].beløp)
            assertEquals(INNTEKT, inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje[21.januar].beløp)
            håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                overstyringer = listOf(
                    OverstyrtArbeidsgiveropplysning(
                        orgnummer = a1,
                        inntekt = INNTEKT,
                        refusjonsopplysninger = listOf(Triple(20.januar, 20.januar, INNTEKT / 2))
                    )
                )
            )
            håndterYtelser(1.vedtaksperiode)
            assertEquals(INNTEKT, inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje[19.januar].beløp)
            assertEquals(INNTEKT / 2, inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje[20.januar].beløp)
            assertEquals(INNTEKT, inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje[21.januar].beløp)
        }
    }

    @Test
    fun `Korrigerene IM som overstyrer deler av refusjonshistorikken fjerner ikke refusjonshistorikk den ikke vet noe om`() {
        nyttVedtak(januar)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 20.januar,
            refusjon = Refusjon(INNTEKT / 2, null),
            beregnetInntekt = INNTEKT
        )
        assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        assertEquals(INNTEKT, inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje[19.januar].beløp)
        assertEquals(INNTEKT / 2, inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje[20.januar].beløp)
    }

    private fun nyttVedtak(
        periode: Periode,
        tidsstempel: LocalDateTime,
        vedtaksperiode: Int = 1,
        arbeidsgiverperiode: List<Periode> = listOf(periode.start til periode.start.plusDays(15)),
        opphørAvRefusjon: LocalDate? = null,
        endringerIRefusjon: List<Refusjon.EndringIRefusjon> = emptyList()
    ): MeldingsreferanseId {
        håndterSøknad(periode)
        val im = håndterInntektsmelding(
            arbeidsgiverperiode,
            INNTEKT,
            førsteFraværsdag = periode.start,
            mottatt = tidsstempel,
            refusjon = Refusjon(INNTEKT, opphørsdato = opphørAvRefusjon, endringerIRefusjon = endringerIRefusjon)
        )
        håndterVilkårsgrunnlag(vedtaksperiode.vedtaksperiode)
        håndterYtelser(vedtaksperiode.vedtaksperiode)
        håndterSimulering(vedtaksperiode.vedtaksperiode)
        håndterUtbetalingsgodkjenning(vedtaksperiode.vedtaksperiode)
        håndterUtbetalt()
        return MeldingsreferanseId(im)
    }

    private fun gjenopprettBeløpstislinjeFor(dato: LocalDate) = dto().tilPersonData().arbeidsgivere.single().ubrukteRefusjonsopplysninger[dato]?.tilDto()?.let { Beløpstidslinje.gjenopprett(it) } ?: Beløpstidslinje()
}
