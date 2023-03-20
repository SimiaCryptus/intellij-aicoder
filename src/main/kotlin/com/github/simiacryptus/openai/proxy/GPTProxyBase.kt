package com.github.simiacryptus.openai.proxy

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.simiacryptus.util.StringTools.indentJoin
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.lang.reflect.*
import kotlin.reflect.KClass

abstract class GPTProxyBase(
    apiLogFile: String?,
    private val deserializerRetries: Int = 3
) {
    abstract fun complete(prompt: ProxyRequest, vararg examples: ProxyRecord): String

    fun <T : Any> create(clazz: Class<T>): T {
        return Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz)) { proxy, method, args ->
            if (method.name == "toString") return@newProxyInstance clazz.simpleName
            val typeString = typeToName(method.genericReturnType)
            val prompt = ProxyRequest(
                method.name,
                typeString,
                (args ?: arrayOf()).zip(method.parameters)
                    .filter<Pair<Any?, Parameter>> { (arg: Any?, _) -> arg != null }
                    .map<Pair<Any?, Parameter>, Pair<String, String>> { (arg, param) ->
                        param.name to toJson(arg!!)
                    }.toMap()
            )
            for (retry in 0 until deserializerRetries) {
                val result = complete(prompt, *examples[method.name]?.toTypedArray() ?: arrayOf())
                writeToJsonLog(ProxyRecord(prompt.methodName, prompt.argList, result))
                try {
                    return@newProxyInstance fromJson(result, method.genericReturnType)
                } catch (e: JsonParseException) {
                    log.warn("Failed to parse response: $result", e)
                    log.info("Retrying...")
                }
            }
        } as T
    }

    private val apiLog = apiLogFile?.let { openApiLog(it) }
    private val examples = HashMap<String, List<ProxyRecord>>()
    private fun loadExamples(file: File = File("api.examples.json")): List<ProxyRecord> {
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
        if (apiLog != null) {
            apiLog.write(toJson(record))
            apiLog.write(",")
            apiLog.newLine()
            apiLog.flush()
        }
    }

    open fun toJson(data: Any): String {
        return objectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(data)
    }

    open fun <T> fromJson(data: String, type: Type): T {
        if (data.isNotEmpty()) try {
            if (type is Class<*> && type.isAssignableFrom(String::class.java)) return data as T
            return objectMapper().readValue(data, objectMapper().typeFactory.constructType(type)) as T
        } catch (e: JsonParseException) {
            throw e
        } catch (e: Exception) {
            log.error("Error parsing JSON", e)
        }
        if (type is Class<*>) return newInstance(type) as T
        return null as T
    }

    open fun <T> newInstance(type: Class<T>): T {
        if (type.isAssignableFrom(String::class.java)) return "" as T
        if (type.isAssignableFrom(Boolean::class.java)) return false as T
        if (type.isAssignableFrom(Int::class.java)) return 0 as T
        if (type.isAssignableFrom(Long::class.java)) return 0L as T
        if (type.isAssignableFrom(Double::class.java)) return 0.0 as T
        if (type.isAssignableFrom(Float::class.java)) return 0.0f as T
        if (type.isAssignableFrom(Short::class.java)) return 0 as T
        if (type.isAssignableFrom(Byte::class.java)) return 0 as T
        if (type.isAssignableFrom(Char::class.java)) return 0 as T
        if (type.isAssignableFrom(Void::class.java)) return null as T
        if (type.isAssignableFrom(Any::class.java)) return null as T
        if (type.isAssignableFrom(Unit::class.java)) return null as T
        if (type.isAssignableFrom(Nothing::class.java)) return null as T
        if (type.isAssignableFrom(List::class.java)) return listOf<Any>() as T
        if (type.isAssignableFrom(Map::class.java)) return mapOf<Any, Any>() as T
        if (type.isAssignableFrom(Set::class.java)) return setOf<Any>() as T
        if (type.isAssignableFrom(Array::class.java)) return arrayOf<Any>() as T
        if (type.isAssignableFrom(Iterable::class.java)) return listOf<Any>() as T
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

        fun typeToName(type: Type?): String {
            // Convert a type to API documentation including type name and type structure, recusively expanding child types
            if (type == null) {
                return "null"
            }
            if(type is KClass<*>) {
                return typeToName(type.java)
            }
            val javaClass = if (type is Class<*>) {
                type
            } else if (type is ParameterizedType) {
                type.rawType as Class<*>
            } else if (type is GenericArrayType) {
                type.genericComponentType as Class<*>
            } else if (type is TypeVariable<*>) {
                type.bounds[0] as Class<*>
            } else if (type is WildcardType) {
                type.upperBounds[0] as Class<*>
            } else if (type is KClass<*>) {
                type.java
            } else {
                null
            }
            if (javaClass != null) {
                if (javaClass.isPrimitive) {
                    return javaClass.simpleName
                }
                if (javaClass.isArray) {
                    return "Array<${typeToName(javaClass.componentType)}>"
                }
                if (javaClass.isEnum) {
                    return javaClass.simpleName
                }
                if (javaClass.isAssignableFrom(List::class.java)) {
                    if (type is ParameterizedType) {
                        val genericType = type.actualTypeArguments[0]
                        return "List<${typeToName(genericType as Class<*>)}>"
                    } else {
                        return "List"
                    }
                }
                if (javaClass.isAssignableFrom(Map::class.java)) {
                    if (type is ParameterizedType) {
                        val keyType = type.actualTypeArguments[0]
                        val valueType = type.actualTypeArguments[1]
                        return "Map<${typeToName(keyType as Class<*>)}, ${typeToName(valueType as Class<*>)}>"
                    } else {
                        return "Map"
                    }
                }
                if (javaClass.getPackage()?.name?.startsWith("java") == true) {
                    return javaClass.simpleName
                }
            }
            return typeDescription(type).toString()
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
                if (clazz.isAssignableFrom(ParameterizedType::class.java)) {
                    val keyType = (clazz as ParameterizedType).actualTypeArguments[0]
                    val valueType = (clazz as ParameterizedType).actualTypeArguments[1]
                    return TypeDescription("Map<${typeDescription(keyType as Class<*>)}, ${typeDescription(valueType as Class<*>)}}>")
                } else {
                    return TypeDescription("Map")
                }
            } else if (clazz == String::class.java) {
                return TypeDescription(clazz.simpleName)
            } else {
                TypeDescription(clazz.simpleName)
            }
            if (clazz.isPrimitive) return apiDocumentation
            if (clazz.isEnum) return apiDocumentation

            for (field in clazz.declaredFields) {
                if (field.name.startsWith("\$")) continue
                var annotation = getAnnotation(field, clazz, Notes::class)
                val notes = if (annotation == null) "" else {
                    " /* " + annotation.value + " */"
                }
                // Get ParameterizedType for field
                val type = field.genericType
                if (type is ParameterizedType) {
                    // Get raw type
                    if ((type.rawType as Class<*>).isAssignableFrom(List::class.java)) {
                        // Get type of list elements
                        val elementType = type.actualTypeArguments[0] as Class<*>
                        apiDocumentation.fields.add(
                            FieldData(
                                field.name + notes,
                                TypeDescription("List<${typeDescription(elementType)}>")
                            )
                        )
                        continue
                    }
                }
                apiDocumentation.fields.add(FieldData(field.name + notes, typeDescription(field.genericType)))
            }
            return apiDocumentation
        }

        private fun getAnnotation(
            field: Field,
            clazz: Class<*>,
            attributeClass: KClass<Notes>
        ): Notes? {
            var annotation = field.getAnnotation(attributeClass.java)
            if (annotation != null) return annotation
            // If this is a kotlin data class, look for the annotation on the constructor parameter
            if (clazz.kotlin.isData) {
                val constructor = clazz.kotlin.constructors.first()
                val parameter = constructor.parameters.firstOrNull { it.name == field.name }
                if (parameter != null) {
                    val parameterAnnotation = parameter.annotations.firstOrNull { it is Notes }
                    if (parameterAnnotation != null) {
                        return parameterAnnotation as Notes
                    }
                }
            }
            return null
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

