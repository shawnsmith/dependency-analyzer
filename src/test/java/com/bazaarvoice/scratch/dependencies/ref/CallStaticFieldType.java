package com.bazaarvoice.scratch.dependencies.ref;

public class CallStaticFieldType {

    public static boolean value = false;  // not final!  or else it gets inlined at compile time
}
