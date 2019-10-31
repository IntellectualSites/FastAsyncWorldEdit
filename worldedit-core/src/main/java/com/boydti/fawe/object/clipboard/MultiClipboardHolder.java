package com.boydti.fawe.object.clipboard;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.session.ClipboardHolder;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class MultiClipboardHolder extends URIClipboardHolder {
    private final List<URIClipboardHolder> holders;
    private Clipboard[] cached;

    public MultiClipboardHolder() {
        this(URI.create(""));
    }

    public MultiClipboardHolder(URI uri) {
        super(uri, EmptyClipboard.INSTANCE);
        holders = new ArrayList<>();
    }

    public MultiClipboardHolder(URI uri, URIClipboardHolder... addHolders) {
        this(uri);
        for (URIClipboardHolder h : addHolders) add(h);
    }

    public MultiClipboardHolder(Clipboard clipboard) {
        super(URI.create(""), EmptyClipboard.INSTANCE);
        holders = new ArrayList<>();
        URI uri = URI.create("");
        if (clipboard.getURI() != null) {
            uri = clipboard.getURI();
        }
        add(uri, clipboard);
    }

    public void remove(URI uri) {
        cached = null;
        if (getUri().equals(uri)) {
            for (ClipboardHolder holder : holders) holder.close();
            holders.clear();
            return;
        }
        for (int i = holders.size() - 1; i >= 0; i--) {
            URIClipboardHolder holder = holders.get(i);
            if (holder.contains(uri)) {
                if (holder instanceof MultiClipboardHolder) {
                    ((MultiClipboardHolder) holder).remove(uri);
                } else {
                    holders.remove(i).close();
                }
            }
        }
    }

    @Override
    public URI getURI(Clipboard clipboard) {
        for (ClipboardHolder holder : getHolders()) {
            if (holder instanceof URIClipboardHolder) {
                URIClipboardHolder uriHolder = (URIClipboardHolder) holder;
                URI uri = uriHolder.getURI(clipboard);
                if (uri != null) return uri;
            }
        }
        return null;
    }

    public void add(URIClipboardHolder holder) {
        add((ClipboardHolder) holder);
    }

    @Override
    public boolean contains(Clipboard clipboard) {
        for (ClipboardHolder holder : holders) {
            if (holder.contains(clipboard)) return true;
        }
        return false;
    }

    @Deprecated
    public void add(ClipboardHolder holder) {
        checkNotNull(holder);
        if (holder instanceof URIClipboardHolder) {
            holders.add((URIClipboardHolder) holder);
        } else {
            URI uri = URI.create(UUID.randomUUID().toString());
            if (!contains(uri)) {
                holders.add(new URIClipboardHolder(uri, holder.getClipboard()));
            }
        }
        cached = null;
    }

    public void add(URI uri, Clipboard clip) {
        checkNotNull(clip);
        checkNotNull(uri);
        add(new URIClipboardHolder(uri, clip));
    }

    @Override
    public List<Clipboard> getClipboards() {
        ArrayList<Clipboard> all = new ArrayList<>();
        for (ClipboardHolder holder : holders) {
            all.addAll(holder.getClipboards());
        }
        return all;
    }

    @Override
    public List<ClipboardHolder> getHolders() {
        ArrayList<ClipboardHolder> holders = new ArrayList<>();
        for (ClipboardHolder holder : this.holders) {
            holders.addAll(holder.getHolders());
        }
        return holders;
    }

    @Override
    public boolean contains(URI uri) {
        if (getUri().equals(uri)) {
            return true;
        }
        for (URIClipboardHolder uch : holders) {
            if (uch.contains(uri)) return true;
        }
        return false;
    }

    @Override
    public Clipboard getClipboard() {
        Clipboard[] available = cached;
        if (available == null) {
            cached = available = getClipboards().toArray(new Clipboard[0]);
        }
        switch (available.length) {
            case 0: return EmptyClipboard.INSTANCE;
            case 1: return available[0];
        }

        int index = ThreadLocalRandom.current().nextInt(available.length);
        return available[index];
    }

    @Override
    public Set<URI> getURIs() {
        Set<URI> set = new HashSet<>();
        for (ClipboardHolder holder : getHolders()) {
            if (holder instanceof URIClipboardHolder) {
                URI uri = ((URIClipboardHolder) holder).getUri();
                if (!uri.toString().isEmpty()) set.add(uri);
            }
        }
        return set;
    }

    @Override
    public void close() {
        cached = null;
        for (ClipboardHolder holder : holders) {
            holder.close();
        }
    }
}
