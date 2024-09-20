package no.nav.helse.spleis.e2e.inntektsmelding

import java.time.LocalDateTime
import java.time.YearMonth
import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dto.VedtaksperiodetilstandDto
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Test

internal class InntektsmeldingKommerIkkeE2ETest : AbstractDslTest() {

    @Test
    fun `lager ikke påminnelse om vedtaksperioden har ventet mindre enn tre måneder`() = Toggle.InntektsmeldingSomIkkeKommer.enable {
        val nå = LocalDateTime.now()
        val tilstandsendringstidspunkt = nå.minusMonths(3).plusDays(1)
        a1 {
            håndterSøknad(januar)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, tilstandsendringstidspunkt, nå)
            assertIngenBehov(1.vedtaksperiode, Aktivitet.Behov.Behovtype.InntekterForSykepengegrunnlagForArbeidsgiver)
        }
    }

    @Test
    fun `lager påminnelse om vedtaksperioden har ventet mer enn tre måneder`() = Toggle.InntektsmeldingSomIkkeKommer.enable {
        val nå = LocalDateTime.now()
        val tilstandsendringstidspunkt = nå.minusMonths(3)
        a1 {
            håndterSøknad(januar)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, tilstandsendringstidspunkt, nå)
            assertBehov(1.vedtaksperiode, Aktivitet.Behov.Behovtype.InntekterForSykepengegrunnlagForArbeidsgiver)
        }
    }

    @Test
    fun `lagrer skatteinntektene som inntektsmelding og går videre`() = Toggle.InntektsmeldingSomIkkeKommer.enable {
        val inntektFraSkatt = 10000.månedlig
        a1 {
            håndterSøknad(januar)
            håndterSykepengegrunnlagForArbeidsgiver(1.vedtaksperiode, 1.januar, listOf(
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 12), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 11), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 10), inntektFraSkatt, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", "")
            ))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertUtbetalingsbeløp(1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 0,
                forventetPersonbeløp = 462,
                subset = 17.januar til 31.januar
            )
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING
            )
        }
    }
}