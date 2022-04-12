package no.nav.helse.person

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class SykepengegrunnlagTest {

    @Test
    fun equals() {
        val inntektID = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        val tidsstempel = LocalDateTime.now()
        val sykepengegrunnlag1 = Sykepengegrunnlag(
                arbeidsgiverInntektsopplysninger = listOf(
                    ArbeidsgiverInntektsopplysning(
                        orgnummer = "orgnummer",
                        inntektsopplysning = Inntektshistorikk.Infotrygd(
                            id = inntektID,
                            dato = 1.januar,
                            hendelseId = hendelseId,
                            beløp = 25000.månedlig,
                            tidsstempel = tidsstempel
                        )
                    )
                ),
                sykepengegrunnlag = 25000.månedlig,
                grunnlagForSykepengegrunnlag = 25000.månedlig,
                begrensning = Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET,
                deaktiverteArbeidsforhold = emptyList()
            )

        assertEquals(
            sykepengegrunnlag1,
            Sykepengegrunnlag(
                arbeidsgiverInntektsopplysninger = listOf(
                    ArbeidsgiverInntektsopplysning(
                        orgnummer = "orgnummer",
                        inntektsopplysning = Inntektshistorikk.Infotrygd(
                            id = inntektID,
                            dato = 1.januar,
                            hendelseId = hendelseId,
                            beløp = 25000.månedlig,
                            tidsstempel = tidsstempel
                        )
                    )
                ),
                sykepengegrunnlag = 25000.månedlig,
                grunnlagForSykepengegrunnlag = 25000.månedlig,
                begrensning = Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET,
                deaktiverteArbeidsforhold = emptyList()
            )
        )
        assertNotEquals(
            sykepengegrunnlag1,
            Sykepengegrunnlag(
                arbeidsgiverInntektsopplysninger = emptyList(),
                sykepengegrunnlag = 25000.månedlig,
                grunnlagForSykepengegrunnlag = 25000.månedlig,
                begrensning = Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET,
                deaktiverteArbeidsforhold = emptyList()
            )
        )
        assertNotEquals(
            sykepengegrunnlag1,
            Sykepengegrunnlag(
                arbeidsgiverInntektsopplysninger = listOf(
                    ArbeidsgiverInntektsopplysning(
                        orgnummer = "orgnummer",
                        inntektsopplysning = Inntektshistorikk.Infotrygd(
                            id = inntektID,
                            dato = 1.januar,
                            hendelseId = hendelseId,
                            beløp = 25000.månedlig,
                            tidsstempel = tidsstempel
                        )
                    )
                ),
                sykepengegrunnlag = 30000.månedlig,
                grunnlagForSykepengegrunnlag = 25000.månedlig,
                begrensning = Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET,
                deaktiverteArbeidsforhold = emptyList()
            )
        )
        assertNotEquals(
            sykepengegrunnlag1,
            Sykepengegrunnlag(
                arbeidsgiverInntektsopplysninger = listOf(
                    ArbeidsgiverInntektsopplysning(
                        orgnummer = "orgnummer",
                        inntektsopplysning = Inntektshistorikk.Infotrygd(
                            id = inntektID,
                            dato = 1.januar,
                            hendelseId = hendelseId,
                            beløp = 25000.månedlig,
                            tidsstempel = tidsstempel
                        )
                    )
                ),
                sykepengegrunnlag = 25000.månedlig,
                grunnlagForSykepengegrunnlag = 32000.månedlig,
                begrensning = Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET,
                deaktiverteArbeidsforhold = emptyList()
            )
        )
        assertNotEquals(
            sykepengegrunnlag1,
            Sykepengegrunnlag(
                arbeidsgiverInntektsopplysninger = listOf(
                    ArbeidsgiverInntektsopplysning(
                        orgnummer = "orgnummer",
                        inntektsopplysning = Inntektshistorikk.Infotrygd(
                            id = inntektID,
                            dato = 1.januar,
                            hendelseId = hendelseId,
                            beløp = 25000.månedlig,
                            tidsstempel = tidsstempel
                        )
                    )
                ),
                sykepengegrunnlag = 25000.månedlig,
                grunnlagForSykepengegrunnlag = 25000.månedlig,
                begrensning = Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET,
                deaktiverteArbeidsforhold = listOf("orgnummer")
            )
        )
        assertNotEquals(
            sykepengegrunnlag1,
            Sykepengegrunnlag(
                arbeidsgiverInntektsopplysninger = listOf(
                    ArbeidsgiverInntektsopplysning(
                        orgnummer = "orgnummer",
                        inntektsopplysning = Inntektshistorikk.Infotrygd(
                            id = inntektID,
                            dato = 1.januar,
                            hendelseId = hendelseId,
                            beløp = 25000.månedlig,
                            tidsstempel = tidsstempel
                        )
                    )
                ),
                sykepengegrunnlag = 25000.månedlig,
                grunnlagForSykepengegrunnlag = 25000.månedlig,
                begrensning = Sykepengegrunnlag.Begrensning.ER_6G_BEGRENSET,
                deaktiverteArbeidsforhold = emptyList()
            )
        )
    }
}