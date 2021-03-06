package com.gdavidpb.tuindice.utils.mappers

import android.net.Uri
import android.util.Base64
import com.gdavidpb.tuindice.domain.model.Auth
import com.gdavidpb.tuindice.domain.model.service.DstCredentials
import com.gdavidpb.tuindice.domain.usecase.request.AuthRequest
import com.gdavidpb.tuindice.domain.usecase.request.ResetRequest
import com.gdavidpb.tuindice.utils.REF_BASE
import com.gdavidpb.tuindice.utils.extensions.parse
import com.gdavidpb.tuindice.utils.extensions.trimAll
import com.gdavidpb.tuindice.utils.extensions.year
import com.google.firebase.auth.FirebaseUser
import java.util.*
import kotlin.math.abs

var REF_YEAR = -1

fun AuthRequest.toDstCredentials() = DstCredentials(usbId = usbId, password = password)

fun FirebaseUser.toAuth() = Auth(
        uid = uid,
        email = email ?: ""
)

fun String.toResetRequest(): ResetRequest {
    fun getCode(uri: Uri) = uri.getQueryParameter("oobCode")!!
    fun getContinueUrl(uri: Uri) = uri.getQueryParameter("continueUrl")!!

    val mainUri = Uri.parse(this)

    val code = getCode(mainUri)
    val continueUrl = getContinueUrl(mainUri)

    val continueUri = Uri.parse(continueUrl)

    val resetPassword = continueUri.getQueryParameter("")!!

    val (email, password) = resetPassword.toResetParam()

    return ResetRequest(code, email, password)
}

fun String.toResetParam(): Pair<String, String> {
    val data = String(Base64.decode(this, Base64.DEFAULT)).split("\n")

    return data.first() to data.last()
}

fun Pair<String, String>.fromResetParam(): String {
    val data = "$first\n$second".toByteArray()

    return Base64.encodeToString(data, Base64.DEFAULT)
}

fun String.toStartEndDate(): List<Date> {
    val normalizedText = "\\w+\\s*-\\s*\\w+\\s*\\d{4}".toRegex().find(this)!!.value

    val year = normalizedText.substringAfterLast(" ").trimAll().toIntOrNull() ?: 0
    val months = normalizedText.substringBeforeLast(" ").trimAll()

    return months
            .split("\\s*-\\s*".toRegex())
            .mapNotNull { month -> "$month $year".parse("MMMM yyyy") }
            .also { output ->
                checkIntegrity(input = this, output = output, refYear = REF_YEAR)
            }
}

fun String.toRefYear() = "^[^-]+".toRegex().find(this)!!.value.toInt() + REF_BASE

private fun checkIntegrity(input: String, output: List<Date>, refYear: Int) {
    if (output.size != 2)
        throw IllegalStateException("toStartEndDate: '$input'")

    val startYear = output[0].year()
    val endYear = output[1].year()

    val isInvalidDistance = abs(startYear - endYear) > 1

    val isInvalid = if (refYear != -1) {
        val validRange = (refYear - 1)..(Date().year() + 1)

        !validRange.contains(startYear) || !validRange.contains(endYear) || isInvalidDistance
    } else
        isInvalidDistance

    if (isInvalid)
        throw IllegalStateException("toStartEndDate: '$input'")
}