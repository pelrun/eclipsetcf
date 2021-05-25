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
package org.eclipse.tcf.internal.services.remote;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.tcf.core.Command;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.services.IRunControl;


public class RunControlProxy implements IRunControl {

    private final IChannel channel;
    private final Map<RunControlListener,IChannel.IEventListener> listeners =
        new HashMap<RunControlListener,IChannel.IEventListener>();

    private class RunContext implements IRunControl.RunControlContext {

        private final Map<String, Object> props;
        private final boolean has_state;

        RunContext(Map<String, Object> props) {
            this.props = props;
            Boolean b = (Boolean)props.get(PROP_HAS_STATE);
            has_state = b != null && b.booleanValue();
        }

        public Map<String, Object> getProperties() {
            return props;
        }

        public String getID() {
            return (String)props.get(PROP_ID);
        }

        public String getParentID() {
            return (String)props.get(PROP_PARENT_ID);
        }

        public String getProcessID() {
            return (String)props.get(PROP_PROCESS_ID);
        }

        public String getCreatorID() {
            return (String)props.get(PROP_CREATOR_ID);
        }

        public String getName() {
            return (String)props.get(PROP_NAME);
        }

        public boolean isContainer() {
            Boolean b = (Boolean)props.get(PROP_IS_CONTAINER);
            return b != null && b.booleanValue();
        }

        public boolean hasState() {
            return has_state;
        }

        public boolean canResume(int mode) {
            if (props.containsKey(PROP_CAN_RESUME)) {
                int b = ((Number)props.get(PROP_CAN_RESUME)).intValue();
                return (b & (1 << mode)) != 0;
            }
            return false;
        }

        public boolean canCount(int mode) {
            if (props.containsKey(PROP_CAN_COUNT)) {
                int b = ((Number)props.get(PROP_CAN_COUNT)).intValue();
                return (b & (1 << mode)) != 0;
            }
            return false;
        }

        public boolean canSuspend() {
            Boolean b = (Boolean)props.get(PROP_CAN_SUSPEND);
            return b != null && b.booleanValue();
        }

        public boolean canTerminate() {
            Boolean b = (Boolean)props.get(PROP_CAN_TERMINATE);
            return b != null && b.booleanValue();
        }

        public boolean canDetach() {
            Boolean b = (Boolean)props.get(PROP_CAN_DETACH);
            return b != null && b.booleanValue();
        }

        public String getRCGroup() {
            return (String)props.get(PROP_RC_GROUP);
        }

        public String getBPGroup() {
            return (String)props.get(PROP_BP_GROUP);
        }

        public String getSymbolsGroup() {
            return (String)props.get(PROP_SYMBOLS_GROUP);
        }

        @SuppressWarnings("unchecked")
        public Collection<String> getRegAccessTypes() {
            return (Collection<String>)props.get(PROP_REG_ACCESS_TYPES);
        }

        public IToken getState(final DoneGetState done) {
            return new Command(channel, RunControlProxy.this, "getState", new Object[]{ getID() }) {
                @SuppressWarnings("unchecked")
                @Override
                public void done(Exception error, Object[] args) {
                    boolean susp = false;
                    String pc = null;
                    String reason = null;
                    Map<String,Object> map = null;
                    if (error == null) {
                        assert args.length == 5;
                        error = toError(args[0]);
                        susp = ((Boolean)args[1]).booleanValue();
                        if (args[2] != null) pc = ((Number)args[2]).toString();
                        reason = (String)args[3];
                        map = (Map<String,Object>)args[4];
                    }
                    done.doneGetState(token, error, susp, pc, reason, map);
                }
            }.token;
        }

        public IToken getMinState(final DoneGetMinState done) {
            return new Command(channel, RunControlProxy.this, "getMinState", new Object[]{ getID() }) {
                @SuppressWarnings("unchecked")
                @Override
                public void done(Exception error, Object[] args) {
                    boolean susp = false;
                    String reason = null;
                    Map<String,Object> map = null;
                    if (error == null) {
                        assert args.length == 4;
                        error = toError(args[0]);
                        susp = ((Boolean)args[1]).booleanValue();
                        reason = (String)args[2];
                        map = (Map<String,Object>)args[3];
                    }
                    done.doneGetMinState(token, error, susp, reason, map);
                }
            }.token;
        }

        public IToken resume(int mode, int count, DoneCommand done) {
            return command("resume", new Object[]{ getID(), mode, count }, done);
        }

        public IToken resume(int mode, int count, Map<String,Object> params, DoneCommand done) {
            if (params == null) return resume(mode, count, done);
            return command("resume", new Object[]{ getID(), mode, count, params }, done);
        }

