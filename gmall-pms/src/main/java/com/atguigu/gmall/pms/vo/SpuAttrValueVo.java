package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class SpuAttrValueVo extends SpuAttrValueEntity {

    /**
     * 页面传递的json属性名：valueSelected
     * 对象属性名：attrValue
     * @param valueSelected
     */
    public void setValueSelected(List<String> valueSelected){

        if (!CollectionUtils.isEmpty(valueSelected)){

            this.setAttrValue(StringUtils.join(valueSelected, ","));
        }
    }
}
