package com.paperdesk.launcher.data

/** Common Madhva sampradaya nama/mantra choices. Guru-upadesha entries are explicitly marked. */
object JapaCatalog {
    data class Entry(
        val id: String,
        val label: String,
        val mantra: String,
        val requiresUpadesha: Boolean = false
    )

    val entries = listOf(
        Entry("narayana", "ನಾರಾಯಣ ಅಷ್ಟಾಕ್ಷರ", "ಓಂ ನಮೋ ನಾರಾಯಣಾಯ"),
        Entry("vasudeva", "ವಾಸುದೇವ ದ್ವಾದಶಾಕ್ಷರ", "ಓಂ ನಮೋ ಭಗವತೇ ವಾಸುದೇವಾಯ"),
        Entry("krishna", "ಶ್ರೀಕೃಷ್ಣ ನಾಮ", "ಶ್ರೀ ಕೃಷ್ಣಾಯ ನಮಃ"),
        Entry("rama", "ಶ್ರೀರಾಮ ನಾಮ", "ಶ್ರೀ ರಾಮಾಯ ನಮಃ"),
        Entry("lakshmi_narayana", "ಶ್ರೀಲಕ್ಷ್ಮೀನಾರಾಯಣ ನಾಮ", "ಓಂ ಶ್ರೀ ಲಕ್ಷ್ಮೀನಾರಾಯಣಾಯ ನಮಃ"),
        Entry("venkatesha", "ಶ್ರೀವೇಂಕಟೇಶ ನಾಮ", "ಓಂ ನಮೋ ವೇಂಕಟೇಶಾಯ"),
        Entry("hayagriva", "ಶ್ರೀಹಯಗ್ರೀವ ನಾಮ", "ಓಂ ಶ್ರೀ ಹಯಗ್ರೀವಾಯ ನಮಃ"),
        Entry("narasimha", "ಶ್ರೀನರಸಿಂಹ ನಾಮ", "ಓಂ ಶ್ರೀ ನರಸಿಂಹಾಯ ನಮಃ"),
        Entry("mukhya_prana", "ಶ್ರೀಮುಖ್ಯಪ್ರಾಣ ನಾಮ", "ಓಂ ಮುಖ್ಯಪ್ರಾಣಾಯ ನಮಃ"),
        Entry("hanuman", "ಶ್ರೀಹನುಮಂತ ನಾಮ", "ಓಂ ಶ್ರೀ ಹನುಮತೇ ನಮಃ"),
        Entry("madhvacharya", "ಶ್ರೀಮಧ್ವಾಚಾರ್ಯ ನಾಮ", "ಓಂ ಶ್ರೀ ಮಧ್ವಾಚಾರ್ಯಾಯ ನಮಃ"),
        Entry("raghavendra", "ಶ್ರೀರಾಘವೇಂದ್ರ ಗುರು ನಾಮ", "ಓಂ ಶ್ರೀ ರಾಘವೇಂದ್ರಾಯ ನಮಃ"),
        Entry("panduranga", "ಶ್ರೀಪಾಂಡುರಂಗ ವಿಠ್ಠಲ ನಾಮ", "ಓಂ ಶ್ರೀ ಪಾಂಡುರಂಗ ವಿಠ್ಠಲಾಯ ನಮಃ"),
        Entry(
            "gayatri",
            "ಗಾಯತ್ರೀ ಮಂತ್ರ — ಉಪದೇಶ ಪಡೆದವರಿಗೆ ಮಾತ್ರ",
            "ಓಂ ಭೂರ್ಭುವಸ್ಸುವಃ । ತತ್ಸವಿತುರ್ವರೇಣ್ಯಂ ಭರ್ಗೋ ದೇವಸ್ಯ ಧೀಮಹಿ । ಧಿಯೋ ಯೋ ನಃ ಪ್ರಚೋದಯಾತ್ ॥",
            requiresUpadesha = true
        )
    )

    fun indexOf(mantra: String): Int = entries.indexOfFirst { it.mantra == mantra }.coerceAtLeast(0)
}
