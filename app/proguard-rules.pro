# kotlinx.serialization and Retrofit ship their own consumer rules.
# Keep the API models so serializers are not stripped.
-keep,includedescriptorclasses class com.kodnex.nexwall.sample.data.** { *; }
