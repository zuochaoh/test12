package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * K线数据对象 k_line_data
 * 
 * @author huangzc
 * @date 2022-02-08
 */
public class KLineData extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Integer id;

    /**  */
    @Excel(name = "")
    private Double data1;

    /**  */
    @Excel(name = "")
    private Double data2;

    /**  */
    @Excel(name = "")
    private Double data3;

    /**  */
    @Excel(name = "")
    private Double data4;

    public void setId(Integer id) 
    {
        this.id = id;
    }

    public Integer getId() 
    {
        return id;
    }
    public void setData1(Double data1)
    {
        this.data1 = data1;
    }

    public Double getData1()
    {
        return data1;
    }
    public void setData2(Double data2)
    {
        this.data2 = data2;
    }

    public Double getData2()
    {
        return data2;
    }
    public void setData3(Double data3)
    {
        this.data3 = data3;
    }

    public Double getData3()
    {
        return data3;
    }
    public void setData4(Double data4)
    {
        this.data4 = data4;
    }

    public Double getData4()
    {
        return data4;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("data1", getData1())
            .append("data2", getData2())
            .append("data3", getData3())
            .append("data4", getData4())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
