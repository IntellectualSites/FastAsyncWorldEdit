package com.boydti.fawe.object.collection;

import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.FaweOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;

/**
 * Records changes made through the {@link #set(int, int, int, char)} method<br/>
 * Changes are not recorded if you edit the raw data
 */
public final class DifferentialBlockBuffer implements DifferentialCollection<char[][][][][]> {

    private final int width, length;
    private final int t1, t2;
    private char[][][][][] data;
    private char[][][][][] changes;

    public DifferentialBlockBuffer(int width, int length) {
        this.width = width;
        this.length = length;
        this.t1 = (length + 15) >> 4;
        this.t2 = (width + 15) >> 4;
    }

    @Override
    public char[][][][][] get() {
        return data;
    }

    @Override
    public void flushChanges(FaweOutputStream out) throws IOException {
        boolean modified = isModified();
        out.writeBoolean(modified);

        if (modified) {
            writeArray(changes, 0, 0, out);
        }
        clearChanges();
    }

    private void writeArray(Object arr, int level, int index, FaweOutputStream out) throws IOException {
        if (level == 4) {
            if (arr != null) {
                int[] level4 = (int[]) arr;
                out.writeVarInt(level4.length);
                for (int c : level4) {
                    out.writeVarInt(c);
                }
            } else {
                out.writeVarInt(0);
            }
        } else {
            int len = arr == null ? 0 : Array.getLength(arr);
            out.writeVarInt(len);
            for (int i = 0; i < len; i++) {
                Object elem = Array.get(arr, i);
                writeArray(elem, level + 1, i, out);
            }
        }
    }

    @Override
    public void undoChanges(FaweInputStream in) throws IOException {
        if (changes != null && changes.length != 0) throw new IllegalStateException("There are uncommitted changes, please flush first");
        boolean modified = in.readBoolean();
        if (modified) {
            int len = in.readVarInt();
            if (len == 0) {
                data = null;
            } else {
                for (int i = 0; i < len; i++) {
                    readArray(data, i, 1, in);
                }
            }
        }

        clearChanges();
    }

    @Override
    public void redoChanges(FaweInputStream in) throws IOException {
        clearChanges();
        throw new UnsupportedOperationException("Not implemented");
    }

    private void readArray(Object dataElem, int index, int level, FaweInputStream in) throws IOException {
        int len = in.readVarInt();
        if (level == 4) {
            int[][] castedElem = (int[][]) dataElem;
            if (len == 0) {
                castedElem[index] = null;
            } else {
                int[] current = castedElem[index];
                for (int i = 0; i < len; i++) {
                    current[i] = in.readVarInt();
                }
            }
        } else {
            if (len == 0) {
                Array.set(dataElem, index, null);
            } else {
                Object nextElem = Array.get(dataElem, index);
                for (int i = 0; i < len; i++) {
                    readArray(nextElem, i, level + 1, in);
                }
            }
        }
    }

    public boolean isModified() {
        return changes != null;
    }

    public void clearChanges() {
        changes = null;
    }

    public void set(int x, int y, int z, char combined) {
        if (combined == 0) combined = 1;
        int localX = x & 15;
        int localZ = z & 15;
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        if (data == null) {
            data = new char[t1][][][][];
            changes = new char[0][][][][];
        }

        char[][][][] arr = data[chunkZ];
        if (arr == null) {
            arr = data[chunkZ] = new char[t2][][][];
        }
        char[][][] arr2 = arr[chunkX];
        if (arr2 == null) {
            arr2 = arr[chunkX] = new char[256][][];
        }

        char[][] yMap = arr2[y];
        if (yMap == null) {
            arr2[y] = yMap = new char[16][];
        }
        boolean newSection;
        int current;
        char[] zMap = yMap[localZ];
        if (zMap == null) {
            yMap[localZ] = zMap = new char[16];

            if (changes == null) {
                changes = new char[t1][][][][];
            } else if (changes != null && changes.length != 0) {
                initialChange(changes, chunkX, chunkZ, localX, localZ, y, (char) -combined);
            }

        } else {
            if (changes == null || changes.length == 0) changes = new char[t1][][][][];
            appendChange(changes, chunkX, chunkZ, localX, localZ, y, (char) (zMap[localX] - combined));
        }

        zMap[localX] = combined;
    }

    private void initialChange(char[][][][][] src, int chunkX, int chunkZ, int localX, int localZ, int y, char combined) {
        char[][][][] arr = src[chunkZ];
        if (arr == null) {
            src[chunkZ] = new char[0][][][];
            return;
        } else if (arr.length == 0) return;

        char[][][] arr2 = arr[chunkX];
        if (arr2 == null) {
            arr[chunkX] = new char[0][][];
            return;
        } else if (arr2.length == 0) return;

        char[][] yMap = arr2[y];
        if (yMap == null) {
            arr2[y] = new char[0][];
            return;
        } else if (yMap.length == 0) return;

        char[] zMap = yMap[localZ];
        if (zMap == null) {
            yMap[localZ] = new char[0];
            return;
        } else if (zMap.length == 0) return;

        int current = zMap[localX];
        zMap[localX] = combined;
    }

    private void appendChange(char[][][][][] src, int chunkX, int chunkZ, int localX, int localZ, int y, char combined) {
        char[][][][] arr = src[chunkZ];
        if (arr == null || arr.length == 0) {
            arr = src[chunkZ] = new char[t2][][][];
        }
        char[][][] arr2 = arr[chunkX];
        if (arr2 == null || arr2.length == 0) {
            arr2 = arr[chunkX] = new char[256][][];
        }

        char[][] yMap = arr2[y];
        if (yMap == null || yMap.length == 0) {
            arr2[y] = yMap = new char[16][];
        }
        char[] zMap = yMap[localZ];
        if (zMap == null || zMap.length == 0) {
            yMap[localZ] = zMap = new char[16];
        }
        zMap[localX] = combined;
    }
}
