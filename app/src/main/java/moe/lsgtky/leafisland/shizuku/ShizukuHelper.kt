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
     * Block network for [uid].
     * Strategy 1: IConnectivityManager binder (Android <= 15)
     * Strategy 2: iptables via Shizuku shell (Android 16+, needs root Shizuku)
     */
    fun blockNetwork(uid: Int): Boolean {
        // Strategy 1: IConnectivityManager
        val cm = wrappedCM
        if (cm != null) {
            val ok = invokeIfExists(
                cm, "setUidFirewallRule",
                arrayOf(Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!),
                arrayOf(9, uid, 2),
            )
            if (ok) {
                invokeIfExists(
                    cm, "setFirewallChainEnabled",
                    arrayOf(Int::class.javaPrimitiveType!!, Boolean::class.javaPrimitiveType!!),
                    arrayOf(9, true),
                )
                Log.d(TAG, "Network BLOCKED for UID: $uid via IConnectivityManager")
                return true
            }
        }

        // Strategy 2: iptables via Shizuku shell
        try {
            val exit = execViaShizuku(
                "sh", "-c",
                "iptables -C OUTPUT -m owner --uid-owner $uid -j REJECT 2>/dev/null || iptables -I OUTPUT -m owner --uid-owner $uid -j REJECT",
            )
            Log.d(TAG, "Network BLOCKED for UID: $uid via iptables (exit=$exit)")
            return exit == 0
        } catch (e: Throwable) {
            Log.w(TAG, "iptables block failed: ${e.message}")
        }

        Log.w(TAG, "No firewall API available")
        return false
    }

    /**
     * Unblock network for [uid].
     */
    fun unblockNetwork(uid: Int) {
        // Strategy 1: IConnectivityManager
        val cm = wrappedCM
        if (cm != null) {
            val ok = invokeIfExists(
                cm, "setUidFirewallRule",
                arrayOf(Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!),
                arrayOf(9, uid, 0),
            )
            if (ok) {
                Log.d(TAG, "Network RESTORED for UID: $uid via IConnectivityManager")
                return
            }
        }

        // Strategy 2: iptables via Shizuku shell
        try {
            val exit = execViaShizuku(
                "sh", "-c",
                "iptables -D OUTPUT -m owner --uid-owner $uid -j REJECT 2>/dev/null; true",
            )
            Log.d(TAG, "Network RESTORED for UID: $uid via iptables (exit=$exit)")
        } catch (e: Throwable) {
            Log.w(TAG, "iptables unblock failed: ${e.message}")
        }
    }
}
