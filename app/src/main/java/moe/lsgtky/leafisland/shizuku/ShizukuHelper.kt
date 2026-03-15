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

    /**
     * Wrapped IConnectivityManager binder proxy via Shizuku.
     * All binder transactions are forwarded through Shizuku's privileged process.
     * We keep the raw Object and use reflection to call methods, because
     * the interface signature varies across Android versions (API 36 removed
     * setFirewallChainEnabled from IConnectivityManager).
     */
    private val wrappedCM: Any by lazy {
        val originalBinder = SystemServiceHelper.getSystemService("connectivity")
        val stubClass = Class.forName("android.net.IConnectivityManager\$Stub")
        val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
        val original = asInterface.invoke(null, originalBinder)!!
        val asBinder = original.javaClass.getMethod("asBinder")
        val binder = asBinder.invoke(original) as IBinder
        val wrapper = ShizukuBinderWrapper(binder)
        asInterface.invoke(null, wrapper)!!.also {
            Log.d(TAG, "Hooked IConnectivityManager created via reflection")
        }
    }

    private fun invokeIfExists(obj: Any, method: String, paramTypes: Array<Class<*>>, args: Array<Any>): Boolean {
        return try {
            obj.javaClass.getMethod(method, *paramTypes).invoke(obj, *args)
            true
        } catch (e: NoSuchMethodException) {
            Log.w(TAG, "Method $method not found on ${obj.javaClass.name}")
            false
        }
    }

    /**
     * Block network for [uid] via FIREWALL_CHAIN_OEM_DENY_3 (chain 9).
     */
    fun blockNetwork(uid: Int) {
        val cm = wrappedCM
        // Try setFirewallChainEnabled (available on Android <= 15)
        invokeIfExists(
            cm, "setFirewallChainEnabled",
            arrayOf(Int::class.javaPrimitiveType!!, Boolean::class.javaPrimitiveType!!),
            arrayOf(9, true),
        )
        // Try setUidFirewallRule: FIREWALL_RULE_DENY = 2
        val ok = invokeIfExists(
            cm, "setUidFirewallRule",
            arrayOf(Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!),
            arrayOf(9, uid, 2),
        )
        if (ok) {
            Log.d(TAG, "Network BLOCKED for UID: $uid via IConnectivityManager")
            return
        }
        // Fallback: use iptables via Shizuku shell
        Log.d(TAG, "Falling back to iptables for UID: $uid")
        val cmd = "iptables -I OUTPUT -m owner --uid-owner $uid -j DROP"
        val process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
        process.waitFor()
        Log.d(TAG, "Network BLOCKED for UID: $uid via iptables (exit=${process.exitValue()})")
    }

    /**
     * Unblock network for [uid].
     */
    fun unblockNetwork(uid: Int) {
        val cm = wrappedCM
        // Try setUidFirewallRule: FIREWALL_RULE_DEFAULT = 0
        val ok = invokeIfExists(
            cm, "setUidFirewallRule",
            arrayOf(Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!),
            arrayOf(9, uid, 0),
        )
        if (ok) {
            Log.d(TAG, "Network RESTORED for UID: $uid via IConnectivityManager")
            return
        }
        // Fallback: remove iptables rule
        Log.d(TAG, "Falling back to iptables removal for UID: $uid")
        val cmd = "iptables -D OUTPUT -m owner --uid-owner $uid -j DROP"
        val process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
        process.waitFor()
        Log.d(TAG, "Network RESTORED for UID: $uid via iptables (exit=${process.exitValue()})")
    }
}