        public IToken suspend(DoneCommand done) {
            return command("suspend", new Object[]{ getID() }, done);
        }

        public IToken terminate(DoneCommand done) {
            return command("terminate", new Object[]{ getID() }, done);
        }

        public IToken detach(DoneCommand done) {
            return command("detach", new Object[]{ getID() }, done);
        }

        private IToken command(String cmd, Object[] args, final DoneCommand done) {
            return new Command(channel, RunControlProxy.this, cmd, args) {
                @Override
                public void done(Exception error, Object[] args) {
                    if (error == null) {
                        assert args.length == 1;
                        error = toError(args[0]);
                    }
                    done.doneCommand(token, error);
                }
            }.token;
        }

        public String toString() {
            return "[Run Control Context " + props.toString() + "]";
        }
    }

    public RunControlProxy(IChannel channel) {
        this.channel = channel;
    }

    public String getName() {
        return NAME;
    }

    public void addListener(final RunControlListener listener) {
        IChannel.IEventListener l = new IChannel.IEventListener() {

            @SuppressWarnings("unchecked")
            public void event(String name, byte[] data) {
                try {
                    Object[] args = JSON.parseSequence(data);
                    if (name.equals("contextSuspended")) {
                        assert args.length == 4;
                        listener.contextSuspended(
                                (String)args[0],
                                args[1] == null ? null : ((Number)args[1]).toString(),
                                        (String)args[2], (Map<String,Object>)args[3]);
                    }
                    else if (name.equals("contextResumed")) {
                        assert args.length == 1;
                        listener.contextResumed((String)args[0]);
                    }
                    else if (name.equals("contextAdded")) {
                        assert args.length == 1;
                        listener.contextAdded(toContextArray(args[0]));
                    }
                    else if (name.equals("contextChanged")) {
                        assert args.length == 1;
                        listener.contextChanged(toContextArray(args[0]));
                    }
                    else if (name.equals("contextRemoved")) {
                        assert args.length == 1;
                        listener.contextRemoved(toStringArray(args[0]));
                    }
                    else if (name.equals("contextException")) {
                        assert args.length == 2;
                        listener.contextException((String)args[0], (String)args[1]);
                    }
                    else if (name.equals("containerSuspended")) {
                        assert args.length == 5;
                        listener.containerSuspended(
                                (String)args[0],
                                args[1] == null ? null : ((Number)args[1]).toString(),
                                        (String)args[2], (Map<String,Object>)args[3],
                                        toStringArray(args[4]));
                    }
                    else if (name.equals("containerResumed")) {
                        assert args.length == 1;
                        listener.containerResumed(toStringArray(args[0]));
                    }
                    else if (name.equals("contextStateChanged")) {
                        assert args.length == 1;
                        if (listener instanceof RunControlListenerV1) {
                            ((RunControlListenerV1)listener).contextStateChanged((String)args[0]);
                        }
                    }
                    else {
                        throw new IOException("RunControl service: unknown event: " + name);
                    }
                }
                catch (Throwable x) {
                    channel.terminate(x);
                }
            }
        };
        channel.addEventListener(this, l);
        listeners.put(listener, l);
    }

    public void removeListener(RunControlListener listener) {
        IChannel.IEventListener l = listeners.remove(listener);
        if (l != null) channel.removeEventListener(this, l);
    }

    public IToken getContext(String context_id, final DoneGetContext done) {
        return new Command(channel, this, "getContext", new Object[]{ context_id }) {
            @SuppressWarnings("unchecked")
            @Override
            public void done(Exception error, Object[] args) {
                RunControlContext ctx = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    if (args[1] != null) ctx = new RunContext((Map<String, Object>)args[1]);
                }
                done.doneGetContext(token, error, ctx);
            }
        }.token;
    }

    public IToken getChildren(String parent_context_id, final DoneGetChildren done) {
        return new Command(channel, this, "getChildren", new Object[]{ parent_context_id }) {
            @Override
            public void done(Exception error, Object[] args) {
                String[] arr = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    arr = toStringArray(args[1]);
                }
                done.doneGetChildren(token, error, arr);
            }
        }.token;
    }

    @SuppressWarnings("unchecked")
    private RunControlContext[] toContextArray(Object o) {
        if (o == null) return null;
        Collection<Map<String,Object>> c = (Collection<Map<String,Object>>)o;
        int n = 0;
        RunControlContext[] ctx = new RunControlContext[c.size()];
        for (Map<String, Object> m : c) ctx[n++] = new RunContext(m);
        return ctx;
    }

    @SuppressWarnings("unchecked")
    private String[] toStringArray(Object o) {
        if (o == null) return null;
        Collection<String> c = (Collection<String>)o;
        return (String[])c.toArray(new String[c.size()]);
    }
}
