package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.desember
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.*
import no.nav.helse.spleis.e2e.OpptjeningE2ETest.ArbeidsforholdVisitor.Companion.assertHarArbeidsforhold
import no.nav.helse.spleis.e2e.OpptjeningE2ETest.ArbeidsforholdVisitor.Companion.assertHarIkkeArbeidsforhold
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class OpptjeningE2ETest : AbstractEndToEndTest() {
    @BeforeEach
    fun setup() {
        Toggle.OpptjeningIModellen.enable()
    }

    @AfterEach
    fun tearDown() {
        Toggle.OpptjeningIModellen.disable()
    }

    @Test
    fun `lagrer arbeidsforhold brukt til opptjening`() {
        personMedArbeidsforhold(Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null))
        assertHarArbeidsforhold(1.januar, a1)
        assertHarIkkeArbeidsforhold(1.januar, a2)
    }

    @Test
    fun `lagrer flere arbeidsforhold brukt til opptjening`() {
        personMedArbeidsforhold(
            Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
        )
        assertHarArbeidsforhold(1.januar, a1)
        assertHarArbeidsforhold(1.januar, a2)
    }

    @Test
    fun `lagrer arbeidsforhold brukt til opptjening om tilstøtende`() {
        personMedArbeidsforhold(
            Vilkårsgrunnlag.Arbeidsforhold(a1, 20.desember(2017), null),
            Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, 19.desember(2017))
        )
        assertHarArbeidsforhold(1.januar, a1)
        assertHarArbeidsforhold(1.januar, a2)
    }

    @Test
    fun `lagrer arbeidsforhold brukt til opptjening ved overlapp`() {
        personMedArbeidsforhold(
            Vilkårsgrunnlag.Arbeidsforhold(a1, 1.desember(2017), null),
            Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, 24.desember(2017))
        )
        assertHarArbeidsforhold(1.januar, a1)
        assertHarArbeidsforhold(1.januar, a2)
    }

    @Test
    fun `opptjening er ikke oppfylt siden det ikke er nok opptjeningsdager`() {
        personMedArbeidsforhold(Vilkårsgrunnlag.Arbeidsforhold(a1, ansattFom = 31.desember(2017), ansattTom = null))
        val grunnlagsdata = person.vilkårsgrunnlagFor(1.januar) as VilkårsgrunnlagHistorikk.Grunnlagsdata
        assertEquals(1, grunnlagsdata.opptjening?.opptjeningsdager())
        assertEquals(false, grunnlagsdata.opptjening?.erOppfylt())
        assertWarning("Perioden er avslått på grunn av manglende opptjening", 1.vedtaksperiode.filter(orgnummer = a1))
    }

    @Test
    fun `tar ikke med inntekter fra A-Ordningen dersom arbeidsforholdet kun er brukt til opptjening og ikke gjelder under skjæringstidspunktet`() = Toggle.OpptjeningIModellen.disable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar)

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(1))
        )

        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Vilkårsgrunnlag.Arbeidsforhold(a2, 1.desember(2017), 31.desember(2017))
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(1))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntekter, arbeidsforhold = emptyList()),
            arbeidsforhold = arbeidsforhold,
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        assertEquals(setOf(a1), person.vilkårsgrunnlagFor(1.januar)?.sykepengegrunnlag()?.inntektsopplysningPerArbeidsgiver()?.keys)
        assertEquals(Inntektskilde.EN_ARBEIDSGIVER, inspektør(a1).inntektskilde(1.vedtaksperiode))
    }

    fun personMedArbeidsforhold(vararg arbeidsforhold: Vilkårsgrunnlag.Arbeidsforhold) {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode, arbeidsforhold = arbeidsforhold.toList(), orgnummer = a1)
    }

    internal class ArbeidsforholdVisitor(val forventetArbeidsforhold: String, val forventetSkjæringstidspunkt: LocalDate) : PersonVisitor {
        private var erIRiktigArbeidsgiver = false
        private var erIRiktigSkjæringstidspunkt = false
        private var harBesøktArbeidsforhold = false

        override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
            erIRiktigArbeidsgiver = organisasjonsnummer == forventetArbeidsforhold
        }

        override fun preVisitArbeidsforholdinnslag(arbeidsforholdinnslag: Arbeidsforholdhistorikk.Innslag, id: UUID, skjæringstidspunkt: LocalDate) {
            erIRiktigSkjæringstidspunkt = forventetSkjæringstidspunkt == skjæringstidspunkt
        }

        override fun visitArbeidsforhold(ansattFom: LocalDate, ansattTom: LocalDate?, deaktivert: Boolean) {
            if (erIRiktigSkjæringstidspunkt && erIRiktigArbeidsgiver) {
                harBesøktArbeidsforhold = true
            }
        }

        companion object {
            fun AbstractEndToEndTest.assertHarArbeidsforhold(skjæringstidspunkt: LocalDate, arbeidsforhold: String) {
                val visitor = ArbeidsforholdVisitor(arbeidsforhold, skjæringstidspunkt)
                person.accept(visitor)
                assertTrue(visitor.harBesøktArbeidsforhold, "Fant ikke arbeidsforhold for $arbeidsforhold på skjæringstidspunkt $skjæringstidspunkt")
            }

            fun AbstractEndToEndTest.assertHarIkkeArbeidsforhold(skjæringstidspunkt: LocalDate, arbeidsforhold: String) {
                val visitor = ArbeidsforholdVisitor(arbeidsforhold, skjæringstidspunkt)
                person.accept(visitor)
                assertFalse(visitor.harBesøktArbeidsforhold, "Fant uforventet arbeidsforhold for $arbeidsforhold på skjæringstidspunkt $skjæringstidspunkt")
            }
        }
    }
}
