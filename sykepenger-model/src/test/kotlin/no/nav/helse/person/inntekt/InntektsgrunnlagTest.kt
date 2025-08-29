package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Grunnbeløp
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.inntektsgrunnlag
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.ArbeidstakerOpptjening
import no.nav.helse.person.ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_8
import no.nav.helse.person.inntekt.Skatteopplysning.Inntekttype.LØNNSINNTEKT
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.sykepengegrunnlag
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.yearMonth
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class InntektsgrunnlagTest {

    @Test
    fun `justerer grunnbeløpet`() {
        val sykepengegrunnlag = 60000.månedlig.inntektsgrunnlag("orgnr", 1.mai(2020), 1.mai(2020))
        val justert = sykepengegrunnlag.grunnbeløpsregulering()
        assertNotNull(justert)
        assertNotSame(sykepengegrunnlag, justert)
        assertNotEquals(sykepengegrunnlag.inspektør.sykepengegrunnlag, justert.inspektør.sykepengegrunnlag)
        assertNotEquals(sykepengegrunnlag.inspektør.`6G`, justert.inspektør.`6G`)
        assertTrue(sykepengegrunnlag.inspektør.`6G` < justert.inspektør.`6G`)
        assertTrue(sykepengegrunnlag.inspektør.sykepengegrunnlag < justert.inspektør.sykepengegrunnlag)
    }

    @Test
    fun `sykepengegrunnlaget skal ikke rundes av - 6g-begrenset`() {
        val `6G` = Grunnbeløp.`6G`.beløp(1.januar)
        val sykepengegrunnlag = `6G`.sykepengegrunnlag
        assertEquals(`6G`, sykepengegrunnlag.inspektør.sykepengegrunnlag)
    }

    @Test
    fun `sykepengegrunnlaget skal ikke rundes av - under 6`() {
        val daglig = 255.5.daglig
        val sykepengegrunnlag = daglig.sykepengegrunnlag
        assertEquals(daglig, sykepengegrunnlag.inspektør.sykepengegrunnlag)
    }

    @Test
    fun `lager varsel dersom arbeidsforhold har opphørt`() {
        val skjæringstidspunkt = 1.mars
        val sluttdatoA1 = skjæringstidspunkt.minusMonths(1).withDayOfMonth(1)
        val startdatoA2 = skjæringstidspunkt.minusMonths(1).withDayOfMonth(2)

        val inntektsgrunnlag = Inntektsgrunnlag(
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgiverInntektsopplysninger = listOf<ArbeidsgiverInntektsopplysning>(
                ArbeidsgiverInntektsopplysning(
                    orgnummer = a1,
                    faktaavklartInntekt = arbeidsgiverinntekt(
                        dato = skjæringstidspunkt,
                        beløp = 25000.månedlig
                    ),
                    korrigertInntekt = null,
                    skjønnsmessigFastsatt = null
                ),
                ArbeidsgiverInntektsopplysning(
                    orgnummer = a2,
                    faktaavklartInntekt = skattSykepengegrunnlag(
                        hendelseId = UUID.randomUUID(),
                        dato = skjæringstidspunkt,
                        inntektsopplysninger = listOf(
                            Skatteopplysning(
                                hendelseId = MeldingsreferanseId(UUID.randomUUID()),
                                beløp = 25000.månedlig,
                                måned = 1.januar.yearMonth,
                                type = LØNNSINNTEKT,
                                fordel = "",
                                beskrivelse = "",
                                tidsstempel = LocalDateTime.now()
                            )
                        )
                    ),
                    korrigertInntekt = null,
                    skjønnsmessigFastsatt = null
                )
            ),
            selvstendigInntektsopplysning = null,
            deaktiverteArbeidsforhold = emptyList(),
            vurdertInfotrygd = false
        )

        val opptjening = ArbeidstakerOpptjening.nyOpptjening(
            listOf(
                ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag(
                    a1, listOf(
                    Arbeidsforhold(
                        ansattFom = LocalDate.EPOCH,
                        ansattTom = sluttdatoA1,
                        deaktivert = false
                    )
                )
                ),
                ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag(
                    a2, listOf(
                    Arbeidsforhold(
                        ansattFom = startdatoA2,
                        ansattTom = null,
                        deaktivert = false
                    )
                )
                )
            ), skjæringstidspunkt
        )

        Aktivitetslogg().also { aktivitetslogg ->
            inntektsgrunnlag.sjekkForNyArbeidsgiver(aktivitetslogg, opptjening, a1)
            aktivitetslogg.assertVarsel(RV_VV_8)
        }

        Aktivitetslogg().also { aktivitetslogg ->
            inntektsgrunnlag.sjekkForNyArbeidsgiver(aktivitetslogg, opptjening, a2)
            aktivitetslogg.assertVarsler(emptyList())
        }
    }

    @Test
    fun `lager varsel dersom en arbeidsgiver i sykepengegrunnlaget ikke har registrert opptjening`() {
        val skjæringstidspunkt = 1.mars
        val inntektsgrunnlag = Inntektsgrunnlag(
            skjæringstidspunkt = skjæringstidspunkt,
            selvstendigInntektsopplysning = null,
            arbeidsgiverInntektsopplysninger = listOf<ArbeidsgiverInntektsopplysning>(
                ArbeidsgiverInntektsopplysning(
                    orgnummer = a1,
                    faktaavklartInntekt = arbeidsgiverinntekt(
                        dato = skjæringstidspunkt,
                        beløp = 25000.månedlig
                    ),
                    korrigertInntekt = null,
                    skjønnsmessigFastsatt = null
                ),
                ArbeidsgiverInntektsopplysning(
                    orgnummer = a2,
                    faktaavklartInntekt = arbeidsgiverinntekt(
                        dato = skjæringstidspunkt,
                        beløp = 25000.månedlig
                    ),
                    korrigertInntekt = null,
                    skjønnsmessigFastsatt = null
                )
            ),
            deaktiverteArbeidsforhold = emptyList<ArbeidsgiverInntektsopplysning>(),
            vurdertInfotrygd = false
        )

        val opptjeningUtenA2 = ArbeidstakerOpptjening.nyOpptjening(
            listOf(
                ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag(
                    a1, listOf(
                    Arbeidsforhold(
                        ansattFom = LocalDate.EPOCH,
                        ansattTom = null,
                        deaktivert = false
                    )
                )
                )
            ), skjæringstidspunkt
        )
        val opptjeningMedA2 = ArbeidstakerOpptjening.nyOpptjening(
            listOf(
                ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag(
                    a1, listOf(
                    Arbeidsforhold(
                        ansattFom = LocalDate.EPOCH,
                        ansattTom = null,
                        deaktivert = false
                    )
                )
                ),
                ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag(
                    a2, listOf(
                    Arbeidsforhold(
                        ansattFom = LocalDate.EPOCH,
                        ansattTom = null,
                        deaktivert = false
                    )
                )
                )
            ), skjæringstidspunkt
        )

        Aktivitetslogg().also { aktivitetslogg ->
            inntektsgrunnlag.måHaRegistrertOpptjeningForArbeidsgivere(aktivitetslogg, opptjeningUtenA2)
            aktivitetslogg.assertVarsel(RV_VV_1)
        }

        Aktivitetslogg().also { aktivitetslogg ->
            inntektsgrunnlag.måHaRegistrertOpptjeningForArbeidsgivere(aktivitetslogg, opptjeningMedA2)
            aktivitetslogg.assertVarsler(emptyList())
        }
    }
}
