package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.KLineData;

/**
 * K线数据Service接口
 * 
 * @author huangzc
 * @date 2022-02-08
 */
public interface IKLineDataService 
{
    /**
     * 查询K线数据
     * 
     * @param id K线数据主键
     * @return K线数据
     */
    public KLineData selectKLineDataById(Integer id);

    /**
     * 查询K线数据列表
     * 
     * @param kLineData K线数据
     * @return K线数据集合
     */
    public List<KLineData> selectKLineDataList(KLineData kLineData);

    /**
     * 新增K线数据
     * 
     * @param kLineData K线数据
     * @return 结果
     */
    public int insertKLineData(KLineData kLineData);

    /**
     * 修改K线数据
     * 
     * @param kLineData K线数据
     * @return 结果
     */
    public int updateKLineData(KLineData kLineData);

    /**
     * 批量删除K线数据
     * 
     * @param ids 需要删除的K线数据主键集合
     * @return 结果
     */
    public int deleteKLineDataByIds(String ids);

    /**
     * 删除K线数据信息
     * 
     * @param id K线数据主键
     * @return 结果
     */
    public int deleteKLineDataById(Integer id);
}
