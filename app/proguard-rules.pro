# Moshi reflection is used by the configurable n8n DTOs, local export, and the
# compact active-workout SavedState snapshot. Retrofit/Room/Hilt ship consumer
# rules for their generated code; these models need their Kotlin metadata and
# members retained until they are migrated to Moshi codegen.
-keep class com.raysix.fitns.core.network.** { *; }
-keep class com.raysix.fitns.core.sync.*SyncPayload { *; }
-keep class com.raysix.fitns.domain.model.** { *; }
-keep class com.raysix.fitns.feature.settings.LocalDataExport { *; }
