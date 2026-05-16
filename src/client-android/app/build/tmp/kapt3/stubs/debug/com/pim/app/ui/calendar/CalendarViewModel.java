package com.pim.app.ui.calendar;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u000b\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\u000f\u001a\u00020\u0010J\u000e\u0010\u0011\u001a\u00020\u00102\u0006\u0010\u0012\u001a\u00020\u0013J\u0014\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00160\u00152\u0006\u0010\u0017\u001a\u00020\u0018J\u0006\u0010\u0019\u001a\u00020\u0010J\u0006\u0010\u001a\u001a\u00020\u0010J\u0012\u0010\u001b\u001a\u00020\u00102\n\b\u0002\u0010\u001c\u001a\u0004\u0018\u00010\u001dJ\u000e\u0010\u001e\u001a\u00020\u00102\u0006\u0010\u001f\u001a\u00020\u0016J\u0006\u0010 \u001a\u00020\u0010J\u0006\u0010!\u001a\u00020\u0010J\u000e\u0010\"\u001a\u00020\u00102\u0006\u0010#\u001a\u00020\u0013J\u000e\u0010$\u001a\u00020\u00102\u0006\u0010#\u001a\u00020\u0013J\u000e\u0010%\u001a\u00020\u00102\u0006\u0010#\u001a\u00020\u0013J\u000e\u0010&\u001a\u00020\u00102\u0006\u0010#\u001a\u00020\u0013J\u000e\u0010\'\u001a\u00020\u00102\u0006\u0010#\u001a\u00020\u0013R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\b\u001a\n \n*\u0004\u0018\u00010\t0\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00070\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000e\u00a8\u0006("}, d2 = {"Lcom/pim/app/ui/calendar/CalendarViewModel;", "Landroidx/lifecycle/ViewModel;", "api", "Lcom/pim/core/network/ApiService;", "(Lcom/pim/core/network/ApiService;)V", "_state", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/pim/app/ui/calendar/CalendarUiState;", "fmt", "Ljava/time/format/DateTimeFormatter;", "kotlin.jvm.PlatformType", "state", "Lkotlinx/coroutines/flow/StateFlow;", "getState", "()Lkotlinx/coroutines/flow/StateFlow;", "closeEditor", "", "deleteEvent", "id", "", "getEventsForDay", "", "Lcom/pim/core/models/EventResponse;", "day", "", "loadEvents", "nextMonth", "openCreateEditor", "date", "Ljava/time/LocalDate;", "openEditEditor", "event", "previousMonth", "saveEvent", "updateEditorDescription", "v", "updateEditorEnd", "updateEditorLocation", "updateEditorStart", "updateEditorTitle", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel
public final class CalendarViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull
    private final com.pim.core.network.ApiService api = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pim.app.ui.calendar.CalendarUiState> _state = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.StateFlow<com.pim.app.ui.calendar.CalendarUiState> state = null;
    private final java.time.format.DateTimeFormatter fmt = null;
    
    @javax.inject.Inject
    public CalendarViewModel(@org.jetbrains.annotations.NotNull
    com.pim.core.network.ApiService api) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<com.pim.app.ui.calendar.CalendarUiState> getState() {
        return null;
    }
    
    public final void loadEvents() {
    }
    
    public final void previousMonth() {
    }
    
    public final void nextMonth() {
    }
    
    public final void openCreateEditor(@org.jetbrains.annotations.Nullable
    java.time.LocalDate date) {
    }
    
    public final void openEditEditor(@org.jetbrains.annotations.NotNull
    com.pim.core.models.EventResponse event) {
    }
    
    public final void closeEditor() {
    }
    
    public final void updateEditorTitle(@org.jetbrains.annotations.NotNull
    java.lang.String v) {
    }
    
    public final void updateEditorDescription(@org.jetbrains.annotations.NotNull
    java.lang.String v) {
    }
    
    public final void updateEditorLocation(@org.jetbrains.annotations.NotNull
    java.lang.String v) {
    }
    
    public final void updateEditorStart(@org.jetbrains.annotations.NotNull
    java.lang.String v) {
    }
    
    public final void updateEditorEnd(@org.jetbrains.annotations.NotNull
    java.lang.String v) {
    }
    
    public final void saveEvent() {
    }
    
    public final void deleteEvent(@org.jetbrains.annotations.NotNull
    java.lang.String id) {
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.util.List<com.pim.core.models.EventResponse> getEventsForDay(int day) {
        return null;
    }
}