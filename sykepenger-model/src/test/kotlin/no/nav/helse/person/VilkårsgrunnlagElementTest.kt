package no.nav.helse.person

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.januar
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.sykepengegrunnlag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosent
import no.nav.helse.økonomi.Prosent.Companion.prosent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VilkårsgrunnlagElementTest {

    @Test
    fun `infotrygd har ikke avviksjekk`() {
        val element = infotrygdgrunnlag()
        assertTrue(element.sjekkAvviksprosent(Aktivitetslogg()))
    }

    @Test
    fun `null avvik er ok`() {
        val element = grunnlagsdata(avviksprosent = null)
        assertTrue(element.sjekkAvviksprosent(Aktivitetslogg()))
    }

    @Test
    fun `litt avvik er lov`() {
        val element = grunnlagsdata(avviksprosent = Prosent.ratio(Prosent.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.ratio() - 0.0001))
        assertTrue(element.sjekkAvviksprosent(Aktivitetslogg()))
    }

    @Test
    fun `avvik er ikke lov`() {
        val element = grunnlagsdata(avviksprosent = 26.prosent)
        assertFalse(element.sjekkAvviksprosent(Aktivitetslogg()))
    }

    @Test
    fun `for mye avvik er ikke lov`() {
        val element = grunnlagsdata(avviksprosent = Prosent.ratio(Prosent.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.ratio() + 0.0001))
        assertFalse(element.sjekkAvviksprosent(Aktivitetslogg()))
    }

    private fun grunnlagsdata(avviksprosent: Prosent? = null): VilkårsgrunnlagHistorikk.Grunnlagsdata {
        return VilkårsgrunnlagHistorikk.Grunnlagsdata(
            skjæringstidspunkt = 1.januar,
            sykepengegrunnlag = 1000.daglig.sykepengegrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag(1000.daglig),
            avviksprosent = avviksprosent,
            opptjening = Opptjening.opptjening(emptyList(), 1.januar, MaskinellJurist()),
            medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            vurdertOk = true,
            meldingsreferanseId = UUID.randomUUID(),
            vilkårsgrunnlagId = UUID.randomUUID()
        )
    }

    private fun infotrygdgrunnlag() =
        VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag(1.januar, 1000.daglig.sykepengegrunnlag)

    private fun sammenligningsgrunnlag(inntekt: Inntekt) = Sammenligningsgrunnlag(
        arbeidsgiverInntektsopplysninger = listOf(
            ArbeidsgiverInntektsopplysning("orgnummer",
                Inntektshistorikk.SkattComposite(UUID.randomUUID(), (0 until 12).map {
                    Inntektshistorikk.Skatt.Sammenligningsgrunnlag(
                        dato = LocalDate.now(),
                        hendelseId = UUID.randomUUID(),
                        beløp = inntekt,
                        måned = YearMonth.of(2017, it + 1),
                        type = Inntektshistorikk.Skatt.Inntekttype.LØNNSINNTEKT,
                        fordel = "fordel",
                        beskrivelse = "beskrivelse"
                    )
                })
            )),
    )
}
