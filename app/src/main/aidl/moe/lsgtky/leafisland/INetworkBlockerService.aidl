package moe.lsgtky.leafisland;

interface INetworkBlockerService {
    void blockNetwork(int uid);
    void unblockNetwork(int uid);
    void destroy();
}
