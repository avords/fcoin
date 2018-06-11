package com.lmm.fcoin;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FcoinUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(FcoinUtils.class);
    private static final String app_key = "42ffbdf4df994f1a8a181350e5b24541";
    private static final String app_secret = "7ae3e81c0e8e47a4b604eeeca39be6ec";
    
    public static String getSign(String data, String secret) throws Exception {
        
        String base64_1 = Base64.getEncoder().encodeToString(data.getBytes("utf-8"));
        SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes("utf-8"), "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(base64_1.getBytes("utf-8"));
        return Base64.getEncoder().encodeToString(rawHmac);
    }
    
    public static String getBalance() throws Exception {
        String url = "https://api.fcoin.com/v2/accounts/balance";
        Long timeStamp = System.currentTimeMillis();
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("FC-ACCESS-KEY",app_key);
        headers.add("FC-ACCESS-SIGNATURE",getSign("GEThttps://api.fcoin.com/v2/accounts/balance"+timeStamp,app_secret));
        headers.add("FC-ACCESS-TIMESTAMP",timeStamp.toString());

        HttpEntity requestEntity = new HttpEntity<>(headers);
        RestTemplate client = new RestTemplate();
        client.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        ResponseEntity<String> response = client.exchange(url, HttpMethod.GET, requestEntity, String.class);
        logger.info(response.getBody());
        return response.getBody();
    }

    public static void buy(String symbol,String type,String amount) throws Exception {
        String side = "buy";
        String url = "https://api.fcoin.com/v2/orders";
        Long timeStamp = System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        headers.add("FC-ACCESS-KEY",app_key);
        headers.add("FC-ACCESS-SIGNATURE",
                getSign("POST"+url+timeStamp+"amount="+amount+"&side="+side+"&symbol="+symbol+"&type="+type,app_secret));
        headers.add("FC-ACCESS-TIMESTAMP",timeStamp.toString());
        MediaType t = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(t);
        headers.setAccept(Collections.singletonList(MediaType.ALL));

        //  封装参数，千万不要替换为Map与HashMap，否则参数无法传递
       Map<String, String> params= new HashMap<>();
        //  也支持中文
        params.put("amount", amount);
        //params.put("price","0.1");
        params.put("side", side);
        params.put("symbol", symbol);
        params.put("type", type);
        HttpEntity<String> requestEntity = new HttpEntity<String>(JSON.toJSONString(params), headers);
        RestTemplate client = new RestTemplate();
        client.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        ResponseEntity<String> response = client.exchange(url, HttpMethod.POST, requestEntity, String.class);
        logger.info(response.getBody());
    }

    public static void sell(String symbol,String type,String amount) throws Exception {
        String side = "sell";
        String url = "https://api.fcoin.com/v2/orders";
        Long timeStamp = System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        headers.add("FC-ACCESS-KEY",app_key);
        headers.add("FC-ACCESS-SIGNATURE",
                getSign("POST"+url+timeStamp+"amount="+amount+"&side="+side+"&symbol="+symbol+"&type="+type,app_secret));
        headers.add("FC-ACCESS-TIMESTAMP",timeStamp.toString());

        MediaType t = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(t);
        headers.setAccept(Collections.singletonList(MediaType.ALL));

        //  封装参数，千万不要替换为Map与HashMap，否则参数无法传递
        Map<String, String> params= new HashMap<>();
        //  也支持中文
        params.put("amount", amount);
        params.put("side", side);
        params.put("symbol", symbol);
        params.put("type", type);
        HttpEntity<String> requestEntity = new HttpEntity<String>(JSON.toJSONString(params), headers);
        RestTemplate client = new RestTemplate();
        client.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        ResponseEntity<String> response = client.exchange(url, HttpMethod.POST, requestEntity, String.class);
        logger.info(response.getBody());
    }

    public static double getFtUsdtPrice() throws Exception {
        String url = "https://api.fcoin.com/v2/market/ticker/ftusdt";
        Long timeStamp = System.currentTimeMillis();
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("FC-ACCESS-KEY",app_key);
        headers.add("FC-ACCESS-SIGNATURE",getSign("GEThttps://api.fcoin.com/v2/market/ticker/ftusdt"+timeStamp,app_secret));
        headers.add("FC-ACCESS-TIMESTAMP",timeStamp.toString());

        HttpEntity requestEntity = new HttpEntity<>(headers);
        RestTemplate client = new RestTemplate();
        client.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        ResponseEntity<String> response = client.exchange(url, HttpMethod.GET, requestEntity, String.class);
        JSONObject jsonObject = JSON.parseObject(response.getBody());
        JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("ticker");
        BigDecimal b = new BigDecimal(jsonArray.get(0).toString()).setScale(3,BigDecimal.ROUND_HALF_UP);
        return b.doubleValue();
    }
    public static String getSymbols() throws Exception {
        String url = "https://api.fcoin.com/v2/public/symbols";
        Long timeStamp = System.currentTimeMillis();
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("FC-ACCESS-KEY",app_key);
        headers.add("FC-ACCESS-SIGNATURE",getSign("GEThttps://api.fcoin.com/v2/accounts/balance"+timeStamp,app_secret));
        headers.add("FC-ACCESS-TIMESTAMP",timeStamp.toString());

        HttpEntity requestEntity = new HttpEntity<>(headers);
        RestTemplate client = new RestTemplate();
        client.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        ResponseEntity<String> response = client.exchange(url, HttpMethod.GET, requestEntity, String.class);
        logger.info(response.getBody());
        return response.getBody();
    }
    
    //ftusdt
    public void ftusdt() throws Exception {
        double marketPrice = getFtUsdtPrice();
        
        while (true) {
            //查询余额
            String balance = getBalance();
            Map<String, Double> balances = buildBalance(balance);
            double ft = balances.get("ft");
            double usdt = balances.get("usdt");
            
            if(usdt<51&&ft<51/marketPrice){
                break;
            }
            //ft:usdt=1:0.6
            if (ft * marketPrice > usdt&&Math.abs(ft * marketPrice -usdt)>10) {
                double half = (ft * marketPrice+usdt)/2;
                //ft太多，需要卖出ft
                BigDecimal b = new BigDecimal((ft * marketPrice - half) / marketPrice).setScale(2, BigDecimal.ROUND_HALF_UP);
                sell("ftusdt", "market", b.toString());
                Thread.sleep(300);
            } else if (ft * marketPrice < usdt&&Math.abs(ft * marketPrice -usdt)>10) {
                double half = (ft * marketPrice+usdt)/2;
                //ft太少，需要买入ft
                BigDecimal b = new BigDecimal((usdt - half) / marketPrice).setScale(2, BigDecimal.ROUND_HALF_UP);
                buy("ftusdt", "market", b.toString());
                Thread.sleep(300);
            }
            //买单 卖单

            buy("ftusdt", "market", String.valueOf(50/marketPrice));
            sell("ftusdt", "market", String.valueOf(50/marketPrice));
            
            Thread.sleep(1000);
        }
    }
    
    private Map<String,Double> buildBalance(String balance) {
        Map<String,Double> map = new HashMap<>();

        JSONObject jsonObject = JSON.parseObject(balance);
        JSONArray jsonArray = jsonObject.getJSONArray("data");
        jsonArray.stream().forEach(jsonObj->{
            JSONObject obj = (JSONObject)jsonObj;
            map.put(obj.getString("currency"),Double.valueOf(obj.getString("balance")));
        });
        
        return map;
    }
    public static void main(String[] args) throws Exception {
        FcoinUtils fcoinUtils = new FcoinUtils();
        while (true) {
            fcoinUtils.ftusdt();
        }
    }
}
