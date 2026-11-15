package com.chani.porumi.feature.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chani.porumi.data.SocialLogin
import com.chani.porumi.feature.legal.LegalDoc
import com.chani.porumi.feature.legal.LegalScreen
import com.chani.porumi.ui.components.ActivityIndicator
import com.chani.porumi.ui.components.BrandIcons
import com.chani.porumi.ui.components.CheckCircle
import com.chani.porumi.ui.components.PlainButton
import com.chani.porumi.ui.components.PorumiTextField
import com.chani.porumi.ui.components.PrimaryButton
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.BoxWithConstraints
import com.chani.porumi.ui.components.SegmentedControl
import com.chani.porumi.ui.components.AdaptiveWidth
import com.chani.porumi.ui.components.centeredMaxWidth

/**
 * 로그인 화면: SNS 4종(카카오·네이버·Apple·Google)이 기본, 이메일은 아래 "이메일로 계속하기"로 숨겨 둔다.
 * SNS 는 가입·로그인이 같은 버튼이라, 버튼 아래 약관 동의 안내를 둔다.
 */
@Composable
fun AuthScreen(viewModel: AuthViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // 로그인 전이라 NavHost 밖 → 약관은 화면 전환 대신 상태로 띄운다
    var openDoc by rememberSaveable { mutableStateOf<LegalDoc?>(null) }
    openDoc?.let { doc ->
        LegalScreen(doc, onBack = { openDoc = null })
        return
    }

    // 로고는 위쪽 공간 가운데, 로그인 버튼은 아래쪽에 붙인다.
    // 화면이 작거나 키보드가 올라오면 스크롤 (heightIn(min) 덕분에 weight 가 화면 높이 기준으로 나뉜다)
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val screenHeight = maxHeight
        Column(
            modifier = Modifier
                .centeredMaxWidth(AdaptiveWidth.Form)
                .verticalScroll(rememberScrollState())
                .heightIn(min = screenHeight)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.showEmail) {
                // 이메일: 위에서부터 고정 배치 → 로그인/회원가입 탭을 바꿔도 로고·탭 위치가 그대로이고 입력칸만 아래로 늘어난다
                Spacer(Modifier.height(72.dp))
                Brand()
                Spacer(Modifier.height(32.dp))
                AuthBody(state, viewModel, onOpenDoc = { openDoc = it })
                Spacer(Modifier.weight(1f))
            } else {
                // SNS: 로고는 위쪽 공간 가운데, 버튼은 아래에 붙인다
                Spacer(Modifier.weight(1f).heightIn(min = 24.dp))
                Brand()
                Spacer(Modifier.weight(1f).heightIn(min = 32.dp))
                AuthBody(state, viewModel, onOpenDoc = { openDoc = it })
            }
        }
    }
}

@Composable
private fun AuthBody(state: AuthUiState, viewModel: AuthViewModel, onOpenDoc: (LegalDoc) -> Unit) {
        AnimatedContent(
            targetState = state.showEmail,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "auth-mode",
        ) { showEmail ->
            if (showEmail) {
                EmailForm(state, viewModel, onOpenDoc = onOpenDoc)
            } else {
                SocialButtons(state, viewModel, onOpenDoc = onOpenDoc)
            }
        }
}

@Composable
private fun Brand() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(88.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Pets, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(48.dp))
        }
        Text(
            buildAnnotatedString {
                append("porumi")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append(".") }
            },
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            "우리 아이 표정으로 읽는 하루 기분",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── SNS 로그인 ──────────────────────────────

@Composable
private fun SocialButtons(state: AuthUiState, viewModel: AuthViewModel, onOpenDoc: (LegalDoc) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SocialLogin.entries.forEach { social ->
            SocialButton(
                social = social,
                loading = state.socialLoading == social,
                enabled = state.socialLoading == null,
                onClick = { viewModel.signInWithSocial(social) },
            )
        }

        state.error?.let { ErrorText(it) }

        Text(
            termsNotice(onOpenDoc),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, start = 8.dp, end = 8.dp),
        )

        PlainButton(
            onClick = { viewModel.showEmail(true) },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textStyle = MaterialTheme.typography.labelMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
        ) {
            Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("이메일로 계속하기", modifier = Modifier.padding(start = 4.dp), textDecoration = TextDecoration.Underline)
        }
    }
}

/** 각 사 로그인 버튼 가이드의 배경·글자 색 */
private data class BrandStyle(val container: Color, val content: Color, val icon: ImageVector, val iconTint: Color?, val border: Color? = null)

