package com.sk89q.worldedit.session;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.transform.Transform;

public class DelegateClipboardHolder extends ClipboardHolder {
    private final ClipboardHolder parent;

    public DelegateClipboardHolder(ClipboardHolder holder) {
        super(holder.getClipboard());
        this.parent = holder;
    }

    @Override
    public Clipboard getClipboard() {
        return parent.getClipboard();
    }

    @Override
    public void setTransform(Transform transform) {
        parent.setTransform(transform);
    }

    @Override
    public Transform getTransform() {
        return parent.getTransform();
    }

    @Override
    public PasteBuilder createPaste(Extent targetExtent) {
        return parent.createPaste(targetExtent);
    }

    @Override
    public void close() {
        parent.close();
    }
}
