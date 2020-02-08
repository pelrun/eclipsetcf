/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems and others.
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
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.tcf.debug.test.services.ResetMap.IResettable;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;

/**
 *
 */
public abstract class TokenCache<V> extends AbstractCache<V> implements IResettable {

    private final IChannel fChannel;
    private AtomicReference<IToken> fToken = new AtomicReference<IToken>();
    private IChannel.IChannelListener fChannelListener = new IChannel.IChannelListener() {
        @Override
        public void onChannelClosed(Throwable error) {
            if (!isValid()) {
                set(null, new IOException("Channel closed.", error), true);
            }
        }
        @Override
        public void congestionLevel(int level) {}
        @Override
        public void onChannelOpened() {}
    };

    public TokenCache(IChannel channel) {
        fChannel = channel;
    }

    private void addChannelListener() {
        fChannel.addChannelListener(fChannelListener);
    }

    private void removeChannelListener() {
        fChannel.removeChannelListener(fChannelListener);
    }

    @Override
    final protected void retrieve() {
        IToken previous = fToken.getAndSet(retrieveToken());
        if (previous == null) {
            addChannelListener();
        }
    }

    protected boolean checkToken(IToken token) {
        boolean tokenMatches = fToken.compareAndSet(token, null);
        if (tokenMatches) {
            removeChannelListener();
        }
        return tokenMatches;
    }

    abstract protected IToken retrieveToken();

    protected void set(IToken token, V data, Throwable error) {
        if (checkToken(token) ) {
            set(data, error, true);
        }
    }

    @Override
    public void set(V data, Throwable error, boolean valid) {
        super.set(data, error, valid);
        // If new value was set to the cache but a command is still
        // outstanding.  Cancel the command.
        IToken token = fToken.getAndSet(null);
        if (token != null) {
            token.cancel();
            removeChannelListener();
        }
    }

    @Override
    protected void canceled() {
        IToken token = fToken.getAndSet(null);
        token.cancel();
        removeChannelListener();
    }
}
