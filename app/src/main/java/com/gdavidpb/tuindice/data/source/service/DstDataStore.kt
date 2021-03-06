package com.gdavidpb.tuindice.data.source.service

import com.gdavidpb.tuindice.domain.model.AuthResponse
import com.gdavidpb.tuindice.domain.model.AuthResponseCode
import com.gdavidpb.tuindice.domain.model.exception.AuthException
import com.gdavidpb.tuindice.domain.model.service.DstEnrollment
import com.gdavidpb.tuindice.domain.model.service.DstPersonal
import com.gdavidpb.tuindice.domain.model.service.DstRecord
import com.gdavidpb.tuindice.domain.repository.DstRepository
import com.gdavidpb.tuindice.domain.usecase.request.AuthRequest
import com.gdavidpb.tuindice.utils.mappers.toAuthResponse
import com.gdavidpb.tuindice.utils.mappers.toEnrollment
import com.gdavidpb.tuindice.utils.mappers.toPersonalData
import com.gdavidpb.tuindice.utils.mappers.toRecord
import okhttp3.ResponseBody

open class DstDataStore(
        private val authService: DstAuthService,
        private val recordService: DstRecordService,
        private val enrollmentService: DstEnrollmentService
) : DstRepository {
    override suspend fun getPersonalData(): DstPersonal? {
        return recordService.getPersonalData().body()?.toPersonalData()
    }

    override suspend fun getRecordData(): DstRecord? {
        return recordService.getRecordData().body()?.toRecord()
    }

    override suspend fun getEnrollment(): DstEnrollment? {
        return runCatching {
            enrollmentService.getEnrollment().body()
        }.getOrNull()?.toEnrollment()
    }

    override suspend fun getEnrollmentProof(): ResponseBody? {
        return enrollmentService.getEnrollmentProof().body()?.let {
            val isValid = "${it.contentType()}" == "application/pdf"

            if (isValid) it else null
        }
    }

    override suspend fun auth(request: AuthRequest): AuthResponse? {
        return authService.auth(request.serviceUrl, request.usbId, request.password).body()?.let {
            val authResponse = it.toAuthResponse()

            when (authResponse.code) {
                AuthResponseCode.SUCCESS, AuthResponseCode.NO_ENROLLED -> authResponse
                else -> throw AuthException(code = authResponse.code, message = authResponse.message)
            }
        }
    }
}