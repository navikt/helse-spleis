package no.nav.helse.spleis.e2e.generasjoner

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeidsgiverdag
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Generasjoner
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.serde.PersonData
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.AVSLUTTET_UTEN_VEDTAK
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class GenerasjonerE2ETest : AbstractDslTest() {

    @Test
    fun `ny periode har en generasjon`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
            inspektør(1.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(1, generasjoner.size)
                assertEquals(1, generasjoner.single().endringer.size)
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
                    assertEquals(6, generasjon.endringer.size)
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
                    assertEquals(PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.VEDTAK_FATTET, generasjon.tilstand)
                    assertEquals(vedtakFattetTidspunkt, generasjon.vedtakFattet)
                    assertNull(generasjon.avsluttet)
                }
                generasjoner[1].also { generasjon ->
                    assertEquals(1, generasjon.endringer.size)
                    assertEquals(Dokumentsporing.søknad(søknad2), generasjon.endringer.last().dokumentsporing)
                    assertEquals(PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.UBEREGNET_REVURDERING, generasjon.tilstand)
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
                    assertEquals(PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.UBEREGNET_REVURDERING, generasjon.tilstand)
                }
            }
            inspektør(2.vedtaksperiode).generasjoner.also { generasjoner ->
                assertEquals(2, generasjoner.size)
                generasjoner[0].also { generasjon ->
                    assertEquals(3, generasjon.endringer.size)
                    assertEquals(PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.VEDTAK_FATTET, generasjon.tilstand)
                }
                generasjoner[1].also { generasjon ->
                    assertEquals(1, generasjon.endringer.size)
                    assertEquals(PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.UBEREGNET_REVURDERING, generasjon.tilstand)
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
}