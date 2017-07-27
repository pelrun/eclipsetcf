/*******************************************************************************
 * Copyright (c) 2017 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.debug.gdb.test;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.util.LinkedList;

class TestThread extends Thread {

    final String gdb;
    final Object sync = new Object();
    final String prompt = "(gdb) ";

    TestThread(String gdb) {
        this.gdb = gdb;
    }

    class OutputReader extends Thread {
        final InputStream inp;
        final StringBuffer buf = new StringBuffer();
        final LinkedList<String> lst = new LinkedList<String>();

        OutputReader(InputStream inp) {
            this.inp = inp;
        }

        @Override
        public void run() {
            try {
                for (;;) {
                    int ch = inp.read();
                    if (ch < 0) break;
                    if (ch == '\r') continue;
                    synchronized (sync) {
                        if (ch == '\n') {
                            lst.add(buf.toString());
                            buf.setLength(0);
                        }
                        else {
                            buf.append((char) ch);
                        }
                        sync.notify();
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                System.exit(7);
            }
        }
    }

    Process prs;
    OutputReader std_out;
    OutputReader std_err;
    Writer std_inp;

    void checkOutput() throws Exception {
        long time = System.currentTimeMillis();
        do {
            synchronized (sync) {
                while (!std_err.lst.isEmpty()) {
                    System.err.println(std_err.lst.removeFirst());
                    System.err.flush();
                }
                while (!std_out.lst.isEmpty()) {
                    System.out.println(std_out.lst.removeFirst());
                    System.out.flush();
                }
                if (std_out.buf.length() > 0 && std_out.buf.toString().equals(prompt)) break;
                sync.wait(500);
            }
        }
        while (System.currentTimeMillis() < time + 10000);
        synchronized (sync) {
            if (std_out.buf.length() > 0) {
                System.out.print(std_out.buf.toString());
                std_out.buf.setLength(0);
            }
        }
    }

    void cmd(String c) throws Exception {
        checkOutput();
        std_inp.write(c);
        std_inp.write("\n");
        std_inp.flush();
        System.out.println(c);
        System.out.flush();
        if (c.startsWith("mon ")) {
            sleep(2000); // Need to sleep because command output goes to stderr
        }
        synchronized (sync) {
            for (;;) {
                if (std_out.buf.length() > 0 && std_out.buf.toString().equals(prompt)) break;
                if (!prs.isAlive()) break;
                sync.wait(500);
            }
        }
    }

    @Override
    public void run() {
        try {
            BigInteger prev_pc = null;
            prs = Runtime.getRuntime().exec(new String[] {
                gdb, "-q",
                "--eval-command=set remotetimeout 1000",
                "--eval-command=target extended-remote 127.0.0.1:3000"
            }, null);
            std_inp = new BufferedWriter(new OutputStreamWriter(prs.getOutputStream()));
            std_out = new OutputReader(prs.getInputStream());
            std_err = new OutputReader(prs.getErrorStream());
            std_out.start();
            std_err.start();

            cmd("mon ps");
            if (std_err.lst.size() < 1)
                throw new Exception("Invalid 'mon ps' reply: cnt < 1");
            if (!std_err.lst.get(0).startsWith("1: "))
                throw new Exception("Invalid 'mon ps' reply: list");

            for (int pass = 0; pass < 10; pass++) {

                cmd("info thread");
                if (std_out.lst.size() < 2)
                    throw new Exception("Invalid 'info thread' reply: cnt < 2");
                if (!std_out.lst.get(0).startsWith("  Id   Target Id"))
                    throw new Exception("Invalid 'info thread' reply: header");
                if (!std_out.lst.get(1).startsWith("* 1    Thread 1.1 ("))
                    throw new Exception("Invalid 'info thread' reply: list");

                cmd("info infer");
                if (std_out.lst.size() < 2)
                    throw new Exception("Invalid 'info infer' reply: cnt < 2");
                if (!std_out.lst.get(0).startsWith("  Num  Description"))
                    throw new Exception("Invalid 'info infer' reply: header");
                if (!std_out.lst.get(1).startsWith("* 1    process "))
                    throw new Exception("Invalid 'info infer' reply: list");

                cmd("info reg");

                cmd("bt");
                if (std_out.lst.size() < 1)
                    throw new Exception("Invalid 'bt' reply: cnt < 1");
                if (!std_out.lst.get(0).startsWith("#0  0x"))
                    throw new Exception("Invalid 'bt' reply");
                String x = std_out.lst.get(0).substring(6);
                BigInteger pc = new BigInteger(x.substring(0, x.indexOf(' ')), 16);
                if (prev_pc != null && pc.equals(prev_pc))
                    throw new Exception("Prev PC = PC: 0x" + pc.toString(16));

                cmd("disass /r 0x" + pc.toString(16) + ",+64");
                if (std_out.lst.size() < 2)
                    throw new Exception("Invalid 'disass' reply: cnt < 2");
                if (!std_out.lst.get(0).startsWith("Dump of assembler code from 0x"))
                    throw new Exception("Invalid 'disass' reply: header");
                if (!std_out.lst.get(1).startsWith("=> 0x"))
                    throw new Exception("Invalid 'disass' reply: list");

                cmd("b *0x" + pc.toString(16));
                if (std_out.lst.size() < 1)
                    throw new Exception("Invalid 'break' reply: cnt < 1");
                if (!std_out.lst.get(0).startsWith("Breakpoint "))
                    throw new Exception("Invalid 'break' reply");
                String y = std_out.lst.get(0).substring(11);
                int b = Integer.parseInt(y.substring(0, y.indexOf(' ')));

                cmd("d " + b);


                if (pass == 0) {
                    cmd("cont");
                }
                else {
                    cmd("stepi");
                }
                prev_pc = pc;

            }

            /* Run until exit */
            cmd("cont");
            if (std_out.lst.size() < 2)
                throw new Exception("Invalid 'cont' reply: cnt < 2");
            if (!std_out.lst.get(0).startsWith("Continuing."))
                throw new Exception("Invalid 'cont' reply: start");
            if (!std_out.lst.get(1).startsWith("[Inferior 1 "))
                throw new Exception("Invalid 'cont' reply: end");

            cmd("info thread");
            if (std_out.lst.size() != 1)
                throw new Exception("Invalid 'infor thread' reply: cnt != 1");
            if (!std_out.lst.get(0).startsWith("No threads."))
                throw new Exception("Invalid 'info threads' reply");

            cmd("quit");
            prs.waitFor();
            std_out.join();
            std_err.join();
            System.exit(0);
        }
        catch (Exception e) {
            try {
                checkOutput();
            }
            catch (Exception x) {
            }
            e.printStackTrace();
            System.exit(7);
        }
    }
}
