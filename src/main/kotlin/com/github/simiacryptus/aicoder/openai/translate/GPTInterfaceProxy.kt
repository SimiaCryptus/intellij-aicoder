package com.github.simiacryptus.aicoder.openai.translate

import com.fasterxml.jackson.databind.ObjectMapper
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy

abstract class GPTInterfaceProxy {
    abstract fun complete(prompt: String): String

    fun <T> proxy(clazz: Class<T>): T {
        return Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz)) { proxy, method, args ->
            if (method.name == "toString") return@newProxyInstance clazz.simpleName
            val prompt = getPrompt(method, args)
            val result = complete(prompt)
            return@newProxyInstance fromJson(result, method.returnType)
        } as T
    }

    open fun toJson(data: Any): String {
        return objectMapper()
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(data)
    }

    open fun <T> fromJson(data: String, type: Class<T>): T {
        if (data.isNotEmpty()) try {
            if (type.isAssignableFrom(String::class.java)) return data as T
            return objectMapper().readValue(data, type)
        } catch (e: Exception) {
            Companion.log.error("Error parsing JSON", e)
        }
        return type.getConstructor().newInstance()
    }

    open fun objectMapper() = ObjectMapper()

    private fun getPrompt(method: Method, args: Array<Any>): String = """
        Method: ${method.name}
        Response Type: 
            ${typeToName(method.returnType).replace("\n", "\n                ")}
        Request: 
            ${args.joinToString("\n", transform = ::toJson).replace("\n", "\n                ")}
        Response:""".trimIndent()

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(GPTInterfaceProxy::class.java)

        fun typeToName(type: Class<*>?): String {
            // Convert a type to API documentation including type name and type structure, recusively expanding child types
            if (type == null) {
                return "null"
            }
            if (type.isPrimitive) {
                return type.simpleName
            }
            if (type.isArray) {
                return "Array<${typeToName(type.componentType)}>"
            }
            if (type.isEnum) {
                return type.simpleName
            }
            if (type.isAssignableFrom(List::class.java)) {
                val genericType = (type as ParameterizedType).actualTypeArguments[0]
                return "List<${typeToName(genericType as Class<*>)}>"
            }
            if (type.isAssignableFrom(Map::class.java)) {
                val keyType = (type as ParameterizedType).actualTypeArguments[0]
                val valueType = (type as ParameterizedType).actualTypeArguments[1]
                return "Map<${typeToName(keyType as Class<*>)}, ${typeToName(valueType as Class<*>)}>"
            }
            if (type.getPackage()?.name?.startsWith("java") == true) {
                return type.simpleName
            }
            val typeDescription = typeDescription(type)
            return typeDescription.toString()
        }

        private fun typeDescription(clazz: Class<*>): TypeDescription {
            val apiDocumentation = if (clazz.isArray) {
                return TypeDescription("Array<${typeDescription(clazz.componentType)}>")
            } else if (clazz.isAssignableFrom(List::class.java)) {
                if (clazz.isAssignableFrom(ParameterizedType::class.java)) {
                    val genericType = (clazz as ParameterizedType).actualTypeArguments[0]
                    return TypeDescription("List<${typeDescription(genericType as Class<*>)}}>")
                } else {
                    return TypeDescription("List")
                }
            } else if (clazz.isAssignableFrom(Map::class.java)) {
                val keyType = (clazz as ParameterizedType).actualTypeArguments[0]
                val valueType = (clazz as ParameterizedType).actualTypeArguments[1]
                return TypeDescription("Map<${typeDescription(keyType as Class<*>)}, ${typeDescription(valueType as Class<*>)}}>")
            } else if (clazz == String::class.java) {
                return TypeDescription(clazz.simpleName)
            } else {
                TypeDescription(clazz.simpleName)
            }
            if (clazz.isPrimitive) return apiDocumentation
            if (clazz.isEnum) return apiDocumentation

            for (field in clazz.declaredFields) {
                if (field.name.startsWith("\$")) continue
                apiDocumentation.fields.add(FieldData(field.name, typeDescription(field.type)))
            }
            return apiDocumentation
        }

        class TypeDescription(val name: String) {
            val fields: ArrayList<FieldData> = ArrayList()
            override fun toString(): String =
                if (fields.isEmpty()) name else "{\n\t" + fields.joinToString("\n\t") + "\n}"
        }

        class FieldData(val name: String, val type: TypeDescription) {
            override fun toString(): String = """"$name": $type"""
        }
    }
}
