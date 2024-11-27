package no.nav.helse.person.inntekt

import java.time.LocalDate.EPOCH
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.forrigeDag
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.nesteDag
import no.nav.helse.person.inntekt.Skatteopplysning.Inntekttype.LØNNSINNTEKT
import no.nav.helse.yearMonth
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SkattInntektsgrunnlagTest {

    @Test
    fun `bruker kun tre måneder før skjæringstidspunktet`() {
        val skatt = SkattSykepengegrunnlag(
            UUID.randomUUID(), 10.april, inntektsopplysninger = listOf(
            Skatteopplysning(
                hendelseId = UUID.randomUUID(),
                beløp = 1000.daglig,
                måned = desember(2017),
                type = LØNNSINNTEKT,
                fordel = "fordel",
                beskrivelse = "beskrivelse"
            ),
            Skatteopplysning(
                hendelseId = UUID.randomUUID(),
                beløp = 1000.daglig,
                måned = januar(2018),
                type = LØNNSINNTEKT,
                fordel = "fordel",
                beskrivelse = "beskrivelse"
            ),
            Skatteopplysning(
                hendelseId = UUID.randomUUID(),
                beløp = 1000.daglig,
                måned = februar(2018),
                type = LØNNSINNTEKT,
                fordel = "fordel",
                beskrivelse = "beskrivelse"
            ),
            Skatteopplysning(
                hendelseId = UUID.randomUUID(),
                beløp = 1000.daglig,
                måned = mars(2018),
                type = LØNNSINNTEKT,
                fordel = "fordel",
                beskrivelse = "beskrivelse"
            ),
        ),
            emptyList()
        )
        assertEquals(1000.daglig, skatt.inspektør.beløp)
    }

    @Test
    fun `bruker ikke inntekter samme måned som skjæringstidspunktet`() {
        val skatt = SkattSykepengegrunnlag(
            UUID.randomUUID(), 10.april, inntektsopplysninger = listOf(
            Skatteopplysning(
                hendelseId = UUID.randomUUID(),
                beløp = 1000.daglig,
                måned = januar(2018),
                type = LØNNSINNTEKT,
                fordel = "fordel",
                beskrivelse = "beskrivelse"
            ),
            Skatteopplysning(
                hendelseId = UUID.randomUUID(),
                beløp = 1000.daglig,
                måned = februar(2018),
                type = LØNNSINNTEKT,
                fordel = "fordel",
                beskrivelse = "beskrivelse"
            ),
            Skatteopplysning(
                hendelseId = UUID.randomUUID(),
                beløp = 1000.daglig,
                måned = mars(2018),
                type = LØNNSINNTEKT,
                fordel = "fordel",
                beskrivelse = "beskrivelse"
            ),
            Skatteopplysning(
                hendelseId = UUID.randomUUID(),
                beløp = 1000.daglig,
                måned = april(2018),
                type = LØNNSINNTEKT,
                fordel = "fordel",
                beskrivelse = "beskrivelse"
            ),
        ),
            emptyList()
        )
        assertEquals(1000.daglig, skatt.inspektør.beløp)
    }

    @Test
    fun `setter negativt omregnet årsinntekt til 0`() {
        val skatt = SkattSykepengegrunnlag(
            UUID.randomUUID(), 1.januar, inntektsopplysninger = listOf(
            Skatteopplysning(
                hendelseId = UUID.randomUUID(),
                beløp = (-2500).daglig,
                måned = desember(2017),
                type = LØNNSINNTEKT,
                fordel = "fordel",
                beskrivelse = "beskrivelse"
            )
        ),
            emptyList()
        )
        assertEquals(Inntekt.INGEN, skatt.fastsattÅrsinntekt())
    }

    @Test
    fun `inntekt må gjelde skjæringstidspunktet`() {
        val skjæringstidspunkt = 2.april
        val skattSykepengegrunnlag = SkattSykepengegrunnlag(
            hendelseId = UUID.randomUUID(),
            dato = skjæringstidspunkt,
            inntektsopplysninger = listOf(
                Skatteopplysning(
                    hendelseId = UUID.randomUUID(),
                    beløp = 25000.månedlig,
                    måned = 1.januar.yearMonth,
                    type = LØNNSINNTEKT,
                    fordel = "",
                    beskrivelse = "",
                    tidsstempel = LocalDateTime.now()
                ),
                Skatteopplysning(
                    hendelseId = UUID.randomUUID(),
                    beløp = 25000.månedlig,
                    måned = 1.februar.yearMonth,
                    type = LØNNSINNTEKT,
                    fordel = "",
                    beskrivelse = "",
                    tidsstempel = LocalDateTime.now()
                ),
                Skatteopplysning(
                    hendelseId = UUID.randomUUID(),
                    beløp = 25000.månedlig,
                    måned = 1.mars.yearMonth,
                    type = LØNNSINNTEKT,
                    fordel = "",
                    beskrivelse = "",
                    tidsstempel = LocalDateTime.now()
                )
            ),
            ansattPerioder = listOf(AnsattPeriode(EPOCH, null))
        )
        assertFalse(skattSykepengegrunnlag.kanBrukes(skjæringstidspunkt.forrigeDag))
        assertTrue(skattSykepengegrunnlag.kanBrukes(skjæringstidspunkt))
        assertFalse(skattSykepengegrunnlag.kanBrukes(skjæringstidspunkt.nesteDag))
    }

    @Test
    fun `inntekt må være innenfor 2 mnd fra skjæringstidspunktet`() {
        val skjæringstidspunkt = 2.april
        val skattSykepengegrunnlag1 = SkattSykepengegrunnlag(
            hendelseId = UUID.randomUUID(),
            dato = skjæringstidspunkt,
            inntektsopplysninger = listOf(
                Skatteopplysning(
                    hendelseId = UUID.randomUUID(),
                    beløp = 25000.månedlig,
                    måned = 1.januar.yearMonth,
                    type = LØNNSINNTEKT,
                    fordel = "",
                    beskrivelse = "",
                    tidsstempel = LocalDateTime.now()
                ),
                Skatteopplysning(
                    hendelseId = UUID.randomUUID(),
                    beløp = 25000.månedlig,
                    måned = 1.februar.yearMonth,
                    type = LØNNSINNTEKT,
                    fordel = "",
                    beskrivelse = "",
                    tidsstempel = LocalDateTime.now()
                )
            ),
            ansattPerioder = listOf(AnsattPeriode(EPOCH, null))
        )
        val skattSykepengegrunnlag2 = SkattSykepengegrunnlag(
            hendelseId = UUID.randomUUID(),
            dato = skjæringstidspunkt,
            inntektsopplysninger = listOf(
                Skatteopplysning(
                    hendelseId = UUID.randomUUID(),
                    beløp = 25000.månedlig,
                    måned = 1.januar.yearMonth,
                    type = LØNNSINNTEKT,
                    fordel = "",
                    beskrivelse = "",
                    tidsstempel = LocalDateTime.now()
                )
            ),
            ansattPerioder = listOf(AnsattPeriode(EPOCH, null))
        )
        assertTrue(skattSykepengegrunnlag1.kanBrukes(skjæringstidspunkt))
        assertFalse(skattSykepengegrunnlag2.kanBrukes(skjæringstidspunkt))

        val resultat = skattSykepengegrunnlag1.somSykepengegrunnlag()
        assertEquals(SkattSykepengegrunnlag::class, resultat::class)
    }

    @Test
    fun `nytt arbeidsforhold innenfor 2 mnd telles som ikke rapportert inntekt`() {
        val skjæringstidspunkt = 2.april
        val skattSykepengegrunnlag1 = SkattSykepengegrunnlag(
            hendelseId = UUID.randomUUID(),
            dato = skjæringstidspunkt,
            inntektsopplysninger = emptyList(),
            ansattPerioder = listOf(AnsattPeriode(1.februar, null))
        )
        val skattSykepengegrunnlag2 = SkattSykepengegrunnlag(
            hendelseId = UUID.randomUUID(),
            dato = skjæringstidspunkt,
            inntektsopplysninger = emptyList(),
            ansattPerioder = listOf(AnsattPeriode(1.januar, null))
        )
        assertTrue(skattSykepengegrunnlag1.kanBrukes(skjæringstidspunkt))
        assertFalse(skattSykepengegrunnlag2.kanBrukes(skjæringstidspunkt))

        val resultat = skattSykepengegrunnlag1.somSykepengegrunnlag()
        assertEquals(IkkeRapportert::class, resultat::class)
    }

    @Test
    fun `sykepengegrunnlag for arbeidsgiver med nytt arbeidsforhold`() {
        val skatt = SkattSykepengegrunnlag(
            hendelseId = UUID.randomUUID(),
            dato = 1.februar,
            inntektsopplysninger = emptyList(),
            ansattPerioder = listOf(AnsattPeriode(1.januar, null))
        )
        val resultat = skatt.somSykepengegrunnlag()
        assertEquals(IkkeRapportert::class, resultat::class)
        assertEquals(INGEN, resultat.inspektør.beløp)
    }

}
