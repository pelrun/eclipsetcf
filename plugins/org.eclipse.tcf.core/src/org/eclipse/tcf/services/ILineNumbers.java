/*******************************************************************************
 * Copyright (c) 2007-2021 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.services;

import java.util.Map;

import org.eclipse.tcf.protocol.IService;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.JSON;

/**
 * Line numbers service associates locations in the source files with the corresponding
 * machine instruction addresses in the executable object.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ILineNumbers extends IService {

    static final String NAME = "LineNumbers";

    /**
     * CodeArea represent a continues area in source text mapped to
     * continues range of code addresses.
     * Line and columns are counted starting from 1.
     * File name can be relative path, in such case client should
     * use CodeArea directory name as origin for the path.
     * File and directory names are valid on a host where code was compiled.
     * It is client responsibility to map names to this host file system.
     */
    final class CodeArea {
        public final String directory;
        public final String file;
        public final int start_line;
        public final int start_column;
        public final int end_line;
        public final int end_column;
        public final Number start_address;
        public final Number end_address;
        public final Number next_stmt_address;
        public final int isa;
        public final boolean is_statement;
        public final boolean basic_block;
        public final boolean prologue_end;
        public final boolean epilogue_begin;

        public CodeArea(String directory, String file, int start_line, int start_column,
                int end_line, int end_column, Number start_address, Number end_address, int isa,
                boolean is_statement, boolean basic_block,
                boolean prologue_end, boolean epilogue_begin) {
            this.directory = directory;
            this.file = file;
            this.start_line = start_line;
            this.start_column = start_column;
            this.end_line = end_line;
            this.end_column = end_column;
            this.start_address = start_address;
            this.end_address = end_address;
            this.next_stmt_address = null;
            this.isa = isa;
            this.is_statement = is_statement;
            this.basic_block = basic_block;
            this.prologue_end = prologue_end;
            this.epilogue_begin = epilogue_begin;
        }

        /**
         * @since 1.7
         */
        public CodeArea(String directory, String file, int start_line, int start_column,
                int end_line, int end_column, Number start_address, Number end_address, Number next_stmt_address, int isa,
                boolean is_statement, boolean basic_block,
                boolean prologue_end, boolean epilogue_begin) {
            this.directory = directory;
            this.file = file;
            this.start_line = start_line;
            this.start_column = start_column;
            this.end_line = end_line;
            this.end_column = end_column;
            this.start_address = start_address;
            this.end_address = end_address;
            this.next_stmt_address = next_stmt_address;
            this.isa = isa;
            this.is_statement = is_statement;
            this.basic_block = basic_block;
            this.prologue_end = prologue_end;
            this.epilogue_begin = epilogue_begin;
        }

        /**
         * @since 1.3
         */
        public CodeArea(Map<String,Object> area, CodeArea prev) {
            this(getString(area, "Dir", prev != null ? prev.directory : null),
            getString(area, "File", prev != null ? prev.file : null),
            getInteger(area, "SLine", 0), getInteger(area, "SCol", 0),
            getInteger(area, "ELine", 0), getInteger(area, "ECol", 0),
            (Number)area.get("SAddr"), (Number)area.get("EAddr"),
            (Number)area.get("NStmtAddr"), getInteger(area, "ISA", 0),
            getBoolean(area, "IsStmt"), getBoolean(area, "BasicBlock"),
            getBoolean(area, "PrologueEnd"), getBoolean(area, "EpilogueBegin"));
        }

        private static int getInteger(Map<String,Object> map, String name, int def) {
            Number n = (Number)map.get(name);
            if (n == null) return def;
            return n.intValue();
        }

        private static String getString(Map<String,Object> map, String name, String def) {
            String s = (String)map.get(name);
            if (s == null) return def;
            return s;
        }

        private static boolean getBoolean(Map<String,Object> map, String name) {
            Boolean b = (Boolean)map.get(name);
            if (b == null) return false;
            return b.booleanValue();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CodeArea)) return false;
            CodeArea a = (CodeArea)o;
            if (start_line != a.start_line) return false;
            if (start_column != a.start_column) return false;
            if (end_line != a.end_line) return false;
            if (end_column != a.end_column) return false;
            if (isa != a.isa) return false;
            if (is_statement != a.is_statement) return false;
            if (basic_block != a.basic_block) return false;
            if (prologue_end != a.prologue_end) return false;
            if (epilogue_begin != a.epilogue_begin) return false;
            if (start_address != null && !start_address.equals(a.start_address)) return false;
            if (start_address == null && a.start_address != null) return false;
            if (end_address != null && !end_address.equals(a.end_address)) return false;
            if (end_address == null && a.end_address != null) return false;
            if (next_stmt_address != null && !next_stmt_address.equals(a.next_stmt_address)) return false;
            if (next_stmt_address == null && a.next_stmt_address != null) return false;
            if (file != null && !file.equals(a.file)) return false;
            if (file == null && a.file != null) return false;
            if (directory != null && !directory.equals(a.directory)) return false;
            if (directory == null && a.directory != null) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int h = 0;
            if (file != null) h += file.hashCode();
            return h + start_line + start_column + end_line + end_column;
        }

        @Override
        public String toString() {
            StringBuffer bf = new StringBuffer();
            bf.append('[');
            if (directory != null) {
                bf.append(directory);
                bf.append(':');
            }
            if (file != null) {
                bf.append(file);
                bf.append(':');
            }
            bf.append(start_line);
            if (start_column != 0) {
                bf.append('.');
                bf.append(start_column);
            }
            bf.append("..");
            bf.append(end_line);
            if (end_column != 0) {
                bf.append('.');
                bf.append(end_column);
            }
            bf.append(" -> ");
            if (start_address != null) {
                bf.append("0x");
                bf.append(JSON.toBigInteger(start_address).toString(16));
            }
            else {
                bf.append('0');
            }
            bf.append("..");
            if (end_address != null) {
                bf.append("0x");
                bf.append(JSON.toBigInteger(end_address).toString(16));
            }
            else {
                bf.append('0');
            }
            if (next_stmt_address != null) {
                bf.append(",next stmt ");
                bf.append("0x");
                bf.append(JSON.toBigInteger(next_stmt_address).toString(16));
            }
            if (isa != 0) {
                bf.append(",isa ");
                bf.append(isa);
            }
            if (is_statement) {
                bf.append(",statement");
            }
            if (basic_block) {
                bf.append(",basic block");
            }
            if (prologue_end) {
                bf.append(",prologue end");
            }
            if (epilogue_begin) {
                bf.append(",epilogue begin");
            }
            bf.append(']');
            return bf.toString();
        }
    }

    IToken mapToSource(String context_id, Number start_address, Number end_address, DoneMapToSource done);

    interface DoneMapToSource {
        void doneMapToSource(IToken token, Exception error, CodeArea[] areas);
    }

    IToken mapToMemory(String context_id, String file, int line, int column, DoneMapToMemory done);

    interface DoneMapToMemory {
        void doneMapToMemory(IToken token, Exception error, CodeArea[] areas);
    }
}
