package com.schedule.app.util

object LocationFormatter {

    /**
     * 超级岛摘要右侧：仅保留 ASCII 可打印字符（英文/数字/符号）
     * "阳光校区 YG崇真楼-A-4053" → "YG-A-4053"
     */
    fun toIslandText(raw: String): String {
        return raw
            .filter { it.code in 0x21..0x7E }
            .trim()
            .trimStart('-')
            .trimEnd('-')
            .trim()
    }

    /**
     * 展开态地点：去除 "某某校区 " 前缀
     * "阳光校区 YG崇真楼-A-4053" → "YG崇真楼-A-4053"
     */
    fun removeCampusPrefix(raw: String): String {
        return raw.replace(Regex("^[\\u4e00-\\u9fff]+校区\\s*"), "").trim()
    }
}
