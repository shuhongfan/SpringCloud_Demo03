package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
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
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.xcontent.XContentType;
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
        //        4.????????????
        SearchHits searchHits = response.getHits();
//        4.1 ??????????????????
        long total = searchHits.getTotalHits().value;
        System.out.println("???????????????" + total + "?????????");
//        4.2 ????????????
        SearchHit[] hits = searchHits.getHits();
//        4.3 ??????
        ArrayList<HotelDoc> hotelDocs = new ArrayList<>();
        for (SearchHit hit : hits) {
//            ????????????source
            String json = hit.getSourceAsString();
//            ????????????
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
//            ??????????????????
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (!CollectionUtils.isEmpty(highlightFields)) {
                //            ?????????????????????????????????
                HighlightField highlightField = highlightFields.get("name");
                if (highlightField != null) {
                    //            ???????????????
                    String name = highlightField.getFragments()[0].string();
                    //            ?????????????????????
                    hotelDoc.setName(name);
                }
            }
//            ???????????????
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
//        1.??????request
            SearchRequest request = new SearchRequest("hotel");
//        2.??????dsl
//        2.1query
            buildBasicQuery(params, request);

//        2.2??????
            Integer page = params.getPage();
            Integer size = params.getSize();
            request.source().from((page - 1) * size).size(size);

//          2.3??????
            String location = params.getLocation();
            if (location != null && !location.equals("")) {
                request.source()
                        .sort(SortBuilders.geoDistanceSort("location", new GeoPoint(location))
                                .order(SortOrder.ASC)
                                .unit(DistanceUnit.KILOMETERS));
            }

//        3.????????????,????????????
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
//        4.????????????
            return handleResponse(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Map<String, List<String>> filters(RequestParams params) {
        try {
//        1.??????request
            SearchRequest request = new SearchRequest("hotel");
//        2.??????DSL
            buildBasicQuery(params, request);
//        2.1??????size
            request.source().size(0);
//        2.2??????
            buildAggregation(request);
//        3.????????????
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
//        4.????????????
            Map<String, List<String>> result = new HashMap<>();
            Aggregations aggregations = response.getAggregations();
//        4.1 ???????????????????????????????????????
            List<String> brandList = getAggByName(aggregations, "brandAgg");
            result.put("brand", brandList);
            List<String> cityList = getAggByName(aggregations, "cityAgg");
            result.put("city", cityList);
            List<String> startList = getAggByName(aggregations, "starAgg");
            result.put("starName", startList);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<String> getSuggestion(String prefix) {
        try {
            //        1.??????request
            SearchRequest request = new SearchRequest("hotel");
//        2.??????DSL
            request.source().suggest(
                    new SuggestBuilder().addSuggestion(
                            "suggestions",
                            SuggestBuilders.completionSuggestion("suggestion")
                                    .prefix(prefix)
                                    .skipDuplicates(true)
                                    .size(10)
                    )
            );
//        3.????????????
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
//        4.????????????
            Suggest suggest = response.getSuggest();
//        4.1 ??????????????????????????????????????????
            CompletionSuggestion suggestions = suggest.getSuggestion("suggestions");
//        4.2 ??????options
            List<CompletionSuggestion.Entry.Option> options = suggestions.getOptions();
//        4.3 ??????
            List<String> list = new ArrayList<>();
            for (CompletionSuggestion.Entry.Option option : options) {
                String text = option.getText().toString();
                list.add(text);
            }
            return list;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void deleteById(Long id) {
        try {
            client.delete(new DeleteRequest("hotel", String.valueOf(id)), RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void insertById(Long id) {
        try {
//        1.??????request
            Hotel hotel = getById(id);
            //        ?????????????????????
            HotelDoc hotelDoc = new HotelDoc(hotel);

//        1.??????Request
            IndexRequest request = new IndexRequest("hotel").id(hotel.getId().toString());
//        2.??????Json??????
            request.source(JSON.toJSONString(hotelDoc), XContentType.JSON);
//        3.????????????
            client.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    private List<String> getAggByName(Aggregations aggregations, String aggName) {
        //        4.1????????????????????????????????????
        Terms brandTerms = aggregations.get(aggName);
//        4.2 ??????buckets
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
//        4.3 ??????
        List<String> brandList = new ArrayList<>();
        for (Terms.Bucket bucket : buckets) {
//            ??????key????????????????????????
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
        //            ??????booleanQuery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

//            ???????????????
        String key = params.getKey();
        if (key == null || "".equals(key)) {
            boolQuery.must(QueryBuilders.matchAllQuery());
        } else {
            boolQuery.must(QueryBuilders.matchQuery("all", key));
        }
//            ????????????
        if (params.getCity() != null && !params.getCity().equals("")) {
            boolQuery.filter(QueryBuilders.termQuery("city", params.getCity()));
        }
        //            ????????????
        if (params.getBrand() != null && !params.getBrand().equals("")) {
            boolQuery.filter(QueryBuilders.termQuery("brand", params.getBrand()));
        }
        //            ????????????
        if (params.getStarName() != null && !params.getStarName().equals("")) {
            boolQuery.filter(QueryBuilders.termQuery("brand", params.getStarName()));
        }
//            ??????
        if (params.getMinPrice() != null && params.getMaxPrice() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price").gte(params.getMinPrice()).lte(params.getMaxPrice()));
        }

//        ????????????
        FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.
                functionScoreQuery(
//                        ???????????????????????????????????????
                        boolQuery,
//                        function score?????????
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
//                                ????????????function score??????
                                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
//                                        ????????????
                                        QueryBuilders.termQuery("isAD", true),
//                                        ????????????
                                        ScoreFunctionBuilders.weightFactorFunction(10)
                                )
                        });

        request.source().query(functionScoreQuery);
    }
}
