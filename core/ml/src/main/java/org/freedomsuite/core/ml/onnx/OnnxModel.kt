package org.freedomsuite.core.ml.onnx

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.nio.FloatBuffer
import java.nio.ShortBuffer

internal class OnnxModel(
    context: Context,
    assetPath: String,
) : AutoCloseable {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    val inputName: String
    val outputNames: List<String>

    init {
        val bytes = context.assets.open(assetPath).readBytes()
        session = env.createSession(bytes, OrtSession.SessionOptions())
        inputName = session.inputNames.first()
        outputNames = session.outputNames.toList()
    }

    fun run(input: OnnxTensor): OrtSession.Result = session.run(mapOf(inputName to input))

    fun run(inputs: Map<String, OnnxTensor>): OrtSession.Result = session.run(inputs)

    fun runFirstOutput(input: OnnxTensor): Any {
        val result = run(input)
        return try {
            val value = result.get(outputNames.first())
            value.get().value
        } finally {
            result.close()
        }
    }

    fun createInput(shape: LongArray, data: FloatArray): OnnxTensor =
        OnnxTensor.createTensor(env, FloatBuffer.wrap(data), shape)

    fun createInputFloat16(shape: LongArray, data: ShortArray): OnnxTensor =
        OnnxTensor.createTensor(env, ShortBuffer.wrap(data), shape, OnnxJavaType.FLOAT16)

    override fun close() {
        session.close()
    }
}
