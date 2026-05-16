package com.pim.app.ui.login;

import com.pim.core.auth.TokenManager;
import com.pim.core.network.ApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class LoginViewModel_Factory implements Factory<LoginViewModel> {
  private final Provider<ApiService> apiProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  public LoginViewModel_Factory(Provider<ApiService> apiProvider,
      Provider<TokenManager> tokenManagerProvider) {
    this.apiProvider = apiProvider;
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public LoginViewModel get() {
    return newInstance(apiProvider.get(), tokenManagerProvider.get());
  }

  public static LoginViewModel_Factory create(Provider<ApiService> apiProvider,
      Provider<TokenManager> tokenManagerProvider) {
    return new LoginViewModel_Factory(apiProvider, tokenManagerProvider);
  }

  public static LoginViewModel newInstance(ApiService api, TokenManager tokenManager) {
    return new LoginViewModel(api, tokenManager);
  }
}
