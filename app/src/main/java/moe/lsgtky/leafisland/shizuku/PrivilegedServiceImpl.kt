package moe.lsgtky.leafisland.shizuku

import android.net.IConnectivityManager
import android.os.IBinder
import android.os.ServiceManager
import android.util.Log
import moe.lsgtky.leafisland.IPrivilegedService

class PrivilegedServiceImpl : IPrivilegedService.Stub() {

    private val TAG = "PrivilegedService"
    private val CHAIN_OEM_DENY_3 = 9

    private val cm: IConnectivityManager by lazy {
        IConnectivityManager.Stub.asInterface(ServiceManager.getService("connectivity"))
    }

    override fun setPackageNetworkingEnabled(uid: Int, enabled: Boolean): Boolean {
        return try {
            val rule = if (enabled) 0 else 2 // 0: DEFAULT/ALLOW, 2: DENY
            
            // 确保链已开启 (仅在断网时强制开启)
            if (!enabled) {
                cm.setFirewallChainEnabled(CHAIN_OEM_DENY_3, true)
            }
            
            cm.setUidFirewallRule(CHAIN_OEM_DENY_3, uid, rule)
            Log.d(TAG, "Successfully set UID $uid network to $enabled via Chain $CHAIN_OEM_DENY_3")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set networking: ${e.message}")
            false
        }
    }
}
