package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {

    @Autowired
    private RestHighLevelClient client;

    private PageResult handleResponse(SearchResponse response) {
        //        4.解析结果
        SearchHits searchHits = response.getHits();
//        4.1 查询的总条数
        long total = searchHits.getTotalHits().value;
        System.out.println("共搜索到：" + total + "条数据");
//        4.2 文档数组
        SearchHit[] hits = searchHits.getHits();
//        4.3 遍历
        ArrayList<HotelDoc> hotelDocs = new ArrayList<>();
        for (SearchHit hit : hits) {
//            获取文档source
            String json = hit.getSourceAsString();
//            反序列化
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
//            获取高亮结果
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (!CollectionUtils.isEmpty(highlightFields)) {
                //            根据字段名获取高亮结果
                HighlightField highlightField = highlightFields.get("name");
                if (highlightField != null) {
                    //            获取高亮值
                    String name = highlightField.getFragments()[0].string();
                    //            覆盖非高亮结果
                    hotelDoc.setName(name);
                }
            }
//            获取排序值
            Object[] sortValues = hit.getSortValues();
            if (sortValues.length > 0) {
                Object sortValue = sortValues[0];
                hotelDoc.setDistance(sortValue);
            }
            hotelDocs.add(hotelDoc);
        }

        return new PageResult(total, hotelDocs);
    }

    @Override
    public PageResult search(RequestParams params) {
        try {
//        1.准备request
            SearchRequest request = new SearchRequest("hotel");
//        2.准备dsl
//        2.1query
            buildBasicQuery(params, request);

//        2.2分页
            Integer page = params.getPage();
            Integer size = params.getSize();
            request.source().from((page - 1) * size).size(size);

//          2.3排序
            String location = params.getLocation();
            if (location != null && !location.equals("")) {
                request.source()
                        .sort(SortBuilders.geoDistanceSort("location", new GeoPoint(location))
                                .order(SortOrder.ASC)
                                .unit(DistanceUnit.KILOMETERS));
            }

//        3.发送请求,得到相应
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
//        4.解析响应
            return handleResponse(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Map<String, List<String>> filters(RequestParams params) {
        try {
//        1.准备request
            SearchRequest request = new SearchRequest("hotel");
//        2.准备DSL
            buildBasicQuery(params, request);
//        2.1设置size
            request.source().size(0);
//        2.2聚合
            buildAggregation(request);
//        3.发出请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
//        4.解析结果
            Map<String, List<String>> result = new HashMap<>();
            Aggregations aggregations = response.getAggregations();
//        4.1 根据品牌名称，获取品牌结果
            List<String> brandList = getAggByName(aggregations, "brandAgg");
            result.put("品牌", brandList);
            List<String> cityList = getAggByName(aggregations, "cityAgg");
            result.put("城市", cityList);
            List<String> startList = getAggByName(aggregations, "starAgg");
            result.put("星级", startList);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private List<String> getAggByName(Aggregations aggregations, String aggName) {
        //        4.1根据聚合名称获取聚合结果
        Terms brandTerms = aggregations.get(aggName);
//        4.2 获取buckets
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
//        4.3 遍历
        List<String> brandList = new ArrayList<>();
        for (Terms.Bucket bucket : buckets) {
//            获取key，也就是品牌信息
            String key = bucket.getKeyAsString();
            brandList.add(key);
        }
        return brandList;
    }

    private void buildAggregation(SearchRequest request) {
        request.source().aggregation(
                AggregationBuilders
                        .terms("brandAgg").
                        field("brand").
                        size(100));
        request.source().aggregation(
                AggregationBuilders
                        .terms("cityAgg").
                        field("city").
                        size(100));
        request.source().aggregation(
                AggregationBuilders
                        .terms("starAgg").
                        field("starName").
                        size(100));
    }

    private void buildBasicQuery(RequestParams params, SearchRequest request) {
        //            构建booleanQuery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

//            关键字搜索
        String key = params.getKey();
        if (key == null || "".equals(key)) {
            boolQuery.must(QueryBuilders.matchAllQuery());
        } else {
            boolQuery.must(QueryBuilders.matchQuery("all", key));
        }
//            城市条件
        if (params.getCity() != null && !params.getCity().equals("")) {
            boolQuery.filter(QueryBuilders.termQuery("city", params.getCity()));
        }
        //            品牌条件
        if (params.getBrand() != null && !params.getBrand().equals("")) {
            boolQuery.filter(QueryBuilders.termQuery("brand", params.getBrand()));
        }
        //            星级条件
        if (params.getStarName() != null && !params.getStarName().equals("")) {
            boolQuery.filter(QueryBuilders.termQuery("brand", params.getStarName()));
        }
//            价格
        if (params.getMinPrice() != null && params.getMaxPrice() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price").gte(params.getMinPrice()).lte(params.getMaxPrice()));
        }

//        算分控制
        FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.
                functionScoreQuery(
//                        原始查询，相关性算分的查询
                        boolQuery,
//                        function score的数组
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
//                                其中一个function score元素
                                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
//                                        过滤条件
                                        QueryBuilders.termQuery("isAD", true),
//                                        算分函数
                                        ScoreFunctionBuilders.weightFactorFunction(10)
                                )
                        });

        request.source().query(functionScoreQuery);
    }
}
