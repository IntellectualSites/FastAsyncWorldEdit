package com.boydti.fawe.object.clipboard;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.session.ClipboardHolder;
import java.net.URI;
import java.util.Collections;
import java.util.Set;


import static com.google.common.base.Preconditions.checkNotNull;

public class URIClipboardHolder extends ClipboardHolder {
    private final URI uri;

    public URIClipboardHolder(URI uri, Clipboard clipboard) {
        super(clipboard);
        checkNotNull(uri);
        this.uri = uri;
    }

    public boolean contains(URI uri) {
        checkNotNull(uri);
        return this.uri.equals(uri);
    }

    /**
     * @deprecated If a holder has multiple sources, this will return an empty URI
     * @return The original source of this clipboard (usually a file or url)
     */
    @Deprecated
    public URI getUri() {
        return uri;
    }

    public Set<URI> getURIs() {
        return Collections.singleton(uri);
    }

    public URI getURI(Clipboard clipboard) {
        return getClipboard() == clipboard ? getUri() : null;
    }
}
