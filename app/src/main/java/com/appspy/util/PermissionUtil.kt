package com.appspy.util

import android.app.AppOpsManager
import android.content.Context
import android.os.Process

object PermissionUtil {

    /**
     * 检测用户是否已在系统设置里开启「使用情况访问权限」。
     * 无法用普通运行时权限申请，只能引导用户手动开启。
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
