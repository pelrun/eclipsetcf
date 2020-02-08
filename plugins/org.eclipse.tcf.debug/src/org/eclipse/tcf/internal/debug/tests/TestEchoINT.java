/*******************************************************************************
 * Copyright (c) 2010, 2016 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.tests;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.Random;

import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IErrorReport;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IDiagnostics;

class TestEchoINT implements ITCFTest, IDiagnostics.DoneEchoINT {

    private final TCFTestSuite test_suite;
    private final IDiagnostics diag;
    private final LinkedList<BigInteger> msgs = new LinkedList<BigInteger>();
    private final Random rnd = new Random();

    private static final int MAX_COUNT = 0x1000;
    private static final int MAX_TIME_MS = 4000;

    private int count = 0;
    private long start_time;

    TestEchoINT(TCFTestSuite test_suite, IChannel channel) {
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
        int t = 0;
        BigInteger n = null;
        switch (count) {
        case 0: n = new BigInteger("-2147483648"); break;
        case 1: n = new BigInteger("2147483647"); break;
        case 2: t = 2; n = new BigInteger("-9223372036854775808"); break;
        case 3: t = 2; n = new BigInteger("9223372036854775807"); break;
        }
        if (n == null) {
            byte[] buf = null;
            t = rnd.nextInt() & 3;
            switch (t) {
            case 0:
                n = BigInteger.valueOf(rnd.nextInt());
                break;
            case 1:
                buf = new byte[4];
                rnd.nextBytes(buf);
                n = new BigInteger(1, buf);
                break;
            case 2:
                buf = new byte[8];
                rnd.nextBytes(buf);
                buf[0] = (byte)(buf[0] & 0x7f);
                n = new BigInteger(rnd.nextBoolean() ? 1 : -1, buf);
                break;
            case 3:
                buf = new byte[8];
                rnd.nextBytes(buf);
                n = new BigInteger(1, buf);
                break;
            }
        }
        msgs.add(n);
        diag.echoINT(t, n, this);
        count++;
    }

    public void doneEchoINT(IToken token, Throwable error, BigInteger b) {
        BigInteger s = msgs.removeFirst();
        if (!test_suite.isActive(this)) return;
        if (error instanceof IErrorReport && ((IErrorReport)error).getErrorCode() == IErrorReport.TCF_ERROR_INV_COMMAND) {
            test_suite.done(this, null);
        }
        else if (error != null) {
            test_suite.done(this, error);
        }
        else if (!s.equals(b)) {
            test_suite.done(this, new Exception("EchoINT test failed: " + s + " != " + b));
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
