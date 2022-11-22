package no.nav.helse.serde

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.Sykepengegrunnlag.Begrensning.ER_6G_BEGRENSET
import no.nav.helse.serde.PersonData.VilkårsgrunnlagElementData.GrunnlagsdataType.Vilkårsprøving
import no.nav.helse.serde.PersonData.VilkårsgrunnlagElementData.OpptjeningData
import no.nav.helse.serde.PersonData.VilkårsgrunnlagElementData.SammenligningsgrunnlagData
import no.nav.helse.serde.PersonData.VilkårsgrunnlagElementData.SykepengegrunnlagData
import no.nav.helse.serde.PersonData.VilkårsgrunnlagInnslagData.Companion.grunnlagMap
import no.nav.helse.serde.mapping.JsonMedlemskapstatus
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.økonomi.Prosent.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class NyesteVilkårsgrunnlagTest {

    @Test
    fun `hvis vilkårsgrunnlaget er i flere innslag velger vi det fra det nyeste innslaget`() {
        val vilkårsgrunnlagId = UUID.randomUUID()
        val vilkårsgrunnlagElementData = PersonData.VilkårsgrunnlagElementData(
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            skjæringstidspunkt = 1.januar,
            type = Vilkårsprøving,
            sykepengegrunnlag = SykepengegrunnlagData(
                sykepengegrunnlag = 30000.0,
                grunnbeløp = 1.0,
                arbeidsgiverInntektsopplysninger = emptyList(),
                skjønnsmessigFastsattBeregningsgrunnlag = null,
                begrensning = ER_6G_BEGRENSET,
                deaktiverteArbeidsforhold = emptyList(),
                vurdertInfotrygd = false
            ),
            sammenligningsgrunnlag = SammenligningsgrunnlagData(
                sammenligningsgrunnlag = 1.0,
                arbeidsgiverInntektsopplysninger = emptyList()
            ),
            avviksprosent = null,
            opptjening = OpptjeningData(LocalDate.EPOCH, 1.januar, emptyList()),
            medlemskapstatus = JsonMedlemskapstatus.JA,
            vurdertOk = true,
            meldingsreferanseId = null,
        )
        val vilkårsgrunnlagElementData2 = vilkårsgrunnlagElementData.copy(avviksprosent = 0.1)

        val vilkårsgrunnlagElementTidspunkt = LocalDateTime.now()
        val vilkårsgrunnlagElementTidspunkt2 = vilkårsgrunnlagElementTidspunkt.plusDays(1)

        val vilkårsgrunnlagInnslag = listOf(
            PersonData.VilkårsgrunnlagInnslagData(
                id = UUID.randomUUID(),
                opprettet = vilkårsgrunnlagElementTidspunkt2,
                vilkårsgrunnlag = listOf(vilkårsgrunnlagElementData2)
            ), PersonData.VilkårsgrunnlagInnslagData(
                id = UUID.randomUUID(),
                opprettet = vilkårsgrunnlagElementTidspunkt,
                vilkårsgrunnlag = listOf(vilkårsgrunnlagElementData)
            )
        )

        val alder = Alder(LocalDate.of(1970, 1, 1))
        val grunnlagMap = vilkårsgrunnlagInnslag.grunnlagMap(alder)
        val forventet = mapOf(vilkårsgrunnlagId to vilkårsgrunnlagElementData2.parseDataForVilkårsvurdering(alder).second)
        assertEquals(10.prosent, grunnlagMap.entries.single().value.inspektør.avviksprosent)

        //assertEquals(forventet, grunnlagMap)
    }
}