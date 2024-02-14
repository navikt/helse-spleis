package no.nav.helse.serde.api

import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import no.nav.helse.desember
import no.nav.helse.etterspurtBehov
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.serde.api.dto.Arbeidsgiverinntekt
import no.nav.helse.serde.api.dto.BeregnetPeriode
import no.nav.helse.serde.api.dto.GhostPeriodeDTO
import no.nav.helse.serde.api.dto.Inntekt
import no.nav.helse.serde.api.dto.InntekterFraAOrdningen
import no.nav.helse.serde.api.dto.Inntektkilde
import no.nav.helse.serde.api.dto.SpleisVilkårsgrunnlag
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrArbeidsforhold
import no.nav.helse.spleis.e2e.håndterOverstyrInntekt
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyeVedtak
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.speilApi
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

internal class SpeilBuilderFlereAGTest : AbstractEndToEndTest() {

    @Test
    fun `sender med ghost tidslinjer til speil`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1,)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 10000.månedlig
                }
            }, emptyList()),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT)
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val speilJson = serializePersonForSpeil(person, observatør.spekemat.resultat())
        assertEquals(
            emptyList<GhostPeriodeDTO>(),
            speilJson.arbeidsgivere.single { it.organisasjonsnummer == a1 }.ghostPerioder
        )


        val perioder = speilJson.arbeidsgivere.single { it.organisasjonsnummer == a2 }.ghostPerioder

        assertEquals(1, perioder.size)
        val actual = perioder.first()
        val expected = GhostPeriodeDTO(
            id = actual.id,
            fom = 1.januar,
            tom = 20.januar,
            skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode),
            vilkårsgrunnlagId = person.vilkårsgrunnlagFor(inspektør.skjæringstidspunkt(1.vedtaksperiode))!!.inspektør.vilkårsgrunnlagId,
            deaktivert = false
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `sender med ghost tidslinjer til speil med flere arbeidsgivere ulik fom`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(4.januar, 31.januar), orgnummer = a2)
        håndterSøknad(Sykdom(4.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1,)
        håndterInntektsmelding(listOf(4.januar til 19.januar), beregnetInntekt = 5000.månedlig, orgnummer = a2,)

        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 5000.månedlig
                    a3 inntekt 10000.månedlig
                }
            }, emptyList()),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
                Vilkårsgrunnlag.Arbeidsforhold(a3, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT)
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val speilJson1 = serializePersonForSpeil(person, observatør.spekemat.resultat())
        val vilkårsgrunnlagId = person.vilkårsgrunnlagFor(1.januar)!!.inspektør.vilkårsgrunnlagId

        speilJson1.arbeidsgivere.single { it.organisasjonsnummer == a1 }.ghostPerioder.also { ghostPerioder ->
            assertEquals(1, ghostPerioder.size)
            ghostPerioder[0].also { actual ->
                val expected = GhostPeriodeDTO(
                    id = actual.id,
                    fom = 21.januar,
                    tom = 31.januar,
                    skjæringstidspunkt = 1.januar,
                    vilkårsgrunnlagId = vilkårsgrunnlagId,
                    deaktivert = false
                )
                assertEquals(expected, actual)
            }
        }

        speilJson1.arbeidsgivere.single { it.organisasjonsnummer == a2 }.ghostPerioder.also { ghostPerioder ->
            assertEquals(1, ghostPerioder.size)
            ghostPerioder[0].also { actual ->
                val expected = GhostPeriodeDTO(
                    id = actual.id,
                    fom = 1.januar,
                    tom = 3.januar,
                    skjæringstidspunkt = 1.januar,
                    vilkårsgrunnlagId = vilkårsgrunnlagId,
                    deaktivert = false
                )
                assertEquals(expected, actual)
            }
        }

        speilJson1.arbeidsgivere.single { it.organisasjonsnummer == a3 }.ghostPerioder.also { perioder ->
            assertEquals(1, perioder.size)
            val actual = perioder.first()
            val expected = GhostPeriodeDTO(
                id = actual.id,
                fom = 1.januar,
                tom = 31.januar,
                skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode),
                vilkårsgrunnlagId = vilkårsgrunnlagId,
                deaktivert = false
            )
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `lager ikke ghosts for forkastede perioder med vilkårsgrunnlag fra spleis`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2,)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = false, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 20.februar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.februar til 16.februar), orgnummer = a1,)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 10000.månedlig
                }
            }, emptyList()),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT)
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val speilJson = serializePersonForSpeil(person, observatør.spekemat.resultat())
        assertEquals(
            emptyList<GhostPeriodeDTO>(),
            speilJson.arbeidsgivere.single { it.organisasjonsnummer == a1 }.ghostPerioder
        )

        val perioder = speilJson.arbeidsgivere.find { it.organisasjonsnummer == a2 }?.ghostPerioder
        assertEquals(1, perioder?.size)
        val actual = perioder!!.first()
        val expected = GhostPeriodeDTO(
            id = actual.id,
            fom = 1.februar,
            tom = 20.februar,
            skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode),
            vilkårsgrunnlagId = person.vilkårsgrunnlagFor(inspektør.skjæringstidspunkt(1.vedtaksperiode))!!.inspektør.vilkårsgrunnlagId,
            deaktivert = false
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `skal ikke lage ghosts for gamle arbeidsgivere`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2017), 20.januar(2017)), orgnummer = a3)
        håndterSøknad(Sykdom(1.januar(2017), 20.januar(2017), 100.prosent), orgnummer = a3)
        håndterInntektsmelding(listOf(1.januar(2017) til 16.januar(2017)), orgnummer = a3,)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a3)
        håndterYtelser(1.vedtaksperiode, orgnummer = a3)
        håndterSimulering(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = true, orgnummer = a3)
        håndterUtbetalt(orgnummer = a3)

        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1,)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 10000.månedlig
                }
            }, emptyList()),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(
                    a1,
                    LocalDate.EPOCH,
                    null,
                    Arbeidsforholdtype.ORDINÆRT
                ),
                Vilkårsgrunnlag.Arbeidsforhold(
                    a2,
                    LocalDate.EPOCH,
                    null,
                    Arbeidsforholdtype.ORDINÆRT
                )
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val speilJson = serializePersonForSpeil(person, observatør.spekemat.resultat())
        assertEquals(
            emptyList<GhostPeriodeDTO>(),
            speilJson.arbeidsgivere.single { it.organisasjonsnummer == a1 }.ghostPerioder
        )


        val perioder = speilJson.arbeidsgivere.single { it.organisasjonsnummer == a2 }.ghostPerioder
        assertEquals(1, perioder.size)
        val actual = perioder.first()
        val expected = GhostPeriodeDTO(
            id = actual.id,
            fom = 1.januar,
            tom = 20.januar,
            skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode),
            vilkårsgrunnlagId = person.vilkårsgrunnlagFor(inspektør.skjæringstidspunkt(1.vedtaksperiode))!!.inspektør.vilkårsgrunnlagId,
            deaktivert = false
        )

        assertEquals(expected, actual)
        assertEquals(emptyList<GhostPeriodeDTO>(), speilJson.arbeidsgivere.single { it.organisasjonsnummer == a3 }.ghostPerioder)
    }

    @Test
    fun `ghost periode kuttes ved skjæringstidspunkt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(3.januar til 18.januar), førsteFraværsdag = 3.januar, orgnummer = a1,)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 10000.månedlig
                }
            }, emptyList()),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT)
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        val speilJson = serializePersonForSpeil(person, observatør.spekemat.resultat())
        assertEquals(
            emptyList<GhostPeriodeDTO>(),
            speilJson.arbeidsgivere.single { it.organisasjonsnummer == a1 }.ghostPerioder
        )

        val perioder = speilJson.arbeidsgivere.find { it.organisasjonsnummer == a2 }?.ghostPerioder

        assertEquals(1, perioder?.size)

        val actual = perioder!!.first()
        val expected = GhostPeriodeDTO(
            id = actual.id,
            fom = 3.januar,
            tom = 31.januar,
            skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode),
            vilkårsgrunnlagId = person.vilkårsgrunnlagFor(inspektør.skjæringstidspunkt(1.vedtaksperiode))!!.inspektør.vilkårsgrunnlagId,
            deaktivert = false
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `arbeidsforhold uten sykepengegrunnlag de tre siste månedene før skjæringstidspunktet skal ikke ha ghostperioder`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1,)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.januar, INNTEKT.repeat(3))
                ),
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT)
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        val personDto = serializePersonForSpeil(person, observatør.spekemat.resultat())
        val ghostpølser = personDto.arbeidsgivere.single { it.organisasjonsnummer == a1 }.ghostPerioder
        assertEquals(0, ghostpølser.size)
    }

    @Test
    fun `legger ved sammenlignignsgrunnlag og sykepengegrunnlag for deaktiverte arbeidsforhold`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1,)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.januar, INNTEKT.repeat(3)),
                    grunnlag(a2, 1.januar, 1000.månedlig.repeat(3))
                ),
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT)
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        håndterOverstyrArbeidsforhold(1.januar, listOf(OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a2, true, "forklaring")))
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val personDto = serializePersonForSpeil(person, observatør.spekemat.resultat())
        val vilkårsgrunnlagId = (personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val vilkårsgrunnlag = personDto.vilkårsgrunnlag[vilkårsgrunnlagId]

        assertEquals(listOf(a1, a2), vilkårsgrunnlag?.inntekter?.map { it.organisasjonsnummer })
        assertEquals(
            Arbeidsgiverinntekt(
                organisasjonsnummer = a2,
                omregnetÅrsinntekt = Inntekt(
                    kilde = Inntektkilde.AOrdningen,
                    beløp = 12000.0,
                    månedsbeløp = 1000.0,
                    inntekterFraAOrdningen = listOf(
                        InntekterFraAOrdningen(YearMonth.of(2017, Month.OCTOBER), 1000.0),
                        InntekterFraAOrdningen(YearMonth.of(2017, Month.NOVEMBER), 1000.0),
                        InntekterFraAOrdningen(YearMonth.of(2017, Month.DECEMBER), 1000.0)
                    )
                ),
                deaktivert = true
            ),
            vilkårsgrunnlag?.inntekter?.find { it.organisasjonsnummer == a2 }
        )
    }

    @Test
    fun `deaktiverte arbeidsforhold vises i speil selvom sammenligninggrunnlag og sykepengegrunnlag ikke er rapportert til A-ordningen enda`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1,)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.januar, INNTEKT.repeat(3))
                ),
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
                Vilkårsgrunnlag.Arbeidsforhold(a2, 1.desember(2017), null, Arbeidsforholdtype.ORDINÆRT)
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        håndterOverstyrArbeidsforhold(1.januar, listOf(OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a2, true, "forklaring")))
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val personDto = serializePersonForSpeil(person, observatør.spekemat.resultat())
        val vilkårsgrunnlagId = (personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val vilkårsgrunnlag = personDto.vilkårsgrunnlag[vilkårsgrunnlagId]

        assertEquals(listOf(a1, a2), vilkårsgrunnlag?.inntekter?.map { it.organisasjonsnummer })
        assertTrue(personDto.arbeidsgivere.single { it.organisasjonsnummer == a2 }.ghostPerioder.isNotEmpty())
    }

    @Test
    fun `deaktivert arbeidsforhold blir med i vilkårsgrunnlag`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar),)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
                Vilkårsgrunnlag.Arbeidsforhold(a2, 1.desember(2017), null, Arbeidsforholdtype.ORDINÆRT)
            )
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        val skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode)
        val relevanteOrgnumre: Iterable<String> = hendelselogg.etterspurtBehov(1.vedtaksperiode.id(ORGNUMMER), Aktivitet.Behov.Behovtype.Godkjenning, "orgnummereMedRelevanteArbeidsforhold") ?: fail { "forventet orgnummereMedRelevanteArbeidsforhold" }
        assertEquals(listOf(a1, a2).toList(), relevanteOrgnumre.toList())
        håndterOverstyrArbeidsforhold(skjæringstidspunkt, listOf(
            OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(
                a2,
                true,
                "forklaring"
            )
        ))
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val personDto = serializePersonForSpeil(person, observatør.spekemat.resultat())
        val vilkårsgrunnlagId = (personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val vilkårsgrunnlag = personDto.vilkårsgrunnlag[vilkårsgrunnlagId]

        val forventet = listOf(
            Arbeidsgiverinntekt(
                organisasjonsnummer = a1,
                omregnetÅrsinntekt = Inntekt(
                    kilde = Inntektkilde.Inntektsmelding,
                    beløp = 372000.0,
                    månedsbeløp = 31000.0,
                    inntekterFraAOrdningen = null
                ),
                deaktivert = false
            ),
            Arbeidsgiverinntekt(
                organisasjonsnummer = a2,
                omregnetÅrsinntekt = Inntekt(
                    kilde = Inntektkilde.IkkeRapportert,
                    beløp = 0.0,
                    månedsbeløp = 0.0,
                    inntekterFraAOrdningen = null
                ),
                deaktivert = true
            )
        )
        assertEquals(forventet, vilkårsgrunnlag?.inntekter)
    }

    @Test
    fun `Skal ikke ta med inntekt på vilkårsgrunnlaget som mangler både sykepengegrunnlag og sammenligningsgrunnlag på skjæringstidspunktet`() {
        nyttVedtak(1.januar(2017), 31.januar(2017), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1,)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.januar, INNTEKT.repeat(3))
                ),
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, 1.oktober(2017), null, Arbeidsforholdtype.ORDINÆRT),
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val personDto = serializePersonForSpeil(person, observatør.spekemat.resultat())
        val vilkårsgrunnlagId =
            (personDto.arbeidsgivere.find { it.organisasjonsnummer == a1 }!!.generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val vilkårsgrunnlag = personDto.vilkårsgrunnlag[vilkårsgrunnlagId]
        assertEquals(listOf(a1), vilkårsgrunnlag?.inntekter?.map { it.organisasjonsnummer })
        assertEquals(listOf(a2, a1), personDto.arbeidsgivere.map { it.organisasjonsnummer })
    }

    @Test
    fun `ikke ta med sykepengegrunnlag fra a-ordningen dersom det ikke er rapportert inntekt de 2 siste mnd`() {
        nyttVedtak(1.januar(2017), 31.januar(2017), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1,)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.januar, INNTEKT.repeat(3)),
                    grunnlag(a2, 1.november(2017), listOf(INNTEKT))
                ),
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val personDto = serializePersonForSpeil(person, observatør.spekemat.resultat())
        val vilkårsgrunnlagId = (personDto.arbeidsgivere.find { it.organisasjonsnummer == a1 }!!.generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val vilkårsgrunnlag = personDto.vilkårsgrunnlag[vilkårsgrunnlagId]
        assertNull(vilkårsgrunnlag?.inntekter?.firstOrNull { it.organisasjonsnummer == a2 }?.omregnetÅrsinntekt)
    }

    @Test
    fun `Ghostpølse forsvinner ikke etter overstyring av ghost-inntekt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1,)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 10000.månedlig
                }
            }, emptyList()),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT)
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterOverstyrInntekt(9000.månedlig, a2, 1.januar)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, a1)

        val speilJson = serializePersonForSpeil(person, observatør.spekemat.resultat())
        val perioder = speilJson.arbeidsgivere.single { it.organisasjonsnummer == a2 }.ghostPerioder

        assertEquals(1, perioder.size)
        val actual = perioder.first()
        val expected = GhostPeriodeDTO(
            id = actual.id,
            fom = 1.januar,
            tom = 20.januar,
            skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode),
            vilkårsgrunnlagId = person.vilkårsgrunnlagFor(inspektør.skjæringstidspunkt(1.vedtaksperiode))!!.inspektør.vilkårsgrunnlagId,
            deaktivert = false
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `Ghosten finnes ikke i vilkårsgrunnlaget`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1,)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 10000.månedlig
                }
            }, emptyList()),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT)
            )
        )
        håndterYtelser(1.vedtaksperiode)

        val speilJson = serializePersonForSpeil(person, observatør.spekemat.resultat())
        val vilkårsgrunnlag = speilJson.vilkårsgrunnlag
        assertEquals(1, vilkårsgrunnlag.size)

        val arbeidsgiverA1 = speilJson.arbeidsgivere.singleOrNull { it.organisasjonsnummer == a1 }
        assertEquals(1, arbeidsgiverA1?.generasjoner?.size)
        assertEquals(1, arbeidsgiverA1?.generasjoner?.single()?.perioder?.size)
        val beregnetPeriode = arbeidsgiverA1?.generasjoner?.single()?.perioder?.single()
        assertInstanceOf(BeregnetPeriode::class.java, beregnetPeriode)

        val arbeidsgiverA2 = speilJson.arbeidsgivere.singleOrNull { it.organisasjonsnummer == a2 }
        assertEquals(0, arbeidsgiverA2?.generasjoner?.size)
        val perioder = arbeidsgiverA2?.ghostPerioder
        val actual = perioder?.single()!!
        assertEquals(
            GhostPeriodeDTO(
                id = actual.id,
                fom = 1.januar,
                tom = 20.januar,
                skjæringstidspunkt = 1.januar,
                vilkårsgrunnlagId = person.vilkårsgrunnlagFor(inspektør.skjæringstidspunkt(1.vedtaksperiode))!!.inspektør.vilkårsgrunnlagId,
                deaktivert = false
            ),
            actual
        )
    }

    @Test
    fun `lager ikke Ghostperiode på et vilkårsgrunnlag som ingen beregnede perioder peker på`() {
        nyPeriode(1.januar til 16.januar)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        nyPeriode(17.januar til 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar),)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 10000.månedlig
                }
            }, emptyList()),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
            )
        )
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, utbetalingGodkjent = false)

        assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)

        val personDto = speilApi()
        val ghostPerioder = personDto.arbeidsgivere.singleOrNull { it.organisasjonsnummer == a2 }?.ghostPerioder
        assertNull(ghostPerioder)
    }

    @Test
    fun `Finner riktig ghostpølse etter overstyring av ghost-inntekt selvom begge arbeidsgiverne har saksbehandlerinntekt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1,)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 10000.månedlig
                }
            }, emptyList()),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterOverstyrInntekt(30000.månedlig, a1, 1.januar)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        håndterOverstyrInntekt(9000.månedlig, a2, 1.januar)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val speilJson = serializePersonForSpeil(person, observatør.spekemat.resultat())
        val perioder = speilJson.arbeidsgivere.single { it.organisasjonsnummer == a2 }.ghostPerioder

        assertEquals(1, perioder.size)
        val actual = perioder.first()
        val expected = GhostPeriodeDTO(
            id = actual.id,
            fom = 1.januar,
            tom = 20.januar,
            skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode),
            vilkårsgrunnlagId = person.vilkårsgrunnlagFor(inspektør.skjæringstidspunkt(1.vedtaksperiode))!!.inspektør.vilkårsgrunnlagId,
            deaktivert = false
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `ghost-perioder før og etter søknad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1,)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt INNTEKT
                }
            }, emptyList()),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        val speilJson = serializePersonForSpeil(person, observatør.spekemat.resultat())
        val perioder = speilJson.arbeidsgivere.single { it.organisasjonsnummer == a2 }.ghostPerioder

        assertEquals(2, perioder.size)
        val skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode)
        val vilkårsgrunnlagId = person.vilkårsgrunnlagFor(skjæringstidspunkt)!!.inspektør.vilkårsgrunnlagId
        perioder[0].also { actual ->
            val expected = GhostPeriodeDTO(
                id = actual.id,
                fom = 1.januar,
                tom = 31.januar,
                skjæringstidspunkt = skjæringstidspunkt,
                vilkårsgrunnlagId = vilkårsgrunnlagId,
                deaktivert = false
            )
            assertEquals(expected, actual)
        }
        perioder[1].also { actual ->
            val expected = GhostPeriodeDTO(
                id = actual.id,
                fom = 1.mars,
                tom = 31.mars,
                skjæringstidspunkt = skjæringstidspunkt,
                vilkårsgrunnlagId = vilkårsgrunnlagId,
                deaktivert = false
            )
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `refusjon for flere arbeidsgivere`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)

        val personDto = speilApi()
        val speilVilkårsgrunnlagIdForAG1 = (personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val speilVilkårsgrunnlagIdForAG2 = (personDto.arbeidsgivere.last().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val vilkårsgrunnlag = personDto.vilkårsgrunnlag[speilVilkårsgrunnlagIdForAG1] as? SpleisVilkårsgrunnlag
        val vilkårsgrunnlag2 = personDto.vilkårsgrunnlag[speilVilkårsgrunnlagIdForAG2] as? SpleisVilkårsgrunnlag
        assertEquals(vilkårsgrunnlag, vilkårsgrunnlag2)

        assertTrue(vilkårsgrunnlag!!.arbeidsgiverrefusjoner.isNotEmpty())
        val arbeidsgiverrefusjonForAG1 = vilkårsgrunnlag.arbeidsgiverrefusjoner.find { it.arbeidsgiver == a1 }!!
        val arbeidsgiverrefusjonForAG2 = vilkårsgrunnlag.arbeidsgiverrefusjoner.find { it.arbeidsgiver == a2 }!!

        val refusjonsopplysningerForAG1 = arbeidsgiverrefusjonForAG1.refusjonsopplysninger.single()
        val refusjonsopplysningerForAG2 = arbeidsgiverrefusjonForAG2.refusjonsopplysninger.single()

        assertEquals(1.januar, refusjonsopplysningerForAG1.fom)
        assertEquals(null, refusjonsopplysningerForAG1.tom)
        assertEquals(20000.månedlig,refusjonsopplysningerForAG1.beløp.månedlig)
        assertEquals(1.januar, refusjonsopplysningerForAG2.fom)
        assertEquals(null, refusjonsopplysningerForAG2.tom)
        assertEquals(20000.månedlig,refusjonsopplysningerForAG2.beløp.månedlig)
    }

    @Test
    fun `flere førstegangsaker for begge arbeidsgivere med samme skjæringstidspunkt`() {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(12.februar, 12.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(17.januar, 26.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(13.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1,)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 12.februar, orgnummer = a1,)

        håndterInntektsmelding(
            listOf(17.januar til 26.januar, 1.februar til 6.februar),
            førsteFraværsdag = 1.februar,
            orgnummer = a2,
        )
        håndterInntektsmelding(
            listOf(17.januar til 26.januar, 1.februar til 6.februar),
            førsteFraværsdag = 13.mars,
            orgnummer = a2,
        )

        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt INNTEKT
                }
            }, emptyList()),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val speilJson = serializePersonForSpeil(person, observatør.spekemat.resultat())
        val skjæringstidspunkt = inspektør(a1).skjæringstidspunkt(1.vedtaksperiode)
        val vilkårsgrunnlagId = person.vilkårsgrunnlagFor(skjæringstidspunkt)!!.inspektør.vilkårsgrunnlagId

        speilJson.arbeidsgivere.single { it.organisasjonsnummer == a1 }.ghostPerioder.also { perioder ->
            assertEquals(2, perioder.size)

            perioder[0].also { actual ->
                val expected = GhostPeriodeDTO(
                    id = actual.id,
                    fom = 1.februar,
                    tom = 11.februar,
                    skjæringstidspunkt = skjæringstidspunkt,
                    vilkårsgrunnlagId = vilkårsgrunnlagId,
                    deaktivert = false
                )
                assertEquals(expected, actual)
            }
            perioder[1].also { actual ->
                val expected = GhostPeriodeDTO(
                    id = actual.id,
                    fom = 13.mars,
                    tom = 31.mars,
                    skjæringstidspunkt = skjæringstidspunkt,
                    vilkårsgrunnlagId = vilkårsgrunnlagId,
                    deaktivert = false
                )
                assertEquals(expected, actual)
            }
        }
        speilJson.arbeidsgivere.single { it.organisasjonsnummer == a2 }.ghostPerioder.also { perioder ->
            assertEquals(2, perioder.size)

            perioder[0].also { actual ->
                val expected = GhostPeriodeDTO(
                    id = actual.id,
                    fom = 1.januar,
                    tom = 16.januar,
                    skjæringstidspunkt = skjæringstidspunkt,
                    vilkårsgrunnlagId = vilkårsgrunnlagId,
                    deaktivert = false
                )
                assertEquals(expected, actual)
            }
            perioder[1].also { actual ->
                val expected = GhostPeriodeDTO(
                    id = actual.id,
                    fom = 1.mars,
                    tom = 12.mars,
                    skjæringstidspunkt = skjæringstidspunkt,
                    vilkårsgrunnlagId = vilkårsgrunnlagId,
                    deaktivert = false
                )
                assertEquals(expected, actual)
            }
        }
    }
}

