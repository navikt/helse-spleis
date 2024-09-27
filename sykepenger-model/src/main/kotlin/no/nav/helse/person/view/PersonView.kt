package no.nav.helse.person.view

data class PersonView(val arbeidsgivere: List<ArbeidsgiverView>)

data class ArbeidsgiverView(val organisasjonsnummer: String)