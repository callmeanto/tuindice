package com.gdavidpb.tuindice.models

import android.accounts.AuthenticatorException
import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.ResultReceiver
import android.support.v4.app.NotificationCompat
import com.gdavidpb.tuindice.*
import com.gdavidpb.tuindice.activities.LoginActivity
import com.gdavidpb.tuindice.activities.MainActivity
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL
import java.net.URLEncoder
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class DstService : Service() {

    inner class DstServiceBinder : Binder() {
        fun getService() = this@DstService
    }

    companion object {
        fun startService(
                usbId: String,
                password: String,
                context: Context,
                receiver: ResultReceiver) {
            val intent = Intent(context, DstService::class.java)

            intent.putExtra(Constants.EXTRA_RECEIVER, receiver)
            intent.putExtra(Constants.EXTRA_ACCOUNT, DstAccount(usbId, password))

            context.getPreferences().resetConnectionRetry()

            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, DstService::class.java)

            context.stopService(intent)
        }

        fun bindService(context: Context, callback: DstService.() -> Unit): ServiceConnection? {
            val intent = Intent(context, DstService::class.java)

            val connection = object : ServiceConnection {
                override fun onServiceConnected(className: ComponentName, service: IBinder) {
                    (service as DstService.DstServiceBinder).getService().callback()
                }

                override fun onServiceDisconnected(className: ComponentName) { }
            }

            return try {
                context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

                connection
            } catch (exception: Exception) {
                exception.printStackTrace()

                null
            }
        }

        fun unbindService(context: Context, connection: ServiceConnection?): Boolean {
            return try {
                if (connection != null)
                    context.unbindService(connection)
                true
            } catch (exception: Exception) {
                exception.printStackTrace()

                false
            }
        }
    }

    private lateinit var receiver: ResultReceiver

    private lateinit var sslContext: SSLContext
    private lateinit var notificationManager: NotificationManager
    private lateinit var notification: NotificationCompat.Builder

    /* Fields */
    private var operation: Operation? = null

    /* Constants */
    private val notificationId = 0

    private val dstRecord = "https://expediente.dii.usb.ve/"
    private val dstEnrollment = "https://comprobante.dii.usb.ve/"

    private val dstRecordLogin = "login.do"
    private val dstEnrollmentLogin = "CAS/login.do"
    private val dstDoRecord = "informeAcademico.do"
    private val dstDoPersonal = "datosPersonales.do"

    private val startFormat by lazy { SimpleDateFormat("MMMM", Locale.getDefault()) }
    private val endFormat by lazy { SimpleDateFormat("MMMM-MMMM yyyy", Locale.getDefault()) }

    private val validHostNames = arrayOf(
            "expediente.dii.usb.ve",
            "comprobante.dii.usb.ve",
            "secure.dst.usb.ve"
    )

    override fun onBind(intent: Intent): IBinder = DstServiceBinder()

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        /* Prevent service loop */
        if (applicationContext.getPreferences().connectionRetryEnough()) {
            stopSelf()
            return START_NOT_STICKY
        }

        /* Prevent service recall */
        if (!applicationContext.getDatabase().getActiveAccount().isEmpty())
            return START_REDELIVER_INTENT

        /* Set Spanish-Venezuela as default locale */
        Locale.setDefault(Locale("es", "VE"))

        /* Get receiver (service communication) */
        receiver = intent.getParcelableExtra(Constants.EXTRA_RECEIVER)

        /* Get account */
        val account = intent.getSerializableExtra(Constants.EXTRA_ACCOUNT) as DstAccount

        /* Get notification service */
        notification = NotificationCompat.Builder(applicationContext, Constants.CHANNEL_SERVICE)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        async( /* on Task */ {
            try {
                collectFrom(account.usbId, account.password, true)
            } catch (exception: Exception) {
                exception.printStackTrace()

                DstResponse<DstAccount>(exception)
            }
        }, /* on Response */ {
            sendOperationResponse(this)
        })

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()

        super.onDestroy()
    }

    fun collectFrom(usbId: String, password: String, notify: Boolean = false): DstResponse<DstAccount> {
        var bundle = Bundle()
        val account = DstAccount(usbId, password)

        initSsl()

        for (operation in enumValues<Operation>()) {
            if (notify)
                sendOperationUpdate(operation)

            bundle = account.serviceOperation(operation, bundle)
        }

        return DstResponse(account)
    }

    fun setReceiver(receiver: ResultReceiver) {
        this.receiver = receiver
    }

    fun getOperation(): Operation? = operation

    fun cancelAllNotification() {
        notificationManager.cancelAll()
    }

    /* Init SSL context */
    private fun initSsl() {
        sslContext = SSLContext.getInstance("TLS")

        val inputStream = resources.openRawResource(R.raw.certificates)
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val certificates = certificateFactory.generateCertificates(inputStream)
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())

        inputStream.close()

        keyStore.load(null)

        for (crt in certificates)
            keyStore.setCertificateEntry((crt as X509Certificate).getProperty("CN"), crt)

        trustManagerFactory.init(keyStore)

        sslContext.init(null, trustManagerFactory.trustManagers, SecureRandom())
    }

    /* Call service operation */
    private fun DstAccount.serviceOperation(operation: Operation, bundle: Bundle): Bundle {
        val result = Bundle()

        this@DstService.operation = operation

        when (operation) {
            Operation.CONNECT -> {
                val https = URL("$dstRecord$dstRecordLogin").openConnection() as HttpsURLConnection

                https.setUp("GET")
                https.connect()

                val document = https.getDocument()

                https.inputStream.close()

                https.disconnect()

                val lt = document.select("input[name=lt]").first().attr("value")
                val cookie = https.getHeaderField("Set-Cookie")

                if (lt == null || lt.isEmpty())
                    throw IllegalStateException("$operation: lt no found")

                if (cookie == null || cookie.isEmpty())
                    throw IllegalStateException("$operation: cookie no found")

                result.putString("lt", lt)
                result.putString("cookie", cookie)
                result.putSerializable("url", https.url)
            }
            Operation.LOGIN_RECORD -> {
                val url = bundle.getSerializable("url") as URL
                val lt = bundle.getString("lt")
                var cookie = bundle.getString("cookie")

                val https = url.openConnection() as HttpsURLConnection

                val payload = hashMapOf(
                        "username" to usbId,
                        "password" to password,
                        "lt" to lt,
                        "_eventId" to "submit").toPayload()

                https.setUp("POST")
                https.setRequestProperty("Cookie", cookie)
                https.setRequestProperty("Content-Length", "${payload.size}")
                https.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                https.connect()

                val outputStream = https.outputStream

                outputStream.write(payload)
                outputStream.flush()
                outputStream.close()

                /* Get response from Post */
                val document = https.getDocument()

                https.inputStream.close()

                https.disconnect()

                val success = document.select("div[class=errors]").isEmpty()

                if (!success)
                    throw AuthenticatorException()

                cookie = https.getHeaderField("Set-Cookie")

                if (cookie == null || cookie.isEmpty())
                    throw IllegalStateException("$operation: cookie no found")

                result.putString("cookie", cookie)
            }
            Operation.COLLECT_RECORD -> {
                val cookie = bundle.getString("cookie")

                /* Get record document */
                val https = URL("$dstRecord$dstDoRecord").openConnection() as HttpsURLConnection

                https.setUp("GET")
                https.setRequestProperty("Cookie", cookie)

                https.connect()

                val document = https.getDocument()

                https.inputStream.close()

                https.disconnect()

                /* Select quarter tables */
                val data = document.select("table[class=tabla] > tbody > tr > td > table")

                if (data == null || data.isEmpty())
                    throw IllegalStateException("$operation: data no found")

                if (data.size < 3)
                    throw IllegalStateException("$operation: invalid data")

                val recordData = data.dropLast(3)
                val summaryData = data.takeLast(1)[0].select("td[align=center]")

                if (recordData.size % 3 != 0)
                    throw IllegalStateException("$operation: invalid record data")

                if (summaryData.size < 8)
                    throw IllegalStateException("$operation: invalid summary data")

                enrolledSubjects = summaryData[0].text().toInt()
                enrolledCredits = summaryData[1].text().toInt()

                approvedSubject = summaryData[2].text().toInt()
                approvedCredits = summaryData[3].text().toInt()

                retiredSubjects = summaryData[4].text().toInt()
                retiredCredits = summaryData[5].text().toInt()

                failedSubjects = summaryData[6].text().toInt()
                failedCredits = summaryData[7].text().toInt()

                if (recordData.isNotEmpty())
                    for (i in recordData.indices.step(3)) {
                    /* HTML raw elements */
                    val quarterPeriod = recordData[i]
                            .select("td")[0]
                            .text()
                            .trim()
                            .replace("\\s*-\\s*".toRegex(), "-")
                            .toLowerCase()

                    val subjectElement = recordData[i + 1]
                            .select("tr > td:not(:has(*))")

                    val summaryElement = "\\d\\.\\d{4}".toRegex()
                            .find(recordData[i + 2].text())

                    /* Get quarter start and end time */
                    val startTime = Calendar.getInstance()
                    val endTime = Calendar.getInstance()
                    val subjects = ArrayList<DstSubject>()

                    /* Find quarter grades */
                    val quarterGrade = summaryElement!!.value.toDouble()
                    val quarterGradeSum = summaryElement.next()!!.value.toDouble()

                    startTime.time = startFormat.parse(quarterPeriod)
                    endTime.time = endFormat.parse(quarterPeriod)

                    startTime.set(Calendar.YEAR, endTime.get(Calendar.YEAR))

                    /* for subject */
                    for (j in subjectElement.indices.step(5)) {
                        val code = subjectElement[j].text().trim()
                        val name = subjectElement[j + 1].text().trim().toSubjectName()
                        val credits = subjectElement[j + 2].text().toIntOrNull()!!
                        val grade = subjectElement[j + 3].text().toIntOrNull() ?: 0
                        val detail = when (subjectElement[j + 4].text().trim()) {
                            SubjectStatus.RETIRED.toString(applicationContext) -> SubjectStatus.RETIRED
                            SubjectStatus.NO_EFFECT.toString(applicationContext) -> SubjectStatus.NO_EFFECT
                            else -> SubjectStatus.OK
                        }

                        subjects.add(DstSubject(code, name, credits, grade, detail))
                    }

                    if (lastUpdate < startTime.timeInMillis)
                        lastUpdate = startTime.timeInMillis

                    val quarter = DstQuarter(
                            QuarterType.COMPLETED,
                            startTime.timeInMillis,
                            endTime.timeInMillis,
                            quarterGrade,
                            quarterGradeSum)

                    quarter.subjects.addAll(subjects)

                    quarters.add(quarter)
                }

                result.putString("cookie", cookie)
            }
            Operation.COLLECT_PERSONAL -> {
                val cookie = bundle.getString("cookie")

                /* Get personal document */
                val https = URL("$dstRecord$dstDoPersonal").openConnection() as HttpsURLConnection

                https.setUp("GET")
                https.setRequestProperty("Cookie", cookie)

                https.connect()

                val document = https.getDocument()

                https.inputStream.close()

                https.disconnect()

                /* Select personal data table */
                val personalData = document.select("table[class=tablaL] > tbody > tr > td > table > tbody > tr > td")

                if (personalData.size < 6)
                    throw IllegalStateException("$operation: invalid personal data")

                val career = personalData[4]
                        .text()
                        .trim()
                        .replace("\\s*-\\s*".toRegex(), "-")
                        .split("-")

                firstNames = personalData[2].text().trim()
                lastNames = personalData[3].text().trim()
                careerCode = Integer.parseInt(career[0])
                careerName = career[1]
            }
            Operation.LOGIN_ENROLLMENT -> {
                val https = URL("$dstEnrollment$dstEnrollmentLogin").openConnection() as HttpsURLConnection

                https.setUp("GET")

                https.connect()

                val document = https.getDocument()

                https.inputStream.close()

                https.disconnect()

                val lt = document.select("input[name=lt]").first().attr("value")

                val cookie = https.getHeaderField("Set-Cookie")

                if (lt == null || lt.isEmpty())
                    throw IllegalStateException("$operation: lt no found")

                if (cookie == null || cookie.isEmpty())
                    throw IllegalStateException("$operation: cookie no found")

                result.putString("lt", lt)
                result.putString("cookie", cookie)
                result.putSerializable("url", https.url)
            }
            Operation.COLLECT_ENROLLMENT -> {
                var url = bundle.getSerializable("url") as URL
                val lt = bundle.getString("lt")
                val cookie = bundle.getString("cookie")

                val https = url.openConnection() as HttpsURLConnection

                val payload = hashMapOf(
                        "username" to usbId,
                        "password" to password,
                        "lt" to lt,
                        "_eventId" to "submit").toPayload()

                https.setUp("POST")
                https.instanceFollowRedirects = false
                https.setRequestProperty("Cookie", cookie)
                https.setRequestProperty("Content-Length", "${payload.size}")
                https.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                https.connect()

                val outputStream = https.outputStream

                outputStream.write(payload)
                outputStream.flush()
                outputStream.close()

                val inputStream = https.inputStream

                inputStream.read()

                inputStream.close()

                https.disconnect()

                val location = https.getHeaderField("Location")

                if (location == null || location.isEmpty())
                    throw IllegalStateException("$operation: url no found")

                url = URL(location)

                result.putSerializable("url", url)
            }
            Operation.FINISH -> {
                if (firstNames.isEmpty() || lastNames.isEmpty())
                    throw IllegalStateException("$operation: invalid continuation")

                val url = bundle.getSerializable("url") as URL

                val https = url.openConnection() as HttpsURLConnection

                https.setUp("GET")

                https.connect()

                val document = https.getDocument()

                https.disconnect()

                /* Select subjects */
                val quarter: DstQuarter
                val startTime = Calendar.getInstance()
                val endTime = Calendar.getInstance()
                val subjects = ArrayList<DstSubject>()

                val subjectData = document.select("table > tbody > tr > td[rowspan]:not(:has(b))")
                val subjectNames = document.select("table > tbody > tr[id]")

                if (subjectData.size % 3 != 0)
                    throw IllegalStateException("$operation: invalid subject data")

                if (subjectNames.size != (subjectData.size / 3))
                    throw IllegalStateException("$operation: invalid subject names")

                /* Quarter enrolled */
                if (subjectData.isNotEmpty()) {
                    val quarterPeriod = "\\w+-\\w+ \\d{4}"
                            .toRegex()
                            .find(document
                                    .select("div[id=horario] > div > div")
                                    .text())!!.value
                            .toLowerCase()

                    startTime.time = startFormat.parse(quarterPeriod)
                    endTime.time = endFormat.parse(quarterPeriod)

                    startTime.set(Calendar.YEAR, endTime.get(Calendar.YEAR))

                    if (lastUpdate < startTime.timeInMillis)
                        lastUpdate = startTime.timeInMillis

                    for (i in subjectData.indices.step(3)) {
                        val code = "^[^\\s]+".toRegex().find(subjectData[i].text())!!.value
                        val name = "(?<=])[^\$]+".toRegex().find(subjectNames[i / 3].text())!!.value.trim().toSubjectName()
                        val credits = subjectData[i + 2].text().toInt()

                        subjects.add(DstSubject(code, name, credits))
                    }

                    quarter = DstQuarter(
                            QuarterType.CURRENT,
                            startTime.timeInMillis,
                            endTime.timeInMillis)

                    quarter.subjects.addAll(subjects)

                    quarters.add(quarter)
                }
            }
        }

        return result
    }

    /* Send operation update to UI handler */
    private fun sendOperationUpdate(operation: Operation) {
        val bundle = Bundle()
        val message = operation.toString(applicationContext)

        bundle.putString(Constants.EXTRA_UPDATE, message)

        if (LifecycleHandler.isAppVisible())
            receiver.send(Constants.RES_UPDATE, bundle)
        else
            sendNotification {
                setOngoing(true)
                setAutoCancel(true)
                setContentText(message)
                setSmallIcon(R.drawable.ic_login)
                setProgress(0, 0, true)
            }
    }

    /* Send operation response to UI handler */
    private fun sendOperationResponse(response: DstResponse<DstAccount>) {
        val bundle = Bundle()

        bundle.putSerializable(Constants.EXTRA_RESPONSE, response)

        if (LifecycleHandler.isAppVisible()) {
            stopSelf()

            receiver.send(Constants.RES_RESPONSE, bundle)
        } else when {
            response.exception != null -> {
                val intent = Intent(this, LoginActivity::class.java)

                intent.putExtra(Constants.EXTRA_RESPONSE, response)

                val pendingIntent = PendingIntent.getActivity(
                        this,
                        1,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or
                                PendingIntent.FLAG_ONE_SHOT)

                sendNotification {
                    setOngoing(false)
                    setAutoCancel(true)
                    setContentIntent(pendingIntent)
                    setSmallIcon(R.drawable.ic_warning)
                    setDefaults(Notification.DEFAULT_ALL)
                    setProgress(0, 0, false)
                    setContentText(getString(response.exception!!.toDescription()))
                }
            }
            response.result != null -> {
                val intent = Intent(this, MainActivity::class.java)

                getDatabase().addAccount(response.result, true)

                val pendingIntent = PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or
                                PendingIntent.FLAG_ONE_SHOT)

                sendNotification {
                    setOngoing(false)
                    setAutoCancel(true)
                    setContentIntent(pendingIntent)
                    setSmallIcon(R.drawable.ic_done)
                    setDefaults(Notification.DEFAULT_ALL)
                    setProgress(0, 0, false)
                    setContentText(getString(R.string.messageReady))
                }
            }
        }
    }

    /* Send notification */
    private fun sendNotification(f: NotificationCompat.Builder.() -> Unit = { }) {
        notification.f()

        notificationManager.notify(notificationId, notification.build())
    }

    /* Capitalize a subject name */
    private fun String.toSubjectName(): String {
        val buffer = StringBuffer()

        val matcher = Pattern
                .compile("(?<=\\W)[xvi]+|(^|(?<=\\s))((\\S(?=\\S{2,}))|(?<=:\\s)\\S)")
                .matcher(toLowerCase()
                        .replace("^\"|\"$".toRegex(), "")
                        .replace("(?<=\\w)\\.(?=\\w)".toRegex(), " "))

        while (matcher.find())
            matcher.appendReplacement(buffer, matcher.group().toUpperCase())

        matcher.appendTail(buffer)

        return buffer.toString()
    }

    /* Get x509 certificate properties by key */
    private fun X509Certificate.getProperty(key: String): String = "(?<=$key=)[^,]+|$".toRegex().find(subjectDN.name)!!.value

    /* HashMap to POST-payload */
    private fun HashMap<String, String>.toPayload(): ByteArray {
        val builder = StringBuilder()

        for ((key, value) in this)
            builder.append("$key=${URLEncoder.encode(value, "UTF-8")}&")

        builder.setLength(builder.length - 1)

        return builder.toString().toByteArray()
    }

    /* Parse Https input stream to Jsoup-document */
    private fun HttpsURLConnection.getDocument(): Document {
        val charset = "(?<=charset=).+$".toRegex().find(getHeaderField("Content-Type"))!!.value

        return Jsoup.parse(inputStream, charset, url.toString())
    }

    /* Default set up for HTTPS connection */
    private fun HttpsURLConnection.setUp(requestMethod: String) {
        sslSocketFactory = sslContext.socketFactory

        setHostnameVerifier {
            hostname, session ->
            validHostNames.contains(hostname) && session.isValid
        }

        when (requestMethod) {
            "GET" -> doInput = true
            "POST" -> {
                doInput = true
                doOutput = true
            }
        }

        readTimeout = 60000
        connectTimeout = 60000

        this.requestMethod = requestMethod
    }
}