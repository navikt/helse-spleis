package no.nav.helse.spleis.mediator.e2e

import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.hendelser.Forsikringsvurdering
import no.nav.helse.hendelser.SelvstendigForsikring
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Test

internal class SelvstendigSpForsikringTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `Selvstendig med forsikringsvurdering går videre når forsikring-toggle er enabled`() = Toggle.NyttForsikringsbehov.enable {
        Toggle.SelvstendigForsikring.enable {
            sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
            sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)), ventetid = 3.januar til 18.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
            sendVilkårsgrunnlagSelvstendig(0, orgnummer = "SELVSTENDIG")
            sendYtelserSelvstendig(
                0,
                orgnummer = "SELVSTENDIG",
                selvstendigForsikring = listOf(SelvstendigForsikring(1.januar, null, SelvstendigForsikring.Forsikringstype.HundreProsentFraDagEn, 450_000.årlig)),
                forsikringsvurdering = Forsikringsvurdering(
                    forsikringsvurderingId = UUID.randomUUID(),
                    harForsikring = true,
                    dekning = Forsikringsvurdering.Dekning(grad = 100, fraDag = 1)
                )
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
    }

    @Test
    fun `Kaster ut selvstendig med forsikringsvurdering når forsikring-toggle er disabled`() = Toggle.NyttForsikringsbehov.enable {
        Toggle.SelvstendigForsikring.disable {
            sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
            sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)), ventetid = 3.januar til 18.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
            sendVilkårsgrunnlagSelvstendig(0, orgnummer = "SELVSTENDIG")
            sendYtelserSelvstendig(
                0,
                orgnummer = "SELVSTENDIG",
                selvstendigForsikring = listOf(SelvstendigForsikring(1.januar, null, SelvstendigForsikring.Forsikringstype.HundreProsentFraDagEn, 450_000.årlig)),
                forsikringsvurdering = Forsikringsvurdering(
                    forsikringsvurderingId = UUID.randomUUID(),
                    harForsikring = true,
                    dekning = Forsikringsvurdering.Dekning(grad = 100, fraDag = 1)
                )
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

    @Test
    fun `NB Kaster faktisk også ut selvstendig med forsikringsvurdering=false hvis (gml)SelvstendigForsikring sier at har, når forsikring-toggle er disabled`() = Toggle.NyttForsikringsbehov.enable {
        Toggle.SelvstendigForsikring.disable {
            sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
            sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)), ventetid = 3.januar til 18.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
            sendVilkårsgrunnlagSelvstendig(0, orgnummer = "SELVSTENDIG")
            sendYtelserSelvstendig(
                0,
                orgnummer = "SELVSTENDIG",
                selvstendigForsikring = listOf(SelvstendigForsikring(1.januar, null, SelvstendigForsikring.Forsikringstype.HundreProsentFraDagEn, 450_000.årlig)),
                forsikringsvurdering = Forsikringsvurdering(
                    forsikringsvurderingId = UUID.randomUUID(),
                    harForsikring = false,
                    dekning = Forsikringsvurdering.Dekning(grad = 100, fraDag = 1)
                )
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

    @Test
    fun `Selvstendig med kun forsikringsvurdering går videre når forsikring-toggle er enabled`() = Toggle.NyttForsikringsbehov.enable {
        Toggle.SelvstendigForsikring.enable {
            sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
            sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)), ventetid = 3.januar til 18.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
            sendVilkårsgrunnlagSelvstendig(0, orgnummer = "SELVSTENDIG")
            sendYtelserSelvstendig(
                0,
                orgnummer = "SELVSTENDIG",
                selvstendigForsikring = emptyList(),
                forsikringsvurdering = Forsikringsvurdering(
                    forsikringsvurderingId = UUID.randomUUID(),
                    harForsikring = true,
                    dekning = Forsikringsvurdering.Dekning(grad = 100, fraDag = 1)
                )
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
    }

    @Test
    fun `Kaster ut selvstendig med kun forsikringsvurdering når forsikring-toggle er disabled`() = Toggle.NyttForsikringsbehov.enable {
        Toggle.SelvstendigForsikring.disable {
            sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
            sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)), ventetid = 3.januar til 18.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
            sendVilkårsgrunnlagSelvstendig(0, orgnummer = "SELVSTENDIG")
            sendYtelserSelvstendig(
                0,
                orgnummer = "SELVSTENDIG",
                selvstendigForsikring = emptyList(),
                forsikringsvurdering = Forsikringsvurdering(
                    forsikringsvurderingId = UUID.randomUUID(),
                    harForsikring = true,
                    dekning = Forsikringsvurdering.Dekning(grad = 100, fraDag = 1)
                )
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



}
