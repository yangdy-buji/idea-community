package com.siyeh.ig.dataflow;

import com.IGInspectionTestCase;

public class TooBroadScopeInspectionTest extends IGInspectionTestCase {

    public void test() throws Exception {
        doTest("com/siyeh/igtest/dataflow/scope", new TooBroadScopeInspection());
    }
}