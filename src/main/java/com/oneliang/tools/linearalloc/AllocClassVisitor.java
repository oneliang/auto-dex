package com.oneliang.tools.linearalloc;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;


public class AllocClassVisitor extends ClassVisitor {

	// DX translates MULTIANEWARRAY into a method call that matches this
	// (owner,name,desc)
	private static final String MULTIARRAY_OWNER = Type.getType(Array.class).getInternalName();
	private static final String MULTIARRAY_NAME = "newInstance";
	private static final String MULTIARRAY_DESC = Type.getMethodType(Type.getType(Object.class), Type.getType(Class.class), Type.getType("[" + Type.INT_TYPE.getDescriptor())).getDescriptor();

	private final Map<Pattern, Integer> penalties;
	private final MethodVisitor methodVisitor = new StatsMethodVisitor();
	private int totalAlloc=0;
	private int methodCount=0;
	private boolean isInterface;
	private List<MethodReference> methodReferenceList=new ArrayList<MethodReference>();
	private String className;

	public AllocClassVisitor(Map<Pattern, Integer> penalties) {
		super(Opcodes.ASM4);
		this.penalties = penalties;
	}

	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {

		this.className = name;
		if ((access & (Opcodes.ACC_INTERFACE)) != 0) {
			// Interfaces don't have vtables.
			// This might undercount annotations, but they are mostly small.
			isInterface = true;
		} else {
			// Some parent classes have big vtable footprints. We try to
			// estimate
			// the parent vtable
			// size based on the name of the class and parent class. This seems
			// to
			// work reasonably
			// well in practice because the heaviest vtables are View and
			// Activity,
			// and many of those
			// classes have clear names and cannot be obfuscated.
			// Non-interfaces inherit the java.lang.Object vtable, which is 48
			// bytes.
			int vtablePenalty = 48;

			String[] names = new String[] { name, superName };
			for (Map.Entry<Pattern, Integer> entry : penalties.entrySet()) {
				for (String cls : names) {
					if (entry.getKey().matcher(cls).find()) {
						vtablePenalty = Math.max(vtablePenalty, entry.getValue());
					}
				}
			}
			this.totalAlloc += vtablePenalty;
		}
	}

	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		// For non-static fields, Field objects are 16 bytes.
		if ((access & Opcodes.ACC_STATIC) == 0) {
			this.totalAlloc += 16;
		}

		return null;
	}

	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		// Method objects are 52 bytes.
		this.totalAlloc += 52;

		// For non-interfaces, each virtual method adds another 4 bytes to the
		// vtable.
		if (!isInterface) {
			boolean isDirect = ((access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) != 0) || name.equals("<init>");
			if (!isDirect) {
				this.totalAlloc += 4;
			}
		}
		methodCount++;
		MethodReference methodReference=new MethodReference(className, name, desc);
		methodReferenceList.add(methodReference);
		return methodVisitor;
	}

	public void visitOuterClass(String owner, String name, String desc) {
		super.visitOuterClass(owner, name, desc);
		if (name != null) {
//			MethodReference methodReference=new MethodReference(className, name, desc);
//			methodReferenceList.add(methodReference);
		}
	}

	private class StatsMethodVisitor extends MethodVisitor {

		public StatsMethodVisitor() {
			super(Opcodes.ASM4);
		}

		public void visitMethodInsn(int opcode, String owner, String name, String desc,boolean sign) {
			super.visitMethodInsn(opcode, owner, name, desc, sign);
			MethodReference methodReference=new MethodReference(owner, name, desc);
			methodReferenceList.add(methodReference);
		}

		public void visitMultiANewArrayInsn(String desc, int dims) {
			// dx translates this instruction into a method invocation on
			// Array.newInstance(Class clazz, int...dims);
			MethodReference methodReference=new MethodReference(MULTIARRAY_OWNER, MULTIARRAY_NAME, MULTIARRAY_DESC);
			methodReferenceList.add(methodReference);
		}
	}

	public static class MethodReference {

		public final String className;
		public final String methodName;
		public final String methodDesc;

		public MethodReference(String className, String methodName, String methodDesc) {
			this.className = className;
			this.methodName = methodName;
			this.methodDesc = methodDesc;
		}

		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof MethodReference)) {
				return false;
			}

			MethodReference that = (MethodReference) o;
			if (className != null ? !className.equals(that.className) : that.className != null) {
				return false;
			}
			if (methodDesc != null ? !methodDesc.equals(that.methodDesc) : that.methodDesc != null) {
				return false;
			}
			if (methodName != null ? !methodName.equals(that.methodName) : that.methodName != null) {
				return false;
			}
			return true;
		}

		public int hashCode() {
			int result = className != null ? className.hashCode() : 0;
			result = 31 * result + (methodName != null ? methodName.hashCode() : 0);
			result = 31 * result + (methodDesc != null ? methodDesc.hashCode() : 0);
			return result;
		}

		public String toString() {
			return className + "." + methodName + ":" + methodDesc;
		}
	}

	/**
	 * @return the totalAlloc
	 */
	public int getTotalAlloc() {
		return totalAlloc;
	}

	/**
	 * @return the methodReferenceList
	 */
	public List<MethodReference> getMethodReferenceList() {
		return methodReferenceList;
	}

	/**
	 * @return the methodCount
	 */
	public int getMethodCount() {
		return methodCount;
	}
}
