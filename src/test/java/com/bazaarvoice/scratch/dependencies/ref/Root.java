package com.bazaarvoice.scratch.dependencies.ref;

import java.util.Arrays;

@AnnotationClassType(1)
public class Root<T extends GenericClassType>
        extends SuperType<T,GenericSuperType>
        implements InterfaceType<T,GenericInterfaceType> {

    @AnnotationFieldType(AnnotationEnumType.SECOND)
    FieldType<T,GenericFieldType[]> field = new FieldValueType<T>();

    ArrayFieldType[] array = new ArrayFieldValueType[5];

    boolean value = CallStaticFieldType.value;

    @AnnotationConstructorType
    public Root() {
    }

    @AnnotationMethodType({1,2})
    MethodReturnType<GenericMethodReturnType>[] method(@AnnotationParameterType MethodParameterType<GenericMethodParameterType>[] argument) throws ExceptionType {
        @AnnotationLocalVariable
        LocalVariableType<GenericLocalVariableType> localVariable = new CallConstructorType<GenericCallType>();

        CallStaticMethodType.invoke();

        ((CallClassCastType) localVariable).invoke();

        try {

            ArrayVariableType[] array = new ArrayVariableType[5];
            for (int i = 0; i < 5; i++) {
                array[i] = new ArrayVariableType();
            }
            Arrays.asList(array);

        } catch (CatchType e) {
            // do nothing
        }

        return null;
    }
}
