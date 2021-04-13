package no.nav.helse.person

import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Opptjeningvurdering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class VilkårsgrunnlagHistorikkTest {

    @Test
    fun `Finner vilkårsgrunnlag for skjæringstidspunkt - ok`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val vilkårsgrunnlag = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            aktørId = "AKTØR_ID",
            fødselsnummer = "20043769969",
            orgnummer = "ORGNUMMER",
            inntektsvurdering = Inntektsvurdering(emptyList()),
            opptjeningvurdering = Opptjeningvurdering(listOf(Opptjeningvurdering.Arbeidsforhold("123456789", 1.desember(2017)))),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja)
        )
        vilkårsgrunnlag.valider(10000.månedlig, 10000.månedlig, 1.januar, Periodetype.FØRSTEGANGSBEHANDLING)
        vilkårsgrunnlagHistorikk.lagre(vilkårsgrunnlag, 1.januar)
        assertNotNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
        assertTrue(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar)!!.isOk())
    }

    @Test
    fun `Finner vilkårsgrunnlag for skjæringstidspunkt - ikke ok`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val vilkårsgrunnlag = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = UUID.randomUUID().toString(),
            aktørId = "AKTØR_ID",
            fødselsnummer = "20043769969",
            orgnummer = "ORGNUMMER",
            inntektsvurdering = Inntektsvurdering(emptyList()),
            opptjeningvurdering = Opptjeningvurdering(listOf(Opptjeningvurdering.Arbeidsforhold("123456789", 1.desember(2017)))),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Nei)
        )
        vilkårsgrunnlag.valider(10000.månedlig, 10000.månedlig, 1.januar, Periodetype.FØRSTEGANGSBEHANDLING)
        vilkårsgrunnlagHistorikk.lagre(vilkårsgrunnlag, 1.januar)
        assertNotNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
        assertFalse(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar)!!.isOk())
    }

    @Test
    fun `lagrer grunnlagsdata fra Infotrygd ved overgang fra IT`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val historikk = Infotrygdhistorikk().apply {
            oppdaterHistorikk(
                InfotrygdhistorikkElement.opprett(
                oppdatert = LocalDateTime.now(),
                hendelseId = UUID.randomUUID(),
                perioder = emptyList(),
                inntekter = emptyList(),
                arbeidskategorikoder = emptyMap(),
                ugyldigePerioder = emptyList(),
                harStatslønn = false
            ))
        }
        historikk.lagreVilkårsgrunnlag(1.januar, Periodetype.OVERGANG_FRA_IT, vilkårsgrunnlagHistorikk)
        assertNotNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
    }

    @Test
    fun `lagrer grunnlagsdata fra Infotrygd ved infotrygdforlengelse`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val historikk = Infotrygdhistorikk().apply {
            oppdaterHistorikk(
                InfotrygdhistorikkElement.opprett(
                oppdatert = LocalDateTime.now(),
                hendelseId = UUID.randomUUID(),
                perioder = emptyList(),
                inntekter = emptyList(),
                arbeidskategorikoder = emptyMap(),
                ugyldigePerioder = emptyList(),
                harStatslønn = false
            ))
        }
        historikk.lagreVilkårsgrunnlag(1.januar, Periodetype.INFOTRYGDFORLENGELSE, vilkårsgrunnlagHistorikk)
        assertNotNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
    }

    @Test
    fun `lagrer ikke grunnlagsdata ved førstegangsbehandling`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val historikk = Infotrygdhistorikk().apply {
            oppdaterHistorikk(
                InfotrygdhistorikkElement.opprett(
                oppdatert = LocalDateTime.now(),
                hendelseId = UUID.randomUUID(),
                perioder = emptyList(),
                inntekter = emptyList(),
                arbeidskategorikoder = emptyMap(),
                ugyldigePerioder = emptyList(),
                harStatslønn = false
            ))
        }
        historikk.lagreVilkårsgrunnlag(1.januar, Periodetype.FØRSTEGANGSBEHANDLING, vilkårsgrunnlagHistorikk)
        assertNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
    }

    @Test
    fun `lagrer ikke grunnlagsdata ved forlengelse`() {
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk()
        val historikk = Infotrygdhistorikk().apply {
            oppdaterHistorikk(
                InfotrygdhistorikkElement.opprett(
                oppdatert = LocalDateTime.now(),
                hendelseId = UUID.randomUUID(),
                perioder = emptyList(),
                inntekter = emptyList(),
                arbeidskategorikoder = emptyMap(),
                ugyldigePerioder = emptyList(),
                harStatslønn = false
            ))
        }
        historikk.lagreVilkårsgrunnlag(1.januar, Periodetype.FORLENGELSE, vilkårsgrunnlagHistorikk)
        assertNull(vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(1.januar))
    }
}
