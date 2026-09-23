package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import no.nav.helse.april
import no.nav.helse.august
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.hendelser.Arbeidsgiveropplysning
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.til
import no.nav.helse.i
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_10
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_7
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.september
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class FlereArbeidsgivereInntektssituasjonTest : AbstractDslTest() {

    @Test
    fun `finner periodenSomGaOpp når eneste kandidat står i AvventerAvsluttetUtenUtbetaling`() {
        a1 {
            håndterSøknad((27.mars til 26.april) i 2026)
            håndterSøknad((27.april til 27.mai) i 2026)
            håndterSøknad((28.mai til 27.juni) i 2026)
            håndterSøknad((28.juni til 26.juli) i 2026)
            håndterSøknad((27.juli til 17.august) i 2026)
            håndterSykmelding((18.august til 3.september) i 2026)
            håndterArbeidsgiveropplysninger(listOf((27.mars til 11.april) i 2026), vedtaksperiodeId = 1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
        a2 {
            håndterSøknad((3.august til 17.august) i 2026)
            håndterSøknad((18.august til 3.september) i 2026)
            // arbeidsgiver sender aldri inntektsmelding for a2, så vi gir opp å vente på den
            håndterPåminnelse(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, flagg = setOf("ønskerInntektFraAOrdningen"))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE)
        }
        a1 {
            // saksbehandler flytter skjæringstidspunktet ved å endre de fem første dagene til pleiepenger
            håndterOverstyrTidslinje(((27.mars til 31.mars) i 2026).map { ManuellOverskrivingDag(it, Dagtype.Pleiepengerdag) })
            assertEquals(1.april i 2026, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

            assertDoesNotThrow { håndterVilkårsgrunnlag(1.vedtaksperiode) }
            assertVarsel(RV_IV_7, 1.vedtaksperiode.filter())
            assertVarsel(RV_IV_10, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `finner periodenSomGaOpp når a2 har sendt inntektsopplysninger på den nyeste perioden`() {
        a1 {
            håndterSøknad((27.mars til 26.april) i 2026)
            håndterSøknad((27.april til 27.mai) i 2026)
            håndterSøknad((28.mai til 27.juni) i 2026)
            håndterSøknad((28.juni til 26.juli) i 2026)
            håndterSøknad((27.juli til 17.august) i 2026)
            håndterSykmelding((18.august til 3.september) i 2026)
            håndterArbeidsgiveropplysninger(listOf((27.mars til 11.april) i 2026), vedtaksperiodeId = 1.vedtaksperiode)
            // sykepengegrunnlaget settes mens a2 fortsatt er en ghost
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
        }
        a2 {
            håndterSøknad((3.august til 17.august) i 2026)
            håndterSøknad((18.august til 3.september) i 2026)
            // sykepengegrunnlaget er alt satt fra den gang a2 var ghost, så a2 spørres
            // kun om arbeidsgiverperiode og refusjon - ikke inntekt
            håndterArbeidsgiveropplysninger(
                2.vedtaksperiode,
                Arbeidsgiveropplysning.OppgittArbeidgiverperiode(listOf((3.august til 18.august) i 2026)),
                Arbeidsgiveropplysning.OppgittRefusjon(INNTEKT, emptyList(), refusjonskravGyldigFra = null)
            )
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE)
        }
        a1 {
            // saksbehandler flytter skjæringstidspunktet ved å endre de fem første dagene til pleiepenger
            håndterOverstyrTidslinje(((27.mars til 31.mars) i 2026).map { ManuellOverskrivingDag(it, Dagtype.Pleiepengerdag) })
            assertEquals(1.april i 2026, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

            assertDoesNotThrow { håndterVilkårsgrunnlag(1.vedtaksperiode) }
            assertVarsel(RV_IV_7, 1.vedtaksperiode.filter())
            assertVarsel(RV_IV_10, 1.vedtaksperiode.filter())
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        }
    }
}
