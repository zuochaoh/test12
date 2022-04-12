package com.ruoyi.web.controller.system;

import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.KLineData;
import com.ruoyi.system.service.IKLineDataService;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * K线数据Controller
 * 
 * @author huangzc
 * @date 2022-02-08
 */
@Controller
@RequestMapping("/system/data")
public class KLineDataController extends BaseController
{
    private String prefix = "system/data";

    @Autowired
    private IKLineDataService kLineDataService;

    @RequiresPermissions("system:data:view")
    @GetMapping()
    public String data()
    {
        return prefix + "/data";
    }

    /**
     * 查询K线数据列表
     */
    @RequiresPermissions("system:data:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(KLineData kLineData)
    {
        startPage();
        List<KLineData> list = kLineDataService.selectKLineDataList(kLineData);
        return getDataTable(list);
    }

    /**
     * 导出K线数据列表
     */
    @RequiresPermissions("system:data:export")
    @Log(title = "K线数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(KLineData kLineData)
    {
        List<KLineData> list = kLineDataService.selectKLineDataList(kLineData);
        ExcelUtil<KLineData> util = new ExcelUtil<KLineData>(KLineData.class);
        return util.exportExcel(list, "K线数据数据");
    }

    /**
     * 新增K线数据
     */
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    /**
     * 新增保存K线数据
     */
    @RequiresPermissions("system:data:add")
    @Log(title = "K线数据", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(KLineData kLineData)
    {
        return toAjax(kLineDataService.insertKLineData(kLineData));
    }

    /**
     * 修改K线数据
     */
    @RequiresPermissions("system:data:edit")
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Integer id, ModelMap mmap)
    {
        KLineData kLineData = kLineDataService.selectKLineDataById(id);
        mmap.put("kLineData", kLineData);
        return prefix + "/edit";
    }

    /**
     * 修改保存K线数据
     */
    @RequiresPermissions("system:data:edit")
    @Log(title = "K线数据", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(KLineData kLineData)
    {
        return toAjax(kLineDataService.updateKLineData(kLineData));
    }

    /**
     * 删除K线数据
     */
    @RequiresPermissions("system:data:remove")
    @Log(title = "K线数据", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(kLineDataService.deleteKLineDataByIds(ids));
    }
}
