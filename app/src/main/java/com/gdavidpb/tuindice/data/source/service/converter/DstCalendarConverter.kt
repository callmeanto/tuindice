package com.gdavidpb.tuindice.data.source.service.converter

import com.gdavidpb.tuindice.data.model.service.DstCalendar
import com.gdavidpb.tuindice.data.utils.DEFAULT_LOCALE
import com.gdavidpb.tuindice.data.utils.parse
import org.jsoup.nodes.Element
import pl.droidsonroids.jspoon.ElementConverter
import pl.droidsonroids.jspoon.annotation.Selector
import java.util.*

class DstCalendarConverter : ElementConverter<DstCalendar> {

    private val year by lazy {
        Calendar.getInstance(DEFAULT_LOCALE).get(Calendar.YEAR)
    }

    override fun convert(node: Element, selector: Selector): DstCalendar {
        val values = node
                .select("td + td")
                .map { it.text() }

        val (startDate, endDate) = values[0]
                .split("-")
                .map {
                    "$it $year"
                            .trim()
                            .toLowerCase()
                            .normalize()
                            .parse("dd 'de' MMM yyyy")
                }

        val (correctionDate, giveUpDeadline, degreeRequestDeadline) = values
                .subList(1, 4)
                .map {
                    "$it $year"
                            .toLowerCase()
                            .normalize()
                            .parse("dd 'de' MMM yyyy")
                }

        val (graduationStartDate, graduationEndDate) = values[4]
                .split(",")
                .map {
                    "$it/$year"
                            .parse("dd/MM/yyyy")
                }

        val (documentsRequestDeadline, nextEnrollmentDate, minutesDeliveryDeadline) = values
                .subList(5, 8)
                .map {
                    "$it $year"
                            .toLowerCase()
                            .normalize()
                            .parse("dd 'de' MMM yyyy")
                }

        return DstCalendar(
                startDate = startDate,
                endDate = endDate,
                correctionDate = correctionDate,
                giveUpDeadline = giveUpDeadline,
                degreeRequestDeadline = degreeRequestDeadline,
                graduationStartDate = graduationStartDate,
                graduationEndDate = graduationEndDate,
                documentsRequestDeadline = documentsRequestDeadline,
                nextEnrollmentDate = nextEnrollmentDate,
                minutesDeliveryDeadline = minutesDeliveryDeadline
        )
    }

    /* Workaround: for unknown reason september is abbreviated by Android as "sept" */
    private fun String.normalize(): String = replace("sep".toRegex(), "sept")
}