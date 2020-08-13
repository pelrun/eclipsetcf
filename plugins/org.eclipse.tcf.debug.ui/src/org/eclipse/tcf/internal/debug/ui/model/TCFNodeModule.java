/*******************************************************************************
 * Copyright (c) 2011-2020 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.model;

import java.math.BigInteger;
import java.util.Map;

import org.eclipse.debug.internal.ui.viewers.model.provisional.ILabelUpdate;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.tcf.core.ErrorReport;
import org.eclipse.tcf.internal.debug.model.TCFSymFileRef;
import org.eclipse.tcf.internal.debug.ui.ColorCache;
import org.eclipse.tcf.internal.debug.ui.ImageCache;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext.MemoryRegion;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.services.IMemoryMap;
import org.eclipse.tcf.util.TCFDataCache;

/**
 * A node representing a memory region (module).
 */
public class TCFNodeModule extends TCFNode implements IDetailsProvider {

    private final TCFData<MemoryRegion> region;
    private int sort_pos;

    protected TCFNodeModule(final TCFNodeExecContext parent, String id, final int index) {
        super(parent, id);
        region = new TCFData<MemoryRegion>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                TCFDataCache<MemoryRegion[]> map_cache = parent.getMemoryMap();
                if (!map_cache.validate(this)) return false;
                Throwable error = map_cache.getError();
                MemoryRegion[] map_data = map_cache.getData();
                MemoryRegion region = null;
                if (map_data != null && index < map_data.length) region = map_data[index];
                set(null, error, region);
                return true;
            }
        };
    }

    public TCFDataCache<MemoryRegion> getRegion() {
        return region;
    }

    void setSortPosition(int sort_pos) {
        this.sort_pos = sort_pos;
    }

    void onMemoryMapChanged() {
        region.reset();
    }

    @Override
    protected boolean getData(ILabelUpdate update, Runnable done) {
        if (!region.validate(done)) return false;
        MemoryRegion mr = region.getData();
        IMemoryMap.MemoryRegion r = mr != null ? mr.region : null;
        if (r == null) {
            update.setLabel("...", 0);
        }
        else {
            String[] col_ids = update.getColumnIds();
            if (col_ids == null) {
                update.setLabel(r.getFileName(), 0);
            }
            else {
                for (int i = 0; i < col_ids.length; i++) {
                    String col_id = col_ids[i];
                    if (TCFColumnPresentationModules.COL_NAME.equals(col_id)) {
                        String name = r.getFileName();
                        if (name != null) {
                            int j0 = name.lastIndexOf('/');
                            int j1 = name.lastIndexOf('\\');
                            if (j0 > j1) name = name.substring(j0 + 1);
                            else if (j0 < j1) name = name.substring(j1 + 1);
                        }
                        update.setLabel(name, i);
                    }
                    else if (TCFColumnPresentationModules.COL_FILE.equals(col_id)) {
                        update.setLabel(r.getFileName(), i);
                    }
                    else if (TCFColumnPresentationModules.COL_ADDRESS.equals(col_id)) {
                        update.setLabel(toHexString(r.getAddress()), i);
                    }
                    else if (TCFColumnPresentationModules.COL_SIZE.equals(col_id)) {
                        update.setLabel(toHexString(r.getSize()), i);
                    }
                    else if (TCFColumnPresentationModules.COL_FLAGS.equals(col_id)) {
                        update.setLabel(getFlagsLabel(r.getFlags()), i);
                    }
                    else if (TCFColumnPresentationModules.COL_OFFSET.equals(col_id)) {
                        update.setLabel(toHexString(r.getOffset()), i);
                    }
                    else if (TCFColumnPresentationModules.COL_SECTION.equals(col_id)) {
                        String sectionName = r.getSectionName();
                        update.setLabel(sectionName != null ? sectionName : "", i);
                    }
                }
            }
        }
        update.setImageDescriptor(ImageCache.getImageDescriptor(ImageCache.IMG_MEMORY_MAP), 0);
        return true;
    }

    @Override
    protected void getFontData(ILabelUpdate update, String view_id) {
        FontData fn = TCFModelFonts.getNormalFontData(view_id);
        String[] cols = update.getColumnIds();
        if (cols == null || cols.length == 0) {
            update.setFontData(fn, 0);
        }
        else {
            String[] ids = update.getColumnIds();
            for (int i = 0; i < cols.length; i++) {
                if (TCFColumnPresentationModules.COL_ADDRESS.equals(ids[i]) ||
                        TCFColumnPresentationModules.COL_OFFSET.equals(ids[i]) ||
                        TCFColumnPresentationModules.COL_SIZE.equals(ids[i])) {
                    update.setFontData(TCFModelFonts.getMonospacedFontData(view_id), i);
                }
                else {
                    update.setFontData(fn, i);
                }
            }
        }
    }

    public boolean getDetailText(StyledStringBuffer bf, Runnable done) {
        if (!region.validate(done)) return false;
        MemoryRegion mr = region.getData();
        IMemoryMap.MemoryRegion r = mr != null ? mr.region : null;
        if (r == null) return true;
        String file_name = r.getFileName();
        if (file_name != null) {
            bf.append("File name: ", SWT.BOLD).append(file_name).append('\n');
            TCFNodeExecContext exe = (TCFNodeExecContext)parent;
            TCFDataCache<TCFSymFileRef> sym_cache = exe.getSymFileInfo(JSON.toBigInteger(r.getAddress()));
            if (sym_cache != null) {
                if (!sym_cache.validate(done)) return false;
                TCFSymFileRef sym_data = sym_cache.getData();
                if (sym_data != null) {
                    if (sym_data.props != null) {
                        String sym_file_name = (String)sym_data.props.get("FileName");
                        if (sym_file_name != null && !sym_file_name.equals(file_name)) {
                            bf.append("Symbol file: ", SWT.BOLD).append(sym_file_name).append('\n');
                        }
                        @SuppressWarnings("unchecked")
                        Map<String,Object> map = (Map<String,Object>)sym_data.props.get("FileError");
                        if (map != null) {
                            String msg = TCFModel.getErrorMessage(new ErrorReport("", map), false);
                            bf.append("Symbol file error: ", SWT.BOLD).append(msg, SWT.ITALIC, null, ColorCache.rgb_error).append('\n');
                        }
                    }
                    if (sym_data.error != null) bf.append("Symbol file error: ", SWT.BOLD).append(
                            TCFModel.getErrorMessage(sym_data.error, false),
                            SWT.ITALIC, null, ColorCache.rgb_error).append('\n');
                }
            }
            String section = r.getSectionName();
            Number offset = r.getOffset();
            if (section != null) bf.append("File section: ", SWT.BOLD).append(section).append('\n');
            if (offset != null) bf.append("File offset: ", SWT.BOLD).append(toHexString(offset), StyledStringBuffer.MONOSPACED).append('\n');
        }
        Number addr = r.getAddress();
        Number size = r.getSize();
        if (addr != null) bf.append("Address: ", SWT.BOLD).append(toHexString(addr), StyledStringBuffer.MONOSPACED).append('\n');
        if (size != null) bf.append("Size: ", SWT.BOLD).append(toHexString(size), StyledStringBuffer.MONOSPACED).append('\n');
        @SuppressWarnings("unchecked")
        Map<String,Object> kernel_module = (Map<String,Object>)r.getProperties().get(IMemoryMap.PROP_KERNEL_MODULE);
        if (kernel_module != null) {
            int cnt = 0;
            Number init = (Number)kernel_module.get("Init");
            Number core = (Number)kernel_module.get("Core");
            Number init_size = (Number)kernel_module.get("InitSize");
            Number core_size = (Number)kernel_module.get("CoreSize");
            bf.append("Kernel module: ", SWT.BOLD);
            if (init != null) {
                bf.append("init addr ", SWT.BOLD).append(toHexString(init), StyledStringBuffer.MONOSPACED);
                cnt++;
            }
            if (init_size != null) {
                if (cnt > 0) bf.append(", ");
                bf.append("init size ", SWT.BOLD).append(toHexString(init_size), StyledStringBuffer.MONOSPACED);
                cnt++;
            }
            if (core != null) {
                if (cnt > 0) bf.append(", ");
                bf.append("core addr ", SWT.BOLD).append(toHexString(core), StyledStringBuffer.MONOSPACED);
                cnt++;
            }
            if (core_size != null) {
                if (cnt > 0) bf.append(", ");
                bf.append("core size ", SWT.BOLD).append(toHexString(core_size), StyledStringBuffer.MONOSPACED);
                cnt++;
            }
            bf.append('\n');
        }
        String query = r.getContextQuery();
        if (query != null) bf.append("Context query: ", SWT.BOLD).append(query).append('\n');
        bf.append("Flags: ", SWT.BOLD).append(getFlagsLabel(r.getFlags())).append('\n');
        return true;
    }

    private String toHexString(Number address) {
        if (address == null) return "";
        BigInteger addr = JSON.toBigInteger(address);
        String s = addr.toString(16);
        int sz = s.length() <= 8 ? 8 : 16;
        int l = sz - s.length();
        if (l < 0) l = 0;
        if (l > 16) l = 16;
        return "0x0000000000000000".substring(0, 2 + l) + s;
    }

    private String getFlagsLabel(int flags) {
        StringBuilder flagsLabel = new StringBuilder(3);
        if ((flags & IMemoryMap.FLAG_READ) != 0) flagsLabel.append('r');
        else flagsLabel.append('-');
        if ((flags & IMemoryMap.FLAG_WRITE) != 0) flagsLabel.append('w');
        else flagsLabel.append('-');
        if ((flags & IMemoryMap.FLAG_EXECUTE) != 0) flagsLabel.append('x');
        else flagsLabel.append('-');
        return flagsLabel.toString();
    }

    @Override
    public int compareTo(TCFNode n) {
        TCFNodeModule e = (TCFNodeModule)n;
        if (sort_pos < e.sort_pos) return -1;
        if (sort_pos > e.sort_pos) return +1;
        return 0;
    }
}
