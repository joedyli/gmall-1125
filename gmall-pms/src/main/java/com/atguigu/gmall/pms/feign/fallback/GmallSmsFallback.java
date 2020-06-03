package com.atguigu.gmall.pms.feign.fallback;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GmallSmsFallback implements GmallSmsClient {

    @Override
    public ResponseVo<Object> saveSkuSales(SkuSaleVo skuSaleVo) {
        return ResponseVo.fail("保存营销信息失败");
    }

    @Override
    public ResponseVo<List<ItemSaleVo>> querySaleVosBySkuId(Long skuId) {
        return ResponseVo.fail("查询营销信息失败");
    }
}
