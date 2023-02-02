package no.nav.helse.person.inntekt

import java.time.LocalDate.EPOCH
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.forrigeDag
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Opptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold
import no.nav.helse.person.inntekt.AvklarbarSykepengegrunnlag.Companion.avklarSykepengegrunnlag
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.yearMonth
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SkattSykepengegrunnlagTest {

    @Test
    fun `inntekt må gjelde skjæringstidspunktet`() {
        val skjæringstidspunkt = 2.april
        val skattSykepengegrunnlag = SkattSykepengegrunnlag(
            id = UUID.randomUUID(),
            hendelseId = UUID.randomUUID(),
            dato = skjæringstidspunkt,
            inntektsopplysninger = listOf(
                Skatteopplysning(
                    hendelseId = UUID.randomUUID(),
                    beløp = 25000.månedlig,
                    måned = 1.januar.yearMonth,
                    type = Skatteopplysning.Inntekttype.LØNNSINNTEKT,
                    fordel = "",
                    beskrivelse = "",
                    tidsstempel = LocalDateTime.now()
                ),
                Skatteopplysning(
                    hendelseId = UUID.randomUUID(),
                    beløp = 25000.månedlig,
                    måned = 1.februar.yearMonth,
                    type = Skatteopplysning.Inntekttype.LØNNSINNTEKT,
                    fordel = "",
                    beskrivelse = "",
                    tidsstempel = LocalDateTime.now()
                ),
                Skatteopplysning(
                    hendelseId = UUID.randomUUID(),
                    beløp = 25000.månedlig,
                    måned = 1.mars.yearMonth,
                    type = Skatteopplysning.Inntekttype.LØNNSINNTEKT,
                    fordel = "",
                    beskrivelse = "",
                    tidsstempel = LocalDateTime.now()
                )
            ),
            ansattPerioder = listOf(
                Arbeidsforhold(EPOCH, null, false)
            ),
            tidsstempel = LocalDateTime.now()
        )
        assertNull(emptyList<Inntektsmelding>().avklarSykepengegrunnlag(
            skjæringstidspunkt.forrigeDag,
            null,
            skattSykepengegrunnlag
        ))
        assertTrue(skattSykepengegrunnlag === emptyList<Inntektsmelding>().avklarSykepengegrunnlag(
            skjæringstidspunkt,
            null,
            skattSykepengegrunnlag
        ))
    }

    @Test
    fun `inntekt må være innenfor 2 mnd fra skjæringstidspunktet`() {
        val skjæringstidspunkt = 2.april
        val skattSykepengegrunnlag1 = SkattSykepengegrunnlag(
            id = UUID.randomUUID(),
            hendelseId = UUID.randomUUID(),
            dato = skjæringstidspunkt,
            inntektsopplysninger = listOf(
                Skatteopplysning(
                    hendelseId = UUID.randomUUID(),
                    beløp = 25000.månedlig,
                    måned = 1.januar.yearMonth,
                    type = Skatteopplysning.Inntekttype.LØNNSINNTEKT,
                    fordel = "",
                    beskrivelse = "",
                    tidsstempel = LocalDateTime.now()
                ),
                Skatteopplysning(
                    hendelseId = UUID.randomUUID(),
                    beløp = 25000.månedlig,
                    måned = 1.februar.yearMonth,
                    type = Skatteopplysning.Inntekttype.LØNNSINNTEKT,
                    fordel = "",
                    beskrivelse = "",
                    tidsstempel = LocalDateTime.now()
                )
            ),
            ansattPerioder = listOf(
                Arbeidsforhold(EPOCH, null, false)
            ),
            tidsstempel = LocalDateTime.now()
        )
        val skattSykepengegrunnlag2 = SkattSykepengegrunnlag(
            id = UUID.randomUUID(),
            hendelseId = UUID.randomUUID(),
            dato = skjæringstidspunkt,
            inntektsopplysninger = listOf(
                Skatteopplysning(
                    hendelseId = UUID.randomUUID(),
                    beløp = 25000.månedlig,
                    måned = 1.januar.yearMonth,
                    type = Skatteopplysning.Inntekttype.LØNNSINNTEKT,
                    fordel = "",
                    beskrivelse = "",
                    tidsstempel = LocalDateTime.now()
                )
            ),
            ansattPerioder = listOf(
                Arbeidsforhold(EPOCH, null, false)
            ),
            tidsstempel = LocalDateTime.now()
        )
        assertTrue(skattSykepengegrunnlag1 === emptyList<Inntektsmelding>().avklarSykepengegrunnlag(
            skjæringstidspunkt,
            null,
            skattSykepengegrunnlag1
        ))
        assertNull(emptyList<Inntektsmelding>().avklarSykepengegrunnlag(
            skjæringstidspunkt,
            null,
            skattSykepengegrunnlag2
        ))
    }

    @Test
    fun `nytt arbeidsforhold innenfor 2 mnd telles som ikke rapportert inntekt`() {
        val skjæringstidspunkt = 2.april
        val skattSykepengegrunnlag1 = SkattSykepengegrunnlag(
            id = UUID.randomUUID(),
            hendelseId = UUID.randomUUID(),
            dato = skjæringstidspunkt,
            inntektsopplysninger = emptyList(),
            ansattPerioder = listOf(
                Arbeidsforhold(1.februar, null, false)
            ),
            tidsstempel = LocalDateTime.now()
        )
        val skattSykepengegrunnlag2 = SkattSykepengegrunnlag(
            id = UUID.randomUUID(),
            hendelseId = UUID.randomUUID(),
            dato = skjæringstidspunkt,
            inntektsopplysninger = emptyList(),
            ansattPerioder = listOf(
                Arbeidsforhold(1.januar, null, false)
            ),
            tidsstempel = LocalDateTime.now()
        )
        val resultat = emptyList<Inntektsmelding>().avklarSykepengegrunnlag(
            skjæringstidspunkt,
            null,
            skattSykepengegrunnlag1
        )
        assertNotNull(resultat)
        assertEquals(IkkeRapportert::class, resultat::class)
        assertNull(emptyList<Inntektsmelding>().avklarSykepengegrunnlag(
            skjæringstidspunkt,
            null,
            skattSykepengegrunnlag2
        ))
    }

}