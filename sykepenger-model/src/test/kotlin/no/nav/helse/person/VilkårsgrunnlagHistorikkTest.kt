package no.nav.helse.person

import no.nav.helse.hendelser.*
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class VilkårsgrunnlagHistorikkTest {
    @Test
    fun `Finner vilkårsgrunnlag for skjæringstidspunkt`() {

        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val vilkårsgrunnlag = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            aktørId = "AKTØR_ID",
            fødselsnummer = "20043769969",
            orgnummer = "ORGNUMMER",
            inntektsvurdering = Inntektsvurdering(emptyList()),
            opptjeningvurdering = Opptjeningvurdering(emptyList()),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja)
        )
        vilkårsgrunnlag.valider(0.månedlig, 0.månedlig, 1.januar, Periodetype.FØRSTEGANGSBEHANDLING)
        vilkårsgrunnlagHistorikk.lagre(vilkårsgrunnlag, 1.januar)
        assertNotNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
    }

    @Test
    fun `Finner utbetalingshistorikk for skjæringstidspunkt`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val infotrygdVilkårsgrunnlag = Utbetalingshistorikk(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            aktørId = "AKTØR_ID",
            fødselsnummer = "20043769969",
            organisasjonsnummer = "ORGNUMMER",
            utbetalinger = emptyList(),
            inntektshistorikk = emptyList(),
            arbeidskategorikoder = emptyMap()
        )
        vilkårsgrunnlagHistorikk.lagre(infotrygdVilkårsgrunnlag, 1.januar)
        assertNotNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
    }

    @Test
    fun `lagrer grunnlagsdata fra Infotrygd`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val historikk = Infotrygdhistorikk().apply {
            oppdaterHistorikk(Infotrygdhistorikk.Element.opprett(
                oppdatert = LocalDateTime.now(),
                hendelseId = UUID.randomUUID(),
                perioder = emptyList(),
                inntekter = emptyList(),
                arbeidskategorikoder = emptyMap(),
                ugyldigePerioder = emptyList()
            ))
        }
        historikk.lagreVilkårsgrunnlag(1.januar, vilkårsgrunnlagHistorikk)
        assertNotNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
    }
}
