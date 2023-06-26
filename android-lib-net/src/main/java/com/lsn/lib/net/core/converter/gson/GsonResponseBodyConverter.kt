package com.lsn.lib.net.core.converter.gson

import com.google.gson.Gson
import com.google.gson.JsonIOException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonToken
import okhttp3.ResponseBody
import okio.IOException
import retrofit2.Converter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType


/**
 * @Author : lsn
 * @CreateTime : 2023/4/27 下午 03:17
 * @Description :
 */
internal class GsonResponseBodyConverter<T>(gson: Gson, adapter: TypeAdapter<T>) :
    Converter<ResponseBody, T> {
    private val gson: Gson
    private val adapter: TypeAdapter<T>

    init {
        this.gson = gson
        this.adapter = adapter
    }

    @Throws(IOException::class)
    override fun convert(value: ResponseBody): T {
        val jsonReader = gson.newJsonReader(value.charStream())
        return try {
            val result = adapter.read(jsonReader)
            if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
                throw JsonIOException("JSON document was not fully consumed.")
            }
            result
        } finally {
            value.close()
        }
    }


}