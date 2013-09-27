package org.javashlook.util.comparatorfactory;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import net.sf.cglib.core.ClassEmitter;
import net.sf.cglib.core.ClassNameReader;
import net.sf.cglib.core.CodeEmitter;
import net.sf.cglib.core.EmitUtils;
import net.sf.cglib.core.Local;
import net.sf.cglib.core.ReflectUtils;
import net.sf.cglib.core.Signature;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Factory for code generation of {@link Comparator}s.
 * 
 */
public final class ComparatorFactory<T> {

	private ComparatorFactory(Class<?> type) {
		if (type == null) {
			throw new IllegalArgumentException();
		}
		this.comparatorClass = type;
	}

	/**
	 * Start generation of {@link Comparator} for a class.
	 */
	public static final <T> ComparatorFactory<T> forClass(Class<T> comparatorClass) {
		return new ComparatorFactory<T>(comparatorClass);
	}

	/**
	 * Add a property from target class to a comparison chain, in ascending order.
	 */
	public ComparatorFactory<T> addProperty(String propertyName, Class<?> propertyClass) {
		return addProperty(propertyName, propertyClass, true);
	}

	/**
	 * Add a property from target class to a comparison chain, in specified order.
	 */
	public ComparatorFactory<T> addProperty(String propertyName, Class<?> propertyClass, boolean ascending) {
		pdl.add(new PropertyDescriptor(propertyName, propertyClass, (ascending) ? 1 : -1));
		return this;
	}

	@SuppressWarnings("unchecked")
	public Comparator<T> generate() {
		try {
			pds = pdl.toArray(new PropertyDescriptor[pdl.size()]);
			Class<?> c = createComparatorClass();
			return (Comparator<T>) c.newInstance();
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new RuntimeException("Error generating comparator for: " + comparatorClass, e);
		}
	}

	// Implementation
	
	private final Class<?> comparatorClass;

	static final class PropertyDescriptor {
		String propertyName;
		Class<?> propertyClass;
		int order;

		public PropertyDescriptor(String propertyName, Class<?> propertyClass, int order) {
			this.propertyName = propertyName;
			this.propertyClass = propertyClass;
			this.order = order;
		}
	}

	private final List<PropertyDescriptor> pdl = new LinkedList<PropertyDescriptor>();

	private PropertyDescriptor[] pds;

	private Type ct;
	private Local b1;
	private Local b2;
	private Local c;

	private ClassWriter cw;
	private ClassEmitter cle;
	private CodeEmitter ce;

	private byte[] byteCode;

	private Class<?> createComparatorClass() throws Exception {
		emitComparatorClass();
		
		ClassLoader loader = comparatorClass.getClassLoader();
		String className = ClassNameReader.getClassName(new ClassReader(byteCode));
		return ReflectUtils.defineClass(className, byteCode, loader);
	}

	private void emitComparatorClass() throws Exception {
		ct = Type.getType(comparatorClass);

		cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		cle = new ClassEmitter(cw);

		String fullClassName = getComparatorClassName();

		cle.begin_class(Opcodes.V1_4,
				Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
				fullClassName,
				OBJECT_TYPE,
				new Type[] { COMPARATOR_TYPE },
				null);

		// default constructor
		EmitUtils.null_constructor(cle);

		emitCompareToMethod();

		cle.end_class();

		byteCode = cw.toByteArray();
	}

	private String getComparatorClassName() {
		String className = comparatorClass.getName();
		int p = className.lastIndexOf('.');
		String packageName = className.substring(0, p);

		StringBuffer sb = new StringBuffer();
		sb.append(packageName);
		sb.append('.');
		sb.append(comparatorClass.getSimpleName());
		sb.append("Comparator$");
		for (int i = 0, n = pds.length; i < n; i++) {
			sb.append(pds[i].propertyName).append("_");
		}
		return sb.toString();
	}

	private static final Type OBJECT_TYPE = Type.getType(Object.class);
	private static final Type COMPARABLE_TYPE = Type.getType(Comparable.class);
	private static final Type COMPARATOR_TYPE = Type.getType(Comparator.class);
	private static final Type COMPARATOR_HELPER_TYPE = Type.getType(ComparatorHelper.class);

	private static final Signature COMPARE =
			new Signature("compare", Type.INT_TYPE, new Type[] { OBJECT_TYPE, OBJECT_TYPE });

	private static final Signature STATIC_COMPARE =
			new Signature("compare", Type.INT_TYPE, new Type[] { COMPARABLE_TYPE, COMPARABLE_TYPE });

	private void emitCompareToMethod() {
		ce = cle.begin_method(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, COMPARE, null);

		emitAssertArguments();
		
		c = ce.make_local(Type.INT_TYPE);
		emitCompareMethodBody();

		ce.end_method();
	}

	private void emitAssertArguments() {
		// b1
		b1 = ce.make_local(ct);
		ce.load_arg(0);
		ce.checkcast(ct);
		ce.store_local(b1);
		
		// b2
		b2 = ce.make_local(ct);
		ce.load_arg(1);
		ce.checkcast(ct);
		ce.store_local(b2);
	}

	private Signature getGetterSignature(int i) {
		String name = pds[i].propertyName;
		String s = Character.toUpperCase(name.charAt(0)) + name.substring(1);
		return new Signature("get" + s, Type.getType(pds[i].propertyClass), new Type[] {});
	}

	private void emitCompareMethodBody() {
		for (int i = 0, n = pds.length - 1; i <= n; i++) {
			emitCompareIf(i, n);
		}
	}

	private void emitCompareIf(int i, int n) {
		// b1.get..(), b2.get..()
		Signature gs = getGetterSignature(i);
		ce.load_local(b1);
		ce.invoke_virtual(ct, gs);
		ce.load_local(b2);
		ce.invoke_virtual(ct, gs);

		// int c = ComparatorHelper.compare(...);
		ce.invoke_static(COMPARATOR_HELPER_TYPE, STATIC_COMPARE);
		ce.store_local(c);

		ce.load_local(c);

		if (i == n) {
			if (pds[i].order < 0) {
				ce.push(-1);
				ce.math(Opcodes.IMUL, Type.INT_TYPE);
			}
			ce.return_value();
			return;
		}

		Label lbl = ce.make_label();
		ce.push(0);
		ce.if_icmp(CodeEmitter.EQ, lbl);

		ce.load_local(c);
		if (pds[i].order < 0) {
			ce.push(-1);
			ce.math(Opcodes.IMUL, Type.INT_TYPE);
		}
		ce.return_value();

		ce.mark(lbl);
	}

}
