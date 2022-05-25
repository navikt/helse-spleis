package no.nav.helse.person

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Grunnbeløp
import no.nav.helse.desember
import no.nav.helse.januar
import no.nav.helse.spleis.e2e.AbstractEndToEndTest.Companion.INNTEKT
import no.nav.helse.sykepengegrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class SykepengegrunnlagTest {

    @Test
    fun equality() {
        val sykepengegrunnlag = INNTEKT.sykepengegrunnlag
        assertEquals(sykepengegrunnlag, sykepengegrunnlag)
        assertNotEquals(sykepengegrunnlag, INNTEKT.sykepengegrunnlag)
        assertNotEquals(INNTEKT.sykepengegrunnlag, INNTEKT.sykepengegrunnlag)
        assertNotEquals(INNTEKT.sykepengegrunnlag, INNTEKT.sykepengegrunnlag("annet orgnr"))
        assertNotEquals(INNTEKT.sykepengegrunnlag, INNTEKT.sykepengegrunnlag(31.desember))
    }

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
            deaktiverteArbeidsforhold = emptyList(),
            `6G` = Grunnbeløp.`6G`.beløp(1.januar),
            vurdertInfotrygd = false
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
                deaktiverteArbeidsforhold = emptyList(),
                `6G` = Grunnbeløp.`6G`.beløp(1.januar),
                vurdertInfotrygd = false,
                cachedGrunnlagForSykepengegrunnlag = 25000.månedlig
            )
        )
        assertNotEquals(
            sykepengegrunnlag1,
            Sykepengegrunnlag(
                arbeidsgiverInntektsopplysninger = emptyList(),
                deaktiverteArbeidsforhold = emptyList(),
                `6G` = Grunnbeløp.`6G`.beløp(1.januar),
                vurdertInfotrygd = false,
                cachedGrunnlagForSykepengegrunnlag = 25000.månedlig
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
                deaktiverteArbeidsforhold = emptyList(),
                `6G` = Grunnbeløp.`6G`.beløp(1.januar),
                vurdertInfotrygd = false,
                cachedGrunnlagForSykepengegrunnlag = 20000.månedlig
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
                deaktiverteArbeidsforhold = emptyList(),
                `6G` = Grunnbeløp.`6G`.beløp(1.januar),
                vurdertInfotrygd = true,
                cachedGrunnlagForSykepengegrunnlag = 25000.månedlig
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
                deaktiverteArbeidsforhold = listOf("orgnummer"),
                `6G` = Grunnbeløp.`6G`.beløp(1.januar),
                vurdertInfotrygd = false
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
                deaktiverteArbeidsforhold = emptyList(),
                `6G` = Grunnbeløp.`6G`.beløp(1.januar(2000)),
                vurdertInfotrygd = false,
                cachedGrunnlagForSykepengegrunnlag = 25000.månedlig
            )
        )
    }
}