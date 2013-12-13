package org.makersoft.shards.feature;

/**
 * Created by yanye.lj on 13-12-13.
 */
public class Feature {
    private static final long OFF_STATUS = 1;
    private static final long ON_STATUS = 0;
    private static final int INDEX_MAX = 62;
    private static final int INDEX_MIN = 0;
    private static final int DEFAULT_VALUE = 0;

    /**
     * 按照二进制状态位表示开关，0表示打开，1表示关闭
     */
    private volatile long value = DEFAULT_VALUE;

    public Feature() {
       on(FeatureEnum.HORIZON_SPLIT.getIndex());
    }

    private void indexCheck(int index){
        if(index < INDEX_MIN || index > INDEX_MAX)
            throw new RuntimeException(String.format("value[%s] out of range.", index));
    }

    public boolean isOn(int index) {
        indexCheck(index);
        return 0 == (value & (OFF_STATUS << index));
    }

    public boolean isOff(int index) {
        return !isOn(index);
    }

    /**
     * 打开某个状态位，即设置index对应的位为0
     *
     * @param index
     */
    public synchronized void on(int index) {
        indexCheck(index);
        value &= ~(OFF_STATUS << index);
    }

    /**
     * 关闭某个状态位，即设置index对应的位为1
     *
     * @param index
     */
    public synchronized void off(int index) {
        indexCheck(index);
        value |= (OFF_STATUS << index);
    }
}
