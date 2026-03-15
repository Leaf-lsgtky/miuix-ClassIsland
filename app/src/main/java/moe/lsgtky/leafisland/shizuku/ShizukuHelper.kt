package moe.lsgtky.leafisland.shizuku

import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object ShizukuHelper {

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
     * IConnectivityManager proxy obtained via [ShizukuBinderWrapper].
     * All binder transactions are forwarded through Shizuku's privileged process,
     * giving shell-level permissions without spawning a remote UserService.
     *
     * This mirrors InstallerX-Revived's "hook mode" approach.
     */
    private val connectivityManager: Any by lazy {
        val originalBinder = SystemServiceHelper.getSystemService("connectivity")
        val wrapper = ShizukuBinderWrapper(originalBinder)
        val stubClass = Class.forName("android.net.IConnectivityManager\$Stub")
        stubClass.getMethod("asInterface", IBinder::class.java).invoke(null, wrapper)!!
    }

    /**
     * Block network for [uid] via FIREWALL_CHAIN_OEM_DENY_3 (chain 9).
     * Must have Shizuku permission.
     */
    fun blockNetwork(uid: Int) {
        val cm = connectivityManager
        // Enable the firewall chain
        cm.javaClass.getMethod(
            "setFirewallChainEnabled",
            Int::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
        ).invoke(cm, 9, true)
        // Apply DENY rule for this UID
        cm.javaClass.getMethod(
            "setUidFirewallRule",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        ).invoke(cm, 9, uid, 2) // FIREWALL_RULE_DENY
    }

    /**
     * Unblock network for [uid] by resetting firewall rule to DEFAULT.
     */
    fun unblockNetwork(uid: Int) {
        val cm = connectivityManager
        cm.javaClass.getMethod(
            "setUidFirewallRule",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        ).invoke(cm, 9, uid, 0) // FIREWALL_RULE_DEFAULT
    }
}
