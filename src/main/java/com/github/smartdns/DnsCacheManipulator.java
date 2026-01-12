
package com.github.smartdns;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

public class DnsCacheManipulator {

    private static final String CACHE_FIELD_NAME = "addressCache";
    private static final long DEFAULT_EXPIRY_NANOS = 1000L * 1000 * 1000 * 3600 * 24 * 365; // 1 year in nanos (approx)
    private static final long DEFAULT_EXPIRY_MILLIS = 1000L * 3600 * 24 * 365; // 1 year in millis

    /**
     * Set DNS cache for a host with multiple IPs.
     * @param host Domain name
     * @param ips List of IP strings (e.g., "192.168.1.1")
     * @throws IllegalArgumentException if host is null or empty, or if ips is null or contains invalid IPs
     * @throws RuntimeException if failed to manipulate DNS cache
     */
    public static void setDnsCache(String host, String... ips) {
        validateInput(host, ips);
        try {
            Object cache = getAddressCache();
            if (cache == null) {
                throw new RuntimeException("Failed to get addressCache from InetAddress");
            }

            // Create InetAddress array
            InetAddress[] addresses = new InetAddress[ips.length];
            for (int i = 0; i < ips.length; i++) {
                addresses[i] = createInetAddress(host, ips[i]);
            }

            putToCache(cache, host, addresses);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set DNS cache: " + e.getMessage(), e);
        }
    }

    /**
     * Remove a host from DNS cache.
     * @param host Domain name to remove
     * @throws IllegalArgumentException if host is null or empty
     * @throws RuntimeException if failed to manipulate DNS cache
     */
    public static void removeDnsCache(String host) {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Host cannot be null or empty");
        }
        try {
            Object cache = getAddressCache();
            if (cache != null) {
                removeFromCache(cache, host);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove DNS cache: " + e.getMessage(), e);
        }
    }

    /**
     * Clear all DNS cache.
     * @throws RuntimeException if failed to manipulate DNS cache
     */
    public static void clearDnsCache() {
        try {
            Object cache = getAddressCache();
            if (cache != null) {
                clearCache(cache);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear DNS cache: " + e.getMessage(), e);
        }
    }

    // --- Reflection Helpers ---

    private static Object getAddressCache() throws Exception {
        // Try "addressCache" first (Java 8-20 approx)
        try {
            Field field = InetAddress.class.getDeclaredField(CACHE_FIELD_NAME);
            field.setAccessible(true);
            return field.get(null);
        } catch (NoSuchFieldException e) {
            // Try "cache" (Java 21+)
            try {
                Field field = InetAddress.class.getDeclaredField("cache");
                field.setAccessible(true);
                return field.get(null);
            } catch (NoSuchFieldException ex) {
                throw new RuntimeException("Could not find address cache field in InetAddress", ex);
            }
        }
    }

    private static void putToCache(Object cache, String host, InetAddress[] addresses) throws Exception {
        Class<?> cacheClass = cache.getClass();

        // Java 21+ logic: cache is ConcurrentHashMap, values are CachedLookup
        if (Map.class.isAssignableFrom(cacheClass)) {
            Map map = (Map) cache;

            // Find CachedLookup class
            Class<?> cachedLookupClass = null;
            for (Class<?> c : InetAddress.class.getDeclaredClasses()) {
                if (c.getSimpleName().equals("CachedLookup")) {
                    cachedLookupClass = c;
                    break;
                }
            }

            if (cachedLookupClass != null) {
                // Constructor: (String host, InetAddress[] addrs, long expiry)
                Constructor<?> constructor = cachedLookupClass.getDeclaredConstructor(String.class, InetAddress[].class, long.class);
                constructor.setAccessible(true);
                long expiry = System.nanoTime() + DEFAULT_EXPIRY_NANOS; // 1 year in nanos (approx)
                Object entry = constructor.newInstance(host, addresses, expiry);
                map.put(host, entry);
                return;
            }
        }

        // Java 8 logic (InetAddress$Cache)
        // ... (existing logic)

        // Strategy 1: Try finding a 'put' method
        try {
            Method putMethod = cacheClass.getDeclaredMethod("put", String.class, InetAddress[].class);
            putMethod.setAccessible(true);
            putMethod.invoke(cache, host, addresses);
            return;
        } catch (NoSuchMethodException e) {
            // Continue
        }

        // Strategy 2: Access the underlying Map (for Java 8 Cache object)
        Field mapField = null;
        for (Field f : cacheClass.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(f.getType())) {
                mapField = f;
                break;
            }
        }

        if (mapField != null) {
            mapField.setAccessible(true);
            Map map = (Map) mapField.get(cache);

            // Find CacheEntry class
            Class<?> cacheEntryClass = null;
            for (Class<?> c : InetAddress.class.getDeclaredClasses()) {
                if (c.getSimpleName().equals("CacheEntry")) {
                    cacheEntryClass = c;
                    break;
                }
            }

            if (cacheEntryClass != null) {
                // Constructor (InetAddress[], long expiration)
                Constructor<?> constructor = cacheEntryClass.getDeclaredConstructors()[0];
                constructor.setAccessible(true);

                Object entry = null;
                if (constructor.getParameterCount() == 2) {
                    long expiration = System.currentTimeMillis() + DEFAULT_EXPIRY_MILLIS;
                    Class<?>[] paramTypes = constructor.getParameterTypes();
                    if (paramTypes[1] == long.class) {
                        entry = constructor.newInstance(addresses, expiration);
                    }
                }

                if (entry != null) {
                    synchronized (cache) {
                        map.put(host, entry);
                    }
                    return;
                }
            }
        }

        throw new UnsupportedOperationException("Could not find a way to put into DNS cache for this Java version: " + System.getProperty("java.version"));
    }

