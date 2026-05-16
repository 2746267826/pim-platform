package com.pim.app.ui.tasks;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\b\n\u0002\b\u001a\b\u0086\b\u0018\u00002\u00020\u0001B]\u0012\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0006\u0012\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\b\u0012\b\b\u0002\u0010\t\u001a\u00020\u0006\u0012\b\b\u0002\u0010\n\u001a\u00020\b\u0012\b\b\u0002\u0010\u000b\u001a\u00020\b\u0012\b\b\u0002\u0010\f\u001a\u00020\r\u0012\b\b\u0002\u0010\u000e\u001a\u00020\u0006\u00a2\u0006\u0002\u0010\u000fJ\u000f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00c6\u0003J\t\u0010\u001b\u001a\u00020\u0006H\u00c6\u0003J\u000b\u0010\u001c\u001a\u0004\u0018\u00010\bH\u00c6\u0003J\t\u0010\u001d\u001a\u00020\u0006H\u00c6\u0003J\t\u0010\u001e\u001a\u00020\bH\u00c6\u0003J\t\u0010\u001f\u001a\u00020\bH\u00c6\u0003J\t\u0010 \u001a\u00020\rH\u00c6\u0003J\t\u0010!\u001a\u00020\u0006H\u00c6\u0003Ja\u0010\"\u001a\u00020\u00002\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\b2\b\b\u0002\u0010\t\u001a\u00020\u00062\b\b\u0002\u0010\n\u001a\u00020\b2\b\b\u0002\u0010\u000b\u001a\u00020\b2\b\b\u0002\u0010\f\u001a\u00020\r2\b\b\u0002\u0010\u000e\u001a\u00020\u0006H\u00c6\u0001J\u0013\u0010#\u001a\u00020\u00062\b\u0010$\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010%\u001a\u00020\rH\u00d6\u0001J\t\u0010&\u001a\u00020\bH\u00d6\u0001R\u0011\u0010\u000b\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0011\u0010\f\u001a\u00020\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0011\u0010\n\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0011R\u0013\u0010\u0007\u001a\u0004\u0018\u00010\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0011R\u0011\u0010\t\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\u0016R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0016R\u0011\u0010\u000e\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0016R\u0017\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019\u00a8\u0006\'"}, d2 = {"Lcom/pim/app/ui/tasks/TaskListUiState;", "", "tasks", "", "Lcom/pim/core/models/TaskResponse;", "isLoading", "", "error", "", "isEditorOpen", "editorTitle", "editorDescription", "editorPriority", "", "showInboxOnly", "(Ljava/util/List;ZLjava/lang/String;ZLjava/lang/String;Ljava/lang/String;IZ)V", "getEditorDescription", "()Ljava/lang/String;", "getEditorPriority", "()I", "getEditorTitle", "getError", "()Z", "getShowInboxOnly", "getTasks", "()Ljava/util/List;", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "copy", "equals", "other", "hashCode", "toString", "app_debug"})
public final class TaskListUiState {
    @org.jetbrains.annotations.NotNull
    private final java.util.List<com.pim.core.models.TaskResponse> tasks = null;
    private final boolean isLoading = false;
    @org.jetbrains.annotations.Nullable
    private final java.lang.String error = null;
    private final boolean isEditorOpen = false;
    @org.jetbrains.annotations.NotNull
    private final java.lang.String editorTitle = null;
    @org.jetbrains.annotations.NotNull
    private final java.lang.String editorDescription = null;
    private final int editorPriority = 0;
    private final boolean showInboxOnly = false;
    
    public TaskListUiState(@org.jetbrains.annotations.NotNull
    java.util.List<com.pim.core.models.TaskResponse> tasks, boolean isLoading, @org.jetbrains.annotations.Nullable
    java.lang.String error, boolean isEditorOpen, @org.jetbrains.annotations.NotNull
    java.lang.String editorTitle, @org.jetbrains.annotations.NotNull
    java.lang.String editorDescription, int editorPriority, boolean showInboxOnly) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.util.List<com.pim.core.models.TaskResponse> getTasks() {
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
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getEditorTitle() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getEditorDescription() {
        return null;
    }
    
    public final int getEditorPriority() {
        return 0;
    }
    
    public final boolean getShowInboxOnly() {
        return false;
    }
    
    public TaskListUiState() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.util.List<com.pim.core.models.TaskResponse> component1() {
        return null;
    }
    
    public final boolean component2() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.String component3() {
        return null;
    }
    
    public final boolean component4() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String component5() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String component6() {
        return null;
    }
    
    public final int component7() {
        return 0;
    }
    
    public final boolean component8() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull
    public final com.pim.app.ui.tasks.TaskListUiState copy(@org.jetbrains.annotations.NotNull
    java.util.List<com.pim.core.models.TaskResponse> tasks, boolean isLoading, @org.jetbrains.annotations.Nullable
    java.lang.String error, boolean isEditorOpen, @org.jetbrains.annotations.NotNull
    java.lang.String editorTitle, @org.jetbrains.annotations.NotNull
    java.lang.String editorDescription, int editorPriority, boolean showInboxOnly) {
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