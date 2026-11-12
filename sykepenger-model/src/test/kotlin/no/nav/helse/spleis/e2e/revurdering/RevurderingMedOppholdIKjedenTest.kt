package no.nav.helse.spleis.e2e.revurdering

import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.selvstendig
import no.nav.helse.dsl.nyttSelvstendigVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.i
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.oktober
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_ANNULLERING
import no.nav.helse.september
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Test

internal class RevurderingMedOppholdIKjedenTest : AbstractDslTest() {
    @Test
    fun `overstyrt tidslinje i gammelt tilfelle revurderer ikke tilfelle mer enn 26 uker senere`() {
        a1 {
            nyttVedtak(januar.i(2017))
            nyttVedtak(januar.i(2018))
            håndterOverstyrTidslinje(listOf(manuellFeriedag(31.januar(2017))))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `overstyrt inntekt i gammelt tilfelle revurderer ikke tilfelle mer enn 26 uker senere`() {
        a1 {
            nyttVedtak(januar.i(2017))
            nyttVedtak(januar.i(2018))
            håndterOverstyrInntekt(1.januar(2017), 500000.årlig)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `annullering av gammelt tilfelle revurderer ikke tilfelle mer enn 26 uker senere`() {
        a1 {
            nyttVedtak(januar.i(2017))
            nyttVedtak(januar.i(2018))
            håndterAnnullering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, TIL_ANNULLERING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `infotrygdutbetaling i gapet revurderer det senere tilfellet`() {
        a1 {
            nyttVedtak(januar.i(2017))
            nyttVedtak(januar.i(2018))
            håndterUtbetalingshistorikkEtterInfotrygdendring(
                listOf(ArbeidsgiverUtbetalingsperiode(a1, 1.juli(2017), 31.juli(2017))),
            )
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `fjernet infotrygdutbetaling som bandt tilfellene sammen revurderer det senere tilfellet`() {
        a1 {
            nyttVedtak(januar.i(2017))
            håndterUtbetalingshistorikkEtterInfotrygdendring(
                listOf(ArbeidsgiverUtbetalingsperiode(a1, 1.juli(2017), 31.juli(2017))),
            )
            nyttVedtak(januar.i(2018))
            håndterUtbetalingshistorikkEtterInfotrygdendring(emptyList())
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `opphold på 181 dager bryter ikke kjeden`() {
        a1 {
            nyttVedtak(1.juli(2017) til 31.juli(2017))
            håndterUtbetalingshistorikkEtterInfotrygdendring(
                listOf(ArbeidsgiverUtbetalingsperiode(a1, 1.desember(2016), 31.desember(2016))),
            )
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `opphold på 182 dager bryter kjeden`() {
        a1 {
            nyttVedtak(2.juli(2017) til 31.juli(2017))
            håndterUtbetalingshistorikkEtterInfotrygdendring(
                listOf(ArbeidsgiverUtbetalingsperiode(a1, 1.desember(2016), 31.desember(2016))),
            )
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertInfo(
                "Revurderer ikke perioden fordi det er minst 26 uker uten sykdom mellom endringen og perioden",
                1.vedtaksperiode.filter(),
            )
        }
    }

    @Test
    fun `sammenhengende tilfelle med avslåtte dager etter maksdato revurderes i sin helhet`() {
        medMaksSykedager(10)
        a1 {
            nyttVedtak(januar.i(2017))
            listOf(februar, mars, april, mai, juni, juli).forEach { måned ->
                forlengVedtakUtenUtbetaling(måned.i(2017))
            }
            håndterOverstyrTidslinje(listOf(manuellFeriedag(31.januar(2017))))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            (2..7).forEach { assertSisteTilstand(it.vedtaksperiode, AVVENTER_REVURDERING) }
        }
    }

    @Test
    fun `kjeden går på tvers av arbeidsgivere`() {
        a1 { nyttVedtak(januar.i(2017)) }
        a2 { nyttVedtak(juni.i(2017)) }
        a1 { nyttVedtak(januar.i(2018)) }
        a1 { håndterOverstyrTidslinje(listOf(manuellFeriedag(31.januar(2017)))) }
        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING) }
    }

    @Test
    fun `periode til godkjenning etter brudd i kjeden går til avventer blokkerende`() {
        a1 {
            nyttVedtak(januar.i(2017))
            tilGodkjenning(januar.i(2018))
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
            håndterOverstyrTidslinje(listOf(manuellFeriedag(31.januar(2017))))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `avsluttet periode etter brudd står i fred mens senere periode til godkjenning venter på revurderingen`() {
        a1 {
            nyttVedtak(15.september(2017) til 10.oktober(2017))
            nyttVedtak(11.april(2018) til 30.april(2018))
            tilGodkjenning(desember.i(2018))
            håndterOverstyrTidslinje(listOf(manuellFeriedag(10.oktober(2017))))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_GODKJENNING)
        }
    }

    @Test
    fun `kjeden går fra selvstendig næringsdrivende til arbeidstaker`() {
        selvstendig { nyttSelvstendigVedtak(januar.i(2017)) }
        a1 { nyttVedtak(juni.i(2017)) }
        selvstendig { håndterOverstyrTidslinje(listOf(manuellArbeidsdag(31.januar(2017)))) }
        selvstendig { assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING) }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING) }
    }

    @Test
    fun `brudd i kjeden fra selvstendig næringsdrivende til arbeidstaker`() {
        selvstendig { nyttSelvstendigVedtak(januar.i(2017)) }
        a1 { nyttVedtak(januar.i(2018)) }
        selvstendig { håndterOverstyrTidslinje(listOf(manuellArbeidsdag(31.januar(2017)))) }
        selvstendig { assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING) }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
    }

    @Test
    fun `kjeden går fra arbeidstaker til selvstendig næringsdrivende`() {
        a1 { nyttVedtak(januar.i(2017)) }
        selvstendig { nyttSelvstendigVedtak(juni.i(2017)) }
        a1 { håndterOverstyrTidslinje(listOf(manuellFeriedag(31.januar(2017)))) }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING) }
        selvstendig { assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING) }
    }

    @Test
    fun `brudd i kjeden fra arbeidstaker til selvstendig næringsdrivende`() {
        a1 { nyttVedtak(januar.i(2017)) }
        selvstendig { nyttSelvstendigVedtak(januar.i(2018)) }
        a1 { håndterOverstyrTidslinje(listOf(manuellFeriedag(31.januar(2017)))) }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING) }
        selvstendig { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
    }

    private fun TestPerson.TestArbeidsgiver.forlengVedtakUtenUtbetaling(periode: Periode) {
        val vedtaksperiode = nyPeriode(periode)
        håndterYtelser(vedtaksperiode)
        håndterUtbetalingsgodkjenning(vedtaksperiode)
    }
}
