package com.lsn.lib.net.core.utils

import com.google.gson.JsonObject
import com.lsn.lib.net.core.entity.KeyValuePairEntity
import com.lsn.lib.net.core.cache.HttpPlugins


/**
 * @Author : lsn
 * @CreateTime : 2023/3/9 下午 01:52
 * @Description :
 */
object CacheUtil {

    //过滤要剔除的cacheKey
    fun <T : Any> excludeCacheKey(objects: List<T>?): List<T>? {
        if (objects == null) return null
        val excludeCacheKeys: List<String> = HttpPlugins.getExcludeCacheKeys()
        if (excludeCacheKeys.isEmpty()) return objects
        val newList: MutableList<Any> = ArrayList()
        for (`object` in objects) {
            if (`object` is KeyValuePairEntity) {
                val pair: KeyValuePairEntity = `object` as KeyValuePairEntity
                if (excludeCacheKeys.contains(pair.key)) continue
            } else if (`object` is Map<*, *>) {
                val map = excludeCacheKey(`object` as Map<*, *>)
                if (map == null || map.size == 0) continue
            } else if (`object` is JsonObject) {
                val jsonObject = excludeCacheKey(`object` as JsonObject)
                if (jsonObject == null || jsonObject.size() == 0) continue
            }
            newList.add(`object`)
        }
        return newList as List<T>
    }

    //过滤要剔除的cacheKey
    fun excludeCacheKey(param: Map<*, *>?): Map<*, *>? {
        if (param == null) return null
        val excludeCacheKeys: List<String> = HttpPlugins.getExcludeCacheKeys()
        if (excludeCacheKeys.isEmpty()) return param
        val newParam: MutableMap<String, Any> = LinkedHashMap()

        for (entity in param.entries) {
            val key = entity.key.toString()
            if (excludeCacheKeys.contains(key)) {
                continue
            }
            newParam[key] = entity.value!!
        }
        return newParam
    }

    //过滤要剔除的cacheKey
    private fun excludeCacheKey(jsonObject: JsonObject?): JsonObject? {
        if (jsonObject == null) return null
        val excludeCacheKeys: List<String> = HttpPlugins.getExcludeCacheKeys()
        if (excludeCacheKeys.isEmpty()) return jsonObject
        val newParam = JsonObject()

        for (cache in jsonObject.entrySet()) {
            if (excludeCacheKeys.contains(cache.key)) {
                continue
            }
            newParam.add(cache.key, cache.value)
        }
        return newParam
    }




}