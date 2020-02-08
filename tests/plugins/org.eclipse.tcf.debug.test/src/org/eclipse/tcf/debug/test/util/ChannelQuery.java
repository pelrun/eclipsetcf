/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.debug.test.util;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.tcf.protocol.IChannel;

/**
 * Query extension which automatically completes when a TCF channel is closed.
 */
abstract public class ChannelQuery<V> extends Query<V> {

    private final IChannel fChannel;

    public ChannelQuery(IChannel channel) {
        fChannel = channel;
    }

    @Override
    final protected void execute(final DataCallback<V> callback) {
        final AtomicBoolean done = new AtomicBoolean(false);
        final IChannel.IChannelListener channelListener = new IChannel.IChannelListener() {
            @Override
            public void onChannelClosed(Throwable error) {
                if (!done.getAndSet(true)) {
                    fChannel.removeChannelListener(this);
                    callback.done(new IOException("Channel closed.", error));
                }
            }
            @Override
            public void congestionLevel(int level) {}
            @Override
            public void onChannelOpened() {}
        };
        fChannel.addChannelListener(channelListener);

        channelExecute(new DataCallback<V>(callback) {
            @Override
            protected void handleCompleted() {
               if (!done.getAndSet(true)) {
                   fChannel.removeChannelListener(channelListener);
                   callback.done(getData(), getError());
               }
            }
        });

    }

    abstract protected void channelExecute(final DataCallback<V> callback);
}
