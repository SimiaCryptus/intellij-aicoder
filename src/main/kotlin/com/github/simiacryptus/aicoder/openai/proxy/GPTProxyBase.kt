package com.github.simiacryptus.aicoder.openai.proxy

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.simiacryptus.aicoder.util.StringTools.indentJoin
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.lang.reflect.*

abstract class GPTProxyBase(
    apiLogFile: String
) {
    abstract fun complete(prompt: ProxyRequest, vararg examples: ProxyRecord): String

    fun <T: Any> create(clazz: Class<T>): T {
        return Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz)) { proxy, method, args ->
            if (method.name == "toString") return@newProxyInstance clazz.simpleName
            val prompt = ProxyRequest(
                method.name,
                typeToName(method.returnType),
                (args ?: arrayOf()).zip(method.parameters)
                    .filter<Pair<Any?, Parameter>> { (arg: Any?, _) -> arg != null }
                    .map<Pair<Any?, Parameter>, Pair<String, String>> { (arg, param) ->
                        param.name to toJson(arg!!)
                    }.toMap())
            for (retry in 0 until 3) {
                val result = complete(prompt, *examples[method.name]?.toTypedArray() ?: arrayOf())
                writeToJsonLog(ProxyRecord(prompt.methodName, prompt.argList, result))
                try {
                    return@newProxyInstance fromJson(result, method.returnType)
                } catch (e: JsonParseException) {
                    println("Failed to parse response: $result")
                    println("Retrying...")
                }
            }
        } as T
    }

    private val apiLog = openApiLog(apiLogFile)
    private val examples = HashMap<String, List<ProxyRecord>>()
    private fun loadExamples(file: File = File("api.examples.json")) : List<ProxyRecord> {
        if (!file.exists()) return listOf<ProxyRecord>()
        val json = file.readText()
        return fromJson(json, object : ArrayList<ProxyRecord>() {}.javaClass)
    }
    fun addExamples(file: File) {
        examples.putAll(loadExamples(file).groupBy { it.methodName })
    }

    private fun openApiLog(file: String): BufferedWriter {
        val writer = BufferedWriter(FileWriter(File(file)))
        writer.write("[")
        writer.newLine()
        writer.flush()
        return writer
    }
    private fun writeToJsonLog(record: ProxyRecord) {
        apiLog.write(toJson(record))
        apiLog.write(",")
        apiLog.newLine()
        apiLog.flush()
    }

    open fun toJson(data: Any): String {
        return objectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(data)
    }

    open fun <T> fromJson(data: String, type: Class<T>): T {
        if (data.isNotEmpty()) try {
            if (type.isAssignableFrom(String::class.java)) return data as T
            return objectMapper().readValue(data, type)
        }
        catch (e: JsonParseException) {
//            log.error("Error parsing JSON", e)
            throw e
        }
        catch (e: Exception) {
            log.error("Error parsing JSON", e)
        }
        return type.getConstructor().newInstance()
    }

    open fun objectMapper(): ObjectMapper {
        return ObjectMapper()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    data class ProxyRequest(
        val methodName: String = "",
        val responseType: String = "",
        val argList: Map<String, String> = mapOf()
    )

    data class ProxyRecord(
        val methodName: String = "",
        val argList: Map<String, String> = mapOf(),
        val response: String = ""
    )

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(GPTProxyBase::class.java)

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
                if (type.isAssignableFrom(ParameterizedType::class.java)) {
                    val genericType = (type as ParameterizedType).actualTypeArguments[0]
                    return "List<${typeToName(genericType as Class<*>)}>"
                } else {
                    return "List"
                }
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
                    return TypeDescription("List<${typeDescription(genericType as Class<*>)}>")
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
                // Get ParameterizedType for field
                val type = field.genericType
                if (type is ParameterizedType) {
                    // Get raw type
                    val rawType = type.rawType as Class<*>
                    if (rawType.isAssignableFrom(List::class.java)) {
                        // Get type of list elements
                        val elementType = type.actualTypeArguments[0] as Class<*>
                        apiDocumentation.fields.add(
                            FieldData(
                                field.name,
                                TypeDescription("List<${typeDescription(elementType)}>")
                            )
                        )
                        continue
                    }
                }
                apiDocumentation.fields.add(FieldData(field.name, typeDescription(field.type)))
            }
            return apiDocumentation
        }

        private fun typeDescription(clazz: Type): TypeDescription {
            if (clazz is Class<*>) return typeDescription(clazz)
            if (clazz is ParameterizedType) {
                val rawType = clazz.rawType as Class<*>
                if (rawType.isAssignableFrom(List::class.java)) {
                    // Get type of list elements
                    val elementType = clazz.actualTypeArguments[0] as Class<*>
                    return TypeDescription("List<${typeDescription(elementType)}>")
                }
            }
            return TypeDescription(clazz.typeName)
        }

        class TypeDescription(val name: String) {
            val fields: ArrayList<FieldData> = ArrayList()
            override fun toString(): String {
                return if (fields.isEmpty()) name else indentJoin(fields)
            }
        }

        class FieldData(val name: String, val type: TypeDescription) {
            override fun toString(): String = """"$name": $type"""
        }
    }
}
