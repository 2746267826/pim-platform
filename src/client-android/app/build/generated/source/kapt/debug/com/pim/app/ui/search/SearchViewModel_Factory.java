package com.pim.app.ui.search;

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
public final class SearchViewModel_Factory implements Factory<SearchViewModel> {
  private final Provider<ApiService> apiProvider;

  public SearchViewModel_Factory(Provider<ApiService> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public SearchViewModel get() {
    return newInstance(apiProvider.get());
  }

  public static SearchViewModel_Factory create(Provider<ApiService> apiProvider) {
    return new SearchViewModel_Factory(apiProvider);
  }

  public static SearchViewModel newInstance(ApiService api) {
    return new SearchViewModel(api);
  }
}
