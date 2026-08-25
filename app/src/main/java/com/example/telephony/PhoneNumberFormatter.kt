package com.example.telephony

import android.content.Context
import android.telephony.TelephonyManager
import com.example.data.model.DialingCountry
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.michaelrocks.libphonenumber.android.Phonenumber.PhoneNumber
import java.util.Locale

object PhoneNumberFormatter {

    @Volatile
    private var phoneUtilInstance: PhoneNumberUtil? = null

    fun getPhoneUtil(context: Context): PhoneNumberUtil {
        return phoneUtilInstance ?: synchronized(this) {
            phoneUtilInstance ?: PhoneNumberUtil.createInstance(context.applicationContext).also {
                phoneUtilInstance = it
            }
        }
    }

    /**
     * Strips whitespace, hyphens, brackets, dots, leaving only digits and '+'
     */
    fun stripSeparators(number: String): String {
        return number.filter { it.isDigit() || it == '+' }
    }

    /**
     * Converts leading '00' to '+' for international notation
     */
    fun convertLeadingZerosToPlus(number: String): String {
        val trimmed = number.trim()
        return if (trimmed.startsWith("00")) {
            "+" + trimmed.substring(2)
        } else {
            trimmed
        }
    }

    /**
     * Cleans raw user input: strips spaces/symbols, converts leading 00 to +
     */
    fun cleanRawInput(number: String): String {
        val converted = convertLeadingZerosToPlus(number.trim())
        val sb = StringBuilder()
        var hasPlus = false
        for (ch in converted) {
            if (ch == '+') {
                if (!hasPlus && sb.isEmpty()) {
                    sb.append(ch)
                    hasPlus = true
                }
            } else if (ch.isDigit()) {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Infers default 2-letter ISO region on first launch:
     * SIM country → network country → device locale → "IN"
     */
    fun inferDefaultRegion(context: Context): String {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val simCountry = tm?.simCountryIso?.trim()?.uppercase(Locale.ROOT)
            if (!simCountry.isNullOrBlank() && simCountry.length == 2) {
                return simCountry
            }
            val networkCountry = tm?.networkCountryIso?.trim()?.uppercase(Locale.ROOT)
            if (!networkCountry.isNullOrBlank() && networkCountry.length == 2) {
                return networkCountry
            }
        } catch (_: Exception) {}

        try {
            val localeCountry = Locale.getDefault().country?.trim()?.uppercase(Locale.ROOT)
            if (!localeCountry.isNullOrBlank() && localeCountry.length == 2) {
                return localeCountry
            }
        } catch (_: Exception) {}

        return "IN"
    }

    /**
     * Detects country from a complete '+' E.164 number.
     */
    fun detectCountryFromE164(
        rawNumber: String,
        context: Context,
        availableCountries: List<DialingCountry>
    ): DialingCountry? {
        val clean = cleanRawInput(rawNumber)
        if (!clean.startsWith("+") || clean.length < 2) return null

        val phoneUtil = getPhoneUtil(context)
        return try {
            val parsed: PhoneNumber = phoneUtil.parse(clean, "")
            val regionCode = phoneUtil.getRegionCodeForNumber(parsed)
            if (!regionCode.isNullOrBlank() && regionCode != "ZZ") {
                availableCountries.firstOrNull { it.isoCode.equals(regionCode, ignoreCase = true) }
                    ?: availableCountries.firstOrNull { it.callingCode == "+${parsed.countryCode}" }
            } else {
                availableCountries.firstOrNull { it.callingCode == "+${parsed.countryCode}" }
            }
        } catch (_: Exception) {
            // Fallback prefix match
            availableCountries
                .filter { clean.startsWith(it.callingCode) }
                .maxByOrNull { it.callingCode.length }
        }
    }

    /**
     * Assembles the full E.164 dialing number according to Task 4 '+' rule:
     * - If input starts with '+' or '00', treat as complete E.164: NEVER prepend dial code!
     * - Otherwise, prepend selected country's dial code to digits (stripping national trunk '0' if needed).
     */
    fun assembleDialerNumber(rawInput: String, country: DialingCountry): String {
        val cleaned = cleanRawInput(rawInput)
        if (cleaned.startsWith("+")) {
            return cleaned
        }

        val digitsOnly = cleaned.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return ""

        val callingCode = if (country.callingCode.startsWith("+")) country.callingCode else "+${country.callingCode}"

        // If user typed national trunk zero (e.g. 07698983441 in India or 020... in UK),
        // strip leading zero for international E.164 assembly if length suggests national number
        val normalizedDigits = if (digitsOnly.startsWith("0") && digitsOnly.length > 5) {
            digitsOnly.drop(1)
        } else {
            digitsOnly
        }

        return "$callingCode$normalizedDigits"
    }

    data class ValidationResult(
        val isValid: Boolean,
        val isPossible: Boolean,
        val assembledE164: String,
        val helperText: String?,
        val isCountryDisabled: Boolean
    )

    /**
     * Validates typed number against country rules using libphonenumber.
     */
    fun validateNumber(
        rawInput: String,
        country: DialingCountry,
        context: Context
    ): ValidationResult {
        if (!country.enabled) {
            val assembled = assembleDialerNumber(rawInput, country)
            return ValidationResult(
                isValid = false,
                isPossible = false,
                assembledE164 = assembled,
                helperText = "Calling ${country.name} is not enabled on this account",
                isCountryDisabled = true
            )
        }

        val cleaned = cleanRawInput(rawInput)
        if (cleaned.isBlank()) {
            return ValidationResult(
                isValid = false,
                isPossible = false,
                assembledE164 = "",
                helperText = null,
                isCountryDisabled = false
            )
        }

        val assembled = assembleDialerNumber(rawInput, country)
        val phoneUtil = getPhoneUtil(context)

        return try {
            val isExplicitE164 = cleaned.startsWith("+")
            val targetRegion = if (isExplicitE164) "" else country.isoCode
            val parsed = phoneUtil.parse(assembled, targetRegion)

            val isValid = phoneUtil.isValidNumber(parsed)
            val validationReason = phoneUtil.isPossibleNumberWithReason(parsed)

            val helperText = when {
                isValid -> null
                validationReason == PhoneNumberUtil.ValidationResult.TOO_SHORT -> {
                    getExpectedLengthHint(country, "Number is too short")
                }
                validationReason == PhoneNumberUtil.ValidationResult.TOO_LONG -> {
                    "Number is too long for ${country.name}"
                }
                validationReason == PhoneNumberUtil.ValidationResult.INVALID_COUNTRY_CODE -> {
                    "Invalid country calling code"
                }
                validationReason == PhoneNumberUtil.ValidationResult.INVALID_LENGTH -> {
                    getExpectedLengthHint(country, "Invalid length")
                }
                else -> {
                    getExpectedLengthHint(country, "Enter a valid phone number")
                }
            }

            ValidationResult(
                isValid = isValid,
                isPossible = validationReason == PhoneNumberUtil.ValidationResult.IS_POSSIBLE,
                assembledE164 = assembled,
                helperText = helperText,
                isCountryDisabled = false
            )
        } catch (e: NumberParseException) {
            val helper = when (e.errorType) {
                NumberParseException.ErrorType.TOO_SHORT_NSN -> getExpectedLengthHint(country, "Number is too short")
                NumberParseException.ErrorType.TOO_LONG -> "Number is too long for ${country.name}"
                NumberParseException.ErrorType.INVALID_COUNTRY_CODE -> "Invalid country calling code"
                else -> getExpectedLengthHint(country, "Please enter a valid number")
            }
            ValidationResult(
                isValid = false,
                isPossible = false,
                assembledE164 = assembled,
                helperText = helper,
                isCountryDisabled = false
            )
        } catch (_: Exception) {
            ValidationResult(
                isValid = false,
                isPossible = false,
                assembledE164 = assembled,
                helperText = "Please enter a valid phone number",
                isCountryDisabled = false
            )
        }
    }

    private fun getExpectedLengthHint(country: DialingCountry, defaultPrefix: String): String {
        return when (country.isoCode.uppercase(Locale.ROOT)) {
            "IN" -> "Indian mobile numbers are 10 digits"
            "US", "CA" -> "US & Canada numbers are 10 digits"
            "GB" -> "UK numbers are 10-11 digits"
            "AU" -> "Australian numbers are 9-10 digits"
            else -> "$defaultPrefix for ${country.name}"
        }
    }

    /**
     * Safety net for contacts/recents stored numbers.
     * Ensures any stored number string is converted to valid E.164.
     */
    fun formatToE164(
        rawNumber: String,
        defaultRegion: String? = null,
        context: Context
    ): String {
        val clean = cleanRawInput(rawNumber)
        if (clean.isBlank()) return rawNumber

        if (clean.startsWith("+") && clean.length >= 8) {
            return clean
        }

        val region = defaultRegion?.ifBlank { null } ?: inferDefaultRegion(context)
        val phoneUtil = getPhoneUtil(context)
        return try {
            val parsed = phoneUtil.parse(clean, region)
            phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
        } catch (_: Exception) {
            val countryCode = phoneUtil.getCountryCodeForRegion(region)
            if (countryCode > 0 && !clean.startsWith("+$countryCode")) {
                "+$countryCode${clean.filter { it.isDigit() }}"
            } else if (!clean.startsWith("+")) {
                "+${clean.filter { it.isDigit() }}"
            } else {
                clean
            }
        }
    }
}
