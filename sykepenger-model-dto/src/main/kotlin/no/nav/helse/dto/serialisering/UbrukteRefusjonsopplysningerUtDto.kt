package no.nav.helse.dto.serialisering

import java.util.UUID
import no.nav.helse.dto.BeløpstidslinjeDto
import no.nav.helse.dto.RefusjonsservitørDto

data class UbrukteRefusjonsopplysningerUtDto(
    val ubrukteRefusjonsopplysninger: RefusjonsservitørDto,
    val sisteRefusjonstidslinje: BeløpstidslinjeDto?,
    val sisteBehandlingId: UUID?
)