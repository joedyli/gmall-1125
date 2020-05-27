package com.atguigu.gmall.search.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.bean.Goods;
import com.atguigu.gmall.search.bean.SearchParam;
import com.atguigu.gmall.search.bean.SearchResponseVo;
import com.atguigu.gmall.search.bean.SearchResposneAttrVo;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    public SearchResponseVo search(SearchParam searchParam) {
        try {
            SearchResponse searchResponse = this.restHighLevelClient.search(new SearchRequest(new String[]{"goods"}, buildDSL(searchParam)), RequestOptions.DEFAULT);

            SearchResponseVo responseVo = parseResult(searchResponse);
            responseVo.setPageNum(searchParam.getPageNum());
            responseVo.setPageSize(searchParam.getPageSize());
            return responseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解析搜索的响应结果集
     * @param searchResponse
     * @return
     */
    private SearchResponseVo parseResult(SearchResponse searchResponse){
        SearchResponseVo responseVo = new SearchResponseVo();

        // 1.分页数据
        // 获取外层的hits
        SearchHits hits = searchResponse.getHits();
        responseVo.setTotal(hits.getTotalHits());

        // 2.当前页数据 内层hists
        SearchHit[] hitsHits = hits.getHits();
        List<Goods> goodsList = Stream.of(hitsHits).map(hit -> {
            // 获取hits中每一个元素的_source
            String sourceAsString = hit.getSourceAsString();
            // 反序列化为goods对象
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);

            // 获取高亮结果集，覆盖普通的标题
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            HighlightField highlightField = highlightFields.get("title");
            String highlightTitle = highlightField.getFragments()[0].toString();
            goods.setTitle(highlightTitle);

            return goods;
        }).collect(Collectors.toList());
        responseVo.setGoodsList(goodsList);

        // 3.聚合数据
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();

        // 3.1. 品牌聚合数据，获取了品牌的聚合结果集
        ParsedLongTerms brandIdAgg = (ParsedLongTerms)aggregationMap.get("brandIdAgg");
        // 获取品牌的桶信息
        List<? extends Terms.Bucket> buckets = brandIdAgg.getBuckets();
        // 判断桶集合是否为空，不为空，解析成品牌集合
        if (!CollectionUtils.isEmpty(buckets)){
            List<BrandEntity> brandEntities = buckets.stream().map(bucket -> {
                BrandEntity brandEntity = new BrandEntity();
                // 从桶中获取key，key就是brandId
                Long brandId = ((Terms.Bucket) bucket).getKeyAsNumber().longValue();
                brandEntity.setId(brandId);

                // 获取桶中子聚合
                Map<String, Aggregation> brandAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();

                // 获取品牌名称的子聚合
                ParsedStringTerms brandNameAgg = (ParsedStringTerms)brandAggregationMap.get("brandNameAgg");
                // 一个品牌id对应的品牌名称肯定有且仅有一个
                brandEntity.setName(brandNameAgg.getBuckets().get(0).getKeyAsString());

                // 获取品牌logo的子聚合
                ParsedStringTerms logoAgg = (ParsedStringTerms)brandAggregationMap.get("logoAgg");
                // 获取logo子聚合中的桶
                List<? extends Terms.Bucket> logoAggBuckets = logoAgg.getBuckets();
                // 判断logo桶集合是否为空，不为空，获取第一个
                if (!CollectionUtils.isEmpty(logoAggBuckets)){
                    brandEntity.setLogo(logoAggBuckets.get(0).getKeyAsString());
                }
                return brandEntity;
            }).collect(Collectors.toList());
            responseVo.setBrands(brandEntities);
        }

        // 3.2. 分类聚合
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms)aggregationMap.get("categoryIdAgg");
        List<? extends Terms.Bucket> cateBuckets = categoryIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(cateBuckets)){
            List<CategoryEntity> categoryEntities = cateBuckets.stream().map(bucket -> {
                CategoryEntity categoryEntity = new CategoryEntity();
                Long categoryId = ((Terms.Bucket) bucket).getKeyAsNumber().longValue();
                categoryEntity.setId(categoryId);

                // 获取桶中的分类名称子聚合
                ParsedStringTerms categoryNameAgg = (ParsedStringTerms)((Terms.Bucket) bucket).getAggregations().get("categoryNameAgg");
                // 有分类id，有且仅有一个分类名称
                categoryEntity.setName(categoryNameAgg.getBuckets().get(0).getKeyAsString());
                return categoryEntity;
            }).collect(Collectors.toList());
            responseVo.setCategories(categoryEntities);
        }

        // 3.3. 搜索属性聚合，获取外层的嵌套聚合
        ParsedNested attrAgg = (ParsedNested)aggregationMap.get("attrAgg");
        // 获取了内层attrIdAgg聚合
        ParsedLongTerms attrIdAgg = (ParsedLongTerms)attrAgg.getAggregations().get("attrIdAgg");
        // 获取规格参数聚合的桶
        List<? extends Terms.Bucket> attrBuckets = attrIdAgg.getBuckets();
        // 判断桶是否为空
        if (!CollectionUtils.isEmpty(attrBuckets)){
            // 把桶集合转化为SearchResposneAttrVo集合
            List<SearchResposneAttrVo> filters = attrBuckets.stream().map(bucket -> {
                SearchResposneAttrVo searchResposneAttrVo = new SearchResposneAttrVo();

                // 桶的key就是规格参数id
                searchResposneAttrVo.setAttrId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());

                // 获取规格参数的所有子聚合map
                Map<String, Aggregation> attrAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();

                // 从子聚合map中获取规格参数名子聚合
                ParsedStringTerms attrNameAgg = (ParsedStringTerms)attrAggregationMap.get("attrNameAgg");
                searchResposneAttrVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());

                // 从子聚合map中获取规格参数可选值的子聚合
                ParsedStringTerms attrValueAgg = (ParsedStringTerms)attrAggregationMap.get("attrValueAgg");
                List<? extends Terms.Bucket> attrValueBuckets = attrValueAgg.getBuckets();
                if (!CollectionUtils.isEmpty(attrValueBuckets)){
                    List<String> attrValues = attrValueBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                    searchResposneAttrVo.setAttrValues(attrValues);
                }
                return searchResposneAttrVo;
            }).collect(Collectors.toList());
            responseVo.setFilters(filters);
        }

        return responseVo;
    }

    /**
     * 构建DSL查询条件
     * @param searchParam
     * @return
     */
    private SearchSourceBuilder buildDSL(SearchParam searchParam){
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        String keyword = searchParam.getKeyword();
        if (StringUtils.isBlank(keyword)){
            // 打广告
            return sourceBuilder;
        }

        // 1.构建查询条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        sourceBuilder.query(boolQueryBuilder);

        // 1.1. 匹配查询
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword).operator(Operator.AND));

        // 1.2. 过滤
        // 1.2.1. 品牌过滤
        List<Long> brandIds = searchParam.getBrandId();
        if (!CollectionUtils.isEmpty(brandIds)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brandIds));
        }

        // 1.2.2. 分类过滤
        List<Long> cids = searchParam.getCid();
        if (!CollectionUtils.isEmpty(cids)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId", cids));
        }

        // 1.2.3. 规格参数过滤
        List<String> props = searchParam.getProps();
        if (!CollectionUtils.isEmpty(props)){
            props.forEach(prop -> {
                String[] attr = StringUtils.split(prop, ":");
                if (attr != null && attr.length == 2){
                    String attrId = attr[0];
                    String[] attrValues = StringUtils.split(attr[1], "-");
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId", attrId));
                    boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue", attrValues));
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs", boolQuery, ScoreMode.None));
                }
            });
        }

        // 1.2.4. 价格区间过滤
        Integer priceFrom = searchParam.getPriceFrom();
        Integer priceTo = searchParam.getPriceTo();
        if (priceFrom != null || priceTo != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            boolQueryBuilder.filter(rangeQuery);
            if (priceFrom != null){
                rangeQuery.gte(priceFrom);
            }
            if (priceTo != null){
                rangeQuery.lte(priceTo);
            }
        }

        // 1.2.5. 是否有货过滤
        Boolean store = searchParam.getStore();
        if (store != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("store", store));
        }

        // 2.构建排序条件
        Integer sort = searchParam.getSort();
        if (sort != null) {
            String field = "_score";
            SortOrder sortOrder = SortOrder.DESC;
            // 1-价格降序 2-价格升序 3-销量降序 4-新品降序
            switch (sort){
                case 1: field = "price"; break;
                case 2: field = "price"; sortOrder=SortOrder.ASC; break;
                case 3: field = "sales"; break;
                case 4: field = "createTime"; break;
            }
            sourceBuilder.sort(field, sortOrder);
        }

        // 3.构建分页条件
        Integer pageNum = searchParam.getPageNum();
        Integer pageSize = searchParam.getPageSize();
        sourceBuilder.from((pageNum - 1) * pageSize);
        sourceBuilder.size(pageSize);

        // 4.构建高亮条件
        sourceBuilder.highlighter(
                new HighlightBuilder()
                        .field("title")
                        .preTags("<font style='color:red'>")
                        .postTags("</font>")
        );

        // 5.构建聚合条件
        // 5.1. 品牌聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo"))
        );

        // 5.2. 分类聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName"))
        );

        // 5.3. 规格参数聚合
        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "searchAttrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue"))
                )
        );

        // 6.结果集过滤
        sourceBuilder.fetchSource(new String[]{"skuId", "title", "subTitle", "defaultImage", "price"}, null);

        System.out.println(sourceBuilder.toString());
        return sourceBuilder;
    }
}
