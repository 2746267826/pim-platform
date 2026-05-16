package com.pim.app.ui.calendar;

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
public final class CalendarViewModel_Factory implements Factory<CalendarViewModel> {
  private final Provider<ApiService> apiProvider;

  public CalendarViewModel_Factory(Provider<ApiService> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public CalendarViewModel get() {
    return newInstance(apiProvider.get());
  }

  public static CalendarViewModel_Factory create(Provider<ApiService> apiProvider) {
    return new CalendarViewModel_Factory(apiProvider);
  }

  public static CalendarViewModel newInstance(ApiService api) {
    return new CalendarViewModel(api);
  }
}
