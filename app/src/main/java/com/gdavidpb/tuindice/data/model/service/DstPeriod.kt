package com.gdavidpb.tuindice.data.model.service

import com.gdavidpb.tuindice.utils.format
import java.util.*

data class DstPeriod(
        val startDate: Date?,
        val endDate: Date?
) {
    override fun toString(): String {
        val start = startDate?.format("MMM")?.capitalize()
        val end = endDate?.format("MMM")?.capitalize()
        val year = startDate?.format("yyyy")

        return "$start - $end $year".replace("\\.".toRegex(), "")
    }
}