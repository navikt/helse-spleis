package no.nav.helse.testhelpers

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.Opptjening
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.inntekt.Inntektsopplysning
import no.nav.helse.sykepengegrunnlag

internal fun Map<LocalDate, Inntektsopplysning>.somVilkårsgrunnlagHistorikk(organisasjonsnummer: String) = VilkårsgrunnlagHistorikk().also { vilkårsgrunnlagHistorikk ->
    forEach { (skjæringstidspunkt, inntektsopplysning) ->
        vilkårsgrunnlagHistorikk.lagre(VilkårsgrunnlagHistorikk.Grunnlagsdata(
            skjæringstidspunkt = skjæringstidspunkt,
            sykepengegrunnlag = inntektsopplysning.inspektør.beløp.sykepengegrunnlag(orgnr = organisasjonsnummer, skjæringstidspunkt = skjæringstidspunkt, virkningstidspunkt = skjæringstidspunkt),
            opptjening = Opptjening.gjenopprett(skjæringstidspunkt, emptyList(), 1.januar til 31.januar),
            medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            vurdertOk = true,
            meldingsreferanseId = UUID.randomUUID(),
            vilkårsgrunnlagId = UUID.randomUUID()
        ))
    }
}