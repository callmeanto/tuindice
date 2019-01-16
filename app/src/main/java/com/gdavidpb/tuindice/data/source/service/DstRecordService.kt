package com.gdavidpb.tuindice.data.source.service

import com.gdavidpb.tuindice.data.source.service.selector.DstPersonalDataSelector
import com.gdavidpb.tuindice.data.source.service.selector.DstRecordDataSelector
import retrofit2.http.GET

interface DstRecordService {
    @GET("datosPersonales.do")
    fun getPersonalData(): DstPersonalDataSelector

    @GET("informeAcademico.do")
    fun getRecordData(): DstRecordDataSelector
}