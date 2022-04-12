package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.KLineData;

/**
 * K线数据Mapper接口
 * 
 * @author huangzc
 * @date 2022-02-08
 */
public interface KLineDataMapper 
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
     * 删除K线数据
     * 
     * @param id K线数据主键
     * @return 结果
     */
    public int deleteKLineDataById(Integer id);

    /**
     * 批量删除K线数据
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteKLineDataByIds(String[] ids);
}
