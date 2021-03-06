package com.gdavidpb.tuindice.data.source.service

import com.gdavidpb.tuindice.data.source.service.selector.DstEnrollmentDataSelector
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

interface DstEnrollmentService {
    @GET("secure/generarComprobante.do")
    suspend fun getEnrollment(
            @Query("pagina") mode: String = "regular"
    ): Response<DstEnrollmentDataSelector>

    @Streaming
    @GET("secure/imprimirComprobante.do")
    suspend fun getEnrollmentProof(): Response<ResponseBody>
}