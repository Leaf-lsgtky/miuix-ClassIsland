package moe.lsgtky.leafisland.shizuku

import android.os.IBinder
import androidx.annotation.Keep
import moe.lsgtky.leafisland.INetworkBlockerService
import kotlin.system.exitProcess

@Keep
class NetworkBlockerService : INetworkBlockerService.Stub() {

    private val cm: Any by lazy {
        val binder = Class.forName("android.os.ServiceManager")
            .getMethod("getService", String::class.java)
            .invoke(null, "connectivity") as IBinder
        val stubClass = Class.forName("android.net.IConnectivityManager\$Stub")
        stubClass.getMethod("asInterface", IBinder::class.java).invoke(null, binder)!!
    }

    override fun blockNetwork(uid: Int) {
        // chain 9 = FIREWALL_CHAIN_OEM_DENY_3 (blacklist mode)
        val setChain = cm.javaClass.getMethod(
            "setFirewallChainEnabled",
            Int::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
        )
        setChain.invoke(cm, 9, true)

        val setRule = cm.javaClass.getMethod(
            "setUidFirewallRule",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        )
        // FIREWALL_RULE_DENY = 2
        setRule.invoke(cm, 9, uid, 2)
    }

    override fun unblockNetwork(uid: Int) {
        val setRule = cm.javaClass.getMethod(
            "setUidFirewallRule",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        )
        // FIREWALL_RULE_DEFAULT = 0
        setRule.invoke(cm, 9, uid, 0)
    }

    override fun destroy() {
        exitProcess(0)
    }
}
