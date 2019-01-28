package com.gdavidpb.tuindice.utils

import java.util.*

/* Dst service endpoints */
const val ENDPOINT_DST_SECURE = "https://secure.dst.usb.ve/"
const val ENDPOINT_DST_RECORD = "https://expediente.dii.usb.ve/"
const val ENDPOINT_DST_ENROLLMENT = "https://comprobante.dii.usb.ve/"
const val ENDPOINT_DST_POLL = "https://consulta.dii.usb.ve/"

const val ENDPOINT_DST_RECORD_AUTH = "${ENDPOINT_DST_RECORD}login.do"
const val ENDPOINT_DST_ENROLLMENT_AUTH = "${ENDPOINT_DST_ENROLLMENT}CAS/login.do"
const val ENDPOINT_DST_POLL_AUTH = "${ENDPOINT_DST_ENROLLMENT}Consulta/Secure/zk/login.zul"

/* Firebase storage urls */
const val URL_PRIVACY_POLICY = "https://tuindice-usb.firebaseapp.com/"

/* Preferences keys */
const val KEY_COUNT_DOWN = "countdown"
const val KEY_FIRST_RUN = "firstRun"
const val KEY_COOL_DOWN = "Cooldown"
const val KEY_IS_AWAITING_FOR_RESET = "isAwaitingForReset"
const val KEY_IS_AWAITING_FOR_VERIFY = "isAwaitingForVerify"
const val KEY_AWAITING_EMAIL = "AwaitingEmail"
const val KEY_AWAITING_PASSWORD = "AwaitingPassword"

/* Romans */
val ROMANS = hashMapOf(
        "1" to "I",
        "2" to "II",
        "3" to "III",
        "4" to "IV",
        "5" to "V",
        "6" to "VI",
        "7" to "VII",
        "8" to "VIII",
        "9" to "IX",
        "10" to "X"
)

val QUARTER_STARTS = arrayOf(
        Calendar.JANUARY,
        Calendar.APRIL,
        Calendar.JULY,
        Calendar.SEPTEMBER)

val QUARTER_ENDS = arrayOf(
        Calendar.MARCH,
        Calendar.JULY,
        Calendar.AUGUST,
        Calendar.DECEMBER
)

val QUARTER_RANGES = QUARTER_STARTS.mapIndexed { index, _ ->
    QUARTER_STARTS[index] to QUARTER_ENDS[index]
}

/* Default locale */
val DEFAULT_LOCALE = Locale("es", "VE")

/* Notification channels */
const val CHANNEL_SERVICE = "CHANNEL_SERVICE"

/* Times */
const val TIME_COUNT_DOWN = 5 * 60 * 1000
const val TIME_BACKGROUND_ANIMATION = 30000L
const val TIME_DELAY_CLICK_ONCE = 500L

/* Extras */
const val AWAITING_STATE = "AWAITING_STATE"
const val AWAITING_EMAIL = "AWAITING_EMAIL"

const val FLAG_RESET = 1000
const val FLAG_VERIFY = 1001