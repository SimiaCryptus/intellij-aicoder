package com.github.simiacryptus.openai.proxy

import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.isKotlinClass
import com.google.common.reflect.TypeToken
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.lang.reflect.*
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaType


abstract class GPTProxyBase(
    apiLogFile: String?,
    private val deserializerRetries: Int = 5
) {
    abstract fun complete(prompt: ProxyRequest, vararg examples: ProxyRecord): String

    fun <T : Any> create(clazz: Class<T>): T {
        return Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz)) { proxy, method, args ->
            if (method.name == "toString") return@newProxyInstance clazz.simpleName
            val type = method.genericReturnType
            val typeString = method.toYaml().trimIndent()
            val prompt = ProxyRequest(
                method.name,
                typeString,
                (args ?: arrayOf()).zip(method.parameters)
                    .filter<Pair<Any?, Parameter>> { (arg: Any?, _) -> arg != null }
                    .map<Pair<Any?, Parameter>, Pair<String, String>> { (arg, param) ->
                        param.name to toJson(arg!!)
                    }.toMap()
            )

            var lastException: Exception? = null
            for (retry in 0 until deserializerRetries) {
                var result = complete(prompt, *examples[method.name]?.toTypedArray() ?: arrayOf())
                // If the requested `type` is a list, check that result is a list
                if (type is ParameterizedType && List::class.java.isAssignableFrom(type.rawType as Class<*>) && !result.startsWith(
                        "["
                    )
                ) {
                    result = "[$result]"
                }
                writeToJsonLog(ProxyRecord(method.name, prompt.argList, result))
                try {
                    val obj = fromJson<Any>(result, type)
                    if (obj is ValidatedObject && !obj.validate()) {
                        log.warn("Invalid response: $result")
                        continue
                    }
                    return@newProxyInstance obj
                } catch (e: Exception) {
                    log.warn("Failed to parse response: $result", e)
                    lastException = e
                    log.info("Retry $retry of $deserializerRetries")
                }
            }
            throw RuntimeException("Failed to parse response", lastException)
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
        if (type is Class<*> && type.isAssignableFrom(String::class.java)) return data as T
        val value = objectMapper().readValue(data, objectMapper().typeFactory.constructType(type)) as T
        //log.debug("Deserialized $data to $value")
        return value
    }

    open fun objectMapper(): ObjectMapper {
        return ObjectMapper()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    data class ProxyRequest(
        val methodName: String = "",
        val apiYaml: String = "",
        val argList: Map<String, String> = mapOf()
    )

    data class ProxyRecord(
        val methodName: String = "",
        val argList: Map<String, String> = mapOf(),
        val response: String = ""
    )

    companion object {
        val log = org.slf4j.LoggerFactory.getLogger(GPTProxyBase::class.java)

        fun Parameter.toYaml(): String {
            val description = getAnnotation(Description::class.java)?.value
            val yaml = if (description != null) {
                """
                |- name: ${this.name}
                |  description: $description
                |  ${this.parameterizedType.toYaml().replace("\n", "\n  ")}
                |""".trimMargin().trim()
            } else {
                """
                |- name: ${this.name}
                |  ${this.parameterizedType.toYaml().replace("\n", "\n  ")}
                |""".trimMargin().trim()
            }
            return yaml
        }


        fun Type.toYaml(): String {
            val typeName = this.typeName.substringAfterLast('.').replace('$', '.').toLowerCase()
            val yaml = if (typeName in setOf("boolean", "integer", "number", "string")) {
                "type: $typeName"
            } else if (this is ParameterizedType && List::class.java.isAssignableFrom(this.rawType as Class<*>)) {
                """
                |type: array
                |items:
                |  ${this.actualTypeArguments[0].toYaml().replace("\n", "\n  ")}
                |""".trimMargin()
            } else if (this.isArray) {
                """
                |type: array
                |items:
                |  ${this.componentType?.toYaml()?.replace("\n", "\n  ")}
                |""".trimMargin()
            } else {
                val rawType = TypeToken.of(this).rawType
                val propertiesYaml = if (rawType.isKotlinClass() && rawType.kotlin.isData) {
                    rawType.kotlin.memberProperties.map {
                        val description = getAllAnnotations(rawType, it).find { x -> x is Description } as? Description
                        // Find annotation on the kotlin data class constructor parameter
                        val yaml = if (description != null) {
                            """
                            |${it.name}:
                            |  description: ${description.value}
                            |  ${it.returnType.javaType.toYaml().replace("\n", "\n  ")}
                            """.trimMargin().trim()
                        } else {
                            """
                            |${it.name}:
                            |  ${it.returnType.javaType.toYaml().replace("\n", "\n  ")}
                            """.trimMargin().trim()
                        }
                        yaml
                    }.toTypedArray()
                } else {
                    rawType.declaredFields.map {
                        """
                        |${it.name}:
                        |  ${it.genericType.toYaml().replace("\n", "\n  ")}
                        """.trimMargin().trim()
                    }.toTypedArray()
                }
                val fieldsYaml = propertiesYaml.toList().joinToString("\n")
                """
                |type: object
                |properties:
                |  ${fieldsYaml.replace("\n", "\n  ")}
                """.trimMargin()
            }
            return yaml
        }

        private fun getAllAnnotations(
            rawType: Class<in Nothing>,
            property: KProperty1<out Any, *>
        ) =
            property.annotations + (rawType.kotlin.constructors.first().parameters.find { x -> x.name == property.name }?.annotations
                ?: listOf())

        fun Method.toYaml(): String {
            val parameterYaml = parameters.map { it.toYaml() }.toTypedArray().joinToString("\n").trim()
            val returnTypeYaml = genericReturnType.toYaml().trim()
            val responseYaml = """
                                      |responses:
                                      |  application/json:
                                      |    schema:
                                      |      ${returnTypeYaml.replace("\n", "\n      ")}
                                      """.trimMargin().trim()
            val yaml = """
                              |operationId: $name
                              |parameters:
                              |  ${parameterYaml.replace("\n", "\n  ")}
                              |$responseYaml
                              """.trimMargin()
            return yaml
        }

        val Type.isArray: Boolean
            get() {
                return this is Class<*> && this.isArray
            }

        val Type.componentType: Type?
            get() {
                return when (this) {
                    is Class<*> -> if (this.isArray) this.componentType else null
                    is ParameterizedType -> this.actualTypeArguments.firstOrNull()
                    else -> null
                }
            }
    }

}

