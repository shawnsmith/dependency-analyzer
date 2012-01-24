package com.bazaarvoice.scratch.dependencies;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

/**
 * Extracts references to class names from class file byte code.
 */
public class ClassExtractor {

    private final ClassCollector _classes;
    private final ClassVisitor _classVisitor = new CollectorClassVisitor();
    private final FieldVisitor _fieldVisitor = new CollectorFieldVisitor();
    private final MethodVisitor _methodVisitor = new CollectorMethodVisitor();
    private final AnnotationVisitor _annotationVisitor = new CollectorAnnotationVisitor();
    private final SignatureVisitor _signatureVisitor = new CollectorSignatureVisitor();

    public ClassExtractor(ClassCollector classes) {
        _classes = classes;
    }

    public void visit(ClassReader reader) {
        reader.accept(_classVisitor, ClassReader.SKIP_FRAMES);
    }

    /** Adds a string using the JVM internal classname format (4.2.1 Binary Class and Interface Names in the JVM spec) */
    private void addObjectName(String internalName) {
        _classes.addType(Type.getObjectType(internalName));
    }

    /** Adds a string using the JVM field descriptor format (4.3.2 Field Descriptors in the JVM spec) */
    private void addFieldDescriptor(String descriptor) {
        _classes.addType(Type.getType(descriptor));
    }

    /** Adds a string using the JVM method descriptor format (4.3.3 Method Descriptors in the JVM spec) */
    private void addMethodDescriptor(String descriptor) {
        _classes.addType(Type.getMethodType(descriptor));
    }

    /** Adds a string using the JVM generic field signature (4.3.4 Signatures in the JVM spec) */
    private void addFieldSignature(String signature) {
        if (signature != null) {
            new SignatureReader(signature).acceptType(_signatureVisitor);
        }
    }

    /** Adds a string using the JVM generic method signature (4.3.4 Signatures in the JVM spec) */
    private void addClassOrMethodSignature(String signature) {
        if (signature != null) {
            new SignatureReader(signature).accept(_signatureVisitor);
        }
    }

    private class CollectorClassVisitor extends ClassVisitor {
        public CollectorClassVisitor() {
            super(Opcodes.ASM4);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            if (superName != null) {
                addObjectName(superName);
            }
            if (interfaces != null) {
                for (String ifc : interfaces) {
                    addObjectName(ifc);
                }
            }
            addClassOrMethodSignature(signature);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            addFieldDescriptor(descriptor);
            return _annotationVisitor;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            addFieldDescriptor(descriptor);
            addFieldSignature(signature);
            return _fieldVisitor;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            addMethodDescriptor(descriptor);
            addClassOrMethodSignature(signature);
            if (exceptions != null) {
                for (String exception : exceptions) {
                    addObjectName(exception);
                }
            }
            return _methodVisitor;
        }
    }

    private class CollectorFieldVisitor extends FieldVisitor {
        public CollectorFieldVisitor() {
            super(Opcodes.ASM4);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            addFieldDescriptor(descriptor);
            return _annotationVisitor;
        }
    }

    private class CollectorMethodVisitor extends MethodVisitor {
        public CollectorMethodVisitor() {
            super(Opcodes.ASM4);
        }

        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            addFieldDescriptor(descriptor);
            return _annotationVisitor;
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
            addFieldDescriptor(descriptor);
            return _annotationVisitor;
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            addObjectName(type);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            addObjectName(owner);
            addFieldDescriptor(descriptor);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor) {
            addObjectName(owner);
            addMethodDescriptor(descriptor);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bsm, Object... bsmArgs) {
            addMethodDescriptor(descriptor);
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int dims) {
            addFieldDescriptor(descriptor);
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            if (type != null) {
                addObjectName(type);
            }
        }

        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
            addFieldDescriptor(descriptor);
            addFieldSignature(signature);
        }
    }

    private class CollectorAnnotationVisitor extends AnnotationVisitor {
        private CollectorAnnotationVisitor() {
            super(Opcodes.ASM4);
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            addFieldDescriptor(descriptor);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            addFieldDescriptor(descriptor);
            return _annotationVisitor;
        }
    }

    private class CollectorSignatureVisitor extends SignatureVisitor {
        public CollectorSignatureVisitor() {
            super(Opcodes.ASM4);
        }

        @Override
        public void visitClassType(String name) {
            addObjectName(name);
        }
    }
}
