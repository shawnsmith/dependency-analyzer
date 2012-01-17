package com.bazaarvoice.scratch.dependencies;

import com.bazaarvoice.scratch.dependencies.ref.AnnotationClassType;
import com.bazaarvoice.scratch.dependencies.ref.AnnotationConstructorType;
import com.bazaarvoice.scratch.dependencies.ref.AnnotationEnumType;
import com.bazaarvoice.scratch.dependencies.ref.AnnotationFieldType;
import com.bazaarvoice.scratch.dependencies.ref.AnnotationMethodType;
import com.bazaarvoice.scratch.dependencies.ref.AnnotationParameterType;
import com.bazaarvoice.scratch.dependencies.ref.ArrayFieldType;
import com.bazaarvoice.scratch.dependencies.ref.ArrayFieldValueType;
import com.bazaarvoice.scratch.dependencies.ref.ArrayVariableType;
import com.bazaarvoice.scratch.dependencies.ref.CallClassCastType;
import com.bazaarvoice.scratch.dependencies.ref.CallConstructorType;
import com.bazaarvoice.scratch.dependencies.ref.CallStaticFieldType;
import com.bazaarvoice.scratch.dependencies.ref.CallStaticMethodType;
import com.bazaarvoice.scratch.dependencies.ref.CatchType;
import com.bazaarvoice.scratch.dependencies.ref.ExceptionType;
import com.bazaarvoice.scratch.dependencies.ref.FieldType;
import com.bazaarvoice.scratch.dependencies.ref.FieldValueType;
import com.bazaarvoice.scratch.dependencies.ref.GenericClassType;
import com.bazaarvoice.scratch.dependencies.ref.GenericFieldType;
import com.bazaarvoice.scratch.dependencies.ref.GenericInterfaceType;
import com.bazaarvoice.scratch.dependencies.ref.GenericLocalVariableType;
import com.bazaarvoice.scratch.dependencies.ref.GenericMethodParameterType;
import com.bazaarvoice.scratch.dependencies.ref.GenericMethodReturnType;
import com.bazaarvoice.scratch.dependencies.ref.GenericSuperType;
import com.bazaarvoice.scratch.dependencies.ref.InterfaceType;
import com.bazaarvoice.scratch.dependencies.ref.LocalVariableType;
import com.bazaarvoice.scratch.dependencies.ref.MethodParameterType;
import com.bazaarvoice.scratch.dependencies.ref.MethodReturnType;
import com.bazaarvoice.scratch.dependencies.ref.Root;
import com.bazaarvoice.scratch.dependencies.ref.SuperType;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.objectweb.asm.ClassReader;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Test
public class ClassExtractorTest {

    public void runTest() throws IOException {
//        ClassExtractor extractor = new ClassExtractor();
        ClassExtractor extractor = new ClassExtractor();
        extractor.visit(new ClassReader(Root.class.getName()));

        Set<String> actual = extractor.getClassNames();

        Set<String> expected = getNames(
                Object.class,
                Arrays.class,
                List.class,

                Root.class,

                AnnotationClassType.class,
                AnnotationConstructorType.class,
                AnnotationEnumType.class,
                AnnotationFieldType.class,
//                AnnotationLocalVariable.class,  // variable annotations get dropped at compile time
                AnnotationMethodType.class,
                AnnotationParameterType.class,
                ArrayFieldType.class,
                ArrayFieldValueType.class,
                ArrayVariableType.class,
                CallClassCastType.class,
                CallConstructorType.class,
                CallStaticFieldType.class,
                CallStaticMethodType.class,
                CatchType.class,
                ExceptionType.class,
                FieldType.class,
                FieldValueType.class,
//                GenericCallType.class,   // some generic parameters on the RHS of expressions get dropped at compile time
                GenericClassType.class,
                GenericFieldType.class,
                GenericInterfaceType.class,
                GenericLocalVariableType.class,
                GenericMethodParameterType.class,
                GenericMethodReturnType.class,
                GenericSuperType.class,
                InterfaceType.class,
                LocalVariableType.class,
                MethodParameterType.class,
                MethodReturnType.class,
                SuperType.class
        );

        assertEquals(actual, expected);
    }

    private <T extends Comparable<T>> void assertEquals(Set<T> actual, Set<T> expected) {
        Set<T> missing = Sets.difference(expected, actual).immutableCopy();
        if (!missing.isEmpty()) {
            Assert.fail("Missing " + missing.size() + " items: " + sorted(missing));
        }
        Set<T> extra = Sets.difference(actual, expected).immutableCopy();
        if (!extra.isEmpty()) {
            Assert.fail("Extra " + extra.size() + " items: " + sorted(extra));
        }
    }

    private Set<String> getNames(Class... classes) {
        Set<String> names = Sets.newHashSet();
        for (Class clazz : classes) {
            names.add(clazz.getName());
        }
        return names;
    }

    private static <T extends Comparable<? super T>> List<T> sorted(Collection<T> col) {
        List<T> list = Lists.newArrayList(col);
        Collections.sort(list);
        return list;
    }
}
/*
com.bazaarvoice.scratch.dependencies.ref.AnnotationLocalVariable,
com.bazaarvoice.scratch.dependencies.ref.CallStaticFieldType,
com.bazaarvoice.scratch.dependencies.ref.GenericCallType,
com.bazaarvoice.scratch.dependencies.ref.GenericLocalVariableType,
com.bazaarvoice.scratch.dependencies.ref.LocalVariableType
*/
