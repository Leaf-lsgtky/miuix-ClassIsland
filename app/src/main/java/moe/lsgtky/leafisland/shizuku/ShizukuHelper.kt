package moe.lsgtky.leafisland.shizuku

import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object ShizukuHelper {

    private const val TAG = "ShizukuHelper"

    fun isAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Exception) {
        false
    }

    fun hasPermission(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) {
        false
    }

    fun requestPermission(requestCode: Int) {
        Shizuku.requestPermission(requestCode)
    }

    fun addPermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.addRequestPermissionResultListener(listener)
    }

    fun removePermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.removeRequestPermissionResultListener(listener)
    }

    private fun wrapService(serviceName: String, stubClassName: String): Any? {
        return try {
            val originalBinder = SystemServiceHelper.getSystemService(serviceName)
            val stubClass = Class.forName(stubClassName)
            val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
            val original = asInterface.invoke(null, originalBinder)!!
            val binder = original.javaClass.getMethod("asBinder").invoke(original) as IBinder
            val wrapper = ShizukuBinderWrapper(binder)
            asInterface.invoke(null, wrapper)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to wrap service $serviceName: ${e.message}")
            null
        }
    }

    private val wrappedCM: Any? by lazy { wrapService("connectivity", "android.net.IConnectivityManager\$Stub") }

    private fun invokeIfExists(obj: Any, method: String, paramTypes: Array<Class<*>>, args: Array<Any>): Boolean {
        return try {
            obj.javaClass.getMethod(method, *paramTypes).invoke(obj, *args)
            true
        } catch (_: NoSuchMethodException) {
            false
        } catch (e: Throwable) {
            Log.w(TAG, "$method failed: ${e.cause?.message ?: e.message}")
            false
        }
    }

    /**
     * Execute a shell command via Shizuku (reflection on private Shizuku.newProcess).
     * Runs as Shizuku's UID (root if Shizuku started via Magisk, shell if via ADB).
     */
    private fun execViaShizuku(vararg cmd: String): Int {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java,
        )
        method.isAccessible = true
        val process = method.invoke(null, cmd, null, null) as Process
        return process.waitFor()
    }

    /**
     * Block network for [packageName] (uid [uid] used as fallback for iptables).
     * Strategy 1: IConnectivityManager binder hook (Android <= 15)
     * Strategy 2: cmd connectivity shell command (Android 16+)
     * Strategy 3: iptables via Shizuku shell (last resort)
     */
    fun blockNetwork(uid: Int, packageName: String): Boolean {
        // Strategy 1: IConnectivityManager (matching InstallerX order: chain first, rule second)
        val cm = wrappedCM
        if (cm != null) {
            val chainOk = invokeIfExists(
                cm, "setFirewallChainEnabled",
                arrayOf(Int::class.javaPrimitiveType!!, Boolean::class.javaPrimitiveType!!),
                arrayOf(9, true),
            )
            if (chainOk) {
                invokeIfExists(
                    cm, "setUidFirewallRule",
                    arrayOf(Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!),
                    arrayOf(9, uid, 2),
                )
                Log.d(TAG, "Network BLOCKED for $packageName via IConnectivityManager")
                return true
            }
        }

        // Strategy 2: cmd connectivity (framework-level, works on Android 16+)
        try {
            val chain = execViaShizuku("sh", "-c", "cmd connectivity set-chain3-enabled true")
            Log.d(TAG, "set-chain3-enabled exit=$chain")
            val exit = execViaShizuku("sh", "-c", "cmd connectivity set-package-networking-enabled false $packageName")
            Log.d(TAG, "Network BLOCKED for $packageName via cmd connectivity (exit=$exit)")
            if (exit == 0) return true
        } catch (e: Throwable) {
            Log.w(TAG, "cmd connectivity block failed: ${e.message}")
        }

        // Strategy 3: iptables via Shizuku shell (kernel-level)
        try {
            val exit = execViaShizuku(
                "sh", "-c",
                "iptables -C OUTPUT -m owner --uid-owner $uid -j REJECT 2>/dev/null || iptables -I OUTPUT -m owner --uid-owner $uid -j REJECT",
            )
            Log.d(TAG, "Network BLOCKED for $packageName via iptables (exit=$exit)")
            return exit == 0
        } catch (e: Throwable) {
            Log.w(TAG, "iptables block failed: ${e.message}")
        }

        Log.w(TAG, "No firewall API available")
        return false
    }

    /**
     * Unblock network for [packageName] (uid [uid] used as fallback for iptables).
     */
    fun unblockNetwork(uid: Int, packageName: String) {
        // Strategy 1: IConnectivityManager
        val cm = wrappedCM
        if (cm != null) {
            val ok = invokeIfExists(
                cm, "setUidFirewallRule",
                arrayOf(Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!),
                arrayOf(9, uid, 0),
            )
            if (ok) {
                Log.d(TAG, "Network RESTORED for $packageName via IConnectivityManager")
                return
            }
        }

        // Strategy 2: cmd connectivity
        try {
            val exit = execViaShizuku("sh", "-c", "cmd connectivity set-package-networking-enabled true $packageName")
            Log.d(TAG, "Network RESTORED for $packageName via cmd connectivity (exit=$exit)")
            if (exit == 0) return
        } catch (e: Throwable) {
            Log.w(TAG, "cmd connectivity unblock failed: ${e.message}")
        }

        // Strategy 3: iptables
        try {
            val exit = execViaShizuku(
                "sh", "-c",
                "iptables -D OUTPUT -m owner --uid-owner $uid -j REJECT 2>/dev/null; true",
            )
            Log.d(TAG, "Network RESTORED for $packageName via iptables (exit=$exit)")
        } catch (e: Throwable) {
            Log.w(TAG, "iptables unblock failed: ${e.message}")
        }
    }
}
