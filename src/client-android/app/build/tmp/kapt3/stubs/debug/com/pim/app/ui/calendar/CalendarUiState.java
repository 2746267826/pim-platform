package com.pim.app.ui.calendar;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0002\b*\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u0091\u0001\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\u000e\b\u0002\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u0012\b\b\u0002\u0010\u0007\u001a\u00020\b\u0012\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\n\u0012\b\b\u0002\u0010\u000b\u001a\u00020\b\u0012\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u0006\u0012\b\b\u0002\u0010\r\u001a\u00020\n\u0012\b\b\u0002\u0010\u000e\u001a\u00020\n\u0012\b\b\u0002\u0010\u000f\u001a\u00020\n\u0012\b\b\u0002\u0010\u0010\u001a\u00020\n\u0012\b\b\u0002\u0010\u0011\u001a\u00020\n\u0012\b\b\u0002\u0010\u0012\u001a\u00020\b\u0012\b\b\u0002\u0010\u0013\u001a\u00020\n\u00a2\u0006\u0002\u0010\u0014J\t\u0010$\u001a\u00020\u0003H\u00c6\u0003J\t\u0010%\u001a\u00020\nH\u00c6\u0003J\t\u0010&\u001a\u00020\nH\u00c6\u0003J\t\u0010\'\u001a\u00020\bH\u00c6\u0003J\t\u0010(\u001a\u00020\nH\u00c6\u0003J\u000f\u0010)\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005H\u00c6\u0003J\t\u0010*\u001a\u00020\bH\u00c6\u0003J\u000b\u0010+\u001a\u0004\u0018\u00010\nH\u00c6\u0003J\t\u0010,\u001a\u00020\bH\u00c6\u0003J\u000b\u0010-\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003J\t\u0010.\u001a\u00020\nH\u00c6\u0003J\t\u0010/\u001a\u00020\nH\u00c6\u0003J\t\u00100\u001a\u00020\nH\u00c6\u0003J\u0095\u0001\u00101\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\u000e\b\u0002\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\b\b\u0002\u0010\u0007\u001a\u00020\b2\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\n2\b\b\u0002\u0010\u000b\u001a\u00020\b2\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u00062\b\b\u0002\u0010\r\u001a\u00020\n2\b\b\u0002\u0010\u000e\u001a\u00020\n2\b\b\u0002\u0010\u000f\u001a\u00020\n2\b\b\u0002\u0010\u0010\u001a\u00020\n2\b\b\u0002\u0010\u0011\u001a\u00020\n2\b\b\u0002\u0010\u0012\u001a\u00020\b2\b\b\u0002\u0010\u0013\u001a\u00020\nH\u00c6\u0001J\u0013\u00102\u001a\u00020\b2\b\u00103\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u00104\u001a\u000205H\u00d6\u0001J\t\u00106\u001a\u00020\nH\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u0011\u0010\u000e\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018R\u0011\u0010\u0011\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0018R\u0011\u0010\u000f\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0018R\u0011\u0010\u0010\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u0018R\u0011\u0010\r\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u0018R\u0013\u0010\t\u001a\u0004\u0018\u00010\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u0018R\u0017\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001fR\u0011\u0010\u0013\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010\u0018R\u0011\u0010\u000b\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010!R\u0011\u0010\u0012\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010!R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010!R\u0013\u0010\f\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010#\u00a8\u00067"}, d2 = {"Lcom/pim/app/ui/calendar/CalendarUiState;", "", "currentMonth", "Ljava/time/YearMonth;", "events", "", "Lcom/pim/core/models/EventResponse;", "isLoading", "", "error", "", "isEditorOpen", "selectedEvent", "editorTitle", "editorDescription", "editorLocation", "editorStart", "editorEnd", "isImporting", "icsInput", "(Ljava/time/YearMonth;Ljava/util/List;ZLjava/lang/String;ZLcom/pim/core/models/EventResponse;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZLjava/lang/String;)V", "getCurrentMonth", "()Ljava/time/YearMonth;", "getEditorDescription", "()Ljava/lang/String;", "getEditorEnd", "getEditorLocation", "getEditorStart", "getEditorTitle", "getError", "getEvents", "()Ljava/util/List;", "getIcsInput", "()Z", "getSelectedEvent", "()Lcom/pim/core/models/EventResponse;", "component1", "component10", "component11", "component12", "component13", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "other", "hashCode", "", "toString", "app_debug"})
public final class CalendarUiState {
    @org.jetbrains.annotations.NotNull
    private final java.time.YearMonth currentMonth = null;
    @org.jetbrains.annotations.NotNull
    private final java.util.List<com.pim.core.models.EventResponse> events = null;
    private final boolean isLoading = false;
    @org.jetbrains.annotations.Nullable
    private final java.lang.String error = null;
    private final boolean isEditorOpen = false;
    @org.jetbrains.annotations.Nullable
    private final com.pim.core.models.EventResponse selectedEvent = null;
    @org.jetbrains.annotations.NotNull
    private final java.lang.String editorTitle = null;
    @org.jetbrains.annotations.NotNull
    private final java.lang.String editorDescription = null;
    @org.jetbrains.annotations.NotNull
    private final java.lang.String editorLocation = null;
    @org.jetbrains.annotations.NotNull
    private final java.lang.String editorStart = null;
    @org.jetbrains.annotations.NotNull
    private final java.lang.String editorEnd = null;
    private final boolean isImporting = false;
    @org.jetbrains.annotations.NotNull
    private final java.lang.String icsInput = null;
    
