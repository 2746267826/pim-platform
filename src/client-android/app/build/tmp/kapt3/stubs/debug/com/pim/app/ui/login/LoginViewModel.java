package com.pim.app.ui.login;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0002\b\u0004\b\u0007\u0018\u00002\u00020\u0001B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0006\u0010\u000e\u001a\u00020\u000fJ\u0006\u0010\u0010\u001a\u00020\u000fJ\u0006\u0010\u0011\u001a\u00020\u000fJ\u000e\u0010\u0012\u001a\u00020\u000f2\u0006\u0010\u0013\u001a\u00020\u0014J\u000e\u0010\u0015\u001a\u00020\u000f2\u0006\u0010\u0013\u001a\u00020\u0014J\u000e\u0010\u0016\u001a\u00020\u000f2\u0006\u0010\u0013\u001a\u00020\u0014J\u000e\u0010\u0017\u001a\u00020\u000f2\u0006\u0010\u0013\u001a\u00020\u0014R\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\t0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0018"}, d2 = {"Lcom/pim/app/ui/login/LoginViewModel;", "Landroidx/lifecycle/ViewModel;", "api", "Lcom/pim/core/network/ApiService;", "tokenManager", "Lcom/pim/core/auth/TokenManager;", "(Lcom/pim/core/network/ApiService;Lcom/pim/core/auth/TokenManager;)V", "_state", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/pim/app/ui/login/LoginUiState;", "state", "Lkotlinx/coroutines/flow/StateFlow;", "getState", "()Lkotlinx/coroutines/flow/StateFlow;", "login", "", "register", "toggleMode", "updateDisplayName", "value", "", "updateEmail", "updatePassword", "updateUsername", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel
public final class LoginViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull
    private final com.pim.core.network.ApiService api = null;
    @org.jetbrains.annotations.NotNull
    private final com.pim.core.auth.TokenManager tokenManager = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pim.app.ui.login.LoginUiState> _state = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.StateFlow<com.pim.app.ui.login.LoginUiState> state = null;
    
    @javax.inject.Inject
    public LoginViewModel(@org.jetbrains.annotations.NotNull
    com.pim.core.network.ApiService api, @org.jetbrains.annotations.NotNull
    com.pim.core.auth.TokenManager tokenManager) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<com.pim.app.ui.login.LoginUiState> getState() {
        return null;
    }
    
    public final void updateUsername(@org.jetbrains.annotations.NotNull
    java.lang.String value) {
    }
    
    public final void updatePassword(@org.jetbrains.annotations.NotNull
    java.lang.String value) {
    }
    
    public final void updateEmail(@org.jetbrains.annotations.NotNull
    java.lang.String value) {
    }
    
    public final void updateDisplayName(@org.jetbrains.annotations.NotNull
    java.lang.String value) {
    }
    
    public final void toggleMode() {
    }
    
    public final void login() {
    }
    
    public final void register() {
    }
}