package com.github.simiacryptus.openai.proxy

import kotlin.reflect.full.memberProperties

interface ValidatedObject {
    fun validate(): Boolean = validateFields(this)

    companion object {
        fun validateFields(obj: Any): Boolean {
            obj.javaClass.declaredFields.forEach { field ->
                field.isAccessible = true
                val value = field.get(obj)
                if (value is ValidatedObject && !value.validate()) {
                    return false
                }
                // If the property is a list, validate each element
                if (value is List<*>) {
                    value.forEach {
                        if (it is ValidatedObject && !it.validate()) {
                            return false
                        }
                    }
                }
            }
            obj.javaClass.kotlin.memberProperties.forEach { property ->
                val value = property.getter.call(obj)
                if (value is ValidatedObject && !value.validate()) {
                    return false
                }
                // If the property is a list, validate each element
                if (value is List<*>) {
                    value.forEach {
                        if (it is ValidatedObject && !it.validate()) {
                            return false
                        }
                    }
                }
            }
            return true
        }
    }
}