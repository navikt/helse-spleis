package no.nav.helse.spleis.e2e.generasjoner

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.august
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.dsl.lagStandardSykepengegrunnlag
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeidsgiverdag
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.VedtaksperiodeInspektør.Generasjon.Generasjonkilde
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.serde.PersonData
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.AVSLUTTET_UTEN_VEDTAK
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.REVURDERT_VEDTAK_AVVIST
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.TIL_INFOTRYGD
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.UBEREGNET_REVURDERING
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.VEDTAK_FATTET
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.VEDTAK_IVERKSATT
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetalingtype
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GenerasjonerE2ETest : AbstractDslTest() {

    @Test
    fun `ny periode har en generasjon`() {
        a1 {
            val søknadId = UUID.randomUUID()
            val registrert = LocalDateTime.now()
            val innsendt = registrert.minusHours(2)
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), søknadId = søknadId, sendtTilNAVEllerArbeidsgiver = innsendt, registrert = registrert)
            inspektør(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(1, generasjoner.size)
                assertEquals(1, generasjoner.single().endringer.size)
                assertEquals(Generasjonkilde(meldingsreferanseId = søknadId, innsendt = innsendt, registert = registrert, avsender = Avsender.SYKMELDT), generasjoner.single().kilde)
            }
        }
    }

    @Test
    fun `korrigerende inntektsmelding`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            val mottatt = LocalDateTime.now()
            val inntektsmeldingId = håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT * 1.1,
                mottatt = mottatt
            )
            inspektør(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(2, generasjoner.size)
                assertEquals(Generasjonkilde(meldingsreferanseId = inntektsmeldingId, innsendt = mottatt, registert = mottatt, avsender = Avsender.ARBEIDSGIVER), generasjoner.last().kilde)
            }
        }
    }

    @Test
    fun `Flere sykefraværstilfeller på flere arbeidsgivere med korrigerende inntektsmelding i snuten`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            nyttVedtak(1.mars, 31.mars)
            assertEquals(1, inspektør(1.vedtaksperiode).generasjoner.size)
            assertEquals(1, inspektør(2.vedtaksperiode).generasjoner.size)
        }
        a2 {
            nyttVedtak(1.mai, 31.mai)
            håndterSøknad(Sykdom(1.juli, 31.juli, 100.prosent))
            assertEquals(1, inspektør(1.vedtaksperiode).generasjoner.size)
            assertEquals(1, inspektør(2.vedtaksperiode).generasjoner.size)
        }

        val korrigerendeImA1 = UUID.randomUUID()
        val mottatt = LocalDateTime.now()
        val forventetKilde = Generasjonkilde(meldingsreferanseId = korrigerendeImA1, innsendt = mottatt, registert = mottatt, avsender = Avsender.ARBEIDSGIVER)

        a1 {
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT * 1.1,
                id = korrigerendeImA1,
                mottatt = mottatt
            )
            inspektør(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(2, generasjoner.size)
                assertEquals(forventetKilde, generasjoner.last().kilde)
            }
            inspektør(2.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(2, generasjoner.size)
                assertEquals(forventetKilde, generasjoner.last().kilde)
            }
        }

        a2 {
            inspektør(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(2, generasjoner.size)
                assertEquals(forventetKilde, generasjoner.last().kilde)
            }
            inspektør(2.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(1, generasjoner.size)
                assertEquals(Avsender.SYKMELDT, generasjoner.first().kilde.avsender)
            }
        }
    }

    @Test
    fun `Saksbehandler må behandle søknad i Infotrygd`() {
        a1 {
            tilGodkjenning(1.januar, 31.januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
            inspektørForkastet(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(1, generasjoner.size)
                assertEquals(Avsender.SYKMELDT, generasjoner.first().kilde.avsender)
            }
        }
    }
    @Test
    fun `annullere iverksatt vedtak`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            håndterAnnullering(inspektør.utbetalinger.single().inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
            inspektørForkastet(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(2, generasjoner.size)
                assertEquals(Avsender.SYKMELDT, generasjoner.first().kilde.avsender)
                assertEquals(Avsender.SAKSBEHANDLER, generasjoner.last().kilde.avsender)
                generasjoner.last().also { sisteGenerasjon ->
                    assertNotNull(sisteGenerasjon.avsluttet)
                    assertEquals(TIL_INFOTRYGD, sisteGenerasjon.tilstand)
                    assertEquals(1, sisteGenerasjon.endringer.size)
                    sisteGenerasjon.endringer.single().also { endring ->
                        assertNotNull(endring.utbetaling)
                        assertNotNull(endring.grunnlagsdata)
                        endring.utbetaling.inspektør.also { annulleringen ->
                            assertEquals(Utbetalingtype.ANNULLERING, annulleringen.type)
                        }
                    }
                }
            }
        }
    }
    @Test
    fun `annullere flere iverksatte vedtak`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            forlengVedtak(1.februar, 28.februar)
            nyttVedtak(10.mars, 31.mars, arbeidsgiverperiode = listOf(10.mars til 25.mars))
            håndterAnnullering(inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
            assertEquals(4, inspektør.utbetalinger.size)
            inspektørForkastet(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(2, generasjoner.size)
                assertEquals(Avsender.SYKMELDT, generasjoner.first().kilde.avsender)
                assertEquals(Avsender.SAKSBEHANDLER, generasjoner.last().kilde.avsender)
                generasjoner.last().also { sisteGenerasjon ->
                    assertNotNull(sisteGenerasjon.avsluttet)
                    assertEquals(TIL_INFOTRYGD, sisteGenerasjon.tilstand)
                    assertEquals(1, sisteGenerasjon.endringer.size)
                    sisteGenerasjon.endringer.single().also { endring ->
                        assertNotNull(endring.utbetaling)
                        assertNotNull(endring.grunnlagsdata)
                        endring.utbetaling.inspektør.also { annulleringen ->
                            assertEquals(Utbetalingtype.ANNULLERING, annulleringen.type)
                        }
                    }
                }
            }
            inspektørForkastet(2.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(2, generasjoner.size)
                assertEquals(Avsender.SYKMELDT, generasjoner.first().kilde.avsender)
                assertEquals(Avsender.SAKSBEHANDLER, generasjoner.last().kilde.avsender)
                generasjoner.last().also { sisteGenerasjon ->
                    assertNotNull(sisteGenerasjon.avsluttet)
                    assertEquals(TIL_INFOTRYGD, sisteGenerasjon.tilstand)
                    assertEquals(1, sisteGenerasjon.endringer.size)
                    sisteGenerasjon.endringer.single().also { endring ->
                        assertNotNull(endring.utbetaling)
                        assertNotNull(endring.grunnlagsdata)
                        endring.utbetaling.inspektør.also { annulleringen ->
                            assertEquals(Utbetalingtype.ANNULLERING, annulleringen.type)
                        }
                    }
                }
            }
            inspektørForkastet(3.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(2, generasjoner.size)
                assertEquals(Avsender.SYKMELDT, generasjoner.first().kilde.avsender)
                assertEquals(Avsender.SAKSBEHANDLER, generasjoner.last().kilde.avsender)
                generasjoner.last().also { sisteGenerasjon ->
                    assertNotNull(sisteGenerasjon.avsluttet)
                    assertEquals(TIL_INFOTRYGD, sisteGenerasjon.tilstand)
                    assertEquals(1, sisteGenerasjon.endringer.size)
                    sisteGenerasjon.endringer.single().also { endring ->
                        assertNotNull(endring.utbetaling)
                        assertNotNull(endring.grunnlagsdata)
                        endring.utbetaling.inspektør.also { annulleringen ->
                            assertEquals(Utbetalingtype.ANNULLERING, annulleringen.type)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `annullere en uberegnet revurdering`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
            håndterAnnullering(inspektør.utbetalinger.single().inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
            inspektørForkastet(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(2, generasjoner.size)
                assertEquals(TIL_INFOTRYGD, generasjoner.last().tilstand)
            }
        }
    }

    @Test
    fun `annullere en beregnet revurdering`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
            håndterYtelser(1.vedtaksperiode)
            håndterAnnullering(inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
            assertEquals(Utbetalingstatus.FORKASTET, inspektør.utbetaling(1).inspektør.tilstand)
            inspektørForkastet(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(2, generasjoner.size)
                assertEquals(TIL_INFOTRYGD, generasjoner.last().tilstand)
            }
        }
    }

    @Test
    fun `avvise en beregnet revurdering`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, reberegning = true)
            assertEquals(Utbetalingstatus.IKKE_GODKJENT, inspektør.utbetaling(1).inspektør.tilstand)
            inspektør(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(3, generasjoner.size)
                assertEquals(VEDTAK_IVERKSATT, generasjoner[0].tilstand)
                assertEquals(REVURDERT_VEDTAK_AVVIST, generasjoner[1].tilstand)
                assertEquals(UBEREGNET_REVURDERING, generasjoner[2].tilstand)
            }
        }
    }

    @Test
    fun `Reberegner en periode`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            håndterPåminnelse(1.vedtaksperiode, AVSLUTTET, reberegning = true)
            inspektør(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(2, generasjoner.size)
                assertEquals(Avsender.SYKMELDT, generasjoner.first().kilde.avsender)
                assertEquals(Avsender.SYSTEM, generasjoner.last().kilde.avsender)
            }
        }
    }

    @Test
    fun `En utbetaling i Infotrygd blander seg`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
            val id = UUID.randomUUID()
            håndterUtbetalingshistorikkEtterInfotrygdendring(utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 5.januar, 100.prosent, INNTEKT)), id = id)
            inspektør(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(2, generasjoner.size)
                assertEquals(Avsender.SYKMELDT, generasjoner.first().kilde.avsender)
                generasjoner.last().let {
                    assertEquals(Avsender.SYSTEM, it.kilde.avsender)
                    assertEquals(id, it.kilde.meldingsreferanseId)
                }
            }
        }
    }

    @Test
    fun `ny periode som starter med egenmeldinger påvirker ikke sykmeldingsperiode`() {
        a1 {
            håndterSøknad(Sykdom(3.januar, 17.januar, 100.prosent), egenmeldinger = listOf(Arbeidsgiverdag(1.januar, 2.januar)))
            inspektør(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(1, generasjoner.size)
                assertEquals(1, generasjoner.single().endringer.size)
                assertEquals(1.januar til 17.januar, generasjoner.single().periode)
            }
        }
    }

    @Test
    fun `korrigert søknad lager ny endring`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
            val søknad2 = UUID.randomUUID()
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), Ferie(19.januar, 20.januar), søknadId = søknad2)
            inspektør(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(1, generasjoner.size)
                assertEquals(2, generasjoner.single().endringer.size)
                assertEquals(Dokumentsporing.søknad(søknad2), generasjoner.single().endringer.last().dokumentsporing)
            }
        }
    }

    @Test
    fun `korrigert tidslinje mens perioden er til godkjenning`() {
        a1 {
            tilGodkjenning(2.januar, 31.januar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(1.januar, Dagtype.Sykedag, 100)))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            inspektør(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(1, generasjoner.size)
                generasjoner[0].also { generasjon ->
                    assertEquals(5, generasjon.endringer.size)
                    assertEquals(PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.BEREGNET, generasjon.tilstand)
                }
            }
        }
    }

    @Test
    fun `korrigert søknad etter fattet vedtak lager ny generasjon`() {
        a1 {
            val søknad1 = UUID.randomUUID()
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), søknadId = søknad1)
            val im = håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            val vedtakFattetTidspunkt = LocalDateTime.now()
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjenttidspunkt = vedtakFattetTidspunkt)
            val søknad2 = UUID.randomUUID()
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), Ferie(19.januar, 20.januar), søknadId = søknad2)
            inspektør(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(2, generasjoner.size)
                generasjoner[0].also { generasjon ->
                    assertEquals(4, generasjon.endringer.size)
                    assertEquals(Dokumentsporing.søknad(søknad1), generasjon.endringer[0].dokumentsporing)
                    assertEquals(Dokumentsporing.inntektsmeldingDager(im), generasjon.endringer[1].dokumentsporing)
                    assertEquals(Dokumentsporing.inntektsmeldingInntekt(im), generasjon.endringer[2].dokumentsporing)
                    assertEquals(Dokumentsporing.inntektsmeldingInntekt(im), generasjon.endringer[3].dokumentsporing)
                    assertEquals(VEDTAK_FATTET, generasjon.tilstand)
                    assertEquals(vedtakFattetTidspunkt, generasjon.vedtakFattet)
                    assertNull(generasjon.avsluttet)
                }
                generasjoner[1].also { generasjon ->
                    assertEquals(1, generasjon.endringer.size)
                    assertEquals(Dokumentsporing.søknad(søknad2), generasjon.endringer.last().dokumentsporing)
                    assertEquals(UBEREGNET_REVURDERING, generasjon.tilstand)
                }
            }
        }
    }

    @Test
    fun `korrigert søknad på tidligere periode, med senere periode til utbetaling, lager ny generasjon`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            tilGodkjenning(1.mars, 31.mars)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterSøknad(Sykdom(1.januar, 31.januar, 90.prosent))

            inspektør(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(2, generasjoner.size)
                generasjoner[0].also { generasjon ->
                    assertEquals(3, generasjon.endringer.size)
                }
                generasjoner[1].also { generasjon ->
                    assertEquals(1, generasjon.endringer.size)
                    assertEquals(UBEREGNET_REVURDERING, generasjon.tilstand)
                }
            }
            inspektør(2.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(2, generasjoner.size)
                generasjoner[0].also { generasjon ->
                    assertEquals(3, generasjon.endringer.size)
                    assertEquals(VEDTAK_FATTET, generasjon.tilstand)
                }
                generasjoner[1].also { generasjon ->
                    assertEquals(1, generasjon.endringer.size)
                    assertEquals(UBEREGNET_REVURDERING, generasjon.tilstand)
                }
            }
        }
    }

    @Test
    fun `korrigert søknad i avsluttet uten utbetaling som ikke medfører omgjøring`() {
        a1 {
            håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent))
            håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent), Ferie(18.januar, 18.januar))
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            inspektør(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(2, generasjoner.size)
                assertEquals(AVSLUTTET_UTEN_VEDTAK, generasjoner[0].tilstand)
                assertEquals(AVSLUTTET_UTEN_VEDTAK, generasjoner[1].tilstand)
            }
        }
    }

    @Test
    fun `overstyr tidslinje i avsluttet uten utbetaling som ikke medfører omgjøring`() {
        a1 {
            håndterSøknad(Sykdom(3.januar, 18.januar, 100.prosent))
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(18.januar, Dagtype.Feriedag)))
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            inspektør(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(2, generasjoner.size)
                assertEquals(AVSLUTTET_UTEN_VEDTAK, generasjoner[0].tilstand)
                assertEquals(AVSLUTTET_UTEN_VEDTAK, generasjoner[1].tilstand)
            }
        }
    }

    @Test
    fun `inntektsmelding med første fraværsdag utenfor sykdom - to tidligere vedtak - inntektsmelding ikke håndtert fordi inntekt håndteres ikke`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            forlengVedtak(1.februar, 28.februar)
            val inntektsmeldingId = håndterInntektsmelding(listOf(1.februar til 16.februar), førsteFraværsdag = 1.mars)
            assertTrue(inntektsmeldingId in observatør.inntektsmeldingIkkeHåndtert)
            assertFalse(inntektsmeldingId in observatør.inntektsmeldingHåndtert.map { it.first })
            inspektør(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(1, generasjoner.size)
            }
            inspektør(2.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(2, generasjoner.size)
                generasjoner.last().also { sisteGenerasjon ->
                    assertEquals(inntektsmeldingId, sisteGenerasjon.kilde.meldingsreferanseId)
                    assertEquals(Dokumentsporing.inntektsmeldingDager(inntektsmeldingId), sisteGenerasjon.endringer.single().dokumentsporing)
                }
            }
        }
    }

    @Test
    fun `delvis overlappende søknad i uberegnet`() {
        a1 {
            val søknad1 = UUID.randomUUID()
            val søknad2 = UUID.randomUUID()
            håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent), søknadId = søknad1)
            håndterSøknad(Sykdom(3.januar, 27.januar, 100.prosent), søknadId = søknad2)
            inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.generasjoner.single().also { generasjon ->
                assertEquals(søknad1, generasjon.kilde.meldingsreferanseId)
                assertEquals(Dokumentsporing.søknad(søknad1), generasjon.endringer.single().dokumentsporing)
            }
            inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.generasjoner.single().also { generasjon ->
                assertEquals(søknad2, generasjon.kilde.meldingsreferanseId)
                assertEquals(Dokumentsporing.søknad(søknad2), generasjon.endringer.single().dokumentsporing)
            }
        }
    }

    @Test
    fun `delvis overlappende søknad i avsluttet uten utbetaling`() {
        a1 {
            håndterSøknad(Sykdom(8.august, 21.august, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(10.august, 31.august))
            val overlappende = UUID.randomUUID()
            håndterSøknad(Sykdom(10.august, 31.august, 100.prosent), søknadId = overlappende)
            assertEquals(8.august til 21.august, inspektør.periode(1.vedtaksperiode))
            assertEquals(10.august til 31.august, inspektør.periode(2.vedtaksperiode))
            assertTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.generasjoner.last().also { generasjon ->
                assertEquals(overlappende, generasjon.kilde.meldingsreferanseId)
                assertEquals(Dokumentsporing.søknad(overlappende), generasjon.endringer.single().dokumentsporing)
            }
            assertFunksjonellFeil(Varselkode.`Mottatt søknad som delvis overlapper`, 1.vedtaksperiode.filter())
            assertForkastetPeriodeTilstander(2.vedtaksperiode, START, TilstandType.TIL_INFOTRYGD)
        }
    }

    @Test
    fun `periode hos ag2 blir innenfor agp mens ag1 har laget utbetaling`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
        }
        a2 {
            håndterSøknad(Sykdom(2.januar, 17.januar, 100.prosent))
        }
        a2 {
            håndterInntektsmelding(emptyList(), førsteFraværsdag = 2.januar, begrunnelseForReduksjonEllerIkkeUtbetalt = "IkkeOpptjening")
        }
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode,
                inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(listOf(a1 to INNTEKT, a2 to INNTEKT), 1.januar),
                arbeidsforhold = listOf(Arbeidsforhold(a1, LocalDate.EPOCH, type = ORDINÆRT), Arbeidsforhold(a2, LocalDate.EPOCH, type = ORDINÆRT))
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
        }
        a2 {
            håndterInntektsmelding(listOf(2.januar til 17.januar))
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            inspektør(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(1, generasjoner.size)
                assertEquals(VEDTAK_IVERKSATT, generasjoner.single().tilstand)
            }
        }
        a2 {
            inspektør(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(2, generasjoner.size)
                assertEquals(AVSLUTTET_UTEN_VEDTAK, generasjoner[0].tilstand)
                assertEquals(AVSLUTTET_UTEN_VEDTAK, generasjoner[1].tilstand)
            }
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
    }
}