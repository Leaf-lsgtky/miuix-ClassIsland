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

    private fun wrapService(serviceName: String, stubClassName: String): Any {
        val originalBinder = SystemServiceHelper.getSystemService(serviceName)
        val stubClass = Class.forName(stubClassName)
        val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
        val original = asInterface.invoke(null, originalBinder)!!
        val binder = original.javaClass.getMethod("asBinder").invoke(original) as IBinder
        val wrapper = ShizukuBinderWrapper(binder)
        return asInterface.invoke(null, wrapper)!!
    }

    /** IConnectivityManager proxy (firewall methods available on Android <= 15) */
    private val wrappedCM: Any by lazy {
        wrapService("connectivity", "android.net.IConnectivityManager\$Stub").also {
            Log.d(TAG, "Hooked IConnectivityManager created")
        }
    }

    /** INetd proxy (stable AIDL, firewall methods should exist across all versions) */
    private val wrappedNetd: Any by lazy {
        wrapService("netd", "android.net.INetd\$Stub").also {
            Log.d(TAG, "Hooked INetd created")
        }
    }

    private fun invokeIfExists(obj: Any, method: String, paramTypes: Array<Class<*>>, args: Array<Any>): Boolean {
        return try {
            obj.javaClass.getMethod(method, *paramTypes).invoke(obj, *args)
            true
        } catch (e: NoSuchMethodException) {
            false
        } catch (e: Exception) {
            Log.w(TAG, "$method invocation failed: ${e.cause?.message ?: e.message}")
            false
        }
    }

    /**
     * Block network for [uid] via FIREWALL_CHAIN_OEM_DENY_3 (chain 9).
     * Tries IConnectivityManager first, then falls back to INetd.
     */
    fun blockNetwork(uid: Int): Boolean {
        // Strategy 1: IConnectivityManager (Android <= 15)
        val cmOk = invokeIfExists(
            wrappedCM, "setUidFirewallRule",
            arrayOf(Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!),
            arrayOf(9, uid, 2),
        )
        if (cmOk) {
            invokeIfExists(
                wrappedCM, "setFirewallChainEnabled",
                arrayOf(Int::class.javaPrimitiveType!!, Boolean::class.javaPrimitiveType!!),
                arrayOf(9, true),
            )
            Log.d(TAG, "Network BLOCKED for UID: $uid via IConnectivityManager")
            return true
        }

        // Strategy 2: INetd (stable AIDL, Android 16+)
        try {
            val netd = wrappedNetd
            val chainOk = invokeIfExists(
                netd, "firewallEnableChildChain",
                arrayOf(Int::class.javaPrimitiveType!!, Boolean::class.javaPrimitiveType!!),
                arrayOf(9, true),
            )
            val ruleOk = invokeIfExists(
                netd, "firewallSetUidRule",
                arrayOf(Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!),
                arrayOf(9, uid, 2),
            )
            if (chainOk || ruleOk) {
                Log.d(TAG, "Network BLOCKED for UID: $uid via INetd")
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "INetd fallback failed: ${e.message}")
        }

        Log.w(TAG, "No firewall API available on this device")
        return false
    }

    /**
     * Unblock network for [uid].
     */
    fun unblockNetwork(uid: Int) {
        // Try IConnectivityManager
        val cmOk = invokeIfExists(
            wrappedCM, "setUidFirewallRule",
            arrayOf(Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!),
            arrayOf(9, uid, 0),
        )
        if (cmOk) {
            Log.d(TAG, "Network RESTORED for UID: $uid via IConnectivityManager")
            return
        }

        // Fallback: INetd
        try {
            invokeIfExists(
                wrappedNetd, "firewallSetUidRule",
                arrayOf(Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!),
                arrayOf(9, uid, 0),
            )
            Log.d(TAG, "Network RESTORED for UID: $uid via INetd")
        } catch (e: Exception) {
            Log.w(TAG, "INetd unblock failed: ${e.message}")
        }
    }
}
