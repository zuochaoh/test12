package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.KLineDataMapper;
import com.ruoyi.system.domain.KLineData;
import com.ruoyi.system.service.IKLineDataService;
import com.ruoyi.common.core.text.Convert;

/**
 * K线数据Service业务层处理
 * 
 * @author huangzc
 * @date 2022-02-08
 */
@Service
public class KLineDataServiceImpl implements IKLineDataService 
{
    @Autowired
    private KLineDataMapper kLineDataMapper;

    /**
     * 查询K线数据
     * 
     * @param id K线数据主键
     * @return K线数据
     */
    @Override
    public KLineData selectKLineDataById(Integer id)
    {
        return kLineDataMapper.selectKLineDataById(id);
    }

    /**
     * 查询K线数据列表
     * 
     * @param kLineData K线数据
     * @return K线数据
     */
    @Override
    public List<KLineData> selectKLineDataList(KLineData kLineData)
    {
        return kLineDataMapper.selectKLineDataList(kLineData);
    }

    /**
     * 新增K线数据
     * 
     * @param kLineData K线数据
     * @return 结果
     */
    @Override
    public int insertKLineData(KLineData kLineData)
    {
        kLineData.setCreateTime(DateUtils.getNowDate());
        return kLineDataMapper.insertKLineData(kLineData);
    }

    /**
     * 修改K线数据
     * 
     * @param kLineData K线数据
     * @return 结果
     */
    @Override
    public int updateKLineData(KLineData kLineData)
    {
        kLineData.setUpdateTime(DateUtils.getNowDate());
        return kLineDataMapper.updateKLineData(kLineData);
    }

    /**
     * 批量删除K线数据
     * 
     * @param ids 需要删除的K线数据主键
     * @return 结果
     */
    @Override
    public int deleteKLineDataByIds(String ids)
    {
        return kLineDataMapper.deleteKLineDataByIds(Convert.toStrArray(ids));
    }

    /**
     * 删除K线数据信息
     * 
     * @param id K线数据主键
     * @return 结果
     */
    @Override
    public int deleteKLineDataById(Integer id)
    {
        return kLineDataMapper.deleteKLineDataById(id);
    }
}
