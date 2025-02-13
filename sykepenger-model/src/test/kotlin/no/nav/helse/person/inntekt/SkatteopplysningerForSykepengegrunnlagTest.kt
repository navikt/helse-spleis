package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDate.EPOCH
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.inntekt.Skatteopplysning.Inntekttype.LØNNSINNTEKT
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SkatteopplysningerForSykepengegrunnlagTest {

    @Test
    fun `ikke ansatt på skjæringstidspunktet`() {
        val skatt = opplysninger(
            skjæringstidspunkt = 10.april,
            skatteopplysninger = listOf(
                opplysning(mars(2018)),
                opplysning(februar(2018)),
                opplysning(januar(2018))
            ),
            ansattPerioder = emptyList()
        )
        assertTrue(skatt.harInntekterToMånederFørSkjæringstidspunkt)
        assertFalse(skatt.ansattVedSkjæringstidspunkt)
        assertFalse(skatt.erNyoppstartetArbeidsforhold)
        assertFalse(skatt.erGhostarbeidsgiver)
    }

    @Test
    fun `nyoppstartet arbeidsforhold og ingen inntekter - mer enn to måneder fra skjæringstidspunktet`() {
        val skatt = opplysninger(
            skjæringstidspunkt = 10.april,
            skatteopplysninger = emptyList(),
            ansattPerioder = listOf(
                SkatteopplysningerForSykepengegrunnlag.AnsattPeriode(
                    ansattFom = 5.januar,
                    ansattTom = null
                )
            )
        )
        assertFalse(skatt.harInntekterToMånederFørSkjæringstidspunkt)
        assertTrue(skatt.ansattVedSkjæringstidspunkt)
        assertFalse(skatt.erNyoppstartetArbeidsforhold)
        assertFalse(skatt.erGhostarbeidsgiver)
    }

    @Test
    fun `nyoppstartet arbeidsforhold og ingen inntekter - innen to måneder fra skjæringstidspunktet`() {
        val skatt = opplysninger(
            skjæringstidspunkt = 10.april,
            skatteopplysninger = emptyList(),
            ansattPerioder = listOf(
                SkatteopplysningerForSykepengegrunnlag.AnsattPeriode(
                    ansattFom = 5.februar,
                    ansattTom = null
                )
            )
        )
        assertFalse(skatt.harInntekterToMånederFørSkjæringstidspunkt)
        assertTrue(skatt.ansattVedSkjæringstidspunkt)
        assertTrue(skatt.erNyoppstartetArbeidsforhold)
        assertTrue(skatt.erGhostarbeidsgiver)
    }

    @Test
    fun `nyoppstartet arbeidsforhold - uten inntekt innen to måneder fra skjæringstidspunktet`() {
        val skatt = opplysninger(
            skjæringstidspunkt = 10.april,
            skatteopplysninger = listOf(
                opplysning(januar(2018))
            ),
            ansattPerioder = listOf(
                SkatteopplysningerForSykepengegrunnlag.AnsattPeriode(
                    ansattFom = 5.februar,
                    ansattTom = null
                )
            )
        )
        assertFalse(skatt.harInntekterToMånederFørSkjæringstidspunkt)
        assertTrue(skatt.ansattVedSkjæringstidspunkt)
        assertFalse(skatt.erNyoppstartetArbeidsforhold)
        assertFalse(skatt.erGhostarbeidsgiver)
    }

    @Test
    fun `nyoppstartet arbeidsforhold - med inntekt innen to måneder fra skjæringstidspunktet`() {
        val skatt = opplysninger(
            skjæringstidspunkt = 10.april,
            skatteopplysninger = listOf(
                opplysning(februar(2018)),
            ),
            ansattPerioder = listOf(
                SkatteopplysningerForSykepengegrunnlag.AnsattPeriode(
                    ansattFom = 5.februar,
                    ansattTom = null
                )
            )
        )
        assertTrue(skatt.harInntekterToMånederFørSkjæringstidspunkt)
        assertTrue(skatt.ansattVedSkjæringstidspunkt)
        assertFalse(skatt.erNyoppstartetArbeidsforhold)
        assertTrue(skatt.erGhostarbeidsgiver)
    }

    @Test
    fun `med inntekt innen to måneder fra skjæringstidspunktet`() {
        val skatt = opplysninger(
            skjæringstidspunkt = 10.april,
            skatteopplysninger = listOf(
                opplysning(februar(2018)),
            ),
            ansattPerioder = listOf(
                SkatteopplysningerForSykepengegrunnlag.AnsattPeriode(
                    ansattFom = EPOCH,
                    ansattTom = null
                )
            )
        )
        assertTrue(skatt.harInntekterToMånederFørSkjæringstidspunkt)
        assertTrue(skatt.ansattVedSkjæringstidspunkt)
        assertFalse(skatt.erNyoppstartetArbeidsforhold)
        assertTrue(skatt.erGhostarbeidsgiver)
    }

    @Test
    fun `uten inntekt innen to måneder fra skjæringstidspunktet`() {
        val skatt = opplysninger(
            skjæringstidspunkt = 10.april,
            skatteopplysninger = listOf(
                opplysning(januar(2018)),
            ),
            ansattPerioder = listOf(
                SkatteopplysningerForSykepengegrunnlag.AnsattPeriode(
                    ansattFom = EPOCH,
                    ansattTom = null
                )
            )
        )
        assertFalse(skatt.harInntekterToMånederFørSkjæringstidspunkt)
        assertTrue(skatt.ansattVedSkjæringstidspunkt)
        assertFalse(skatt.erNyoppstartetArbeidsforhold)
        assertFalse(skatt.erGhostarbeidsgiver)
    }

    @Test
    fun `ingen inntekter`() {
        val skatt = opplysninger(
            skjæringstidspunkt = 10.april,
            skatteopplysninger = emptyList(),
            ansattPerioder = listOf(
                SkatteopplysningerForSykepengegrunnlag.AnsattPeriode(
                    ansattFom = EPOCH,
                    ansattTom = null
                )
            )
        )
        assertFalse(skatt.harInntekterToMånederFørSkjæringstidspunkt)
        assertTrue(skatt.ansattVedSkjæringstidspunkt)
        assertFalse(skatt.erNyoppstartetArbeidsforhold)
        assertFalse(skatt.erGhostarbeidsgiver)
    }

    @Test
    fun `ser bort fra inntekter utenfor beregningsperioden`() {
        val skatt = opplysninger(
            skjæringstidspunkt = 10.april,
            skatteopplysninger = listOf(
                opplysning(april(2018), 10_000.månedlig),
                opplysning(mars(2018), 3000.månedlig),
                opplysning(februar(2018), 1500.månedlig),
                opplysning(januar(2018), 1500.månedlig),
                opplysning(desember(2017), 10_000.månedlig),
            ),
            ansattPerioder = listOf(
                SkatteopplysningerForSykepengegrunnlag.AnsattPeriode(
                    ansattFom = EPOCH,
                    ansattTom = null
                )
            )
        )
        val forventetSnitt = (3000.månedlig + 1500.månedlig + 1500.månedlig) / 3
        assertTrue(skatt.harInntekterToMånederFørSkjæringstidspunkt)
        assertTrue(skatt.ansattVedSkjæringstidspunkt)
        assertFalse(skatt.erNyoppstartetArbeidsforhold)
        assertTrue(skatt.erGhostarbeidsgiver)
        assertEquals(forventetSnitt, skatt.inntektsdata.beløp)
    }

    @Test
    fun `teller negative beløp som 0 kr`() {
        val skatt = opplysninger(
            skjæringstidspunkt = 10.april,
            skatteopplysninger = listOf(
                opplysning(mars(2018), (-1500).månedlig),
            ),
            ansattPerioder = listOf(
                SkatteopplysningerForSykepengegrunnlag.AnsattPeriode(
                    ansattFom = EPOCH,
                    ansattTom = null
                )
            )
        )
        assertTrue(skatt.harInntekterToMånederFørSkjæringstidspunkt)
        assertTrue(skatt.ansattVedSkjæringstidspunkt)
        assertFalse(skatt.erNyoppstartetArbeidsforhold)
        assertTrue(skatt.erGhostarbeidsgiver)
        assertEquals(INGEN, skatt.inntektsdata.beløp)
    }

    private fun opplysninger(
        skjæringstidspunkt: LocalDate,
        skatteopplysninger: List<Skatteopplysning> = emptyList(),
        ansattPerioder: List<SkatteopplysningerForSykepengegrunnlag.AnsattPeriode> = emptyList()
    ) =
        SkatteopplysningerForSykepengegrunnlag(
            arbeidsgiver = "arbeidsgiver",
            hendelseId = MeldingsreferanseId(UUID.randomUUID()),
            skjæringstidspunkt = skjæringstidspunkt,
            inntektsopplysninger = skatteopplysninger,
            ansattPerioder = ansattPerioder,
            tidsstempel = LocalDateTime.now()
        )

    private fun opplysning(måned: YearMonth, beløp: Inntekt = 1000.daglig) =
        Skatteopplysning(
            hendelseId = MeldingsreferanseId(UUID.randomUUID()),
            beløp = beløp,
            måned = måned,
            type = LØNNSINNTEKT,
            fordel = "fordel",
            beskrivelse = "beskrivelse"
        )
}
