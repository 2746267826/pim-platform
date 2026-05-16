package com.pim.core.network;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000~\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\b\n\u0002\u0010 \n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\b\bf\u0018\u00002\u00020\u0001J\u001e\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u0006H\u00a7@\u00a2\u0006\u0002\u0010\u0007J\u001e\u0010\b\u001a\b\u0012\u0004\u0012\u00020\t0\u00032\b\b\u0001\u0010\u0005\u001a\u00020\nH\u00a7@\u00a2\u0006\u0002\u0010\u000bJ\u001e\u0010\f\u001a\b\u0012\u0004\u0012\u00020\r0\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u000eH\u00a7@\u00a2\u0006\u0002\u0010\u000fJ\u001e\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00110\u00032\b\b\u0001\u0010\u0012\u001a\u00020\u0011H\u00a7@\u00a2\u0006\u0002\u0010\u0013J\u001e\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00110\u00032\b\b\u0001\u0010\u0012\u001a\u00020\u0011H\u00a7@\u00a2\u0006\u0002\u0010\u0013J(\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00110\u00032\b\b\u0001\u0010\u0016\u001a\u00020\u00112\b\b\u0001\u0010\u0017\u001a\u00020\u0011H\u00a7@\u00a2\u0006\u0002\u0010\u0018J\u001a\u0010\u0019\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00040\u001a0\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u001bJ.\u0010\u001c\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\u001a0\u00032\b\b\u0001\u0010\u0016\u001a\u00020\u00112\b\b\u0001\u0010\u0017\u001a\u00020\u0011H\u00a7@\u00a2\u0006\u0002\u0010\u0018J&\u0010\u001d\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\r0\u001a0\u00032\n\b\u0003\u0010\u001e\u001a\u0004\u0018\u00010\u001fH\u00a7@\u00a2\u0006\u0002\u0010 J\u001e\u0010!\u001a\b\u0012\u0004\u0012\u00020\"0\u00032\b\b\u0001\u0010#\u001a\u00020$H\u00a7@\u00a2\u0006\u0002\u0010%J\u001e\u0010&\u001a\b\u0012\u0004\u0012\u00020\'0\u00032\b\b\u0001\u0010\u0005\u001a\u00020(H\u00a7@\u00a2\u0006\u0002\u0010)J\u001e\u0010*\u001a\b\u0012\u0004\u0012\u00020\'0\u00032\b\b\u0001\u0010\u0005\u001a\u00020+H\u00a7@\u00a2\u0006\u0002\u0010,J\u001e\u0010-\u001a\b\u0012\u0004\u0012\u00020\'0\u00032\b\b\u0001\u0010\u0005\u001a\u00020.H\u00a7@\u00a2\u0006\u0002\u0010/J0\u00100\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u0002010\u001a0\u00032\b\b\u0001\u00102\u001a\u00020\u00112\n\b\u0003\u00103\u001a\u0004\u0018\u00010\u0011H\u00a7@\u00a2\u0006\u0002\u0010\u0018J\u0014\u00104\u001a\b\u0012\u0004\u0012\u00020\u00110\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u001bJ(\u00105\u001a\b\u0012\u0004\u0012\u00020\t0\u00032\b\b\u0001\u0010\u0012\u001a\u00020\u00112\b\b\u0001\u0010\u0005\u001a\u00020\nH\u00a7@\u00a2\u0006\u0002\u00106J(\u00107\u001a\b\u0012\u0004\u0012\u00020\r0\u00032\b\b\u0001\u0010\u0012\u001a\u00020\u00112\b\b\u0001\u0010\u0005\u001a\u00020\u000eH\u00a7@\u00a2\u0006\u0002\u00108\u00a8\u00069"}, d2 = {"Lcom/pim/core/network/ApiService;", "", "createCalendar", "Lcom/pim/core/models/ApiResponse;", "Lcom/pim/core/models/CalendarResponse;", "request", "Lcom/pim/core/models/CreateCalendarRequest;", "(Lcom/pim/core/models/CreateCalendarRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "createEvent", "Lcom/pim/core/models/EventResponse;", "Lcom/pim/core/models/CreateEventRequest;", "(Lcom/pim/core/models/CreateEventRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "createTask", "Lcom/pim/core/models/TaskResponse;", "Lcom/pim/core/models/CreateTaskRequest;", "(Lcom/pim/core/models/CreateTaskRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteEvent", "", "id", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteTask", "exportIcs", "start", "end", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getCalendars", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getEvents", "getTasks", "inbox", "", "(Ljava/lang/Boolean;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "importIcs", "", "body", "Lokhttp3/RequestBody;", "(Lokhttp3/RequestBody;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "login", "Lcom/pim/core/models/AuthResponse;", "Lcom/pim/core/models/LoginRequest;", "(Lcom/pim/core/models/LoginRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "refresh", "Lcom/pim/core/models/RefreshRequest;", "(Lcom/pim/core/models/RefreshRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "register", "Lcom/pim/core/models/RegisterRequest;", "(Lcom/pim/core/models/RegisterRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "search", "Lcom/pim/core/models/SearchResult;", "query", "type", "syncOutlook", "updateEvent", "(Ljava/lang/String;Lcom/pim/core/models/CreateEventRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateTask", "(Ljava/lang/String;Lcom/pim/core/models/CreateTaskRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "core_debug"})
public abstract interface ApiService {
    
    @retrofit2.http.POST(value = "auth/login")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object login(@retrofit2.http.Body
    @org.jetbrains.annotations.NotNull
    com.pim.core.models.LoginRequest request, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.pim.core.models.ApiResponse<com.pim.core.models.AuthResponse>> $completion);
    
    @retrofit2.http.POST(value = "auth/register")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object register(@retrofit2.http.Body
    @org.jetbrains.annotations.NotNull
    com.pim.core.models.RegisterRequest request, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.pim.core.models.ApiResponse<com.pim.core.models.AuthResponse>> $completion);
    
    @retrofit2.http.POST(value = "auth/refresh")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object refresh(@retrofit2.http.Body
    @org.jetbrains.annotations.NotNull
    com.pim.core.models.RefreshRequest request, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.pim.core.models.ApiResponse<com.pim.core.models.AuthResponse>> $completion);
    
    @retrofit2.http.GET(value = "calendar/calendars")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object getCalendars(@org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.pim.core.models.ApiResponse<java.util.List<com.pim.core.models.CalendarResponse>>> $completion);
    
    @retrofit2.http.POST(value = "calendar/calendars")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object createCalendar(@retrofit2.http.Body
    @org.jetbrains.annotations.NotNull
    com.pim.core.models.CreateCalendarRequest request, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.pim.core.models.ApiResponse<com.pim.core.models.CalendarResponse>> $completion);
    
    @retrofit2.http.GET(value = "calendar/events")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object getEvents(@retrofit2.http.Query(value = "start")
    @org.jetbrains.annotations.NotNull
    java.lang.String start, @retrofit2.http.Query(value = "end")
    @org.jetbrains.annotations.NotNull
    java.lang.String end, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.pim.core.models.ApiResponse<java.util.List<com.pim.core.models.EventResponse>>> $completion);
    
    @retrofit2.http.POST(value = "calendar/events")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object createEvent(@retrofit2.http.Body
    @org.jetbrains.annotations.NotNull
    com.pim.core.models.CreateEventRequest request, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.pim.core.models.ApiResponse<com.pim.core.models.EventResponse>> $completion);
    
    @retrofit2.http.PUT(value = "calendar/events/{id}")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object updateEvent(@retrofit2.http.Path(value = "id")
    @org.jetbrains.annotations.NotNull
    java.lang.String id, @retrofit2.http.Body
    @org.jetbrains.annotations.NotNull
    com.pim.core.models.CreateEventRequest request, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.pim.core.models.ApiResponse<com.pim.core.models.EventResponse>> $completion);
    
    @retrofit2.http.DELETE(value = "calendar/events/{id}")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object deleteEvent(@retrofit2.http.Path(value = "id")
    @org.jetbrains.annotations.NotNull
    java.lang.String id, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.pim.core.models.ApiResponse<java.lang.String>> $completion);
    
    @retrofit2.http.GET(value = "calendar/tasks")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object getTasks(@retrofit2.http.Query(value = "inbox")
    @org.jetbrains.annotations.Nullable
    java.lang.Boolean inbox, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.pim.core.models.ApiResponse<java.util.List<com.pim.core.models.TaskResponse>>> $completion);
    
    @retrofit2.http.POST(value = "calendar/tasks")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object createTask(@retrofit2.http.Body
    @org.jetbrains.annotations.NotNull
    com.pim.core.models.CreateTaskRequest request, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.pim.core.models.ApiResponse<com.pim.core.models.TaskResponse>> $completion);
    
    @retrofit2.http.PUT(value = "calendar/tasks/{id}")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object updateTask(@retrofit2.http.Path(value = "id")
    @org.jetbrains.annotations.NotNull
    java.lang.String id, @retrofit2.http.Body
    @org.jetbrains.annotations.NotNull
    com.pim.core.models.CreateTaskRequest request, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.pim.core.models.ApiResponse<com.pim.core.models.TaskResponse>> $completion);
    
    @retrofit2.http.DELETE(value = "calendar/tasks/{id}")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object deleteTask(@retrofit2.http.Path(value = "id")
    @org.jetbrains.annotations.NotNull
    java.lang.String id, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.pim.core.models.ApiResponse<java.lang.String>> $completion);
    
    @retrofit2.http.GET(value = "search")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object search(@retrofit2.http.Query(value = "q")
    @org.jetbrains.annotations.NotNull
    java.lang.String query, @retrofit2.http.Query(value = "type")
    @org.jetbrains.annotations.Nullable
    java.lang.String type, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.pim.core.models.ApiResponse<java.util.List<com.pim.core.models.SearchResult>>> $completion);
    
    @retrofit2.http.POST(value = "calendar/import-ics")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object importIcs(@retrofit2.http.Body
    @org.jetbrains.annotations.NotNull
    okhttp3.RequestBody body, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.pim.core.models.ApiResponse<java.lang.Integer>> $completion);
    
    @retrofit2.http.GET(value = "calendar/export-ics")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object exportIcs(@retrofit2.http.Query(value = "start")
    @org.jetbrains.annotations.NotNull
    java.lang.String start, @retrofit2.http.Query(value = "end")
    @org.jetbrains.annotations.NotNull
    java.lang.String end, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.pim.core.models.ApiResponse<java.lang.String>> $completion);
    
    @retrofit2.http.POST(value = "calendar/outlook/sync")
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object syncOutlook(@org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.pim.core.models.ApiResponse<java.lang.String>> $completion);
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}