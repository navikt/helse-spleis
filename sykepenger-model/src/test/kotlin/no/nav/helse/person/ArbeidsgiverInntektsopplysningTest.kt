package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.inspectors.SubsumsjonInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.medInntekt
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.overstyrInntekter
import no.nav.helse.person.Inntektshistorikk.Skatt.Inntekttype.LØNNSINNTEKT
import no.nav.helse.person.Inntektshistorikk.Skatt.Sykepengegrunnlag
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.NullObserver
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class ArbeidsgiverInntektsopplysningTest {

    @Test
    fun `overstyr inntekter`() {
        val skjæringstidspunkt = 1.januar
        val opptjening = Opptjening(emptyList(), skjæringstidspunkt, NullObserver)
        val a1Opplysning = ArbeidsgiverInntektsopplysning("a1", Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), 1000.månedlig))
        val a2Opplysning = ArbeidsgiverInntektsopplysning("a2", Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), 2000.månedlig))
        val a1Overstyrt = ArbeidsgiverInntektsopplysning("a1", Inntektshistorikk.Saksbehandler(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), 3000.månedlig, "", null))
        val a3Overstyrt = ArbeidsgiverInntektsopplysning("a3", Inntektshistorikk.Saksbehandler(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), 4000.månedlig, "", null))

        val original = listOf(a1Opplysning, a2Opplysning)
        val new = listOf(a1Overstyrt)

        assertEquals(original, original.overstyrInntekter(opptjening, listOf(a1Overstyrt, a1Overstyrt), NullObserver)) { "kan ikke velge mellom inntekter for samme orgnr" }

        assertEquals(emptyList<ArbeidsgiverInntektsopplysning>(), emptyList<ArbeidsgiverInntektsopplysning>().overstyrInntekter(opptjening, new, NullObserver))
        assertEquals(original, original.overstyrInntekter(opptjening, emptyList(), NullObserver))
        assertEquals(listOf(a1Overstyrt, a2Opplysning), original.overstyrInntekter(opptjening, new, NullObserver))
        val forMange = listOf(a1Overstyrt, a3Overstyrt)
        assertEquals(listOf(a1Overstyrt, a2Opplysning), original.overstyrInntekter(opptjening, forMange, NullObserver)) { "skal ikke kunne legge til inntekter som ikke finnes fra før" }
    }

    @Test
    fun `subsummerer etter overstyring`() {
        val skjæringstidspunkt = 1.april
        val ansattFom = 1.januar
        val orgnummer = "a1"

        val opptjening = Opptjening(listOf(
            Opptjening.ArbeidsgiverOpptjeningsgrunnlag(orgnummer, listOf(Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom, null, false)))
        ), skjæringstidspunkt, NullObserver)


        val paragraf = Paragraf.PARAGRAF_8_28
        val ledd = Ledd.LEDD_3
        val bokstav = Bokstav.BOKSTAV_B
        val overstyrtBeløp = 3000.månedlig

        val subsumsjon = Subsumsjon(paragraf.ref, ledd.nummer, bokstav.ref.toString())
        val a1Opplysning = ArbeidsgiverInntektsopplysning(orgnummer, Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), 1000.månedlig))
        val a1Overstyrt = ArbeidsgiverInntektsopplysning(orgnummer, Inntektshistorikk.Saksbehandler(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), overstyrtBeløp, "Jeg bare måtte gjøre det", subsumsjon))

        val jurist = MaskinellJurist()
        listOf(a1Opplysning).overstyrInntekter(opptjening, listOf(a1Overstyrt), jurist)
        SubsumsjonInspektør(jurist).assertBeregnet(
            paragraf = paragraf,
            versjon = LocalDate.of(2019, 1, 1),
            ledd = ledd,
            punktum = null,
            bokstav = bokstav,
            input = mapOf(
                "organisasjonsnummer" to orgnummer,
                "skjæringstidspunkt" to skjæringstidspunkt,
                "startdatoArbeidsforhold" to ansattFom,
                "overstyrtInntektFraSaksbehandler" to mapOf("dato" to skjæringstidspunkt, "beløp" to overstyrtBeløp.reflection { _, månedlig, _, _ -> månedlig }),
                "forklaring" to "Jeg bare måtte gjøre det"
            ),
            output = mapOf(
                "beregnetGrunnlagForSykepengegrunnlagPrÅr" to overstyrtBeløp.reflection { årlig, _, _, _ -> årlig},
                "beregnetGrunnlagForSykepengegrunnlagPrMåned" to overstyrtBeløp.reflection { _, månedlig, _, _ -> månedlig }
            )
        )
    }

    @Test
    fun equals() {
        val inntektID = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        val tidsstempel = LocalDateTime.now()
        val inntektsopplysning1 = ArbeidsgiverInntektsopplysning(
            orgnummer = "orgnummer",
            inntektsopplysning = Inntektshistorikk.Infotrygd(
                id = inntektID,
                dato = 1.januar,
                hendelseId = hendelseId,
                beløp = 25000.månedlig,
                tidsstempel = tidsstempel
            )
        )
        assertEquals(
            inntektsopplysning1,
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
        )
        assertNotEquals(
            inntektsopplysning1,
            ArbeidsgiverInntektsopplysning(
                orgnummer = "orgnummer2",
                inntektsopplysning = Inntektshistorikk.Infotrygd(
                    id = inntektID,
                    dato = 1.januar,
                    hendelseId = hendelseId,
                    beløp = 25000.månedlig,
                    tidsstempel = tidsstempel
                )
            )
        )
        assertNotEquals(
            inntektsopplysning1,
            ArbeidsgiverInntektsopplysning(
                orgnummer = "orgnummer",
                inntektsopplysning = Inntektshistorikk.Infotrygd(
                    id = inntektID,
                    dato = 5.januar,
                    hendelseId = hendelseId,
                    beløp = 25000.månedlig,
                    tidsstempel = tidsstempel
                )
            )
        )
    }

    @Test
    fun `setter negativt omregnet årsinntekt til 0`() {
        val arbeidsgiverInntektsopplysning = ArbeidsgiverInntektsopplysning(
            "orgnummer",
            Inntektshistorikk.SkattComposite(
                UUID.randomUUID(), inntektsopplysninger = listOf(
                    Sykepengegrunnlag(
                        dato = 1.januar,
                        hendelseId = UUID.randomUUID(),
                        beløp = (-2500).daglig,
                        måned = desember(2017),
                        type = LØNNSINNTEKT,
                        fordel = "fordel",
                        beskrivelse = "beskrivelse"
                    )
                )
            )
        )

        val økonomi = listOf(arbeidsgiverInntektsopplysning).medInntekt(
            organisasjonsnummer = "orgnummer",
            skjæringstidspunkt = 1.januar,
            dato = 1.januar,
            økonomi = Økonomi.sykdomsgrad(100.prosent),
            regler = NormalArbeidstaker,
            subsumsjonObserver = NullObserver,
            arbeidsgiverperiode = null
        )
        assertNotNull(økonomi)
        assertEquals(Inntekt.INGEN, økonomi.inspektør.dekningsgrunnlag)
        assertEquals(Inntekt.INGEN, økonomi.inspektør.aktuellDagsinntekt)
    }
}