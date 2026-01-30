package com.arbergashi.charts.core.nativeapi;

interface CommandStreamWriter {
    boolean putByte(int value);
    void putInt(int value);
    void putFloat(float value);
    void putShort(short value);
    void putBytes(byte[] data, int length);
    void putIntAt(int offset, int value);
    int position();
}
