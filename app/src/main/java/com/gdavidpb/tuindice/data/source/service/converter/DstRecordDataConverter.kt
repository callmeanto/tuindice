package com.gdavidpb.tuindice.data.source.service.converter

import com.gdavidpb.tuindice.data.model.service.DstPeriod
import com.gdavidpb.tuindice.data.model.service.DstQuarter
import com.gdavidpb.tuindice.data.model.service.DstRecord
import com.gdavidpb.tuindice.data.model.service.DstSubject
import com.gdavidpb.tuindice.data.utils.component6
import com.gdavidpb.tuindice.data.utils.component7
import com.gdavidpb.tuindice.data.utils.component8
import com.gdavidpb.tuindice.data.utils.parse
import org.jsoup.nodes.Element
import pl.droidsonroids.jspoon.ElementConverter
import pl.droidsonroids.jspoon.annotation.Selector

class DstRecordDataConverter : ElementConverter<DstRecord> {
    override fun convert(node: Element, selector: Selector): DstRecord {
        /* Select record table */
        val selectedRecordTable = node.select("table[class=tabla] table:has(table)")

        /* Record is empty */
        if (selectedRecordTable.size < 2) return DstRecord.EMPTY

        /* Select quarter tables, drop history */
        val selectedQuartersTable = selectedRecordTable.dropLast(1)

        /* Select history from quarter tables */
        val selectedHistoryTable = selectedRecordTable.last()

        /* Map quarters */
        val quarters = selectedQuartersTable.map {
            /* Select quarter table */
            val selectedQuarterTable = it.select("td:not(:has(*))")

            /* Take quarter subjects, drop first/last and chunk */
            val subjects = selectedQuarterTable
                    .drop(1)
                    .dropLast(1)
                    .chunked(5)
                    /* Map subjects */
                    .map { subject ->
                        val (code, name, credits, grade, status)
                                = subject.map { element -> element.text() }

                        DstSubject(
                                code = code,
                                name = name,
                                credits = credits.toInt(),
                                grade = grade.toIntOrNull() ?: 0,
                                status = status
                        )
                    }

            /* Take period (first) */
            val quarterPeriod = selectedQuarterTable.first().text()

            /* Take history (last) */
            val quarterHistory = selectedQuarterTable.last().text()

            /* Parse quarter period */
            val period = quarterPeriod.run {
                val startDate = replace("-[^\\d]+".toRegex(), " ")
                        .trim()
                        .parse("MMMM yyyy")

                val endDate = replace("[^-]+-".toRegex(), "")
                        .trim()
                        .parse("MMMM yyyy")

                DstPeriod(
                        startDate = startDate,
                        endDate = endDate
                )
            }

            val (grade, gradeSum) = "\\d\\.\\d{4}".toRegex()
                    .findAll(quarterHistory)
                    .toList()
                    .map { match -> match.value.toDouble() }

            DstQuarter(
                    period = period,
                    subjects = subjects,
                    grade = grade,
                    gradeSum = gradeSum
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

        return DstRecord(quarters,
                enrolledSubjects, enrolledCredits,
                approvedSubjects, approvedCredits,
                retiredSubjects, retiredCredits,
                failedSubjects, failedCredits)
    }
}