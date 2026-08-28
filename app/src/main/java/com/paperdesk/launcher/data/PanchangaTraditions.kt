package com.paperdesk.launcher.data

data class PanchangaTradition(
    val id: String,
    val kn: String,
    val sourceUrl: String,
    val liveDailyData: Boolean = false
)

object PanchangaTraditions {
    const val SRS = "srs_mantralaya"
    const val UTTARADI = "uttaradi"

    val all = listOf(
        PanchangaTradition(SRS, "ಶ್ರೀ ರಾಘವೇಂದ್ರ ಸ್ವಾಮಿ ಮಠ — ಮಂತ್ರಾಲಯ", "https://srsmatha.org/panchanga.php", true),
        PanchangaTradition(UTTARADI, "ಶ್ರೀ ಉತ್ತರಾದಿ ಮಠ", "https://www.uttaradimath.org/panchanga"),
        PanchangaTradition("udupi_palimaru", "ಉಡುಪಿ ಶ್ರೀ ಪಲಿಮಾರು ಮಠ", "https://shripalimarumatha.org"),
        PanchangaTradition("udupi_adamaru", "ಉಡುಪಿ ಶ್ರೀ ಅದಮಾರು ಮಠ", "https://adamarumatha.com"),
        PanchangaTradition("udupi_krishnapura", "ಉಡುಪಿ ಶ್ರೀ ಕೃಷ್ಣಾಪುರ ಮಠ", ""),
        PanchangaTradition("udupi_puttige", "ಉಡುಪಿ ಶ್ರೀ ಪುತ್ತಿಗೆ ಮಠ", "https://puthige.org"),
        PanchangaTradition("udupi_shirur", "ಉಡುಪಿ ಶ್ರೀ ಶಿರೂರು ಮಠ", ""),
        PanchangaTradition("udupi_sodhe", "ಉಡುಪಿ ಶ್ರೀ ಸೋದೆ ಮಠ", "https://sodematha.in"),
        PanchangaTradition("udupi_kaniyooru", "ಉಡುಪಿ ಶ್ರೀ ಕಾಣಿಯೂರು ಮಠ", ""),
        PanchangaTradition("udupi_pejavara", "ಉಡುಪಿ ಶ್ರೀ ಪೇಜಾವರ ಮಠ", "https://pejavara.org")
    )

    fun find(id: String): PanchangaTradition = all.firstOrNull { it.id == id } ?: all.first()
}
