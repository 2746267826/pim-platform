package com.pim.core.di;

import com.pim.core.auth.TokenManager;
import com.pim.core.network.ApiService;
import dagger.Lazy;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
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
public final class CoreModule_ProvideOkHttpClientFactory implements Factory<OkHttpClient> {
  private final Provider<TokenManager> tokenManagerProvider;

  private final Provider<ApiService> apiServiceProvider;

  public CoreModule_ProvideOkHttpClientFactory(Provider<TokenManager> tokenManagerProvider,
      Provider<ApiService> apiServiceProvider) {
    this.tokenManagerProvider = tokenManagerProvider;
    this.apiServiceProvider = apiServiceProvider;
  }

  @Override
  public OkHttpClient get() {
    return provideOkHttpClient(tokenManagerProvider.get(), DoubleCheck.lazy(apiServiceProvider));
  }

  public static CoreModule_ProvideOkHttpClientFactory create(
      Provider<TokenManager> tokenManagerProvider, Provider<ApiService> apiServiceProvider) {
    return new CoreModule_ProvideOkHttpClientFactory(tokenManagerProvider, apiServiceProvider);
  }

  public static OkHttpClient provideOkHttpClient(TokenManager tokenManager,
      Lazy<ApiService> apiServiceProvider) {
    return Preconditions.checkNotNullFromProvides(CoreModule.INSTANCE.provideOkHttpClient(tokenManager, apiServiceProvider));
  }
}
