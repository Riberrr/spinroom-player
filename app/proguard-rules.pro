# Project-specific R8 rules. Add keep rules here when a library needs them (e.g. kotlinx.serialization in S2).

# Ktor (server) references JVM-only management classes that don't exist on Android.
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
