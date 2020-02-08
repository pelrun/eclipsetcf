/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.debug.test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.cdt.debug.core.CDIDebugModel;
import org.eclipse.cdt.debug.core.model.ICBreakpoint;
import org.eclipse.cdt.debug.core.model.ICBreakpointType;
import org.eclipse.cdt.debug.core.model.ICLineBreakpoint;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointsListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.tcf.debug.test.BreakpointsListener.EventTester;
import org.eclipse.tcf.debug.test.BreakpointsListener.EventType;
import org.eclipse.tcf.debug.test.services.RunControlCM.ContextState;
import org.eclipse.tcf.debug.test.util.Transaction;
import org.eclipse.tcf.internal.debug.model.TCFBreakpointsModel;
import org.eclipse.tcf.internal.debug.ui.launch.TCFLaunchContext;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IBreakpoints;
import org.eclipse.tcf.services.ILineNumbers.CodeArea;
import org.eclipse.tcf.services.ISymbols.Symbol;
import org.junit.Assert;

@SuppressWarnings("restriction")
public class BreakpointsTest extends AbstractTcfUITest
{
    private BreakpointsListener fBpListener;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        fBpListener = new BreakpointsListener();

        // CDT Breakpoint integration depends on the TCF-CDT breakpoint
        // integration to be active.  This is normally triggered by selecting
        // a stack frame in the UI.  Here force activation of the plugin
        // artificially.  None of the cdt integration packages are exported, so
        // use the TCF Launch Context extension point indirectly to force the
        // plugin to load.
        TCFLaunchContext.getLaunchContext(null);
    }

    @Override
    protected void tearDown() throws Exception {
        fBpListener.dispose();
        super.tearDown();
    }

    private CodeArea getFunctionCodeArea(final TestProcessInfo processInfo, String functionName) throws Exception {
        return new Transaction<CodeArea>() {
            @Override
            protected CodeArea process() throws InvalidCacheException, ExecutionException {
                ContextState state = validate ( fRunControlCM.getState(processInfo.fThreadId) );
                String symId = validate ( fSymbolsCM.find(processInfo.fProcessId, new BigInteger(state.pc), "tcf_test_func0") );
                Symbol sym = validate ( fSymbolsCM.getContext(symId) );
                CodeArea[] area = validate ( fLineNumbersCM.mapToSource(
                    processInfo.fProcessId,
                        sym.getAddress(),
                        new BigInteger(sym.getAddress().toString()).add(BigInteger.valueOf(1))) );
                return area[0];
            }
        }.get();
    }

    private ICLineBreakpoint createLineBreakpoint(String file, int line) throws CoreException, ExecutionException, InterruptedException {
        // Initiate wait for the context changed event.
        final Object contextChangedWaitKey = new Object();
        Protocol.invokeAndWait(new Runnable() { public void run() {
            fBreakpointsCM.waitContextAdded(contextChangedWaitKey);
        }});

        final ICLineBreakpoint bp = CDIDebugModel.createLineBreakpoint(file, ResourcesPlugin.getWorkspace().getRoot(), ICBreakpointType.REGULAR, line, true, 0, "", true);

        Map<String, Object>[] addedBps = new Transaction<Map<String, Object>[]>() {
            @Override
            protected Map<String, Object>[] process() throws InvalidCacheException ,ExecutionException {
                return validate(fBreakpointsCM.waitContextAdded(contextChangedWaitKey));
            }

        }.get();

        fBpListener.setTester(new EventTester() {
            public boolean checkEvent(EventType type, IBreakpoint testBp, Map<String, Object> deltaAttributes) {
                return (type == EventType.CHANGED && bp == testBp);
            }
        });

        fBpListener.waitForEvent();

        Assert.assertEquals(1, addedBps.length);
        Assert.assertEquals(1, bp.getMarker().getAttribute(ICBreakpoint.INSTALL_COUNT, -1));

        return bp;
    }

    public void testContextAddedOnLineBrakpointCreate() throws Exception {
        TestProcessInfo processInfo = initProcessModel("tcf_test_func0");

        CodeArea bpCodeArea = getFunctionCodeArea(processInfo, "tcf_test_func0");
        ICLineBreakpoint bp = createLineBreakpoint(bpCodeArea.file, bpCodeArea.start_line);
    }

    public void testForeignBreakpointCreate() throws Exception {
        initProcessModel("tcf_test_func0");

        final List<IBreakpoint> addedBps = Collections.synchronizedList(new ArrayList<IBreakpoint>());

        IBreakpointsListener listener = new IBreakpointsListener() {
            @Override
            public void breakpointsAdded(IBreakpoint[] breakpoints) {
                for (IBreakpoint bp : breakpoints) addedBps.add(bp);
                addedBps.notify();
            }

            @Override
            public void breakpointsChanged(IBreakpoint[] breakpoints, IMarkerDelta[] deltas) {}

            @Override
            public void breakpointsRemoved(IBreakpoint[] breakpoints, IMarkerDelta[] deltas) {
                for (IBreakpoint bp : breakpoints) addedBps.remove(bp);
            }
        };
        DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(listener);
        try {
            final String bpId = "testBp";
            new Transaction<Map<String, Object>[]>() {
                Map<String, Object> bp = new TreeMap<String, Object>();
                {
                    bp.put(IBreakpoints.PROP_LOCATION, "tcf_test_func1");
                    bp.put(IBreakpoints.PROP_ID, bpId);
                    bp.put(IBreakpoints.PROP_ENABLED, Boolean.TRUE);
                    // Bug 385965: test translation of prop_type attribute
                    bp.put(IBreakpoints.PROP_TYPE, IBreakpoints.TYPE_HARDWARE);
                }
                @Override
                protected Map<String, Object>[] process() throws InvalidCacheException ,ExecutionException {
                    fBreakpointsCM.waitContextAdded(this);
                    validate( fBreakpointsCM.add(bp, this) );
                    return validate( fBreakpointsCM.waitContextAdded(this) );
                }}.get(60, TimeUnit.SECONDS);

            long timeout = System.currentTimeMillis() + TIMEOUT_DEFAULT;
            wait: while(System.currentTimeMillis() < timeout) {
                synchronized(addedBps) {
                    addedBps.wait(100);
                    for (IBreakpoint bp : addedBps) {
                        IMarker marker = bp.getMarker();
                        if (marker != null && bpId.equals(marker.getAttribute(TCFBreakpointsModel.ATTR_ID))) {
                            break wait;
                        }
                    }
                }
            }

            if (System.currentTimeMillis() > timeout) {
                Assert.fail("timed out");
            }
        } finally {
            DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(listener);
        }
    }


}
