package no.nav.helse

import java.time.LocalDate

fun Int.januar(year: Int): LocalDate = LocalDate.of(year, 1, this)
fun Int.februar(year: Int): LocalDate = LocalDate.of(year, 2, this)
fun Int.mars(year: Int): LocalDate = LocalDate.of(year, 3, this)
fun Int.april(year: Int): LocalDate = LocalDate.of(year, 4, this)
fun Int.mai(year: Int): LocalDate = LocalDate.of(year, 5, this)
fun Int.juni(year: Int): LocalDate = LocalDate.of(year, 6, this)
fun Int.juli(year: Int): LocalDate = LocalDate.of(year, 7, this)
fun Int.august(year: Int): LocalDate = LocalDate.of(year, 8, this)
fun Int.september(year: Int): LocalDate = LocalDate.of(year, 9, this)
fun Int.oktober(year: Int): LocalDate = LocalDate.of(year, 10, this)
fun Int.november(year: Int): LocalDate = LocalDate.of(year, 11, this)
fun Int.desember(year: Int): LocalDate = LocalDate.of(year, 12, this)
