package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.person.inntekt.ArbeidstakerFaktaavklartInntekt
import no.nav.helse.person.inntekt.Arbeidstakerinntektskilde
import no.nav.helse.person.inntekt.Inntektsdata
import no.nav.helse.økonomi.Inntekt

class InntektsopplysningerFraLagretInnteksmelding(
    meldingsreferanseId: MeldingsreferanseId,
    override val behandlingsporing: Behandlingsporing.Yrkesaktivitet.Arbeidstaker,
    internal val vedtaksperiodeId: UUID,
    internal val inntetksmeldingMeldingsreferanseId: MeldingsreferanseId,
    internal val inntektsmeldingMottatt: LocalDateTime,
    private val inntekt: Inntekt,
    // Det finnes jo utallige måter å opplyse om endringer i refusjon osv, men bruker "hovedopplysaningen", så kan heller saksbehandler ordne ev. endringer i refusjon i Speil
    private val refusjon: Inntekt
) : Hendelse {
    override val metadata = LocalDateTime.now().let { nå ->
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = SYSTEM,
            innsendt = nå,
            registrert = nå,
            automatiskBehandling = false
        )
    }

    internal fun faktaavklartInntekt(skjæringstidspunkt: LocalDate) =  ArbeidstakerFaktaavklartInntekt(
        id = UUID.randomUUID(),
        inntektsopplysningskilde = Arbeidstakerinntektskilde.Arbeidsgiver,
        inntektsdata = Inntektsdata(
            hendelseId = inntetksmeldingMeldingsreferanseId,
            // Datoen er ikke så fryktelig viktig lenger, eneste den brukes til i dag er å matche mot rett periode, men her har vi jo spisset det til vedtaksperiodeId
            // I tilleggg er det veldig kjedelig å finne ut av inntektsdato. Kjedelig sjekk med FF og AGP
            dato = skjæringstidspunkt,
            beløp = inntekt,
            tidsstempel = inntektsmeldingMottatt
        )
    )

    internal fun refusjonstidslinkje(periode: Periode) = Beløpstidslinje.fra(periode, refusjon, Kilde(
        meldingsreferanseId = inntetksmeldingMeldingsreferanseId,
        avsender = ARBEIDSGIVER,
        tidsstempel = inntektsmeldingMottatt
    ))
}