private val SocialLogin.style: BrandStyle
    get() = when (this) {
        SocialLogin.KAKAO -> BrandStyle(Color(0xFFFEE500), Color(0xD9000000), BrandIcons.Kakao, Color.Black)
        SocialLogin.NAVER -> BrandStyle(Color(0xFF03C75A), Color.White, BrandIcons.Naver, Color.White)
        SocialLogin.APPLE -> BrandStyle(Color.Black, Color.White, BrandIcons.Apple, Color.White)
        SocialLogin.GOOGLE -> BrandStyle(Color.White, Color(0xFF1F1F1F), BrandIcons.Google, null, border = Color(0xFFDADCE0))
    }

@Composable
private fun SocialButton(social: SocialLogin, loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val style = social.style
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .alpha(if (pressed) 0.8f else if (enabled || loading) 1f else 0.6f)
            .clip(CircleShape)
            .background(style.container)
            .then(if (style.border != null) Modifier.border(1.dp, style.border, CircleShape) else Modifier)
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        // 로고는 왼쪽 고정, 글자는 가운데 (각 사 가이드의 기본 배치)
        Icon(
            style.icon,
            contentDescription = null,
            tint = style.iconTint ?: Color.Unspecified,
            modifier = Modifier.align(Alignment.CenterStart).size(if (social == SocialLogin.NAVER) 16.dp else 20.dp),
        )
        if (loading) {
            ActivityIndicator(color = style.content, size = 20.dp)
        } else {
            Text(social.label, style = MaterialTheme.typography.labelLarge, color = style.content)
        }
    }
}

/** SNS 는 가입·로그인 구분이 없어 동의를 안내로 받는다 (만 14세 이상, 국외 이전 포함) */
@Composable
private fun termsNotice(onOpenDoc: (LegalDoc) -> Unit) = buildAnnotatedString {
    val link = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline))
    append("시작하면 만 14세 이상이며 ")
    withLink(LinkAnnotation.Clickable("terms", link) { onOpenDoc(LegalDoc.TERMS) }) { append(LegalDoc.TERMS.title) }
    append("과 ")
    withLink(LinkAnnotation.Clickable("privacy", link) { onOpenDoc(LegalDoc.PRIVACY) }) { append(LegalDoc.PRIVACY.title) }
    append("(국외 이전 포함)에 동의하는 것으로 봐요.")
}

// ── 이메일 로그인 ──────────────────────────────

@Composable
private fun EmailForm(state: AuthUiState, viewModel: AuthViewModel, onOpenDoc: (LegalDoc) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // 지금 모드가 한눈에 보이고 한 번에 바꿀 수 있게 맨 위에 탭으로
        SegmentedControl(
            options = listOf("로그인", "회원가입"),
            selectedIndex = if (state.isSignUp) 1 else 0,
            onSelect = { viewModel.setSignUp(it == 1) },
            modifier = Modifier.padding(bottom = 4.dp),
        )
        PorumiTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            placeholder = "이메일",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        PorumiTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            placeholder = if (state.isSignUp) "비밀번호 (6자 이상)" else "비밀번호",
            secure = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.isSignUp) {
            PorumiTextField(
                value = state.passwordConfirm,
                onValueChange = viewModel::onPasswordConfirmChange,
                placeholder = "비밀번호 확인",
                errorText = if (state.passwordMismatch) "비밀번호가 일치하지 않아요" else null,
                secure = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            TermsAgreement(
                checked = state.agreedToTerms,
                onCheckedChange = viewModel::onAgreeChange,
                onOpen = onOpenDoc,
            )
        }
        state.error?.let { ErrorText(it) }
        state.info?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
        PrimaryButton(
            onClick = viewModel::submit,
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.loading) ActivityIndicator(color = LocalContentColor.current, size = 20.dp)
            else Text(if (state.isSignUp) "가입하기" else "로그인")
        }
        PlainButton(
            onClick = { viewModel.showEmail(false) },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textStyle = MaterialTheme.typography.labelMedium,
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("다른 방법으로 시작하기", modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TermsAgreement(checked: Boolean, onCheckedChange: (Boolean) -> Unit, onOpen: (LegalDoc) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CheckCircle(checked = checked, onCheckedChange = onCheckedChange)
            Text(
                "[필수] 만 14세 이상이며, 이용약관과 개인정보 처리방침(국외 이전 포함)에 동의해요",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        FlowRow(Modifier.padding(start = 40.dp)) {
            LegalDoc.entries.forEach { doc ->
                PlainButton(onClick = { onOpen(doc) }, textStyle = MaterialTheme.typography.labelMedium) {
                    Text("${doc.title} 보기")
                }
            }
        }
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(
        message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