    private static void removeFromCache(Object cache, String host) throws Exception {
        Class<?> cacheClass = cache.getClass();

        // Java 21+ Map
        if (Map.class.isAssignableFrom(cacheClass)) {
            ((Map) cache).remove(host);
            return;
        }

        // Java 8 Cache object
        // ... (existing logic)

        Field mapField = null;
        for (Field f : cacheClass.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(f.getType())) {
                mapField = f;
                break;
            }
        }
        if (mapField != null) {
            mapField.setAccessible(true);
            Map map = (Map) mapField.get(cache);
            synchronized (cache) {
                map.remove(host);
            }
            return;
        }
    }

    private static void clearCache(Object cache) throws Exception {
        Class<?> cacheClass = cache.getClass();

        // Java 21+
        if (Map.class.isAssignableFrom(cacheClass)) {
            ((Map) cache).clear();
            return;
        }

        // Access map and clear
        Field mapField = null;
        for (Field f : cacheClass.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(f.getType())) {
                mapField = f;
                break;
            }
        }
        if (mapField != null) {
            mapField.setAccessible(true);
            Map map = (Map) mapField.get(cache);
            synchronized (cache) {
                map.clear();
            }
        }
    }

    private static InetAddress createInetAddress(String host, String ip) throws Exception {
        // Use InetAddress.getByAddress(String host, byte[] addr)
        // or getByAddress(byte[]) then set host via reflection if needed?
        // Actually InetAddress.getByAddress(String host, byte[] addr) is public since 1.4.

        byte[] ipBytes = ipToBytes(ip);
        return InetAddress.getByAddress(host, ipBytes);
    }

    private static byte[] ipToBytes(String ip) {
        try {
            return InetAddress.getByName(ip).getAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid IP address: " + ip, e);
        }
    }

    private static void validateInput(String host, String[] ips) {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Host cannot be null or empty");
        }
        if (ips == null) {
            throw new IllegalArgumentException("IPs array cannot be null");
        }
        for (String ip : ips) {
            if (ip == null || ip.trim().isEmpty()) {
                throw new IllegalArgumentException("IP address cannot be null or empty");
            }
            // Validate IP format by attempting conversion
            ipToBytes(ip);
        }
    }
}