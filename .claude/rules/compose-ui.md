---
paths:
  - "shared/src/commonMain/kotlin/com/chani/porumi/feature/**"
  - "shared/src/commonMain/kotlin/com/chani/porumi/ui/**"
  - "shared/src/commonMain/kotlin/com/chani/porumi/App.kt"
---

# Compose UI rules

- All user-facing text is Korean, polite 해요체 (e.g. "다시 시도해 주세요", "저장하지 못했어요"). Pets are "아이". Error messages say what happened and what to do next. `verify.sh` warns on new non-Korean `Text(...)` literals.
- Reuse components before writing new ones: `PorumiTopBar` (sub-screen header with back), `PrimaryButton`, `ActivityIndicator` (`ui/components/PorumiControls.kt`), `ClickableCard`, `PetPhoto`, `EmotionBadge` (`ui/components/Common.kt`), `SettingsPage`/`SettingsSection`/`SettingsRow` for settings-style screens.
- Colors come from `MaterialTheme.colorScheme` or `LocalPorumiColors.current` — no raw `Color(0x...)` in feature screens.
- Wide screens (foldable/tablet): constrain content with `Modifier.centeredMaxWidth(...)` (640dp content, 440dp auth/first-pet forms) from `ui/components/Adaptive.kt`; make tall content scroll on short screens.
- Screens take navigation callbacks (`onBack`, `onXxxClick`) and `viewModel: XViewModel = koinViewModel()` as a default parameter; collect flows with `collectAsStateWithLifecycle()`.
- ViewModels observe repository `StateFlow`s instead of fetching on their own; expose a `XUiState` data class via `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), XUiState())` or a private `MutableStateFlow`. Wrap repository calls in `runCatching` and surface a Korean error message.
- Navigation: `@Serializable` route objects/data classes in `App.kt`, sub-screens pop with `navController.popBackStack()`, tabs use `navigateTab`. New screens → `/new-feature`.
