package no.nav.helse.spleis.rettighetskategori

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.dto.deserialisering.YrkesaktivitetstypeDto
import no.nav.helse.dto.deserialisering.YrkesaktivitetstypeDto.ARBEIDSTAKER
import no.nav.helse.dto.deserialisering.YrkesaktivitetstypeDto.SELVSTENDIG
import no.nav.helse.dto.serialisering.ArbeidsgiverUtDto
import no.nav.helse.dto.serialisering.ForkastetVedtaksperiodeUtDto
import no.nav.helse.dto.serialisering.InntektsgrunnlagUtDto
import no.nav.helse.dto.serialisering.PersonUtDto
import no.nav.helse.dto.serialisering.VedtaksperiodeUtDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlagInnslagUtDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlagUtDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlaghistorikkUtDto
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RettighetskategorierTest {
    @Test
    fun `person uten vilkårsgrunnlag gir ingen rettighetskategorier`() {
        assertEquals(emptyList(), rettighetskategorier(person()))
    }

    @Test
    fun `arbeidstaker med vilkårsgrunnlag fra Spleis`() {
        val vilkårsgrunnlag = spleis(skjæringstidspunkt = "2018-01-01")
        val person =
            person(
                historikk = listOf(innslag("2018-02-01T12:00:00", vilkårsgrunnlag)),
                arbeidsgivere = listOf(arbeidsgiver(ARBEIDSTAKER, aktive = listOf(periode("2018-01-01")))),
            )

        assertEquals(
            listOf(
                Rettighetskategori(
                    rettighetskategoriId = vilkårsgrunnlag.rettighetskategoriId,
                    skjæringstidspunkt = LocalDate.parse("2018-01-01"),
                    opprettet = LocalDateTime.parse("2018-02-01T12:00:00"),
                    kilde = Rettighetskategori.Kilde.SPLEIS,
                    harSelvstendigInntekt = false,
                    aktive = listOf(ARBEIDSTAKER),
                    forkastede = emptyList(),
                ),
            ),
            rettighetskategorier(person),
        )
    }

    @Test
    fun `selvstendig med selvstendig inntekt i inntektsgrunnlaget`() {
        val vilkårsgrunnlag = spleis(skjæringstidspunkt = "2018-01-01", harSelvstendigInntekt = true)
        val person =
            person(
                historikk = listOf(innslag("2018-02-01T12:00:00", vilkårsgrunnlag)),
                arbeidsgivere = listOf(arbeidsgiver(SELVSTENDIG, aktive = listOf(periode("2018-01-01")))),
            )

        val rettighetskategori = rettighetskategorier(person).single()

        assertTrue(rettighetskategori.harSelvstendigInntekt)
        assertEquals(listOf(SELVSTENDIG), rettighetskategori.aktive)
        assertEquals(emptyList(), rettighetskategori.forkastede)
    }

    @Test
    fun `arbeidstaker og selvstendig som bruker samme vilkårsgrunnlag`() {
        val vilkårsgrunnlag = spleis(skjæringstidspunkt = "2018-01-01", harSelvstendigInntekt = true)
        val person =
            person(
                historikk = listOf(innslag("2018-02-01T12:00:00", vilkårsgrunnlag)),
                arbeidsgivere =
                    listOf(
                        arbeidsgiver(ARBEIDSTAKER, aktive = listOf(periode("2018-01-01"))),
                        arbeidsgiver(SELVSTENDIG, aktive = listOf(periode("2018-01-01"))),
                    ),
            )

        assertEquals(listOf(ARBEIDSTAKER, SELVSTENDIG), rettighetskategorier(person).single().aktive)
    }

    @Test
    fun `hver arbeidsgiver er en egen yrkesaktivitet`() {
        val vilkårsgrunnlag = spleis(skjæringstidspunkt = "2018-01-01")
        val person =
            person(
                historikk = listOf(innslag("2018-02-01T12:00:00", vilkårsgrunnlag)),
                arbeidsgivere =
                    listOf(
                        arbeidsgiver(ARBEIDSTAKER, aktive = listOf(periode("2018-01-01"))),
                        arbeidsgiver(ARBEIDSTAKER, aktive = listOf(periode("2018-01-01"))),
                    ),
            )

        assertEquals(listOf(ARBEIDSTAKER, ARBEIDSTAKER), rettighetskategorier(person).single().aktive)
    }

    @Test
    fun `aktive og forkastede perioder holdes hver for seg`() {
        val vilkårsgrunnlag = spleis(skjæringstidspunkt = "2018-01-01")
        val person =
            person(
                historikk = listOf(innslag("2018-02-01T12:00:00", vilkårsgrunnlag)),
                arbeidsgivere =
                    listOf(
                        arbeidsgiver(
                            ARBEIDSTAKER,
                            aktive = listOf(periode("2018-01-01")),
                            forkastede = listOf(periode("2018-01-01")),
                        ),
                        arbeidsgiver(SELVSTENDIG, forkastede = listOf(periode("2018-01-01"))),
                    ),
            )

        val rettighetskategori = rettighetskategorier(person).single()

        assertEquals(listOf(ARBEIDSTAKER), rettighetskategori.aktive)
        assertEquals(listOf(ARBEIDSTAKER, SELVSTENDIG), rettighetskategori.forkastede)
    }

    @Test
    fun `flere perioder for samme yrkesaktivitet gir én yrkesaktivitetstype`() {
        val vilkårsgrunnlag = spleis(skjæringstidspunkt = "2018-01-01")
        val person =
            person(
                historikk = listOf(innslag("2018-02-01T12:00:00", vilkårsgrunnlag)),
                arbeidsgivere =
                    listOf(
                        arbeidsgiver(
                            ARBEIDSTAKER,
                            aktive =
                                listOf(
                                    periode("2018-01-01"),
                                    periode("2018-01-01"),
                                    periode("2018-01-01"),
                                ),
                        ),
                    ),
            )

        assertEquals(listOf(ARBEIDSTAKER), rettighetskategorier(person).single().aktive)
    }

    @Test
    fun `perioder med et annet skjæringstidspunkt er ikke med`() {
        val vilkårsgrunnlag = spleis(skjæringstidspunkt = "2018-01-01")
        val annetVilkårsgrunnlag = spleis(skjæringstidspunkt = "2018-06-01")
        val person =
            person(
                historikk = listOf(innslag("2018-07-01T12:00:00", annetVilkårsgrunnlag, vilkårsgrunnlag)),
                arbeidsgivere =
                    listOf(
                        arbeidsgiver(ARBEIDSTAKER, aktive = listOf(periode("2018-01-01"))),
                        arbeidsgiver(SELVSTENDIG, aktive = listOf(periode("2018-06-01"))),
                    ),
            )

        val resultat = rettighetskategorier(person).associateBy { it.rettighetskategoriId }

        assertEquals(listOf(ARBEIDSTAKER), resultat.getValue(vilkårsgrunnlag.rettighetskategoriId).aktive)
        assertEquals(listOf(SELVSTENDIG), resultat.getValue(annetVilkårsgrunnlag.rettighetskategoriId).aktive)
    }

    @Test
    fun `kopiert vilkårsgrunnlag gir én oppføring med opprettet fra det første innslaget`() {
        val opprinnelig = spleis(skjæringstidspunkt = "2018-01-01")
        val kopi = kopi(opprinnelig)
        val person =
            person(
                historikk =
                    listOf(
                        innslag("2018-03-01T12:00:00", kopi),
                        innslag("2018-02-01T12:00:00", opprinnelig),
                    ),
                arbeidsgivere =
                    listOf(
                        arbeidsgiver(ARBEIDSTAKER, aktive = listOf(periode("2018-01-01"))),
                        arbeidsgiver(SELVSTENDIG, forkastede = listOf(periode("2018-01-01"))),
                    ),
            )

        val rettighetskategori = rettighetskategorier(person).single()

        assertEquals(opprinnelig.rettighetskategoriId, rettighetskategori.rettighetskategoriId)
        assertEquals(LocalDateTime.parse("2018-02-01T12:00:00"), rettighetskategori.opprettet)
        assertEquals(listOf(ARBEIDSTAKER), rettighetskategori.aktive)
        assertEquals(listOf(SELVSTENDIG), rettighetskategori.forkastede)
    }

    @Test
    fun `nytt vilkårsgrunnlag for samme skjæringstidspunkt gir ny oppføring med de samme periodene`() {
        val gammelt = spleis(skjæringstidspunkt = "2018-01-01")
        val nytt = spleis(skjæringstidspunkt = "2018-01-01")
        val person =
            person(
                historikk =
                    listOf(
                        innslag("2018-03-01T12:00:00", nytt),
                        innslag("2018-02-01T12:00:00", gammelt),
                    ),
                arbeidsgivere = listOf(arbeidsgiver(ARBEIDSTAKER, aktive = listOf(periode("2018-01-01")))),
            )

        val resultat = rettighetskategorier(person)

        assertEquals(listOf(nytt.rettighetskategoriId, gammelt.rettighetskategoriId), resultat.map { it.rettighetskategoriId })
        assertEquals(listOf(listOf(ARBEIDSTAKER), listOf(ARBEIDSTAKER)), resultat.map { it.aktive })
    }

    @Test
    fun `nyeste vilkårsgrunnlag med ID-en avgjør om inntektsgrunnlaget har selvstendig inntekt`() {
        val opprinnelig = spleis(skjæringstidspunkt = "2018-01-01", harSelvstendigInntekt = false)
        val kopi = kopi(opprinnelig, harSelvstendigInntekt = true)
        val person =
            person(
                historikk =
                    listOf(
                        innslag("2018-03-01T12:00:00", kopi),
                        innslag("2018-02-01T12:00:00", opprinnelig),
                    ),
            )

        assertTrue(rettighetskategorier(person).single().harSelvstendigInntekt)
    }

    @Test
    fun `vilkårsgrunnlag fra Infotrygd`() {
        val vilkårsgrunnlag = infotrygd(skjæringstidspunkt = "2017-12-01")
        val person =
            person(
                historikk = listOf(innslag("2018-02-01T12:00:00", vilkårsgrunnlag)),
                arbeidsgivere =
                    listOf(
                        arbeidsgiver(ARBEIDSTAKER, aktive = listOf(periode("2017-12-01"))),
                    ),
            )

        val rettighetskategori = rettighetskategorier(person).single()

        assertEquals(Rettighetskategori.Kilde.INFOTRYGD, rettighetskategori.kilde)
        assertEquals(LocalDate.parse("2017-12-01"), rettighetskategori.skjæringstidspunkt)
        assertEquals(listOf(ARBEIDSTAKER), rettighetskategori.aktive)
    }

    @Test
    fun `vilkårsgrunnlag uten perioder med samme skjæringstidspunkt`() {
        val vilkårsgrunnlag = spleis(skjæringstidspunkt = "2018-01-01", harSelvstendigInntekt = true)
        val person = person(historikk = listOf(innslag("2018-02-01T12:00:00", vilkårsgrunnlag)))

        val rettighetskategori = rettighetskategorier(person).single()

        assertEquals(emptyList(), rettighetskategori.aktive)
        assertEquals(emptyList(), rettighetskategori.forkastede)
        assertTrue(rettighetskategori.harSelvstendigInntekt)
    }

    private fun spleis(
        skjæringstidspunkt: String,
        rettighetskategoriId: UUID = UUID.randomUUID(),
        harSelvstendigInntekt: Boolean = false,
    ) = mockk<VilkårsgrunnlagUtDto.Spleis> {
        every { this@mockk.skjæringstidspunkt } returns LocalDate.parse(skjæringstidspunkt)
        every { this@mockk.rettighetskategoriId } returns rettighetskategoriId
        every { inntektsgrunnlag } returns inntektsgrunnlag(harSelvstendigInntekt)
    }

    private fun infotrygd(
        skjæringstidspunkt: String,
        rettighetskategoriId: UUID = UUID.randomUUID(),
    ) = mockk<VilkårsgrunnlagUtDto.Infotrygd> {
        every { this@mockk.skjæringstidspunkt } returns LocalDate.parse(skjæringstidspunkt)
        every { this@mockk.rettighetskategoriId } returns rettighetskategoriId
        every { inntektsgrunnlag } returns inntektsgrunnlag(harSelvstendigInntekt = false)
    }

    private fun kopi(
        vilkårsgrunnlag: VilkårsgrunnlagUtDto,
        harSelvstendigInntekt: Boolean = false,
    ) = spleis(
        skjæringstidspunkt = vilkårsgrunnlag.skjæringstidspunkt.toString(),
        rettighetskategoriId = vilkårsgrunnlag.rettighetskategoriId,
        harSelvstendigInntekt = harSelvstendigInntekt,
    )

    private fun inntektsgrunnlag(harSelvstendigInntekt: Boolean) =
        mockk<InntektsgrunnlagUtDto> {
            every { selvstendigInntektsopplysning } returns if (harSelvstendigInntekt) mockk() else null
        }

    private fun innslag(
        opprettet: String,
        vararg vilkårsgrunnlag: VilkårsgrunnlagUtDto,
    ) = VilkårsgrunnlagInnslagUtDto(
        id = UUID.randomUUID(),
        opprettet = LocalDateTime.parse(opprettet),
        vilkårsgrunnlag = vilkårsgrunnlag.toList(),
    )

    private fun arbeidsgiver(
        yrkesaktivitetstype: YrkesaktivitetstypeDto,
        aktive: List<VedtaksperiodeUtDto> = emptyList(),
        forkastede: List<VedtaksperiodeUtDto> = emptyList(),
    ) = mockk<ArbeidsgiverUtDto> {
        every { this@mockk.yrkesaktivitetstype } returns yrkesaktivitetstype
        every { vedtaksperioder } returns aktive
        every { this@mockk.forkastede } returns forkastede.map { ForkastetVedtaksperiodeUtDto(it) }
    }

    private fun periode(skjæringstidspunkt: String) =
        mockk<VedtaksperiodeUtDto> {
            every { this@mockk.skjæringstidspunkt } returns LocalDate.parse(skjæringstidspunkt)
        }

    private fun person(
        historikk: List<VilkårsgrunnlagInnslagUtDto> = emptyList(),
        arbeidsgivere: List<ArbeidsgiverUtDto> = emptyList(),
    ) = mockk<PersonUtDto> {
        every { vilkårsgrunnlagHistorikk } returns VilkårsgrunnlaghistorikkUtDto(historikk)
        every { this@mockk.arbeidsgivere } returns arbeidsgivere
    }
}
