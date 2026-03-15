package moe.lsgtky.leafisland.shizuku

import android.content.pm.PackageManager
import android.net.IConnectivityManager
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
     * IConnectivityManager proxy obtained via [ShizukuBinderWrapper].
     * All binder transactions are forwarded through Shizuku's privileged process,
     * giving shell-level permissions without spawning a remote UserService.
     *
     * This mirrors InstallerX-Revived's "hook mode" approach.
     */
    private val connectivityManager: IConnectivityManager by lazy {
        val originalBinder = SystemServiceHelper.getSystemService("connectivity")
        val originalCM = IConnectivityManager.Stub.asInterface(originalBinder)
        val wrapper = ShizukuBinderWrapper(originalCM.asBinder())
        IConnectivityManager.Stub.asInterface(wrapper).also {
            Log.d(TAG, "Hooked IConnectivityManager created")
        }
    }

    /**
     * Block network for [uid] via FIREWALL_CHAIN_OEM_DENY_3 (chain 9).
     * Must have Shizuku permission.
     */
    fun blockNetwork(uid: Int) {
        val cm = connectivityManager
        cm.setFirewallChainEnabled(9, true)
        cm.setUidFirewallRule(9, uid, 2) // FIREWALL_RULE_DENY
        Log.d(TAG, "Network BLOCKED for UID: $uid")
    }

    /**
     * Unblock network for [uid] by resetting firewall rule to DEFAULT.
     */
    fun unblockNetwork(uid: Int) {
        val cm = connectivityManager
        cm.setUidFirewallRule(9, uid, 0) // FIREWALL_RULE_DEFAULT
        Log.d(TAG, "Network RESTORED for UID: $uid")
    }
}
