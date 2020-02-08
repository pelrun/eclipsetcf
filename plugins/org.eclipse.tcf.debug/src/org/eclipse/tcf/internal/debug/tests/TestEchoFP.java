/*******************************************************************************
 * Copyright (c) 2010, 2016 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.tests;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Random;

import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IDiagnostics;

class TestEchoFP implements ITCFTest, IDiagnostics.DoneEchoFP {

    private final TCFTestSuite test_suite;
    private final IDiagnostics diag;
    private final LinkedList<BigDecimal> msgs = new LinkedList<BigDecimal>();
    private final Random rnd = new Random();

    private static final int MAX_COUNT = 0x1000;
    private static final int MAX_TIME_MS = 4000;

    private int count = 0;
    private long start_time;

    TestEchoFP(TCFTestSuite test_suite, IChannel channel) {
        this.test_suite = test_suite;
        diag = channel.getRemoteService(IDiagnostics.class);
    }

    public void start() {
        if (diag == null) {
            test_suite.done(this, null);
        }
        else {
            start_time = System.currentTimeMillis();
            for (int i = 0; i < 100; i++) sendMessage();
        }
    }

    private void sendMessage() {
        BigDecimal n = BigDecimal.valueOf(rnd.nextInt(), rnd.nextInt(61) - 30);
        msgs.add(n);
        diag.echoFP(n, this);
        count++;
    }

    private boolean cmp(double x, double y) {
        if ((float)x == (float)y) return true;
        if (x == 0) return false;
        // EchoFP test failed: 7.21866475E+21 != 7.218664750000001E+21
        // (float)x = 7.2186645E21
        // (float)y = 7.218665E21
        double d = Math.abs((x - y) / x);
        return d < 1.0e-12;
    }

    public void doneEchoFP(IToken token, Throwable error, BigDecimal b) {
        BigDecimal s = msgs.removeFirst();
        if (!test_suite.isActive(this)) return;
        if (error != null) {
            test_suite.done(this, error);
        }
        else if (!cmp(s.doubleValue(), b.doubleValue())) {
            test_suite.done(this, new Exception("EchoFP test failed: " + s + " != " + b));
        }
        else if (count < MAX_COUNT) {
            sendMessage();
            // Don't run the test longer then MAX_TIME_MS ms
            if (count % 0x40 == 0 && System.currentTimeMillis() - start_time >= MAX_TIME_MS) {
                count = MAX_COUNT;
            }
        }
        else if (msgs.isEmpty()) {
            test_suite.done(this, null);
        }
    }

    public boolean canResume(String id) {
        return true;
    }
}
