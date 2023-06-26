package com.lsn.lib.net.core.code

/**
 * @Author : lsn
 * @CreateTime : 2023/3/30 上午 11:20
 * @Description :
 */
interface Code {

    interface Response {

        companion object {


        }
    }


    interface Exception {

        companion object {


            // body.string 返回为空
            const val NO_BODY_EXCEPTION_CODE = 1001

            // body.string 返回为空
            const val NO_BODY_STRING_EXCEPTION_CODE = 1002

            // body.string 服务器返回数据为空
            const val NO_SERVICE_DATA_EXCEPTION_CODE = 1003
        }

    }


}