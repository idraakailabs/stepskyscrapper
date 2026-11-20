package com.idraak.stepskyscraper.core.domain.model

/** Typed result used across the domain. Errors are values, never swallowed exceptions. */
sealed interface Outcome<out T> {
    data class Success<out T>(val value: T) : Outcome<T>

    data class Failure(val error: DomainError) : Outcome<Nothing>
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

inline fun <T> Outcome<T>.onSuccess(block: (T) -> Unit): Outcome<T> {
    if (this is Outcome.Success) block(value)
    return this
}

inline fun <T> Outcome<T>.onFailure(block: (DomainError) -> Unit): Outcome<T> {
    if (this is Outcome.Failure) block(error)
    return this
}

fun <T> Outcome<T>.getOrNull(): T? = (this as? Outcome.Success)?.value

/** Every failure the app knows how to explain to the user. No red states: each maps to calm copy. */
sealed interface DomainError {
    /** Health Connect app is missing on this device (Android 13 and lower). */
    data object HealthConnectNotInstalled : DomainError

    /** Health Connect is installed but must be updated before it can be used. */
    data object HealthConnectNeedsUpdate : DomainError

    /** The user declined or revoked the Steps read permission. */
    data object StepPermissionDenied : DomainError

    /** Neither Health Connect nor a step-counter sensor is available. */
    data object NoStepSource : DomainError

    /** Health Connect refused the request (rate limit, background restrictions, IPC failure). */
    data class StepSourceFailed(val message: String) : DomainError

    /** Local database or preferences could not be read or written. */
    data class Storage(val message: String) : DomainError

    /** Not enough Blueprints for the requested cosmetic. */
    data class InsufficientBlueprints(val needed: Int, val balance: Int) : DomainError

    /** Item is locked behind a rule the user has not met yet. */
    data class ItemLocked(val itemId: String) : DomainError

    /** Google Play Billing problem. */
    data class Billing(val reason: BillingFailure, val message: String = "") : DomainError

    /** The rewarded ad could not be shown. */
    data class AdUnavailable(val reason: AdFailure) : DomainError

    /** An operation already happened and may not be repeated (for example a second rewarded ad per tower). */
    data object AlreadyClaimed : DomainError
}

enum class BillingFailure { UserCancelled, ServiceUnavailable, ItemUnavailable, AlreadyOwned, Pending, Developer, Unknown }

enum class AdFailure { NoConsent, NotLoaded, NoFill, Dismissed, Premium, Offline }
