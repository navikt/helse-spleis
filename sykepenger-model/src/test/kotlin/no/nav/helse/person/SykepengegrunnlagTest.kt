package no.nav.helse.person

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.spleis.e2e.AbstractEndToEndTest.Companion.INNTEKT
import no.nav.helse.sykepengegrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
    fun `justerer grunnbeløpet`() {
        val sykepengegrunnlag = 60000.månedlig.sykepengegrunnlag(1.mai(2020))
        val justert = sykepengegrunnlag.justerGrunnbeløp()
        assertNotEquals(sykepengegrunnlag, justert)
        assertNotEquals(sykepengegrunnlag.inspektør.sykepengegrunnlag, justert.inspektør.sykepengegrunnlag)
        assertTrue(sykepengegrunnlag.inspektør.sykepengegrunnlag < justert.inspektør.sykepengegrunnlag)
    }

    @Test
    fun equals() {
        val inntektID = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        val tidsstempel = LocalDateTime.now()
        val sykepengegrunnlag1 = Sykepengegrunnlag(
            skjæringstidspunkt = 1.januar,
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
            vurdertInfotrygd = false
        )

        assertEquals(
            sykepengegrunnlag1,
            Sykepengegrunnlag(
                skjæringstidspunkt = 1.januar,
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
                vurdertInfotrygd = false,
                cachedGrunnlagForSykepengegrunnlag = 25000.månedlig
            )
        )
        assertEquals(sykepengegrunnlag1, sykepengegrunnlag1.justerGrunnbeløp()) { "grunnbeløpet trenger ikke justering" }
        assertNotEquals(
            sykepengegrunnlag1,
            Sykepengegrunnlag(
                skjæringstidspunkt = 1.januar,
                arbeidsgiverInntektsopplysninger = emptyList(),
                deaktiverteArbeidsforhold = emptyList(),
                vurdertInfotrygd = false,
                cachedGrunnlagForSykepengegrunnlag = 25000.månedlig
            )
        )
        assertNotEquals(
            sykepengegrunnlag1,
            Sykepengegrunnlag(
                skjæringstidspunkt = 1.januar,
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
                vurdertInfotrygd = false,
                cachedGrunnlagForSykepengegrunnlag = 20000.månedlig
            )
        )
        assertNotEquals(
            sykepengegrunnlag1,
            Sykepengegrunnlag(
                skjæringstidspunkt = 1.januar,
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
                vurdertInfotrygd = true,
                cachedGrunnlagForSykepengegrunnlag = 25000.månedlig
            )
        )
        assertNotEquals(
            sykepengegrunnlag1,
            Sykepengegrunnlag(
                skjæringstidspunkt = 1.januar,
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
                vurdertInfotrygd = false
            )
        )
    }
}