package no.nav.helse.spleis.mediator.e2e

import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.hendelser.Forsikringsvurdering
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import org.junit.jupiter.api.Test

internal class SelvstendigSpForsikringTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `Selvstendig med forsikringsvurdering går videre når forsikring-toggle er enabled`() = Toggle.SelvstendigForsikring.enable {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)), ventetid = 3.januar til 18.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        val forsikringsvurderingId = UUID.randomUUID()
        sendVilkårsgrunnlagSelvstendig(vedtaksperiodeIndeks = 0, forsikringsvurderingId = forsikringsvurderingId)
        sendYtelser(
            vedtaksperiodeIndeks = 0,
            forsikringsvurdering = Forsikringsvurdering(
                forsikringsvurderingId = forsikringsvurderingId,
                harForsikring = true,
                dekning = Forsikringsvurdering.Dekning(grad = 100, fraDag = 1),
                opphørsdato = null,
            ),
            orgnummer = "SELVSTENDIG"
        )
        sendSimuleringSelvstendig(0, orgnummer = "SELVSTENDIG")
        assertTilstander(
            0,
            "SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK",
            "SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE",
            "SELVSTENDIG_AVVENTER_VILKÅRSPRØVING",
            "SELVSTENDIG_AVVENTER_HISTORIKK",
            "SELVSTENDIG_AVVENTER_SIMULERING",
            "SELVSTENDIG_AVVENTER_GODKJENNING"
        )
    }

    @Test
    fun `Kaster ut selvstendig med forsikringsvurdering når forsikring-toggle er disabled`() = Toggle.SelvstendigForsikring.disable {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)), ventetid = 3.januar til 18.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        val forsikringsvurderingId = UUID.randomUUID()
        sendVilkårsgrunnlagSelvstendig(vedtaksperiodeIndeks = 0, forsikringsvurderingId = forsikringsvurderingId)
        sendYtelser(
            vedtaksperiodeIndeks = 0,
            forsikringsvurdering = Forsikringsvurdering(
                forsikringsvurderingId = forsikringsvurderingId,
                harForsikring = true,
                dekning = Forsikringsvurdering.Dekning(grad = 100, fraDag = 1),
                opphørsdato = null,
            ),
            orgnummer = "SELVSTENDIG"
        )
        assertTilstander(
            0,
            "SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK",
            "SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE",
            "SELVSTENDIG_AVVENTER_VILKÅRSPRØVING",
            "SELVSTENDIG_AVVENTER_HISTORIKK",
            "TIL_INFOTRYGD"
        )

    }
}
