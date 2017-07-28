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

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.util.LinkedList;

class TestThread extends Thread {

    final Object sync = new Object();
    final String prompt = "(gdb) ";

    class OutputReader extends Thread {
        final InputStream inp;
        final StringBuffer buf = new StringBuffer();
        final LinkedList<String> lst = new LinkedList<String>();

        OutputReader(InputStream inp) {
            this.inp = new BufferedInputStream(inp);
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
                    System.out.println(std_err.lst.removeFirst());
                }
                while (!std_out.lst.isEmpty()) {
                    System.out.println(std_out.lst.removeFirst());
                }
                System.out.flush();
                sync.wait(100);
                if (std_err.lst.isEmpty() && std_out.lst.isEmpty() &&
                        std_out.buf.length() > 0 && std_out.buf.toString().equals(prompt)) break;
            }
        }
        while (System.currentTimeMillis() < time + 10000);
        synchronized (sync) {
            if (std_out.buf.length() > 0) {
                System.out.print(std_out.buf.toString());
                std_out.buf.setLength(0);
            }
        }
        System.out.flush();
    }

    void cmd(String c) throws Exception {
        checkOutput();
        std_inp.write(c);
        std_inp.write("\n");
        std_inp.flush();
        System.out.println(c);
        System.out.flush();
        if (c.startsWith("mon ") || c.startsWith("target ")) {
            sleep(1000); // Need to sleep because command output goes to stderr
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
            String gdb = System.getenv().get("TCF_TEST_GDB_EXE");
            if (gdb == null) gdb = "gdb";

            String port = System.getenv().get("TCF_TEST_GDB_PORT");
            if (port == null) port = "3000";

            BigInteger prev_pc = null;
            prs = Runtime.getRuntime().exec(new String[] { gdb, "-q" }, null);
            std_inp = new BufferedWriter(new OutputStreamWriter(prs.getOutputStream()));
            std_out = new OutputReader(prs.getInputStream());
            std_err = new OutputReader(prs.getErrorStream());
            std_out.start();
            std_err.start();

            cmd("set remotetimeout 1000");

            cmd("target extended-remote 127.0.0.1:" + port);

            cmd("mon ps");
            if (std_err.lst.size() < 1)
                throw new Exception("Invalid 'mon ps' reply: cnt < 1");
            if (!std_err.lst.get(0).startsWith("1: "))
                throw new Exception("Invalid 'mon ps' reply: list");

            cmd("detach");
            if (std_out.lst.size() < 1)
                throw new Exception("Invalid 'detach' reply: cnt < 1");
            if (!std_out.lst.get(0).startsWith("Detaching from program"))
                throw new Exception("Invalid 'detach' reply");

            cmd("disconnect");
            if (std_out.lst.size() < 1)
                throw new Exception("Invalid 'disconnect' reply: cnt < 1");
            if (!std_out.lst.get(0).startsWith("Ending remote debugging"))
                throw new Exception("Invalid 'disconnect' reply");

            cmd("target extended-remote 127.0.0.1:" + port);

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

                String reg_name = null;
                BigInteger reg_val = null;
                cmd("info reg");
                if (std_out.lst.size() < 1)
                    throw new Exception("Invalid 'info reg' reply: cnt < 1");
                {
                    String s0 = std_out.lst.get(0);
                    int s0i = s0.indexOf(' ');
                    reg_name = s0.substring(0, s0i);
                    s0 = s0.substring(s0i);
                    s0i = s0.indexOf("0x");
                    s0 = s0.substring(s0i + 2);
                    s0i = s0.indexOf('\t');
                    reg_val = new BigInteger(s0.substring(0, s0i), 16);
                }

                cmd("p/x $" + reg_name);
                if (std_out.lst.size() < 1)
                    throw new Exception("Invalid 'p/x' reply: cnt < 1");
                {
                    String s0 = std_out.lst.get(0);
                    int s0i = s0.indexOf("0x");
                    s0 = s0.substring(s0i + 2);
                    if (!reg_val.equals(new BigInteger(s0, 16)))
                        throw new Exception("Invalid 'p/x' reply: value");
                }

                cmd("set $" + reg_name + " = 0x1234");
                if (std_out.lst.size() > 0 || std_err.lst.size() > 0)
                    throw new Exception("Invalid 'set' reply");

                cmd("p/x $" + reg_name);
                if (std_out.lst.size() < 1)
                    throw new Exception("Invalid 'p/x' reply: cnt < 1");
                {
                    String s0 = std_out.lst.get(0);
                    int s0i = s0.indexOf("0x");
                    s0 = s0.substring(s0i + 2);
                    if (!s0.equals("1234"))
                        throw new Exception("Invalid 'p/x' reply: value");
                }

                cmd("set $" + reg_name + " = 0x" + reg_val.toString(16));
                if (std_out.lst.size() > 0 || std_err.lst.size() > 0)
                    throw new Exception("Invalid 'set' reply");

                cmd("p/x $" + reg_name);
                if (std_out.lst.size() < 1)
                    throw new Exception("Invalid 'p/x' reply: cnt < 1");
                {
                    String s0 = std_out.lst.get(0);
                    int s0i = s0.indexOf("0x");
                    s0 = s0.substring(s0i + 2);
                    if (!reg_val.equals(new BigInteger(s0, 16)))
                        throw new Exception("Invalid 'p/x' reply: value");
                }

                BigInteger pc = null;
                cmd("bt");
                if (std_out.lst.size() < 1)
                    throw new Exception("Invalid 'bt' reply: cnt < 1");
                if (!std_out.lst.get(0).startsWith("#0  0x"))
                    throw new Exception("Invalid 'bt' reply");
                {
                    String x = std_out.lst.get(0).substring(6);
                    pc = new BigInteger(x.substring(0, x.indexOf(' ')), 16);
                    if (prev_pc != null && pc.equals(prev_pc))
                        throw new Exception("Prev PC = PC: 0x" + pc.toString(16));
                }

                cmd("disass /r 0x" + pc.toString(16) + ",+64");
                if (std_out.lst.size() < 2)
                    throw new Exception("Invalid 'disass' reply: cnt < 2");
                if (!std_out.lst.get(0).startsWith("Dump of assembler code from 0x"))
                    throw new Exception("Invalid 'disass' reply: header");
                if (!std_out.lst.get(1).startsWith("=> 0x"))
                    throw new Exception("Invalid 'disass' reply: list");

                int infer = 0;
                cmd("add-infer");
                if (std_out.lst.size() < 1)
                    throw new Exception("Invalid 'add-infer' reply: cnt < 1");
                if (!std_out.lst.get(0).startsWith("Added inferior "))
                    throw new Exception("Invalid 'add-infer' reply");
                {
                    String z = std_out.lst.get(0).substring(15);
                    infer = Integer.parseInt(z);
                }

                cmd("infer " + infer);
                if (std_out.lst.size() < 1)
                    throw new Exception("Invalid 'infer' reply: cnt < 1");
                if (!std_out.lst.get(0).startsWith("[Switching to inferior " + infer + " "))
                    throw new Exception("Invalid 'infer' reply");

                cmd("infer 1");
                if (std_out.lst.size() < 1)
                    throw new Exception("Invalid 'infer' reply: cnt < 1");
                if (!std_out.lst.get(0).startsWith("[Switching to inferior 1 "))
                    throw new Exception("Invalid 'infer' reply");

                cmd("remove-infer " + infer);
                if (std_out.lst.size() > 0 || std_err.lst.size() > 0)
                    throw new Exception("Invalid 'remove-infer' reply");

                int bp = 0;
                cmd("b *0x" + pc.toString(16));
                if (std_out.lst.size() < 1)
                    throw new Exception("Invalid 'break' reply: cnt < 1");
                if (!std_out.lst.get(0).startsWith("Breakpoint "))
                    throw new Exception("Invalid 'break' reply");
                {
                    String y = std_out.lst.get(0).substring(11);
                    bp = Integer.parseInt(y.substring(0, y.indexOf(' ')));
                }

                cmd("d " + bp);
                if (std_out.lst.size() > 0 || std_err.lst.size() > 0)
                    throw new Exception("Invalid 'd' reply");

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
