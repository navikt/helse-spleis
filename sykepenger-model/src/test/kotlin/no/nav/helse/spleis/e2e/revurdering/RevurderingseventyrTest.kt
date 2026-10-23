package no.nav.helse.spleis.e2e.revurdering

import no.nav.helse.april
import no.nav.helse.dsl.*
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.tilstandsmaskin.TilstandType.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Test

internal class RevurderingseventyrTest : AbstractDslTest() {
    @Test
    fun `happy case revurdering`() =
        a1 {
            nyttVedtak(januar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }

    @Test
    fun `happy case overstyring`() =
        a1 {
            tilGodkjenning(januar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        }

    @Test
    fun skjønnsfastsetting() =
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beregnetInntekt = 60000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            håndterSkjønnsmessigFastsettelse(
                1.januar,
                listOf(
                    OverstyrtArbeidsgiveropplysning(
                        orgnummer = a1,
                        inntekt = 60000.månedlig,
                        refusjonsopplysninger = listOf(Triple(1.januar, null, 31000.månedlig)),
                    ),
                ),
            )

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        }

    @Test
    fun `auu skal utbetales`() =
        a1 {
            håndterSøknad(1.januar til 16.januar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(16.januar, Dagtype.SykedagNav, 100)))

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }

    @Test
    fun `Overstyring av inntekt`() =
        a1 {
            tilGodkjenning(januar)
            håndterOverstyrInntekt(1.januar, 30000.månedlig)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        }

    @Test
    fun `to vedtaksperioder berørt av en revurdering`() =
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }

    @Test
    fun `reberegning av revurdering`() =
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            forlengVedtak(mars)
            håndterSøknad(27.februar til 28.februar)
            nullstillTilstandsendringer()
            håndterPåminnelse(3.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, flagg = setOf("ønskerReberegning"))

            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(3.vedtaksperiode, AVVENTER_REVURDERING)
        }

    @Test
    fun `flere revurderinger`() =
        a1 {
            nyttVedtak(januar)
            håndterSøknad(29.januar til 30.januar)
            håndterOverstyrInntekt(1.januar, 30000.månedlig)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }

    @Test
    fun `flere arbeidsgivere`() {
        (a1 og a2).nyeVedtak(januar)
        a1 {
            håndterOverstyrInntekt(1.januar, 30000.månedlig)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `tidligere skjæringstidspunkt -- revurderer inntekt`() =
        a1 {
            nyttVedtak(januar)
            forlengVedtak(1.februar til 15.februar)
            nyttVedtak(mars, arbeidsgiverperiode = emptyList())
            forlengVedtak(april)

            håndterOverstyrInntekt(1.januar, 30000.månedlig)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(4.vedtaksperiode, AVVENTER_REVURDERING)
        }

    @Test
    fun `tidligere skjæringstidspunkt -- revurderer tidslinje`() =
        a1 {
            nyttVedtak(januar)
            forlengVedtak(1.februar til 15.februar)
            nyttVedtak(mars, arbeidsgiverperiode = emptyList())
            forlengVedtak(april)

            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Dagtype.Feriedag)))

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(4.vedtaksperiode, AVVENTER_REVURDERING)
        }
}
