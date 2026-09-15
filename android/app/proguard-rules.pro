# Rhino loads built-ins by reflection. Preserve its runtime and resource package names.
-keep class !org.mozilla.javascript.tools.**,org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.** { *; }

# Rhino's optional JavaBean-to-JSON converter uses desktop introspection APIs.
# Textage runs JavaScript arrays and objects; the app never enables that converter.
-dontwarn java.beans.BeanDescriptor
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
