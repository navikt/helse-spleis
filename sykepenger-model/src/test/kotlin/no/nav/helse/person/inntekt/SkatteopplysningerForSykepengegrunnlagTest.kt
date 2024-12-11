package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDate.EPOCH
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.inntekt.Skatteopplysning.Inntekttype.LØNNSINNTEKT
import no.nav.helse.testhelpers.assertInstanceOf
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
        assertNull(skatt.ghostInntektsgrunnlag(10.april))
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
        assertNull(skatt.ghostInntektsgrunnlag(10.april))
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
        assertInstanceOf<IkkeRapportert>(skatt.ghostInntektsgrunnlag(10.april))
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
        assertNull(skatt.ghostInntektsgrunnlag(10.april))
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
        assertInstanceOf<SkattSykepengegrunnlag>(skatt.ghostInntektsgrunnlag(10.april))
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
        assertInstanceOf<SkattSykepengegrunnlag>(skatt.ghostInntektsgrunnlag(10.april))
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
        assertNull(skatt.ghostInntektsgrunnlag(10.april))
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
        assertNull(skatt.ghostInntektsgrunnlag(10.april))
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
        assertEquals(forventetSnitt, skatt.ghostInntektsgrunnlag(10.april)!!.fastsattÅrsinntekt())
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
        assertEquals(INGEN, skatt.ghostInntektsgrunnlag(10.april)!!.fastsattÅrsinntekt())
    }

    private fun opplysninger(
        skjæringstidspunkt: LocalDate,
        skatteopplysninger: List<Skatteopplysning> = emptyList(),
        ansattPerioder: List<SkatteopplysningerForSykepengegrunnlag.AnsattPeriode> = emptyList()
    ) =
        SkatteopplysningerForSykepengegrunnlag(
            arbeidsgiver = "arbeidsgiver",
            hendelseId = UUID.randomUUID(),
            skjæringstidspunkt = skjæringstidspunkt,
            inntektsopplysninger = skatteopplysninger,
            ansattPerioder = ansattPerioder,
            tidsstempel = LocalDateTime.now()
        )

    private fun opplysning(måned: YearMonth, beløp: Inntekt = 1000.daglig) =
        Skatteopplysning(
            hendelseId = UUID.randomUUID(),
            beløp = beløp,
            måned = måned,
            type = LØNNSINNTEKT,
            fordel = "fordel",
            beskrivelse = "beskrivelse"
        )
}
