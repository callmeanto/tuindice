package com.gdavidpb.tuindice.data.source.service.converter

import com.gdavidpb.tuindice.domain.model.service.DstQuarter
import com.gdavidpb.tuindice.domain.model.service.DstRecord
import com.gdavidpb.tuindice.domain.model.service.DstRecordStats
import com.gdavidpb.tuindice.domain.model.service.DstSubject
import com.gdavidpb.tuindice.utils.*
import com.gdavidpb.tuindice.utils.extensions.*
import com.gdavidpb.tuindice.utils.mappers.toStartEndDate
import com.gdavidpb.tuindice.utils.mappers.toSubjectName
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import pl.droidsonroids.jspoon.ElementConverter
import pl.droidsonroids.jspoon.annotation.Selector

open class DstRecordDataConverter : ElementConverter<DstRecord> {
    override fun convert(node: Element, selector: Selector): DstRecord {
        /* Select record table */
        val selectedRecordTable = node.select("table[class=tabla] table:has(table)")

        /* Record is empty */
        if (selectedRecordTable.size < 2) {
            val stats = DstRecordStats(0.0,
                    0, 0,
                    0, 0,
                    0, 0,
                    0, 0
            )

            return DstRecord(stats)
        }

        /* Select quarter tables, drop history */
        val selectedQuartersTable = selectedRecordTable.dropLast(1)

        /* Select history from quarter tables */
        val selectedHistoryTable = selectedRecordTable.last()

        /* Map quarters */
        val quarters = selectedQuartersTable.map {
            /* Select quarter table */
            val selectedQuarterTable = it.select("td:not(:has(*))")

            /* Take quarter subjects, drop first/last and chunk */
            val subjects = selectedQuarterTable.parseSubjects()

            /* Take period (first) */
            val quarterPeriod = selectedQuarterTable.first().text()

            /* Take history (last) */
            val quarterHistory = selectedQuarterTable.last().text()

            /* Parse quarter period */
            val (startDate, endDate) = quarterPeriod.toStartEndDate()

            val (grade, gradeSum) = "\\d\\.\\d{4}".toRegex()
                    .findAll(quarterHistory)
                    .toList()
                    .map { match -> match.value.toDouble() }

            DstQuarter(
                    startDate = startDate,
                    endDate = endDate,
                    subjects = subjects,
                    grade = grade,
                    gradeSum = gradeSum,
                    status = if (grade != 0.0) STATUS_QUARTER_COMPLETED else STATUS_QUARTER_RETIRED
            )
        }

        val (enrolledSubjects, enrolledCredits,
                approvedSubjects, approvedCredits,
                retiredSubjects, retiredCredits,
                failedSubjects, failedCredits) = selectedHistoryTable
                .select("td:matchesOwn(\\d+)")
                .map {
                    it.text().toIntOrNull() ?: 0
                }

        val grade = quarters
                .maxBy { it.startDate }
                ?.gradeSum
                ?.toGrade() ?: 0.0

        val stats = DstRecordStats(
                grade,
                enrolledSubjects, enrolledCredits,
                approvedSubjects, approvedCredits,
                retiredSubjects, retiredCredits,
                failedSubjects, failedCredits
        )

        return DstRecord(stats = stats, quarters = quarters)
    }

    private fun Elements.parseSubjects(): List<DstSubject> {
        return drop(1)
                .dropLast(1)
                .chunked(5)
                /* Map subjects */
                .map { subject ->
                    val (code, name, credits, grade, status)
                            = subject.map { element -> element.text() }

                    DstSubject(
                            code = code,
                            name = name.toSubjectName(),
                            credits = credits.toInt(),
                            grade = grade.toIntOrNull() ?: 0,
                            status = status
                    )
                }
    }
}