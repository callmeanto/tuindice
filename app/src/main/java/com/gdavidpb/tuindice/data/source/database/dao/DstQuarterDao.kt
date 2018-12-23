package com.gdavidpb.tuindice.data.source.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.gdavidpb.tuindice.data.utils.*
import com.gdavidpb.tuindice.data.model.database.QuarterEntity
import com.gdavidpb.tuindice.data.model.database.QuarterAndSubjectsEntity
import com.gdavidpb.tuindice.domain.model.QuarterStatus
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
interface DstQuarterDao : BaseDao<QuarterEntity> {
    @Transaction
    @Query("SELECT * FROM $TABLE_QUARTERS WHERE $COLUMN_AID = :aid")
    fun getForAccount(aid: Long): Single<List<QuarterAndSubjectsEntity>>

    @Query("SELECT COUNT(*) FROM $TABLE_QUARTERS WHERE date('now') BETWEEN date($COLUMN_START_TIME, 'unixepoch') AND date($COLUMN_END_TIME, 'unixepoch')")
    fun isCurrentQuarterEnrolled(): Single<Boolean>

    @Query("SELECT (1.0 * SUM($COLUMN_CREDITS * $COLUMN_GRADE) / SUM($COLUMN_CREDITS)) FROM $TABLE_SUBJECTS WHERE $COLUMN_QID = :qid AND $COLUMN_STATUS = 0")
    fun computeGrade(qid: Long): Single<Double>

    @Query("SELECT (1.0 * SUM($COLUMN_CREDITS * $COLUMN_GRADE) / SUM($COLUMN_CREDITS)) FROM $TABLE_SUBJECTS WHERE $COLUMN_QID <= :qid AND $COLUMN_STATUS = 0 AND $COLUMN_ID NOT IN (SELECT $COLUMN_ID FROM $TABLE_SUBJECTS WHERE $COLUMN_QID < :qid AND $COLUMN_GRADE < 3 AND $COLUMN_STATUS = 0 AND $COLUMN_CODE IN (SELECT $COLUMN_CODE FROM $TABLE_SUBJECTS WHERE $COLUMN_QID = :qid AND $COLUMN_STATUS = 0 AND $COLUMN_GRADE >= 3) GROUP BY $COLUMN_CODE)")
    fun computeGradeSum(qid: Long): Single<Double>

    @Query("SELECT * FROM $TABLE_QUARTERS WHERE $COLUMN_STATUS = :status ORDER BY RANDOM() LIMIT 1")
    fun getSample(status: QuarterStatus): Maybe<QuarterEntity>
}