    public CalendarUiState(@org.jetbrains.annotations.NotNull
    java.time.YearMonth currentMonth, @org.jetbrains.annotations.NotNull
    java.util.List<com.pim.core.models.EventResponse> events, boolean isLoading, @org.jetbrains.annotations.Nullable
    java.lang.String error, boolean isEditorOpen, @org.jetbrains.annotations.Nullable
    com.pim.core.models.EventResponse selectedEvent, @org.jetbrains.annotations.NotNull
    java.lang.String editorTitle, @org.jetbrains.annotations.NotNull
    java.lang.String editorDescription, @org.jetbrains.annotations.NotNull
    java.lang.String editorLocation, @org.jetbrains.annotations.NotNull
    java.lang.String editorStart, @org.jetbrains.annotations.NotNull
    java.lang.String editorEnd, boolean isImporting, @org.jetbrains.annotations.NotNull
    java.lang.String icsInput) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.time.YearMonth getCurrentMonth() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.util.List<com.pim.core.models.EventResponse> getEvents() {
        return null;
    }
    
    public final boolean isLoading() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.String getError() {
        return null;
    }
    
    public final boolean isEditorOpen() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable
    public final com.pim.core.models.EventResponse getSelectedEvent() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getEditorTitle() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getEditorDescription() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getEditorLocation() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getEditorStart() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getEditorEnd() {
        return null;
    }
    
    public final boolean isImporting() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getIcsInput() {
        return null;
    }
    
    public CalendarUiState() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.time.YearMonth component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String component10() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String component11() {
        return null;
    }
    
    public final boolean component12() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String component13() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.util.List<com.pim.core.models.EventResponse> component2() {
        return null;
    }
    
    public final boolean component3() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.String component4() {
        return null;
    }
    
    public final boolean component5() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable
    public final com.pim.core.models.EventResponse component6() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String component7() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String component8() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String component9() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final com.pim.app.ui.calendar.CalendarUiState copy(@org.jetbrains.annotations.NotNull
    java.time.YearMonth currentMonth, @org.jetbrains.annotations.NotNull
    java.util.List<com.pim.core.models.EventResponse> events, boolean isLoading, @org.jetbrains.annotations.Nullable
    java.lang.String error, boolean isEditorOpen, @org.jetbrains.annotations.Nullable
    com.pim.core.models.EventResponse selectedEvent, @org.jetbrains.annotations.NotNull
    java.lang.String editorTitle, @org.jetbrains.annotations.NotNull
    java.lang.String editorDescription, @org.jetbrains.annotations.NotNull
    java.lang.String editorLocation, @org.jetbrains.annotations.NotNull
    java.lang.String editorStart, @org.jetbrains.annotations.NotNull
    java.lang.String editorEnd, boolean isImporting, @org.jetbrains.annotations.NotNull
    java.lang.String icsInput) {
        return null;
    }
    
    @java.lang.Override
    public boolean equals(@org.jetbrains.annotations.Nullable
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override
    @org.jetbrains.annotations.NotNull
    public java.lang.String toString() {
        return null;
    }
}