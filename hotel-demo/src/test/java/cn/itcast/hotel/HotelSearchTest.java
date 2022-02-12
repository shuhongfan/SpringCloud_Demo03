package cn.itcast.hotel;

import cn.itcast.hotel.pojo.HotelDoc;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class HotelSearchTest {
    private RestHighLevelClient client;

    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.21.129:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }

    private void handleResponse(SearchResponse response) {
        //        4.解析结果
        SearchHits searchHits = response.getHits();
//        4.1 查询的总条数
        long total = searchHits.getTotalHits().value;
        System.out.println("共搜索到：" + total + "条数据");
//        4.2 文档数组
        SearchHit[] hits = searchHits.getHits();
//        4.3 遍历
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
            System.out.println("hotelDoc=" + hotelDoc);
        }

    }

    @Test
    void testMatchAll() throws IOException {
//        1.准备request
        SearchRequest request = new SearchRequest("hotel");

//        2.准备DSL
        request.source().query(QueryBuilders.matchAllQuery());
//        3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        System.out.println(response);

//        4.解析结果
        handleResponse(response);
    }

    @Test
    void testMatch() throws IOException {
//        1.准备request
        SearchRequest request = new SearchRequest("hotel");

//        2.准备DSL
        request.source().query(QueryBuilders.matchQuery("all", "如家"));
//        3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        System.out.println(response);

        handleResponse(response);
    }

    @Test
    void testBool() throws IOException {
//        1.准备request
        SearchRequest request = new SearchRequest("hotel");

//        2.准备DSL
//        2.1准备BooleanQuery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
//        2.2添加term
        boolQuery.must(QueryBuilders.termQuery("city", "上海"));
//        2.3添加range
        boolQuery.filter(QueryBuilders.rangeQuery("price").lte(250));
        request.source().query(boolQuery);
//        3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        System.out.println(response);

        handleResponse(response);
    }

    @Test
    void testPageAndSort() throws IOException {
//        页码，每页大小
        int page = 2, size = 5;

//        1.准备request
        SearchRequest request = new SearchRequest("hotel");

//        2.准备DSL
//        2.1query
        request.source().query(QueryBuilders.matchAllQuery());
//        2.2排序
        request.source().sort("price", SortOrder.ASC);
//        2.3分页 from size
        request.source().from((page - 1) * size).size(5);
//        3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        handleResponse(response);
    }

    @Test
    void testHilight() throws IOException {
//        1.准备request
        SearchRequest request = new SearchRequest("hotel");

//        2.准备DSL
        request.source().query(QueryBuilders.matchQuery("all", "如家"));
        request.source().highlighter(new HighlightBuilder().field("name").requireFieldMatch(false));
//        3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        System.out.println(response);

        handleResponse(response);
    }

    @Test
    void testAggration() throws IOException {
//        1.准备request
        SearchRequest request = new SearchRequest("hotel");
//        2.准备DSL
//        2.1设置size
        request.source().size(0);
//        2.2聚合
        request.source().aggregation(
                AggregationBuilders
                        .terms("brandAgg").
                        field("brand").
                        size(10));
//        3.发出请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
//        4.解析结果
        System.out.println(response);

        Aggregations aggregations = response.getAggregations();
//        4.1根据聚合名称获取聚合结果
        Terms brandTerms = aggregations.get("brandAgg");
//        4.2 获取buckets
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
//        4.3 遍历
        for (Terms.Bucket bucket : buckets) {
//            获取key，也就是品牌信息
            String key = bucket.getKeyAsString();
            System.out.println(key);
        }
    }
}
