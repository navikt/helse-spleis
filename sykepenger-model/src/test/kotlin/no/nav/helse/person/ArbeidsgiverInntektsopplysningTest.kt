package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Grunnbeløp.Companion.`6G`
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.inspectors.SubsumsjonInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.aktiver
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.deaktiver
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.medInntekt
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.nyeRefusjonsopplysninger
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.overstyrInntekter
import no.nav.helse.person.Inntektshistorikk.Skatt.Inntekttype.LØNNSINNTEKT
import no.nav.helse.person.Inntektshistorikk.Skatt.Sykepengegrunnlag
import no.nav.helse.person.Refusjonsopplysning.Refusjonsopplysninger
import no.nav.helse.person.Refusjonsopplysning.Refusjonsopplysninger.Companion.refusjonsopplysninger
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ArbeidsgiverInntektsopplysningTest {

    @Test
    fun `overstyr inntekter`() {
        val skjæringstidspunkt = 1.januar
        val opptjening = Opptjening(emptyList(), skjæringstidspunkt, NullObserver)
        val a1Opplysning = ArbeidsgiverInntektsopplysning("a1", Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), 1000.månedlig), Refusjonsopplysninger())
        val a2Opplysning = ArbeidsgiverInntektsopplysning("a2", Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), 2000.månedlig), Refusjonsopplysninger())
        val a1Overstyrt = ArbeidsgiverInntektsopplysning("a1", Inntektshistorikk.Saksbehandler(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), 3000.månedlig, "", null), Refusjonsopplysninger())
        val a3Overstyrt = ArbeidsgiverInntektsopplysning("a3", Inntektshistorikk.Saksbehandler(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), 4000.månedlig, "", null), Refusjonsopplysninger())

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
    fun `To like refusjonsopplysninger`() {
        val inntektsmeldingId = UUID.randomUUID()
        val refusjonsopplysninger = Refusjonsopplysning(inntektsmeldingId, 1.januar, null, 1000.månedlig).refusjonsopplysninger
        val arbeidsgiverInntektsopplysning = ArbeidsgiverInntektsopplysning(
            orgnummer = "a1",
            inntektsopplysning = Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), 1000.månedlig),
            refusjonsopplysninger = refusjonsopplysninger
        )
        assertTrue(arbeidsgiverInntektsopplysning == listOf(arbeidsgiverInntektsopplysning).nyeRefusjonsopplysninger("a1", refusjonsopplysninger).single())
        assertTrue(arbeidsgiverInntektsopplysning === listOf(arbeidsgiverInntektsopplysning).nyeRefusjonsopplysninger("a1", refusjonsopplysninger).single())
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
        val a1Opplysning = ArbeidsgiverInntektsopplysning(orgnummer, Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), 1000.månedlig), Refusjonsopplysninger())
        val a1Overstyrt = ArbeidsgiverInntektsopplysning(orgnummer, Inntektshistorikk.Saksbehandler(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), overstyrtBeløp, "Jeg bare måtte gjøre det", subsumsjon), Refusjonsopplysninger())

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
    fun `deaktiverer en inntekt`() {
        val skjæringstidspunkt = 1.januar
        val a1Opplysning = ArbeidsgiverInntektsopplysning("a1", Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), 1000.månedlig), Refusjonsopplysninger())
        val a2Opplysning = ArbeidsgiverInntektsopplysning("a2", Inntektshistorikk.IkkeRapportert(UUID.randomUUID(), skjæringstidspunkt), Refusjonsopplysninger())

        val opprinnelig = listOf(a1Opplysning, a2Opplysning)
        val (aktive, deaktiverte) = opprinnelig.deaktiver(emptyList(), "a2", "Denne må bort", NullObserver)
        assertEquals(a1Opplysning, aktive.single())
        assertEquals(a2Opplysning, deaktiverte.single())

        val (nyDeaktivert, nyAktivert) = deaktiverte.aktiver(aktive, "a2", "Jeg gjorde en feil, jeg angrer!", NullObserver)
        assertEquals(0, nyDeaktivert.size)
        assertEquals(opprinnelig, nyAktivert)

        assertThrows<RuntimeException> { opprinnelig.deaktiver(emptyList(), "a3", "jeg vil deaktivere noe som ikke finnes", NullObserver) }
        assertThrows<RuntimeException> { emptyList<ArbeidsgiverInntektsopplysning>().aktiver(opprinnelig, "a3", "jeg vil aktivere noe som ikke finnes", NullObserver) }
    }

    @Test
    fun `subsummerer deaktivering`() {
        val skjæringstidspunkt = 1.januar
        val a1Opplysning = ArbeidsgiverInntektsopplysning("a1", Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), 1000.månedlig), Refusjonsopplysninger())
        val a2Opplysning = ArbeidsgiverInntektsopplysning("a2", Inntektshistorikk.IkkeRapportert(UUID.randomUUID(), skjæringstidspunkt), Refusjonsopplysninger())

        val jurist = MaskinellJurist()
        val opprinnelig = listOf(a1Opplysning, a2Opplysning)
        val (aktive, deaktiverte) = opprinnelig.deaktiver(emptyList(), "a2", "Denne må bort", jurist)
        assertEquals(a1Opplysning, aktive.single())
        assertEquals(a2Opplysning, deaktiverte.single())
        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = Paragraf.PARAGRAF_8_15,
            versjon = LocalDate.of(1998, 12, 18),
            ledd = null,
            punktum = null,
            bokstav = null,
            input = mapOf(
                "organisasjonsnummer" to "a2",
                "skjæringstidspunkt" to skjæringstidspunkt,
                "inntekterSisteTreMåneder" to emptyList<Any>(),
                "forklaring" to "Denne må bort"
            ),
            output = mapOf("arbeidsforholdAvbrutt" to "a2")
        )
    }

    @Test
    fun `subsummerer aktivering`() {
        val skjæringstidspunkt = 1.januar
        val a1Opplysning = ArbeidsgiverInntektsopplysning("a1", Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), 1000.månedlig), Refusjonsopplysninger())
        val a2Opplysning = ArbeidsgiverInntektsopplysning("a2", Inntektshistorikk.IkkeRapportert(UUID.randomUUID(), skjæringstidspunkt), Refusjonsopplysninger())

        val jurist = MaskinellJurist()
        val opprinneligAktive = listOf(a1Opplysning)
        val opprinneligDeaktiverte = listOf(a2Opplysning)

        val (deaktiverte, aktive) = opprinneligDeaktiverte.aktiver(opprinneligAktive, "a2", "Denne må tilbake", jurist)
        assertEquals(listOf(a1Opplysning, a2Opplysning), aktive)
        assertEquals(0, deaktiverte.size)
        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            paragraf = Paragraf.PARAGRAF_8_15,
            versjon = LocalDate.of(1998, 12, 18),
            ledd = null,
            punktum = null,
            bokstav = null,
            input = mapOf(
                "organisasjonsnummer" to "a2",
                "skjæringstidspunkt" to skjæringstidspunkt,
                "inntekterSisteTreMåneder" to emptyList<Any>(),
                "forklaring" to "Denne må tilbake"
            ),
            output = mapOf("aktivtArbeidsforhold" to "a2")
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
            ),
            refusjonsopplysninger = Refusjonsopplysninger()
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
                ),
                refusjonsopplysninger = Refusjonsopplysninger()
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
                ),
                refusjonsopplysninger = Refusjonsopplysninger()
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
                ),
                refusjonsopplysninger = Refusjonsopplysninger()
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
            ),
            Refusjonsopplysninger()
        )

        val økonomi = listOf(arbeidsgiverInntektsopplysning).medInntekt(
            organisasjonsnummer = "orgnummer",
            `6G` = `6G`.beløp(1.januar),
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