package com.hacybeyker.cartera.demo.contract

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.model.Request
import com.atlassian.oai.validator.model.SimpleRequest
import com.atlassian.oai.validator.model.SimpleResponse
import com.atlassian.oai.validator.report.LevelResolver
import com.atlassian.oai.validator.report.ValidationReport

/** Un envoltorio de swagger-request-validator sobre `api/openapi.yaml`, que devuelve los errores como texto. */
object ContractValidator {
    // Dos reglas del validador se apagan, y las dos con motivo:
    // - additionalProperties: cierra todo objeto que no lo declara, así que rechaza en falso las composiciones
    //   (`MovementDetail`); los campos de más los busca UndocumentedFields, que sí entiende `allOf`.
    // - format.email: contrasta el dominio con la lista de dominios de nivel superior y rechaza los reservados por
    //   el RFC 2606 (`.invalid`, `.example`), que son los que usan los fixtures a propósito.
    private val validator: OpenApiInteractionValidator = OpenApiInteractionValidator
        .createFor(ContractSpec.openApiFile.absolutePath)
        .withLevelResolver(
            LevelResolver.create()
                .withLevel("validation.schema.additionalProperties", ValidationReport.Level.IGNORE)
                .withLevel("validation.response.body.schema.format.email", ValidationReport.Level.IGNORE)
                .withLevel("validation.request.body.schema.format.email", ValidationReport.Level.IGNORE)
                .build()
        )
        .build()

    fun response(operation: String, status: Int, contentType: String?, body: String?): List<String> {
        val builder = SimpleResponse.Builder(status)
        contentType?.let(builder::withContentType)
        body?.let(builder::withBody)
        val (method, path) = split(operation)
        return errors(validator.validateResponse(path, method, builder.build()))
    }

    fun request(operation: String, headers: Map<String, String>, query: Map<String, String>, body: String?) =
        split(operation).let { (method, path) ->
            val builder = SimpleRequest.Builder(method, path)
            headers.forEach { (name, value) -> builder.withHeader(name, value) }
            query.forEach { (name, value) -> builder.withQueryParam(name, value) }
            body?.takeIf { it.isNotEmpty() }?.let { builder.withBody(it).withContentType("application/json") }
            errors(validator.validateRequest(builder.build()))
        }

    private fun split(operation: String): Pair<Request.Method, String> {
        val (method, path) = operation.split(' ')
        return Request.Method.valueOf(method.uppercase()) to ContractSpec.concrete(path)
    }

    private fun errors(report: ValidationReport): List<String> = report.messages
        .filter { it.level == ValidationReport.Level.ERROR }
        .map { "${it.key}: ${it.message}" }
}
