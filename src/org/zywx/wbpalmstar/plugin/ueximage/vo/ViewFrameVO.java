package org.zywx.wbpalmstar.plugin.ueximage.vo;

public class ViewFrameVO {
    public int x = 0;
    public int y = 0;
    public int width = 0;
    public int height = 0;
    /**
     * 记录一下参数是否为js传入的（从而决定addview时是否乘scale）
     */
    public boolean isWebParam = true;

}
