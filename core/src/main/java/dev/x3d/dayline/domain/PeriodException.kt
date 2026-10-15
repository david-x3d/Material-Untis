package dev.x3d.dayline.domain

sealed class PeriodException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Rpc(val code: Int, message: String) : PeriodException("RPC $code: $message")
    class Network(cause: Throwable) : PeriodException("Network error", cause)
    class Auth(message: String = "Sign-in failed") : PeriodException(message)
    class SessionExpired : PeriodException("Session expired")
    /** RPC -7004 "no allowed date": the school does not publish the timetable for this range. */
    class DateNotAllowed : PeriodException("Timetable not published for this date")
    class SchoolNotFound : PeriodException("School not found")
}
