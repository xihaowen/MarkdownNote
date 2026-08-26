# ================================================================================
# MarkdownNote ProGuard Configuration
# 代码混淆配置 - 防止反编译
# ================================================================================

# ------------------------------------------------------------------------------
# 通用优化设置
# ------------------------------------------------------------------------------

# 启用优化
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# 优化选项
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

# 保留注解
-keepattributes *Annotation*

# 保留源文件名和行号信息（调试崩溃日志需要）
-keepattributes SourceFile,LineNumberTable

# 保留泛型签名
-keepattributes Signature

# 保留异常信息
-keepattributes Exceptions

# ------------------------------------------------------------------------------
# Kotlin 相关
# ------------------------------------------------------------------------------

# Kotlin 反射
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Kotlin 协程
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Kotlin 序列化
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn kotlinx.serialization.**

# ------------------------------------------------------------------------------
# AndroidX / Jetpack Compose
# ------------------------------------------------------------------------------

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class androidx.compose.runtime.** { *; }

# Material3
-keep class androidx.compose.material3.** { *; }

# Lifecycle
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class androidx.lifecycle.** { *; }

# ViewModel
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <methods>;
}

# SavedStateHandle
-keep class androidx.lifecycle.SavedStateHandle { *; }

# Activity / Fragment
-keep public class * extends android.app.Activity
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends androidx.activity.ComponentActivity

# ------------------------------------------------------------------------------
# Room 数据库
# ------------------------------------------------------------------------------

# Room 实体类 - 必须保留，否则数据库操作会失败
-keep class com.mdnote.markdownnote.data.local.** { *; }

# Room 生成的类
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Room DAO 接口
-keep interface * extends androidx.room.Dao
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static <methods>;
}

# ------------------------------------------------------------------------------
# Hilt 依赖注入 - 关键！必须完整保留
# ------------------------------------------------------------------------------

# Hilt 核心类
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class dagger.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# 保留所有 @Inject 注解的构造函数
-keep class * {
    @javax.inject.Inject <init>(...);
}

# 保留所有 @Singleton 注解的类（完整保留，不混淆）
-keep @javax.inject.Singleton class * { *; }

# 保留所有 Hilt Module
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.Module class * { *; }

# HiltViewModel 必须完整保留
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# 保留 Hilt 生成的组件类
-keep class **_HiltComponents { *; }
-keep class **_HiltModules { *; }
-keep class hilt_aggregated_deps.** { *; }

# 保留 Activity 和 Fragment 的注入
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager { *; }
-keep class * extends dagger.hilt.android.internal.managers.FragmentComponentManager { *; }

-dontwarn dagger.hilt.**
-dontwarn javax.inject.**
-dontwarn dagger.**

# ------------------------------------------------------------------------------
# Coil 图片加载
# ------------------------------------------------------------------------------

-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# ------------------------------------------------------------------------------
# DataStore
# ------------------------------------------------------------------------------

-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ------------------------------------------------------------------------------
# WorkManager
# ------------------------------------------------------------------------------

-keep class androidx.work.** { *; }
-keepclassmembers class androidx.work.** { *; }
-dontwarn androidx.work.**

# ------------------------------------------------------------------------------
# Navigation
# ------------------------------------------------------------------------------

-keep class androidx.navigation.** { *; }
-keepclassmembers class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ------------------------------------------------------------------------------
# 项目特定类 - 必须保留
# ------------------------------------------------------------------------------

# 领域模型类（用于数据传递）
-keep class com.mdnote.markdownnote.domain.model.** { *; }

# 数据仓库 - 被 Hilt 注入，必须完整保留
-keep class com.mdnote.markdownnote.data.repository.** { *; }

# 工具类 - SettingsManager, ExternalFileManager 等被 Hilt 注入
-keep class com.mdnote.markdownnote.util.SettingsManager { *; }
-keep class com.mdnote.markdownnote.util.ExternalFileManager { *; }
-keep class com.mdnote.markdownnote.util.ImageManager { *; }
-keep class com.mdnote.markdownnote.util.ExportManager { *; }

# ViewModel 类 - 被 Hilt 注入
-keep class com.mdnote.markdownnote.ui.**ViewModel { *; }
-keepclassmembers class com.mdnote.markdownnote.ui.**ViewModel { *; }

# UI 状态类
-keep class com.mdnote.markdownnote.ui.**UiState { *; }

# Application 类
-keep class com.mdnote.markdownnote.MarkdownNoteApp { *; }

# MainActivity
-keep class com.mdnote.markdownnote.MainActivity { *; }

# Sealed 类（用于状态管理）
-keep class com.mdnote.markdownnote.util.MarkdownElement { *; }
-keep class com.mdnote.markdownnote.util.MarkdownElement$* { *; }
-keep class com.mdnote.markdownnote.util.Span { *; }
-keep class com.mdnote.markdownnote.util.Span$* { *; }

# 枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ------------------------------------------------------------------------------
# 移除日志（Release 构建）
# ------------------------------------------------------------------------------

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# 移除 System.out.print
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
    public void printf(...);
}

# ------------------------------------------------------------------------------
# OkHttp
# ------------------------------------------------------------------------------

-dontwarn okhttp3.**
-dontwarn okio.**

# ------------------------------------------------------------------------------
# Kotlinx Serialization
# ------------------------------------------------------------------------------

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KClass serializer();
}
-keep class * {
    @kotlinx.serialization.Serializable <init>(...);
}

# ------------------------------------------------------------------------------
# AI Models
# ------------------------------------------------------------------------------

-keep class com.mdnote.markdownnote.data.ai.openai.** { *; }
-keep class com.mdnote.markdownnote.data.ai.anthropic.** { *; }

# ------------------------------------------------------------------------------
# 混淆字典 - 使用无意义的名称增加逆向难度
# ------------------------------------------------------------------------------

-obfuscationdictionary proguard-dictionary.txt
-classobfuscationdictionary proguard-dictionary.txt
-packageobfuscationdictionary proguard-dictionary.txt

# ------------------------------------------------------------------------------
# 额外警告抑制
# ------------------------------------------------------------------------------

-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn java.nio.file.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
# androidx.security:security-crypto 使用的 Tink 包含未被本应用调用的 KeysDownloader。
# 该工具的 Google HTTP Client/Joda-Time 是可选依赖；Error Prone 注解也不参与运行时。
# 保持规则精确到 R8 报告的不可达引用，避免为本地 API Key 加密引入额外网络栈。
-dontwarn com.google.api.client.http.GenericUrl
-dontwarn com.google.api.client.http.HttpHeaders
-dontwarn com.google.api.client.http.HttpRequest
-dontwarn com.google.api.client.http.HttpRequestFactory
-dontwarn com.google.api.client.http.HttpResponse
-dontwarn com.google.api.client.http.HttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport$Builder
-dontwarn com.google.api.client.http.javanet.NetHttpTransport
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
-dontwarn org.joda.time.Instant
