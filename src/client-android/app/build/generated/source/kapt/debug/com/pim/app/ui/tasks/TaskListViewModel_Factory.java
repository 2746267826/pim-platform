package com.pim.app.ui.tasks;

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
public final class TaskListViewModel_Factory implements Factory<TaskListViewModel> {
  private final Provider<ApiService> apiProvider;

  public TaskListViewModel_Factory(Provider<ApiService> apiProvider) {
    this.apiProvider = apiProvider;
  }

  @Override
  public TaskListViewModel get() {
    return newInstance(apiProvider.get());
  }

  public static TaskListViewModel_Factory create(Provider<ApiService> apiProvider) {
    return new TaskListViewModel_Factory(apiProvider);
  }

  public static TaskListViewModel newInstance(ApiService api) {
    return new TaskListViewModel(api);
  }
}
