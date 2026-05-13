package dev.a10101100.snipcraft.feature.onboarding

data class OnboardingUiState(
    val currentCard: Int = 0,
    val accessibilityGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val batteryExemptGranted: Boolean = false,
    val sandboxText: String = "",
    val sandboxExpanded: Boolean = false,
)

sealed interface OnboardingEvent {
    data object NavigateToLibrary : OnboardingEvent
}

const val TOTAL_CARDS = 4  // accessibility, notification, battery, sandbox
