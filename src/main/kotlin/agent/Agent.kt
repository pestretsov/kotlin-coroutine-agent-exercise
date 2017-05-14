package agent

import org.objectweb.asm.*
import org.objectweb.asm.ClassWriter.COMPUTE_MAXS
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

class Agent {
    companion object {
        @JvmStatic
        fun premain(agentArgs: String?, inst: Instrumentation) {
            println("Agent started.")
            inst.addTransformer(TestTransformer())
        }
    }
}

class TestTransformer : ClassFileTransformer {
    override fun transform(loader: ClassLoader?, className: String?, classBeingRedefined: Class<*>?,
                           protectionDomain: ProtectionDomain?, classfileBuffer: ByteArray?): ByteArray? {
        val classReader = ClassReader(classfileBuffer)

        // Flag to automatically compute the maximum stack size and the maximum number of local variables of methods.
        val classWriter = ClassWriter(classReader, COMPUTE_MAXS)
        val classVisitor = TestClassVisitor(classWriter)

        classReader.accept(classVisitor, 0)

        return classWriter.toByteArray()
    }
}

class TestClassVisitor(cv: ClassVisitor) : ClassVisitor(Opcodes.ASM5, cv), Opcodes {
    override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?,
                             exceptions: Array<out String>?): MethodVisitor {

        val methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions)

        return TestMethodVisitor(methodVisitor)
    }
}

class TestMethodVisitor(mv: MethodVisitor) : MethodVisitor(Opcodes.ASM5, mv) {
    private val METHOD_OPCODE = Opcodes.INVOKESTATIC
    private val METHOD_OWNER = "example/CoroutineExampleKt"
    private val METHOD_NAME = "test"
    private val METHOD_DESC = "(Lkotlin/coroutines/experimental/Continuation;)Ljava/lang/Object;"

    override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, desc: String?, itf: Boolean) {
        if (opcode == METHOD_OPCODE && owner == METHOD_OWNER && name == METHOD_NAME && desc == METHOD_DESC) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
            mv.visitLdcInsn("Test detected")
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false)

            // values ignored because of COMPUTE_MAX flag
            mv.visitMaxs(0, 0)
        }

        mv.visitMethodInsn(opcode, owner, name, desc, itf)
    }
}